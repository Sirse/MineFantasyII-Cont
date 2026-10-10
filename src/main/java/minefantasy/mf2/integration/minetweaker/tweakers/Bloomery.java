package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
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
 * <pre>
 * mods.minefantasy.Bloomery.add("steel_scrap", &lt;minefantasy2:bar&gt;, &lt;ore:scrapSteel&gt;);
 * mods.minefantasy.Bloomery.replace("minefantasy2:bloomery/minecraft.iron_ore", &lt;...&gt;, &lt;minecraft:iron_ore&gt;);
 * mods.minefantasy.Bloomery.remove("minefantasy2:bloomery/minecraft.gold_ore");
 * mods.minefantasy.Bloomery.removeByOutput(&lt;minefantasy2:bar&gt;);
 * </pre>
 */
@ZenClass("mods.minefantasy.Bloomery")
public class Bloomery {

    private static final String STATION = "bloomery";

    /** Adds {@code crafttweaker:bloomery/<name>}. */
    @ZenMethod
    public static void add(@NotNull String name, @NotNull IItemStack output, @NotNull IIngredient input,
            @Optional String research, @Optional int priority) {
        RecipeId id = ScriptRecipes.scriptId(STATION, name);
        ScriptRecipes.apply(
                "Adding bloomery recipe " + id,
                tx -> tx.add(MFRecipes.BLOOMERY, id, recipe(output, input, research), priority));
    }

    /** Replaces an existing recipe, keeping its position. */
    @ZenMethod
    public static void replace(@NotNull String id, @NotNull IItemStack output, @NotNull IIngredient input,
            @Optional String research, @Optional int priority) {
        RecipeId recipeId = ScriptRecipes.parseId(STATION, id);
        ScriptRecipes.apply(
                "Replacing bloomery recipe " + recipeId,
                tx -> tx.replace(MFRecipes.BLOOMERY, recipeId, recipe(output, input, research), priority));
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        RecipeId recipeId = ScriptRecipes.parseId(STATION, id);
        ScriptRecipes.apply("Removing bloomery recipe " + recipeId, tx -> tx.remove(MFRecipes.BLOOMERY, recipeId));
    }

    /** Removes every recipe with a matching output; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional int expected) {
        ScriptRecipes.removeWhere(
                MFRecipes.BLOOMERY,
                "for " + output,
                recipe -> TweakedIngredients.names(output, recipe.getOutput()),
                expected);
    }

    /** Removes every recipe that would take the given stack; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeAccepting(@NotNull IItemStack input, @Optional int expected) {
        ScriptRecipes.removeWhere(
                MFRecipes.BLOOMERY,
                "taking " + input,
                recipe -> TweakedIngredients.takes(recipe.getInput(), input),
                expected);
    }

    private static BloomRecipe recipe(IItemStack output, IIngredient input, String research) {
        return BloomRecipe.of(ScriptInputs.toInput(input), ScriptInputs.toOutput(output), research);
    }
}
