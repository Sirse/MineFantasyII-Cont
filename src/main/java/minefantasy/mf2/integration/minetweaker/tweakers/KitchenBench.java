package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.config.ConfigKitchen;
import minefantasy.mf2.integration.minetweaker.helpers.GridBuilder;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.KitchenBench")
public class KitchenBench {

    /**
     * Adds {@code crafttweaker:kitchen/<name>} (or {@code carpenter/<name>} when the kitchen bench is disabled and food
     * is made on the carpenter's bench); the grid is at most 4 by 4. A dirty amount of 0 uses the bench's default.
     */
    /** A shaped recipe built step by step; see {@link GridBuilder}. Nothing is added before register(). */
    @ZenMethod
    public static GridBuilder shaped(@NotNull String name, @NotNull IItemStack output) {
        return new GridBuilder(GridBuilder.Station.KITCHEN, name, output, true);
    }

    /** A shapeless recipe built step by step; see {@link GridBuilder}. */
    @ZenMethod
    public static GridBuilder shapeless(@NotNull String name, @NotNull IItemStack output) {
        return new GridBuilder(GridBuilder.Station.KITCHEN, name, output, false);
    }

    /** A dirty amount of 0 uses the bench's default. */
    @ZenMethod
    public static void addShaped(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            String sound, String tool, int time, float dirtyAmount, IIngredient[][] ingreds, @Optional int priority) {
        dirt(shaped(name, output).cells(ingreds), dirtyAmount).skill(skill).research(research).sound(sound)
                .tool(tool, -1).time(time).priority(priority).register();
    }

    @ZenMethod
    public static void addShapeless(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            String sound, String tool, int time, float dirtyAmount, IIngredient[] ingreds, @Optional int priority) {
        dirt(shapeless(name, output).ingredients(ingreds), dirtyAmount).skill(skill).research(research).sound(sound)
                .tool(tool, -1).time(time).priority(priority).register();
    }

    private static GridBuilder dirt(GridBuilder builder, float dirtyAmount) {
        // 0 is the bench's default; anything else, a negative included, is the script's own
        return dirtyAmount != 0 ? builder.dirt(dirtyAmount) : builder;
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        CarpentersBench.removeById(target(), id);
    }

    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional int expected) {
        CarpentersBench.removeByOutput(target(), output, expected);
    }

    /** Removes every recipe that would take the given stack; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeAccepting(@NotNull IItemStack input, @Optional int expected) {
        CarpentersBench.removeAccepting(target(), input, expected);
    }

    private static RecipeRegistry<GridRecipe> target() {
        return ConfigKitchen.enableBench ? MFRecipes.KITCHEN : MFRecipes.CARPENTER;
    }
}
