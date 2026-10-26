package minefantasy.mf2.block.basic;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import minefantasy.mf2.api.helpers.Heading;
import minefantasy.mf2.api.helpers.Tiles;

/** A block with a tile of its own: finds that tile, and may turn to face whoever placed it. */
public abstract class BlockTiled<T extends TileEntity> extends BlockContainer {

    private final Class<T> type;

    protected BlockTiled(Material material, Class<T> type) {
        super(material);
        this.type = type;
    }

    /** The block's tile at the position, or null when there is none or another block's. */
    protected T getTile(IBlockAccess world, int x, int y, int z) {
        return Tiles.get(world, x, y, z, type);
    }

    /** Whether the block turns to face the player who places it. */
    protected boolean facesPlacer() {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack item) {
        if (facesPlacer()) {
            int direction = Heading.of(placer);
            world.setBlockMetadataWithNotify(x, y, z, direction, 2);
        }
        super.onBlockPlacedBy(world, x, y, z, placer, item);
    }

    @Override
    public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
        return facesPlacer() && turnHeading(world, x, y, z, axis);
    }

    @Override
    public ForgeDirection[] getValidRotations(World world, int x, int y, int z) {
        return facesPlacer() ? HEADING_ROTATIONS : super.getValidRotations(world, x, y, z);
    }

    /** The axes a block facing a {@link Heading} in its metadata turns about. */
    public static final ForgeDirection[] HEADING_ROTATIONS = { ForgeDirection.UP, ForgeDirection.DOWN };

    /**
     * Turns a block whose two lowest metadata bits hold a {@link Heading} a quarter about the axis, clockwise seen from
     * above for UP, keeping its other bits and its tile. False for any other axis.
     */
    public static boolean turnHeading(World world, int x, int y, int z, ForgeDirection axis) {
        int turn;
        if (axis == ForgeDirection.UP) {
            turn = 1;
        } else if (axis == ForgeDirection.DOWN) {
            turn = 3;
        } else {
            return false;
        }
        int meta = world.getBlockMetadata(x, y, z);
        return world.setBlockMetadataWithNotify(x, y, z, (meta & ~3) | ((meta + turn) & 3), 3);
    }
}
