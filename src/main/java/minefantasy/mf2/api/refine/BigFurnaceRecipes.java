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

    /** The input may be an item, block, stack (its main material counts) or ore name. */
    public static RecipeEntry<ProcessRecipe> addRecipe(Object input, ItemStack output, int tier) {
        return NativeRecipes.addNative(MFRecipes.BIG_FURNACE, input, recipe(input, output, tier));
    }

    public static ProcessRecipe recipe(Object input, ItemStack output, int tier) {
        return ProcessRecipe
                .of(NativeRecipes.input(input), output, RecipeMetadata.builder().put(MFRecipeKeys.TIER, tier).build());
    }
}
