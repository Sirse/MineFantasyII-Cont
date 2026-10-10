package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.integration.minetweaker.helpers.GridBuilder;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.Anvil")
public class Anvil {

    private static final String STATION = "anvil";

    /** Adds {@code crafttweaker:anvil/<name>}; the grid is at most 6 wide and 4 high. */
    /** A shaped recipe built step by step; see {@link GridBuilder}. Nothing is added before register(). */
    @ZenMethod
    public static GridBuilder shaped(@NotNull String name, @NotNull IItemStack output) {
        return new GridBuilder(GridBuilder.Station.ANVIL, name, output, true);
    }

    /** A shapeless recipe built step by step; see {@link GridBuilder}. */
    @ZenMethod
    public static GridBuilder shapeless(@NotNull String name, @NotNull IItemStack output) {
        return new GridBuilder(GridBuilder.Station.ANVIL, name, output, false);
    }

    @ZenMethod
    public static void addShaped(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            boolean hot, String tool, int hammer, int anvil, int time, IIngredient[][] ingreds,
            @Optional int priority) {
        shaped(name, output).cells(ingreds).skill(skill).research(research).hot(hot).tool(tool, hammer)
                .stationTier(anvil).time(time).priority(priority).register();
    }

    @ZenMethod
    public static void addShapeless(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            boolean hot, String tool, int hammer, int anvil, int time, IIngredient[] ingreds, @Optional int priority) {
        shapeless(name, output).ingredients(ingreds).skill(skill).research(research).hot(hot).tool(tool, hammer)
                .stationTier(anvil).time(time).priority(priority).register();
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        RecipeId recipeId = ScriptRecipes.parseId(STATION, id);
        ScriptRecipes.apply("Removing anvil recipe " + recipeId, tx -> tx.remove(MFRecipes.ANVIL, recipeId));
    }

    /** Removes every recipe with a matching output; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional int expected) {
        CarpentersBench.removeByOutput(MFRecipes.ANVIL, output, expected);
    }

    /** Removes every recipe that would take the given stack; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeAccepting(@NotNull IItemStack input, @Optional int expected) {
        CarpentersBench.removeAccepting(MFRecipes.ANVIL, input, expected);
    }
}
