package minefantasy.mf2.api.crafting.refine;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeMetadata;

/** Native quern recipes; see {@link MFRecipes#QUERN}. */
public final class QuernRecipes {

    private QuernRecipes() {}

    /** A native recipe grinding the input, an item, block, stack or ore name, into the output. */
    public static Builder recipe(Object input, ItemStack output) {
        return new Builder(input, output);
    }

    /** A native quern recipe: any quern, and the pot used up, unless said otherwise. */
    public static final class Builder {

        private final Object input;
        private final ItemStack output;
        private int tier;
        private boolean consumePot = true;

        private Builder(Object input, ItemStack output) {
            this.input = input;
            this.output = output;
        }

        /** The quern tier it needs. */
        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        /** Grinding leaves the pot. */
        public Builder keepPot() {
            this.consumePot = false;
            return this;
        }

        public RecipeEntry<ProcessRecipe> register() {
            return NativeRecipes.addNative(MFRecipes.QUERN, input, recipe(input, output, tier, consumePot));
        }
    }

    private static ProcessRecipe recipe(Object input, ItemStack output, int tier, boolean consumePot) {
        return ProcessRecipe.of(
                NativeRecipes.input(input),
                output,
                RecipeMetadata.builder().put(MFRecipeKeys.TIER, tier).put(MFRecipeKeys.CONSUME_POT, consumePot)
                        .build());
    }
}
