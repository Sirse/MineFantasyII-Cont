package minefantasy.mf2.api.crafting.refine;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;

/**
 * Native paint oil recipes; see {@link MFRecipes#PAINT_OIL}. Blocks are stored as their items; an output with the
 * wildcard metadata keeps the painted block's metadata.
 */
public final class PaintOilRecipe {

    private PaintOilRecipe() {}

    /** Turn one specific meta block to another. */
    public static RecipeEntry<ProcessRecipe> addRecipe(Block input, int inMeta, Block output, int outMeta) {
        ItemStack in = new ItemStack(Item.getItemFromBlock(input), 1, inMeta);
        return NativeRecipes.addNative(MFRecipes.PAINT_OIL, in, recipe(input, inMeta, output, outMeta));
    }

    /** Turn a block to a specific meta. */
    public static RecipeEntry<ProcessRecipe> addRecipe(Block input, Block output, int meta) {
        return addRecipe(input, OreDictionary.WILDCARD_VALUE, output, meta);
    }

    /** Turn a block to a block without a meta change. */
    public static RecipeEntry<ProcessRecipe> addRecipe(Block input, Block output) {
        return addRecipe(input, OreDictionary.WILDCARD_VALUE, output, OreDictionary.WILDCARD_VALUE);
    }

    public static ProcessRecipe recipe(Block input, int inMeta, Block output, int outMeta) {
        return ProcessRecipe.of(
                Input.of(Item.getItemFromBlock(input), inMeta),
                new ItemStack(Item.getItemFromBlock(output), 1, outMeta));
    }

    /** The painted form of a block (as an item stack), or null. */
    public static ItemStack getPaintResult(ItemStack block) {
        RecipeEntry<ProcessRecipe> entry = MFRecipes.find(MFRecipes.PAINT_OIL, block);
        return entry == null ? null : entry.getRecipe().getOutput();
    }
}
