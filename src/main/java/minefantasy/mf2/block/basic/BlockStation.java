package minefantasy.mf2.block.basic;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import minefantasy.mf2.block.tileentity.InventorySlots;
import minefantasy.mf2.block.tileentity.TileEntityStation;

/**
 * The block of a station: throws the station's contents out when it is broken.
 */
public abstract class BlockStation<T extends TileEntityStation> extends BlockTiled<T> {

    protected BlockStation(Material material, Class<T> type) {
        super(material, type);
    }

    /**
     * Whether the block is only being swapped for its lit or unlit twin: the station stays, contents and tile alike,
     * and breaking does nothing.
     */
    protected boolean keepsContents() {
        return false;
    }

    /** How many of the station's slots, from the first, are thrown out when it is broken; all by default. */
    protected int spilledSlots(T station) {
        return station.getSizeInventory();
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (keepsContents()) {
            return;
        }
        T station = getTile(world, x, y, z);
        if (station != null) {
            InventorySlots.spill(world, x, y, z, station, 0, spilledSlots(station));
            world.func_147453_f(x, y, z, block);
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
