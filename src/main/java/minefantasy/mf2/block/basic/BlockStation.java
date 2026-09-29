package minefantasy.mf2.block.basic;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import minefantasy.mf2.block.tileentity.InventorySlots;
import minefantasy.mf2.block.tileentity.TileEntityStation;

/**
 * The block of a station: finds its station, throws the station's contents out when it is broken, and may turn to face
 * whoever placed it.
 */
public abstract class BlockStation<T extends TileEntityStation> extends BlockContainer {

    private final Class<T> type;

    protected BlockStation(Material material, Class<T> type) {
        super(material);
        this.type = type;
    }

    /** The station at the position, or null when there is none or another block's. */
    protected T getTile(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return type.isInstance(tile) ? type.cast(tile) : null;
    }

    /** Whether the block turns to face the player who places it. */
    protected boolean facesPlacer() {
        return false;
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
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack item) {
        if (facesPlacer()) {
            int direction = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
            world.setBlockMetadataWithNotify(x, y, z, direction, 2);
        }
        super.onBlockPlacedBy(world, x, y, z, placer, item);
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
