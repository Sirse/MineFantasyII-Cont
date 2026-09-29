package minefantasy.mf2.block.basic;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import minefantasy.mf2.api.helpers.Heading;

/** A block with a tile of its own: finds that tile, and may turn to face whoever placed it. */
public abstract class BlockTiled<T extends TileEntity> extends BlockContainer {

    private final Class<T> type;

    protected BlockTiled(Material material, Class<T> type) {
        super(material);
        this.type = type;
    }

    /** The block's tile at the position, or null when there is none or another block's. */
    protected T getTile(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return type.isInstance(tile) ? type.cast(tile) : null;
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
}
