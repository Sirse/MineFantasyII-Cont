package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedIngredients;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * Cooking recipes. One that can burn also gets its burn stage, {@code <id>_burnt}, turning the output into burnt food;
 * add, replace and remove handle both together.
 */
@ZenClass("mods.minefantasy.Cooking")
public class Cooking {

    private static final String STATION = "cooking";

    @ZenMethod
    public static void add(@NotNull String name, @NotNull IItemStack output, @NotNull IIngredient input, int minTemp,
            int maxTemp, int time, int burnTime, boolean requireBaking, @Optional boolean canBurn,
            @Optional int priority) {
        RecipeId id = ScriptRecipes.scriptId(STATION, name);
        ScriptRecipes.apply(
                "Adding cooking recipe " + id,
                tx -> recipe(output, input, minTemp, maxTemp, time, burnTime, requireBaking, canBurn)
                        .addTo(tx, id, priority));
    }

    /** Replaces a recipe; its old burn stage goes, and the new one comes if the new recipe can burn. */
    @ZenMethod
    public static void replace(@NotNull String id, @NotNull IItemStack output, @NotNull IIngredient input, int minTemp,
            int maxTemp, int time, int burnTime, boolean requireBaking, @Optional boolean canBurn,
            @Optional int priority) {
        RecipeId recipeId = ScriptRecipes.parseId(STATION, id);
        ScriptRecipes.apply("Replacing cooking recipe " + recipeId, tx -> {
            CookRecipe recipe = recipe(output, input, minTemp, maxTemp, time, burnTime, requireBaking, canBurn);
            RecipeId burnt = CookRecipe.burntId(recipeId);
            tx.replace(MFRecipes.COOKING, recipeId, recipe, priority);
            if (MFRecipes.COOKING.containsWorking(burnt)) {
                tx.remove(MFRecipes.COOKING, burnt);
            }
            CookRecipe burnStage = recipe.burnStage();
            if (burnStage != null) {
                tx.add(MFRecipes.COOKING, burnt, burnStage, priority);
            }
        });
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        RecipeId recipeId = ScriptRecipes.parseId(STATION, id);
        ScriptRecipes.apply("Removing cooking recipe " + recipeId, tx -> CookRecipe.removeFrom(tx, recipeId));
    }

    /** Removes the recipes making the output and their burn stages; with {@code expected}, only if that many. */
    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional int expected) {
        removeWhere("for " + output, recipe -> TweakedIngredients.names(output, recipe.getOutput()), expected);
    }

    /** Removes every recipe that would take the given stack; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeAccepting(@NotNull IItemStack input, @Optional int expected) {
        removeWhere("taking " + input, recipe -> TweakedIngredients.takes(recipe.getInput(), input), expected);
    }

    private static void removeWhere(String what, java.util.function.Predicate<CookRecipe> filter, int expected) {
        ScriptRecipes.removeWhere(
                MFRecipes.COOKING,
                what,
                entry -> !entry.getId().getPath().endsWith(CookRecipe.BURNT_SUFFIX) && filter.test(entry.getRecipe()),
                expected,
                (tx, removed) -> {
                    for (RecipeId id : removed) {
                        RecipeId burnt = CookRecipe.burntId(id);
                        if (MFRecipes.COOKING.containsWorking(burnt)) {
                            tx.remove(MFRecipes.COOKING, burnt);
                        }
                    }
                });
    }

    private static CookRecipe recipe(IItemStack output, IIngredient input, int minTemp, int maxTemp, int time,
            int burnTime, boolean requireBaking, boolean canBurn) {
        return CookRecipe.builder(ScriptInputs.toInput(input), ScriptInputs.toOutput(output))
                .temperature(minTemp, maxTemp).time(time).burnTime(burnTime).oven(requireBaking).canBurn(canBurn)
                .build();
    }
}
