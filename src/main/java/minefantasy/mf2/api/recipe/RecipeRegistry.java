package minefantasy.mf2.api.recipe;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Recipes of one station. Writes go through {@link RecipeTransaction}s into the working layer (the base layer while
 * mods load, a draft during a script reload); reads go to the published {@link RecipeSnapshot}.
 */
public final class RecipeRegistry<R> {

    /** Index keys of a recipe: the item (or ore name, ...) of its main input. Empty means "check always". */
    @FunctionalInterface
    public interface Indexer<R> {

        Collection<?> keys(R recipe);
    }

    /** Refuses a recipe the station cannot honour, by throwing IllegalArgumentException with the reason. */
    @FunctionalInterface
    public interface Validator<R> {

        void check(R recipe);
    }

    private final RecipeRegistries owner;
    private final String station;
    private final Indexer<R> indexer;
    private final Validator<R> validator;

    private final Map<RecipeId, RecipeEntry<R>> base = new LinkedHashMap<>();
    private long baseNextOrder;
    private Map<RecipeId, RecipeEntry<R>> draft;
    private long draftNextOrder;

    RecipeRegistry(RecipeRegistries owner, String station, Indexer<R> indexer, Validator<R> validator) {
        this.owner = owner;
        this.station = station;
        this.indexer = indexer;
        this.validator = validator;
    }

    /** The reason the station cannot honour the recipe, or null when it can. */
    @SuppressWarnings("unchecked")
    String problemWith(Object recipe) {
        try {
            if (recipe instanceof RecipeChecks.Validated) {
                ((RecipeChecks.Validated) recipe).validate();
            }
            if (validator != null) {
                validator.check((R) recipe);
            }
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    public String getStation() {
        return station;
    }

    /** The published recipes. Never null; empty until the first publication. */
    public RecipeSnapshot<R> published() {
        return owner.published(this);
    }

    /** Whether the layer being written (base while mods load, the draft during a reload) has the id. */
    public boolean containsWorking(RecipeId id) {
        return working().containsKey(id);
    }

    /**
     * The id a recipe object was registered under by mods (not scripts), or null. Holders of a native recipe object,
     * such as knowledge book pages, use it to find what is published under that id now.
     */
    public RecipeId idOf(R nativeRecipe) {
        for (RecipeEntry<R> entry : base.values()) {
            if (entry.getRecipe() == nativeRecipe) {
                return entry.getId();
            }
        }
        return null;
    }

    /**
     * The published recipe under the id the native recipe was registered with: the script's replacement if one replaced
     * it, null if it was removed, the recipe itself otherwise. Recipes not registered natively come back as they are.
     */
    public R current(R nativeRecipe) {
        RecipeId id = idOf(nativeRecipe);
        if (id == null) {
            return nativeRecipe;
        }
        RecipeEntry<R> entry = published().get(id);
        return entry == null ? null : entry.getRecipe();
    }

    /** The entry in the layer being written, or null. */
    public RecipeEntry<R> workingEntry(RecipeId id) {
        return working().get(id);
    }

    // region single-operation shortcuts

    public RecipeEntry<R> add(RecipeId id, R recipe, RecipeSource source) {
        return add(id, recipe, source, 0);
    }

    public RecipeEntry<R> add(RecipeId id, R recipe, RecipeSource source, int priority) {
        try (RecipeTransaction tx = owner.begin(source)) {
            tx.add(this, id, recipe, priority);
            tx.commit();
        }
        return working().get(id);
    }

    public RecipeEntry<R> replace(RecipeId id, R recipe, RecipeSource source, int priority) {
        try (RecipeTransaction tx = owner.begin(source)) {
            tx.replace(this, id, recipe, priority);
            tx.commit();
        }
        return working().get(id);
    }

    public void remove(RecipeId id) {
        try (RecipeTransaction tx = owner.begin(RecipeSource.NATIVE)) {
            tx.remove(this, id);
            tx.commit();
        }
    }

    public List<RecipeId> removeWhere(Predicate<? super RecipeEntry<R>> filter) {
        try (RecipeTransaction tx = owner.begin(RecipeSource.NATIVE)) {
            List<RecipeId> removed = tx.removeWhere(this, filter);
            tx.commit();
            return removed;
        }
    }

    // endregion

    // region layers, driven by RecipeRegistries and RecipeTransaction

    Map<RecipeId, RecipeEntry<R>> working() {
        switch (owner.getState()) {
            case LOADING:
                return base;
            case RELOADING:
                return draft;
            default:
                throw new RecipeRegistrationException(
                        "Recipes for " + station + " can only change while mods load or scripts reload");
        }
    }

    long takeOrder() {
        return owner.getState() == RecipeRegistries.State.RELOADING ? draftNextOrder++ : baseNextOrder++;
    }

    void openDraft() {
        draft = new LinkedHashMap<>(base);
        draftNextOrder = baseNextOrder;
    }

    RecipeSnapshot<R> snapshot(long generation) {
        Map<RecipeId, RecipeEntry<R>> source = draft != null ? draft : base;
        return new RecipeSnapshot<>(generation, source.values(), indexer);
    }

    void closeDraft() {
        draft = null;
    }

    // endregion

    @Override
    public String toString() {
        return "RecipeRegistry[" + station + "]";
    }
}
