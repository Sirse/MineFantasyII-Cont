package minefantasy.mf2.api.helpers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Sounds played the same way from several places. */
public final class Sounds {

    private Sounds() {}

    /** Played from the middle of the tile's block. */
    public static void at(TileEntity tile, String sound, float volume, float pitch) {
        tile.getWorldObj()
                .playSoundEffect(tile.xCoord + 0.5D, tile.yCoord + 0.5D, tile.zCoord + 0.5D, sound, volume, pitch);
    }

    /** A soft splash of scooping water up. */
    public static void scoop(World world, EntityPlayer player) {
        world.playSoundAtEntity(
                player,
                "random.splash",
                0.125F + world.rand.nextFloat() / 4F,
                0.5F + world.rand.nextFloat());
    }

    /** Hot metal plunged into water. */
    public static void quench(EntityPlayer player) {
        player.playSound("random.splash", 1F, 1F);
        player.playSound("random.fizz", 2F, 0.5F);
    }
}
