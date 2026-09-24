package minefantasy.mf2.api.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * One registration action, possibly spanning several registries (a special forging recipe and its salvage entry).
 * Operations are validated against the working layer plus the earlier operations of the same transaction, and applied
 * only by {@link #commit()}. A failed operation poisons the transaction, and closing it without a commit discards
 * everything, so no half of an action ever reaches the working layer.
 */
public final class RecipeTransaction implements AutoCloseable {

    private final RecipeRegistries owner;
    private final RecipeSource source;
    /** Per registry: id → pending entry, or {@code null} for a pending removal. */
    private final Map<RecipeRegistry<?>, Map<RecipeId, RecipeEntry<?>>> pending = new IdentityHashMap<>();
    private final List<Runnable> apply = new ArrayList<>();
    private boolean failed;
    private boolean done;

    RecipeTransaction(RecipeRegistries owner, RecipeSource source) {
        this.owner = owner;
        this.source = source;
    }

    public RecipeSource getSource() {
        return source;
    }

    public <R> void add(RecipeRegistry<R> registry, RecipeId id, R recipe, int priority) {
        guard(registry);
        checkRecipe(registry, id, recipe);
        if (exists(registry, id)) {
            fail("Recipe " + id + " already exists; use replace to change it");
        }
        stage(registry, id, new RecipeEntry<>(id, recipe, source, priority, -1), () -> {
            RecipeEntry<R> created = new RecipeEntry<>(id, recipe, source, priority, registry.takeOrder());
            registry.working().put(id, created);
        });
    }

    public <R> void replace(RecipeRegistry<R> registry, RecipeId id, R recipe, int priority) {
        guard(registry);
        checkRecipe(registry, id, recipe);
        if (!exists(registry, id)) {
            fail("Recipe " + id + " does not exist; use add to create it");
        }
        stage(registry, id, new RecipeEntry<>(id, recipe, source, priority, -1), () -> {
            RecipeEntry<R> old = registry.working().get(id);
            registry.working().put(id, new RecipeEntry<>(id, recipe, source, priority, old.getOrder()));
        });
    }

    /**
     * Stages an add, or a replace keeping the position when the id is taken, as this transaction sees it: its own
     * earlier adds count as taken and its own removals as free.
     */
    public <R> void set(RecipeRegistry<R> registry, RecipeId id, R recipe, int priority) {
        guard(registry);
        if (exists(registry, id)) {
            replace(registry, id, recipe, priority);
        } else {
            add(registry, id, recipe, priority);
        }
    }

    public <R> void remove(RecipeRegistry<R> registry, RecipeId id) {
        guard(registry);
        if (!exists(registry, id)) {
            fail("Recipe " + id + " does not exist");
        }
        pendingOf(registry).put(id, null);
        apply.add(() -> registry.working().remove(id));
    }

    /** Removes every recipe the filter accepts, in the state this transaction sees; returns the ids removed. */
    public <R> List<RecipeId> removeWhere(RecipeRegistry<R> registry, Predicate<? super RecipeEntry<R>> filter) {
        guard(registry);
        Map<RecipeId, RecipeEntry<R>> view = new LinkedHashMap<>(registry.working());
        // The filter judges the state this transaction would leave: staged adds and replaces included
        for (Map.Entry<RecipeId, RecipeEntry<?>> change : pendingOf(registry).entrySet()) {
            if (change.getValue() == null) {
                view.remove(change.getKey());
            } else {
                @SuppressWarnings("unchecked")
                RecipeEntry<R> staged = (RecipeEntry<R>) change.getValue();
                view.put(change.getKey(), staged);
            }
        }
        List<RecipeId> removed = new ArrayList<>();
        for (RecipeEntry<R> entry : view.values()) {
            if (filter.test(entry)) {
                removed.add(entry.getId());
            }
        }
        for (RecipeId id : removed) {
            remove(registry, id);
        }
        return Collections.unmodifiableList(removed);
    }

    /** Applies every staged operation. Throws if an operation failed or the transaction is finished. */
    public void commit() {
        if (done) {
            throw new IllegalStateException("Transaction already finished");
        }
        if (failed) {
            throw new RecipeRegistrationException("Cannot commit a transaction with a failed operation");
        }
        done = true;
        for (Runnable op : apply) {
            op.run();
        }
    }

    /** Discards the staged operations unless {@link #commit()} ran. */
    @Override
    public void close() {
        done = true;
        pending.clear();
        apply.clear();
    }

    private void guard(RecipeRegistry<?> registry) {
        if (done) {
            throw new IllegalStateException("Transaction already finished");
        }
        if (failed) {
            throw new RecipeRegistrationException("Transaction already failed");
        }
        owner.checkOwned(registry);
        try {
            registry.working();
        } catch (RecipeRegistrationException e) {
            failed = true;
            throw e;
        }
    }

    private void checkRecipe(RecipeRegistry<?> registry, RecipeId id, Object recipe) {
        if (id == null || recipe == null) {
            fail("Recipe and id must not be null");
        }
        if (!id.getStation().equals(registry.getStation())) {
            fail("Recipe " + id + " belongs to station " + id.getStation() + ", not " + registry.getStation());
        }
        String problem = registry.problemWith(recipe);
        if (problem != null) {
            fail("Recipe " + id + " (" + source + ") refused by " + registry.getStation() + ": " + problem);
        }
    }

    private boolean exists(RecipeRegistry<?> registry, RecipeId id) {
        Map<RecipeId, RecipeEntry<?>> own = pending.get(registry);
        if (own != null && own.containsKey(id)) {
            return own.get(id) != null;
        }
        return registry.working().containsKey(id);
    }

    private void stage(RecipeRegistry<?> registry, RecipeId id, RecipeEntry<?> staged, Runnable op) {
        // The staged entry (its order is assigned on commit) marks the id as present for later operations.
        pendingOf(registry).put(id, staged);
        apply.add(op);
    }

    private Map<RecipeId, RecipeEntry<?>> pendingOf(RecipeRegistry<?> registry) {
        return pending.computeIfAbsent(registry, r -> new HashMap<>());
    }

    private void fail(String message) {
        failed = true;
        throw new RecipeRegistrationException(message);
    }
}
