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

    /** The input may be an item, block, stack or ore name. */
    public static RecipeEntry<ProcessRecipe> addRecipe(Object input, ItemStack output, int tier, boolean consumePot) {
        return NativeRecipes.addNative(MFRecipes.QUERN, input, recipe(input, output, tier, consumePot));
    }

    public static ProcessRecipe recipe(Object input, ItemStack output, int tier, boolean consumePot) {
        return ProcessRecipe.of(
                NativeRecipes.input(input),
                output,
                RecipeMetadata.builder().put(MFRecipeKeys.TIER, tier).put(MFRecipeKeys.CONSUME_POT, consumePot)
                        .build());
    }
}
