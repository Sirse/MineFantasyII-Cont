package minefantasy.mf2.network;

import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.relauncher.ReflectionHelper;
import io.netty.buffer.ByteBuf;

/**
 * Here I used code in Railcraft.
 * <p>
 * https://github.com/Railcraft/Railcraft/blob/mc-1.7.10/src/main/java/mods/railcraft/common/util/network/PacketDispatcher.java
 */

public class NetworkUtils {

    private static final Class playerInstanceClass;
    private static final Method getOrCreateChunkWatcher;
    private static final Method sendToAllPlayersWatchingChunk;

    static {
        try {
            playerInstanceClass = PlayerManager.class.getDeclaredClasses()[0];
            getOrCreateChunkWatcher = ReflectionHelper.findMethod(
                    PlayerManager.class,
                    null,
                    new String[] { "func_72690_a", "getOrCreateChunkWatcher" },
                    int.class,
                    int.class,
                    boolean.class);
            sendToAllPlayersWatchingChunk = ReflectionHelper.findMethod(
                    playerInstanceClass,
                    null,
                    new String[] { "func_151251_a", "sendToAllPlayersWatchingChunk" },
                    Packet.class);
            getOrCreateChunkWatcher.setAccessible(true);
            sendToAllPlayersWatchingChunk.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void sendToWatchers(Packet packet, WorldServer world, int worldX, int worldZ) {
        try {
            Object playerInstance = getOrCreateChunkWatcher
                    .invoke(world.getPlayerManager(), worldX >> 4, worldZ >> 4, false);
            if (playerInstance != null) sendToAllPlayersWatchingChunk.invoke(playerInstance, packet);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void writeCoords(ByteBuf buf, int x, int y, int z) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    public static int[] readCoords(ByteBuf buf) {
        return new int[] { buf.readInt(), buf.readInt(), buf.readInt() };
    }

    public static boolean isServer(EntityPlayer player) {
        return player != null && !player.worldObj.isRemote;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getTile(World world, int[] coords, Class<T> clazz) {
        if (world == null || coords == null || coords.length < 3) return null;
        if (!world.blockExists(coords[0], coords[1], coords[2])) return null;
        TileEntity te = world.getTileEntity(coords[0], coords[1], coords[2]);
        if (clazz.isInstance(te)) return (T) te;
        return null;
    }

    public static boolean isWithinDistanceSq(EntityPlayer player, int[] coords, double maxDistanceSq) {
        if (player == null || coords == null || coords.length < 3) return false;
        double dx = player.posX - (coords[0] + 0.5);
        double dy = player.posY - (coords[1] + 0.5);
        double dz = player.posZ - (coords[2] + 0.5);
        return (dx * dx + dy * dy + dz * dz) <= maxDistanceSq;
    }
}
