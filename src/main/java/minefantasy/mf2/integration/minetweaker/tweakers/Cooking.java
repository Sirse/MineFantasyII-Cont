package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
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

    /** Removes the recipes making the output (with a matching input, if given) and their burn stages. */
    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional IIngredient input) {
        ScriptRecipes.apply("Removing cooking recipes for " + output, tx -> {
            List<RecipeId> removed = tx.removeWhere(
                    MFRecipes.COOKING,
                    entry -> !entry.getId().getPath().endsWith(CookRecipe.BURNT_SUFFIX)
                            && output.matches(MineTweakerMC.getIItemStack(entry.getRecipe().getOutput()))
                            && inputMatches(entry.getRecipe(), input));
            for (RecipeId id : removed) {
                RecipeId burnt = CookRecipe.burntId(id);
                if (MFRecipes.COOKING.containsWorking(burnt)) {
                    tx.remove(MFRecipes.COOKING, burnt);
                }
            }
            if (removed.isEmpty()) {
                MineTweakerAPI.logWarning("No cooking recipes for " + output);
            } else {
                MineTweakerAPI.logInfo("Removed cooking recipes " + removed);
            }
        });
    }

    private static boolean inputMatches(CookRecipe recipe, IIngredient input) {
        if (input == null) {
            return true;
        }
        for (ItemStack example : recipe.getInput().examples()) {
            if (input.matches(MineTweakerMC.getIItemStack(example))) {
                return true;
            }
        }
        return false;
    }

    private static CookRecipe recipe(IItemStack output, IIngredient input, int minTemp, int maxTemp, int time,
            int burnTime, boolean requireBaking, boolean canBurn) {
        return CookRecipe.builder(ScriptInputs.toInput(input), ScriptInputs.toOutput(output))
                .temperature(minTemp, maxTemp).time(time).burnTime(burnTime).oven(requireBaking).canBurn(canBurn)
                .build();
    }
}
