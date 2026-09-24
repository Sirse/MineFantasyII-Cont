package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.function.Consumer;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistrationException;
import minefantasy.mf2.api.recipe.RecipeRegistries;
import minefantasy.mf2.api.recipe.RecipeSource;
import minefantasy.mf2.api.recipe.RecipeTransaction;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;

/**
 * Bridges CraftTweaker's reload cycle to the recipe registries. A reload opens drafts, script actions write them in one
 * transaction each, and the drafts are published once all scripts ran. Actions never need an undo: the next reload
 * starts again from the native recipes.
 */
public final class ScriptRecipes {

    public static final String NAMESPACE = "crafttweaker";
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
                MineTweakerAPI.logError(description + ": " + e.getMessage());
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
