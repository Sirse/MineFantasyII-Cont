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

/** Tanning rack recipes: work time, tool tier (0 for any) and tool type (default "knife"). */
@ZenClass("mods.minefantasy.TanningRack")
public class TanningRack {

    @ZenMethod
    public static void add(@NotNull String name, @NotNull IItemStack output, @NotNull IIngredient input, float time,
            @Optional int tier, @Optional String tool, @Optional int priority) {
        ScriptProcess.add(MFRecipes.TANNING, name, () -> recipe(output, input, time, tier, tool), priority);
    }

    @ZenMethod
    public static void replace(@NotNull String id, @NotNull IItemStack output, @NotNull IIngredient input, float time,
            @Optional int tier, @Optional String tool, @Optional int priority) {
        ScriptProcess.replace(MFRecipes.TANNING, id, () -> recipe(output, input, time, tier, tool), priority);
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        ScriptProcess.remove(MFRecipes.TANNING, id);
    }

    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional int expected) {
        ScriptProcess.removeByOutput(MFRecipes.TANNING, output, expected);
    }

    /** Removes every recipe that would take the given stack; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeAccepting(@NotNull IItemStack input, @Optional int expected) {
        ScriptProcess.removeAccepting(MFRecipes.TANNING, input, expected);
    }

    private static ProcessRecipe recipe(IItemStack output, IIngredient input, float time, int tier, String tool) {
        return ProcessRecipe.of(
                ScriptInputs.toInput(input),
                ScriptInputs.toOutput(output),
                RecipeMetadata.builder().put(MFRecipeKeys.TIME, time).put(MFRecipeKeys.TOOL_TIER, tier)
                        .put(MFRecipeKeys.TOOL, tool == null || tool.isEmpty() ? "knife" : tool).build());
    }
}
