package minefantasy.mf2.api.tool;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** A tool that gives extra drops for a block, paid only once the player really broke it. */
public interface IMiningDrops {

    /**
     * Called on the server after a confirmed break, with the block as it was and the tool as it was held before it
     * wore. The block is already gone.
     */
    void giveMiningDrops(World world, int x, int y, int z, Block block, int meta, EntityPlayer player, ItemStack held);
}
