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

/** Quern recipes; the quern itself is tier 0. */
@ZenClass("mods.minefantasy.Quern")
public class Quern {

    @ZenMethod
    public static void add(@NotNull String name, @NotNull IItemStack output, @NotNull IIngredient input,
            @Optional int tier, @Optional boolean consumePot, @Optional int priority) {
        ScriptProcess.add(MFRecipes.QUERN, name, () -> recipe(output, input, tier, consumePot), priority);
    }

    @ZenMethod
    public static void replace(@NotNull String id, @NotNull IItemStack output, @NotNull IIngredient input,
            @Optional int tier, @Optional boolean consumePot, @Optional int priority) {
        ScriptProcess.replace(MFRecipes.QUERN, id, () -> recipe(output, input, tier, consumePot), priority);
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        ScriptProcess.remove(MFRecipes.QUERN, id);
    }

    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional int expected) {
        ScriptProcess.removeByOutput(MFRecipes.QUERN, output, expected);
    }

    /** Removes every recipe that would take the given stack; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeAccepting(@NotNull IItemStack input, @Optional int expected) {
        ScriptProcess.removeAccepting(MFRecipes.QUERN, input, expected);
    }

    private static ProcessRecipe recipe(IItemStack output, IIngredient input, int tier, boolean consumePot) {
        return ProcessRecipe.of(
                ScriptInputs.toInput(input),
                ScriptInputs.toOutput(output),
                RecipeMetadata.builder().put(MFRecipeKeys.TIER, tier).put(MFRecipeKeys.CONSUME_POT, consumePot)
                        .build());
    }
}
