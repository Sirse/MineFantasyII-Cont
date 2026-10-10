package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistrationException;
import minefantasy.mf2.api.recipe.RecipeRegistries;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.api.recipe.RecipeSource;
import minefantasy.mf2.api.recipe.RecipeTransaction;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;

/**
 * Bridges CraftTweaker's reload cycle to the recipe registries. A reload opens drafts, script actions write them in one
 * transaction each, and the drafts are published once all scripts ran. Actions never need an undo: the next reload
 * starts again from the native recipes.
 */
public final class ScriptRecipes {

    /** The named skill, none for an empty name; an unknown name stops the recipe. */
    public static Skill skill(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Skill skill = RPGElements.getSkillByName(name);
        if (skill == null) {
            throw new IllegalArgumentException("unknown skill " + name);
        }
        return skill;
    }

    public static final String NAMESPACE = "crafttweaker";
    /** How every refused script action ends its error: a transaction applies whole or not at all. */
    public static final String NOTHING_CHANGED = "Nothing changed.";
    public static final RecipeSource SOURCE = RecipeSource.script("");

    private ScriptRecipes() {}

    public static void hookReloads() {
        MineTweakerImplementationAPI.onReloadEvent(event -> MFRecipes.REGISTRIES.beginReload());
        MineTweakerImplementationAPI.onPostReload(event -> {
            if (MFRecipes.REGISTRIES.getState() == RecipeRegistries.State.RELOADING) {
                MFRecipes.REGISTRIES.publish();
            }
        });
    }

    /** A script's own recipe: {@code crafttweaker:station/name}. */
    public static RecipeId scriptId(String station, String name) {
        return RecipeId.of(NAMESPACE, station + "/" + name);
    }

    /** A full id (for replacing or removing someone else's recipe), or a bare name for a script recipe. */
    public static RecipeId parseId(String station, String id) {
        return id.indexOf(':') >= 0 ? RecipeId.parse(id) : scriptId(station, id);
    }

    /**
     * Removes every recipe the filter picks, logging their ids. With {@code expected} above 0 the count must match, or
     * nothing is removed: a script meaning to drop one recipe that finds ten stops instead.
     */
    public static <R> void removeWhere(RecipeRegistry<R> registry, String what, Predicate<R> filter, int expected) {
        removeWhere(registry, what, entry -> filter.test(entry.getRecipe()), expected, (tx, removed) -> {});
    }

    /** As above, judging whole entries, with {@code after} removing what goes with the recipes removed. */
    public static <R> void removeWhere(RecipeRegistry<R> registry, String what, Predicate<RecipeEntry<R>> filter,
            int expected, BiConsumer<RecipeTransaction, List<RecipeId>> after) {
        String station = registry.getStation();
        apply("Removing " + station + " recipes " + what, tx -> {
            List<RecipeId> removed = tx.removeWhere(registry, filter);
            if (expected > 0 && removed.size() != expected) {
                throw new IllegalArgumentException(
                        "expected " + expected
                                + " recipes, found "
                                + removed.size()
                                + " "
                                + removed
                                + "; nothing removed");
            }
            after.accept(tx, removed);
            if (removed.isEmpty()) {
                MineTweakerAPI.logWarning("No " + station + " recipes " + what);
            } else {
                MineTweakerAPI.logInfo("Removed " + station + " recipes " + removed);
            }
        });
    }

    /** Applies the body as one transaction; errors go to the CraftTweaker log and leave nothing behind. */
    public static void apply(String description, Consumer<RecipeTransaction> body) {
        MineTweakerAPI.apply(new Action(description, body));
    }

    private static final class Action implements IUndoableAction {

        private final String description;
        private final Consumer<RecipeTransaction> body;

        Action(String description, Consumer<RecipeTransaction> body) {
            this.description = description;
            this.body = body;
        }

        @Override
        public void apply() {
            if (MFRecipes.REGISTRIES.getState() != RecipeRegistries.State.RELOADING) {
                MineTweakerAPI.logError(description + ": recipes can only change while scripts load");
                return;
            }
            try (RecipeTransaction tx = MFRecipes.REGISTRIES.begin(SOURCE)) {
                body.accept(tx);
                tx.commit();
            } catch (RecipeRegistrationException | IllegalArgumentException e) {
                MineTweakerAPI.logError(description + ": " + e.getMessage() + ". " + NOTHING_CHANGED);
            }
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        /** Nothing to do: the next reload rebuilds the drafts from the native recipes. */
        @Override
        public void undo() {}

        @Override
        public String describe() {
            return description;
        }

        @Override
        public String describeUndo() {
            return "Dropping: " + description;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
