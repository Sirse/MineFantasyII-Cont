package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.function.Supplier;

import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;

/**
 * The script operations shared by every one-input station: add, replace, remove by id and remove by output. Recipes are
 * built inside the action, so a bad ingredient is reported and skipped like any other error.
 */
public final class ScriptProcess {

    private ScriptProcess() {}

    /** Adds {@code crafttweaker:station/<name>}. */
    public static void add(RecipeRegistry<ProcessRecipe> registry, String name, Supplier<ProcessRecipe> recipe,
            int priority) {
        RecipeId id = ScriptRecipes.scriptId(registry.getStation(), name);
        ScriptRecipes.apply(
                "Adding " + registry.getStation() + " recipe " + id,
                tx -> tx.add(registry, id, recipe.get(), priority));
    }

    /** Replaces a recipe by full id (or a script recipe by name), keeping its position. */
    public static void replace(RecipeRegistry<ProcessRecipe> registry, String id, Supplier<ProcessRecipe> recipe,
            int priority) {
        RecipeId recipeId = ScriptRecipes.parseId(registry.getStation(), id);
        ScriptRecipes.apply(
                "Replacing " + registry.getStation() + " recipe " + recipeId,
                tx -> tx.replace(registry, recipeId, recipe.get(), priority));
    }

    public static void remove(RecipeRegistry<ProcessRecipe> registry, String id) {
        RecipeId recipeId = ScriptRecipes.parseId(registry.getStation(), id);
        ScriptRecipes.apply(
                "Removing " + registry.getStation() + " recipe " + recipeId,
                tx -> tx.remove(registry, recipeId));
    }

    /** Removes every recipe with a matching output; logs the ids. */
    public static void removeByOutput(RecipeRegistry<ProcessRecipe> registry, IIngredient output, int expected) {
        ScriptRecipes.removeWhere(
                registry,
                "for " + output,
                recipe -> TweakedIngredients.names(output, recipe.getOutput()),
                expected);
    }

    /** Removes every recipe whose input would take the stack; logs the ids. */
    public static void removeAccepting(RecipeRegistry<ProcessRecipe> registry, IItemStack input, int expected) {
        ScriptRecipes.removeWhere(
                registry,
                "taking " + input,
                recipe -> TweakedIngredients.takes(recipe.getInput(), input),
                expected);
    }
}
