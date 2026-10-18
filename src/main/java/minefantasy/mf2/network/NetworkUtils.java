package minefantasy.mf2.network;

import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class NetworkUtils {

    /**
     * After a right click on a block changed slots other than the held one. Vanilla checks the inventory after the use
     * with isChangingQuantityOnly set, which drops every change but the held slot's, so the client shows the old
     * contents until something else resends them. Checking here, during the use, sends them.
     */
    public static void afterBlockUse(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) player.inventoryContainer.detectAndSendChanges();
    }

    /** Sends to the players watching the chunk; a world that is not a server's (a client, a test) has none. */
    public static void sendToWatchers(Packet packet, World world, int worldX, int worldZ) {
        if (world instanceof WorldServer) {
            sendToWatchers(packet, (WorldServer) world, worldX, worldZ);
        }
    }

    /**
     * Sends to the players watching the chunk, leaving out those still waiting for the chunk itself: a tile packet
     * ahead of its chunk would be lost under it. The same players vanilla's own chunk watcher sends to.
     */
    public static void sendToWatchers(Packet packet, WorldServer world, int worldX, int worldZ) {
        sendToWatchers(() -> packet, world, worldX, worldZ);
    }

    /** As {@link #sendToWatchers(Packet, World, int, int)}, building the packet only once someone watches. */
    public static void sendToWatchers(Supplier<? extends Packet> packet, World world, int worldX, int worldZ) {
        if (world instanceof WorldServer) {
            sendToWatchers(packet, (WorldServer) world, worldX, worldZ);
        }
    }

    private static void sendToWatchers(Supplier<? extends Packet> build, WorldServer world, int worldX, int worldZ) {
        PlayerManager watchers = world.getPlayerManager();
        Packet packet = null;
        for (Object player : world.playerEntities) {
            if (player instanceof EntityPlayerMP
                    && watchers.isPlayerWatchingChunk((EntityPlayerMP) player, worldX >> 4, worldZ >> 4)) {
                if (packet == null) packet = build.get();
                ((EntityPlayerMP) player).playerNetServerHandler.sendPacket(packet);
            }
        }
    }
}
