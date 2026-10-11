package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeMetadata;
import minefantasy.mf2.integration.minetweaker.helpers.RecipeBuilderState;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptProcess;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptWarnings;
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
        ScriptWarnings.deprecated("TanningRack.add", "TanningRack.recipe(name, output, input).register()");
        ScriptProcess.add(MFRecipes.TANNING, name, () -> recipe(output, input, time, tier, tool), priority);
    }

    @ZenMethod
    public static void replace(@NotNull String id, @NotNull IItemStack output, @NotNull IIngredient input, float time,
            @Optional int tier, @Optional String tool, @Optional int priority) {
        ScriptWarnings.deprecated("TanningRack.replace", "TanningRack.recipe(name, output, input).replace(id)");
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

    /** Named recipe options; output precedes input for every station. */
    @ZenMethod
    public static Builder recipe(String name, IItemStack output, IIngredient input) {
        return new Builder(name, output, input);
    }

    @ZenClass("mods.minefantasy.TanningRackRecipeBuilder")
    public static final class Builder {

        private final IItemStack output;
        private final IIngredient input;
        private final RecipeBuilderState state;
        private Float time;
        private int tier;
        private String tool = "knife";

        private Builder(String name, IItemStack output, IIngredient input) {
            this.state = new RecipeBuilderState(name);
            this.output = output;
            this.input = input;
        }

        @ZenMethod
        public Builder time(float work) {
            this.time = work;
            return this;
        }

        @ZenMethod
        public Builder tool(String type, int tier) {
            this.tool = type;
            this.tier = tier;
            return this;
        }

        @ZenMethod
        public Builder priority(int priority) {
            state.priority(priority);
            return this;
        }

        private ProcessRecipe build() {
            state.claim();
            if (time == null) {
                throw new IllegalArgumentException("give time(work)");
            }
            return TanningRack.recipe(output, input, time, tier, tool);
        }

        @ZenMethod
        public void register() {
            state.register(MFRecipes.TANNING, this::build);
        }

        @ZenMethod
        public void replace(String id) {
            state.replace(MFRecipes.TANNING, id, this::build);
        }

        @ZenMethod
        public void replaceAccepting(IItemStack stack, @Optional int expected) {
            state.replaceAccepting(MFRecipes.TANNING, stack, this::build, expected);
        }
    }

    private static ProcessRecipe recipe(IItemStack output, IIngredient input, float time, int tier, String tool) {
        return ProcessRecipe.of(
                ScriptInputs.toInput(input),
                ScriptInputs.toOutput(output),
                RecipeMetadata.builder().put(MFRecipeKeys.TIME, time).put(MFRecipeKeys.TOOL_TIER, tier)
                        .put(MFRecipeKeys.TOOL, tool == null || tool.isEmpty() ? "knife" : tool).build());
    }
}
