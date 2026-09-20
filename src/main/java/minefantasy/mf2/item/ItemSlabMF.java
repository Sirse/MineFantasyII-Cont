package minefantasy.mf2.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemSlab;

import minefantasy.mf2.block.basic.BlockSlabMF;

/**
 * ItemBlock for {@link BlockSlabMF}.
 * <p>
 * GameRegistry.registerBlock builds the constructor signature from getClass() on each extra argument and then asks for
 * an exact match, so passing the slab pair through it looked for ItemSlab(Block, BlockSlabMF, BlockSlabMF, BlockSlabMF,
 * Boolean) and failed at load. Taking a single Block keeps the reflective lookup to the one signature Forge always
 * prepends, and the pair is read off the block itself.
 */
public class ItemSlabMF extends ItemSlab {

    public ItemSlabMF(Block block) {
        super(block, (BlockSlabMF) block, ((BlockSlabMF) block).getDoubleSlab(), false);
    }
}
