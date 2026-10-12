package minefantasy.mf2.block.basic;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
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

    /** Set while a station's block is swapped, when breaking it must leave the station be. */
    private static boolean swapping;

    /**
     * Swaps a station's block, for its lit or unlit twin or for an upgrade, keeping its metadata and the station,
     * contents and tile alike. Does nothing when that block is already there.
     */
    protected static void swapBlock(World world, int x, int y, int z, Block twin) {
        if (twin == null || world.getBlock(x, y, z) == twin) return;
        int meta = world.getBlockMetadata(x, y, z);
        TileEntity station = world.getTileEntity(x, y, z);
        swapping = true;
        try {
            world.setBlock(x, y, z, twin, meta, 3);
        } finally {
            swapping = false;
        }
        if (station != null) {
            station.validate();
            world.setTileEntity(x, y, z, station);
            // It now stands on another block: forget the one it read before
            station.updateContainingBlockInfo();
        }
    }

    /** How many of the station's slots, from the first, are thrown out when it is broken; all by default. */
    protected int spilledSlots(T station) {
        return station.getSizeInventory();
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (swapping) {
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
