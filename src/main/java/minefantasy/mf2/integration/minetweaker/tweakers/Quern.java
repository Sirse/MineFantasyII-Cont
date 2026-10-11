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

/** Quern recipes; the quern itself is tier 0. */
@ZenClass("mods.minefantasy.Quern")
public class Quern {

    @ZenMethod
    public static void add(@NotNull String name, @NotNull IItemStack output, @NotNull IIngredient input,
            @Optional int tier, @Optional boolean consumePot, @Optional int priority) {
        ScriptWarnings.deprecated("Quern.add", "Quern.recipe(name, output, input).register()");
        ScriptProcess.add(MFRecipes.QUERN, name, () -> recipe(output, input, tier, consumePot), priority);
    }

    @ZenMethod
    public static void replace(@NotNull String id, @NotNull IItemStack output, @NotNull IIngredient input,
            @Optional int tier, @Optional boolean consumePot, @Optional int priority) {
        ScriptWarnings.deprecated("Quern.replace", "Quern.recipe(name, output, input).replace(id)");
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

    /** Named recipe options; output precedes input for every station. */
    @ZenMethod
    public static Builder recipe(String name, IItemStack output, IIngredient input) {
        return new Builder(name, output, input);
    }

    @ZenClass("mods.minefantasy.QuernRecipeBuilder")
    public static final class Builder {

        private final String name;
        private final IItemStack output;
        private final IIngredient input;
        private int priority;
        private boolean registered;
        private int tier;
        private boolean consumePot;

        private Builder(String name, IItemStack output, IIngredient input) {
            this.name = name;
            this.output = output;
            this.input = input;
        }

        @ZenMethod
        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        @ZenMethod
        public Builder consumePot() {
            this.consumePot = true;
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
            return Quern.recipe(output, input, tier, consumePot);
        }

        @ZenMethod
        public void register() {
            ScriptProcess.add(MFRecipes.QUERN, name, this::build, priority);
        }

        @ZenMethod
        public void replace(String id) {
            ScriptProcess.replace(MFRecipes.QUERN, id, this::build, priority);
        }

        @ZenMethod
        public void replaceAccepting(IItemStack stack, @Optional int expected) {
            ScriptProcess.replaceAccepting(MFRecipes.QUERN, stack, this::build, priority, expected);
        }
    }

    private static ProcessRecipe recipe(IItemStack output, IIngredient input, int tier, boolean consumePot) {
        return ProcessRecipe.of(
                ScriptInputs.toInput(input),
                ScriptInputs.toOutput(output),
                RecipeMetadata.builder().put(MFRecipeKeys.TIER, tier).put(MFRecipeKeys.CONSUME_POT, consumePot)
                        .build());
    }
}
