package minefantasy.mf2.api.crafting.tanning;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeMetadata;

/** Native tanning recipes; see {@link MFRecipes#TANNING}. */
public final class TanningRecipe {

    private TanningRecipe() {}

    public static RecipeEntry<ProcessRecipe> addRecipe(Object input, float time, ItemStack output) {
        return addRecipe(input, time, -1, output);
    }

    public static RecipeEntry<ProcessRecipe> addRecipe(Object input, float time, int tier, ItemStack output) {
        return addRecipe(input, time, tier, "knife", output);
    }

    /** The input may be an item, block, stack or ore name. */
    public static RecipeEntry<ProcessRecipe> addRecipe(Object input, float time, int tier, String toolType,
            ItemStack output) {
        return NativeRecipes.addNative(MFRecipes.TANNING, input, recipe(input, time, tier, toolType, output));
    }

    public static ProcessRecipe recipe(Object input, float time, int tier, String toolType, ItemStack output) {
        return ProcessRecipe.of(
                NativeRecipes.input(input),
                output,
                RecipeMetadata.builder().put(MFRecipeKeys.TIME, time).put(MFRecipeKeys.TIER, tier)
                        .put(MFRecipeKeys.TOOL, toolType).build());
    }
}
