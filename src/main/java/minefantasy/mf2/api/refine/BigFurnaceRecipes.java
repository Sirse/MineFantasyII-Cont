package minefantasy.mf2.api.refine;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeMetadata;

/** Native big furnace recipes; see {@link MFRecipes#BIG_FURNACE}. */
public final class BigFurnaceRecipes {

    private BigFurnaceRecipes() {}

    /**
     * A native recipe smelting the input, an item, block, stack (its main material counts) or ore name, into the
     * output.
     */
    public static Builder recipe(Object input, ItemStack output) {
        return new Builder(input, output);
    }

    /** A native big furnace recipe: any furnace unless a tier is given. */
    public static final class Builder {

        private final Object input;
        private final ItemStack output;
        private int tier;

        private Builder(Object input, ItemStack output) {
            this.input = input;
            this.output = output;
        }

        /** The furnace tier it needs. */
        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        public RecipeEntry<ProcessRecipe> register() {
            return NativeRecipes.addNative(MFRecipes.BIG_FURNACE, input, recipe(input, output, tier));
        }
    }

    private static ProcessRecipe recipe(Object input, ItemStack output, int tier) {
        return ProcessRecipe
                .of(NativeRecipes.input(input), output, RecipeMetadata.builder().put(MFRecipeKeys.TIER, tier).build());
    }
}
