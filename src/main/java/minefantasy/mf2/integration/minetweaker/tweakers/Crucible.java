package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.api.refine.AlloyRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedAlloyRecipe;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.mc1710.item.MCItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.Crucible")
public class Crucible {

    private static final String STATION = "alloy";

    /**
     * Adds {@code crafttweaker:alloy/<name>} and, for {@code dupe} above 1, its ratio copies {@code <name>_x2}... with
     * every ingredient and the output multiplied.
     */
    @ZenMethod
    public static void add(@NotNull String name, @NotNull IItemStack out, int level, int dupe,
            @NotNull IIngredient[] ingred, @Optional int priority) {
        RecipeId id = ScriptRecipes.scriptId(STATION, name);
        ScriptRecipes.apply("Adding alloy " + id, tx -> {
            List<IIngredient> ingredients = new ArrayList<IIngredient>();
            Collections.addAll(ingredients, ingred);
            ItemStack output = ScriptInputs.toOutput(out);
            Alloy[] ratios = AlloyRecipes.ratioAlloys(output, level, ingredients, Math.max(1, dupe));
            if (ratios.length == 0) {
                throw new IllegalArgumentException("Too many ingredients for the crucible");
            }
            Alloy[] alloys = new Alloy[ratios.length];
            for (int i = 0; i < ratios.length; i++) {
                alloys[i] = new TweakedAlloyRecipe(
                        MineTweakerMC.getIItemStack(ratios[i].getRecipeOutput()),
                        level,
                        AlloyRecipes.createDupeList(ingredients, i + 1));
            }
            AlloyRecipes.stageRatio(tx, id, alloys, priority);
        });
    }

    /** Removes one alloy by id; ratio copies have their own ids ({@code _x2}...). */
    @ZenMethod
    public static void remove(@NotNull String id) {
        RecipeId recipeId = ScriptRecipes.parseId(STATION, id);
        ScriptRecipes.apply("Removing alloy " + recipeId, tx -> tx.remove(MFRecipes.ALLOY, recipeId));
    }

    /** Removes every alloy with a matching output (and ingredient, if given), ratio copies included. */
    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional IIngredient input) {
        ScriptRecipes.apply("Removing alloys for " + output, tx -> {
            List<RecipeId> removed = tx.removeWhere(MFRecipes.ALLOY, entry -> {
                Alloy alloy = entry.getRecipe();
                return alloy.getRecipeOutput() != null && output.matches(new MCItemStack(alloy.getRecipeOutput()))
                        && (input == null || matchesInput(alloy, input));
            });
            if (removed.isEmpty()) {
                MineTweakerAPI.logWarning("No alloys for " + output);
            } else {
                MineTweakerAPI.logInfo("Removed alloys " + removed);
            }
        });
    }

    private static boolean matchesInput(Alloy alloy, IIngredient input) {
        for (Object object : alloy.getIngredients()) {
            if (object instanceof IIngredient) {
                IIngredient ingredient = (IIngredient) object;
                for (IItemStack stack : ingredient.getItems()) {
                    if (input.matches(stack)) {
                        return true;
                    }
                }
            } else if (object instanceof IItemStack) {
                if (input.matches((IItemStack) object)) {
                    return true;
                }
            } else if (object instanceof ItemStack) {
                if (input.matches(new MCItemStack((ItemStack) object))) {
                    return true;
                }
            }
        }
        return false;
    }
}
