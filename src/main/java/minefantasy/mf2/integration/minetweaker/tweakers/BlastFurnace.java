package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptProcess;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** Blast furnace recipes: one input item per chamber turns into the output. */
@ZenClass("mods.minefantasy.BlastFurnace")
public class BlastFurnace {

    @ZenMethod
    public static void add(@NotNull String name, @NotNull IItemStack output, @NotNull IIngredient input,
            @Optional int priority) {
        ScriptProcess.add(MFRecipes.BLAST_FURNACE, name, () -> recipe(output, input), priority);
    }

    @ZenMethod
    public static void replace(@NotNull String id, @NotNull IItemStack output, @NotNull IIngredient input,
            @Optional int priority) {
        ScriptProcess.replace(MFRecipes.BLAST_FURNACE, id, () -> recipe(output, input), priority);
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        ScriptProcess.remove(MFRecipes.BLAST_FURNACE, id);
    }

    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional IIngredient input) {
        ScriptProcess.removeByOutput(MFRecipes.BLAST_FURNACE, output, input);
    }

    private static ProcessRecipe recipe(IItemStack output, IIngredient input) {
        return ProcessRecipe.of(ScriptInputs.toInput(input), ScriptInputs.toOutput(output));
    }
}
