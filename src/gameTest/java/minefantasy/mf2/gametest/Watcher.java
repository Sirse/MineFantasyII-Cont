package minefantasy.mf2.gametest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.world.WorldServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;

import io.netty.buffer.Unpooled;

/**
 * A connected survival player watching the test area: what the server tells its client about blocks is kept, so a test
 * can ask what that client would show. Close it in a finally block.
 */
public final class Watcher implements AutoCloseable {

    public final EntityPlayerMP player;
    private final WorldServer world;
    private final Map<String, Block> shown = new HashMap<String, Block>();
    /** Every packet sent to this client, in order. */
    public final List<Packet> sent = new ArrayList<Packet>();

    public Watcher(GameTestHelper helper, double x, double y, double z) {
        this(helper, x, y, z, true);
    }

    /**
     * A watcher whose chunk stays queued: block sends wait for the chunk, as they do for a player still loading it.
     */
    public static Watcher stillLoading(GameTestHelper helper, double x, double y, double z) {
        return new Watcher(helper, x, y, z, false);
    }

    private Watcher(GameTestHelper helper, double x, double y, double z, boolean chunkShownAtOnce) {
        world = (WorldServer) helper.getWorld();
        player = Players.connected(helper, "mf2_watcher", this::see);
        player.setPosition(x, y, z);
        world.playerEntities.add(player);
        world.getPlayerManager().addPlayer(player);
        // Chunks already loaded around the test: block changes are sent at once, not with a queued chunk
        if (chunkShownAtOnce) player.loadedChunks.clear();
    }

    /** Sends the block changes the server gathered this tick, as it does at the end of each tick. */
    public void flush() {
        world.getPlayerManager().updatePlayerInstances();
    }

    /** The block last told to this client at a position, or null when it was never told about it. */
    public Block shown(int x, int y, int z) {
        return shown.get(key(x, y, z));
    }

    public void forget() {
        shown.clear();
    }

    @Override
    public void close() {
        world.getPlayerManager().removePlayer(player);
        world.playerEntities.remove(player);
    }

    private void see(Packet packet) {
        sent.add(packet);
        if (!(packet instanceof S23PacketBlockChange) && !(packet instanceof S22PacketMultiBlockChange)) return;
        PacketBuffer data = new PacketBuffer(Unpooled.buffer());
        try {
            packet.writePacketData(data);
        } catch (IOException exception) {
            throw new AssertionError("could not read a block change", exception);
        }
        if (packet instanceof S23PacketBlockChange) {
            int x = data.readInt();
            int y = data.readUnsignedByte();
            int z = data.readInt();
            shown.put(key(x, y, z), Block.getBlockById(data.readVarIntFromBuffer()));
            return;
        }
        int chunkX = data.readInt();
        int chunkZ = data.readInt();
        int count = data.readShort() & 0xFFFF;
        data.readInt();
        for (int i = 0; i < count; i++) {
            int at = data.readShort();
            int block = data.readShort() & 0xFFFF;
            shown.put(
                    key((chunkX << 4) + (at >> 12 & 15), at & 255, (chunkZ << 4) + (at >> 8 & 15)),
                    Block.getBlockById(block >> 4));
        }
    }

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
}
