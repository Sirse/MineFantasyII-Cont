package minefantasy.mf2.integration.minetweaker.tweakers;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptProcess;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptWarnings;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * Paint oil turns one block into another. Both sides must be blocks; an output with any metadata ({@code :*}) keeps the
 * painted block's metadata.
 */
@ZenClass("mods.minefantasy.PaintOil")
public class PaintOil {

    @ZenMethod
    public static void add(@NotNull String name, @NotNull IItemStack input, @NotNull IItemStack output,
            @Optional int priority) {
        ScriptWarnings.deprecated("PaintOil.add", "PaintOil.recipe(name, output, input).register()");
        ScriptProcess.add(MFRecipes.PAINT_OIL, name, () -> recipe(input, output), priority);
    }

    @ZenMethod
    public static void replace(@NotNull String id, @NotNull IItemStack input, @NotNull IItemStack output,
            @Optional int priority) {
        ScriptWarnings.deprecated("PaintOil.replace", "PaintOil.recipe(name, output, input).replace(id)");
        ScriptProcess.replace(MFRecipes.PAINT_OIL, id, () -> recipe(input, output), priority);
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        ScriptProcess.remove(MFRecipes.PAINT_OIL, id);
    }

    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional int expected) {
        ScriptProcess.removeByOutput(MFRecipes.PAINT_OIL, output, expected);
    }

    /** Removes every recipe that would take the given stack; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeAccepting(@NotNull IItemStack input, @Optional int expected) {
        ScriptProcess.removeAccepting(MFRecipes.PAINT_OIL, input, expected);
    }

    /** Named recipe options; output precedes input for every station. */
    @ZenMethod
    public static Builder recipe(String name, IItemStack output, IItemStack input) {
        return new Builder(name, output, input);
    }

    @ZenClass("mods.minefantasy.PaintOilRecipeBuilder")
    public static final class Builder {

        private final String name;
        private final IItemStack output;
        private final IItemStack input;
        private int priority;
        private boolean registered;

        private Builder(String name, IItemStack output, IItemStack input) {
            this.name = name;
            this.output = output;
            this.input = input;
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
            return PaintOil.recipe(input, output);
        }

        @ZenMethod
        public void register() {
            ScriptProcess.add(MFRecipes.PAINT_OIL, name, this::build, priority);
        }

        @ZenMethod
        public void replace(String id) {
            ScriptProcess.replace(MFRecipes.PAINT_OIL, id, this::build, priority);
        }

        @ZenMethod
        public void replaceAccepting(IItemStack stack, @Optional int expected) {
            ScriptProcess.replaceAccepting(MFRecipes.PAINT_OIL, stack, this::build, priority, expected);
        }
    }

    private static ProcessRecipe recipe(IItemStack input, IItemStack output) {
        ItemStack in = ScriptInputs.toOutput(input);
        ItemStack out = ScriptInputs.toOutput(output);
        if (!(in.getItem() instanceof ItemBlock) || !(out.getItem() instanceof ItemBlock)) {
            throw new IllegalArgumentException("Paint oil recipes need blocks on both sides");
        }
        return ProcessRecipe.of(Input.of(in.getItem(), in.getItemDamage()), out);
    }
}
