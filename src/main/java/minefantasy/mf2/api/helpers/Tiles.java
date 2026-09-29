package minefantasy.mf2.api.helpers;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

/** Tiles looked up in the world by what they are: a tile of another kind, or none, counts as absent. */
public final class Tiles {

    private Tiles() {}

    /** The tile at the spot if it is of the given kind, null otherwise. */
    public static <T> T get(IBlockAccess world, int x, int y, int z, Class<T> type) {
        if (world == null) {
            return null;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        return type.isInstance(tile) ? type.cast(tile) : null;
    }

    /** Whether the tile at the spot is of the given kind. */
    public static boolean is(IBlockAccess world, int x, int y, int z, Class<?> type) {
        return get(world, x, y, z, type) != null;
    }
}
