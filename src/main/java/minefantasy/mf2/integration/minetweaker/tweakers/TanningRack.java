package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeMetadata;
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

        private final String name;
        private final IItemStack output;
        private final IIngredient input;
        private int priority;
        private boolean registered;
        private Float time;
        private int tier;
        private String tool = "knife";

        private Builder(String name, IItemStack output, IIngredient input) {
            this.name = name;
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
            this.priority = priority;
            return this;
        }

        private ProcessRecipe build() {
            if (registered) {
                throw new IllegalArgumentException("builder is already registered");
            }
            registered = true;
            if (time == null) {
                throw new IllegalArgumentException("give time(work)");
            }
            return TanningRack.recipe(output, input, time, tier, tool);
        }

        @ZenMethod
        public void register() {
            ScriptProcess.add(MFRecipes.TANNING, name, this::build, priority);
        }

        @ZenMethod
        public void replace(String id) {
            ScriptProcess.replace(MFRecipes.TANNING, id, this::build, priority);
        }

        @ZenMethod
        public void replaceAccepting(IItemStack stack, @Optional int expected) {
            ScriptProcess.replaceAccepting(MFRecipes.TANNING, stack, this::build, priority, expected);
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
