package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeMetadata;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptProcess;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** Big furnace recipes with the furnace tier they need. */
@ZenClass("mods.minefantasy.BigFurnace")
public class BigFurnace {

    @ZenMethod
    public static void add(@NotNull String name, @NotNull IItemStack output, @NotNull IIngredient input,
            @Optional int tier, @Optional int priority) {
        ScriptProcess.add(MFRecipes.BIG_FURNACE, name, () -> recipe(output, input, tier), priority);
    }

    @ZenMethod
    public static void replace(@NotNull String id, @NotNull IItemStack output, @NotNull IIngredient input,
            @Optional int tier, @Optional int priority) {
        ScriptProcess.replace(MFRecipes.BIG_FURNACE, id, () -> recipe(output, input, tier), priority);
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        ScriptProcess.remove(MFRecipes.BIG_FURNACE, id);
    }

    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional IIngredient input) {
        ScriptProcess.removeByOutput(MFRecipes.BIG_FURNACE, output, input);
    }

    private static ProcessRecipe recipe(IItemStack output, IIngredient input, int tier) {
        return ProcessRecipe.of(
                ScriptInputs.toInput(input),
                ScriptInputs.toOutput(output),
                RecipeMetadata.builder().put(MFRecipeKeys.TIER, tier).build());
    }
}
