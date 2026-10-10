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
import minefantasy.mf2.integration.minetweaker.helpers.ScriptWarnings;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedAlloyRecipe;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedIngredients;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
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
        for (IIngredient ingredient : ingred) {
            ScriptWarnings.inputTag(ingredient);
        }
        ScriptRecipes.apply("Adding alloy " + id, tx -> {
            for (int i = 0; i < ingred.length; i++) {
                try {
                    TweakedIngredients.requireNoTransformers(ingred[i]);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("ingredient " + (i + 1) + ": " + e.getMessage(), e);
                }
            }
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

    /** Removes every alloy with a matching output, ratio copies included; with {@code expected}, only if that many. */
    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional int expected) {
        ScriptRecipes.removeWhere(
                MFRecipes.ALLOY,
                "for " + output,
                alloy -> alloy.getRecipeOutput() != null && TweakedIngredients.names(output, alloy.getRecipeOutput()),
                expected);
    }

    /** Removes every recipe that would take the given stack; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeAccepting(@NotNull IItemStack input, @Optional int expected) {
        ScriptRecipes.removeWhere(
                MFRecipes.ALLOY,
                "taking " + input,
                alloy -> alloy.takes(MineTweakerMC.getItemStack(input)),
                expected);
    }
}
