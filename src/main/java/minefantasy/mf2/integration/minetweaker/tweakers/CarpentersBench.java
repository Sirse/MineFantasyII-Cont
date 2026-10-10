package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.integration.minetweaker.helpers.GridBuilder;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedIngredients;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.CarpenterBench")
public class CarpentersBench {

    /** Adds {@code crafttweaker:carpenter/<name>}; the grid is at most 4 by 4. */
    /** A shaped recipe built step by step; see {@link GridBuilder}. Nothing is added before register(). */
    @ZenMethod
    public static GridBuilder shaped(@NotNull String name, @NotNull IItemStack output) {
        return new GridBuilder(GridBuilder.Station.CARPENTER, name, output, true);
    }

    /** A shapeless recipe built step by step; see {@link GridBuilder}. */
    @ZenMethod
    public static GridBuilder shapeless(@NotNull String name, @NotNull IItemStack output) {
        return new GridBuilder(GridBuilder.Station.CARPENTER, name, output, false);
    }

    @ZenMethod
    public static void addShaped(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            String sound, double exp, String tool, int hammer, int anvil, int time, IIngredient[][] ingreds,
            @Optional int priority) {
        shaped(name, output).cells(ingreds).skill(skill).research(research).sound(sound).experience(exp)
                .tool(tool, hammer).stationTier(anvil).time(time).priority(priority).register();
    }

    @ZenMethod
    public static void addShapeless(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            String sound, double exp, String tool, int hammer, int anvil, int time, IIngredient[] ingreds,
            @Optional int priority) {
        shapeless(name, output).ingredients(ingreds).skill(skill).research(research).sound(sound).experience(exp)
                .tool(tool, hammer).stationTier(anvil).time(time).priority(priority).register();
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        removeById(MFRecipes.CARPENTER, id);
    }

    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional int expected) {
        removeByOutput(MFRecipes.CARPENTER, output, expected);
    }

    /** Removes every recipe that would take the given stack; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeAccepting(@NotNull IItemStack input, @Optional int expected) {
        removeAccepting(MFRecipes.CARPENTER, input, expected);
    }

    static void removeById(RecipeRegistry<GridRecipe> registry, String id) {
        RecipeId recipeId = ScriptRecipes.parseId(registry.getStation(), id);
        ScriptRecipes.apply(
                "Removing " + registry.getStation() + " recipe " + recipeId,
                tx -> tx.remove(registry, recipeId));
    }

    static void removeByOutput(RecipeRegistry<GridRecipe> registry, IIngredient output, int expected) {
        ScriptRecipes.removeWhere(
                registry,
                "for " + output,
                recipe -> recipe.getRecipeOutput() != null
                        && TweakedIngredients.names(output, recipe.getRecipeOutput()),
                expected);
    }

    static void removeAccepting(RecipeRegistry<GridRecipe> registry, IItemStack input, int expected) {
        ScriptRecipes.removeWhere(
                registry,
                "taking " + input,
                recipe -> recipe.takes(MineTweakerMC.getItemStack(input)),
                expected);
    }

}
