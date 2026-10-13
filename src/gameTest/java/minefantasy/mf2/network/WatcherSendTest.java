package minefantasy.mf2.network;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.function.Supplier;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.gametest.Watcher;

/** A packet for a chunk's watchers reaches a player who has the chunk, and not one still waiting for it. */
@GameTestHolder("minefantasy2")
public class WatcherSendTest {

    private WatcherSendTest() {}

    @GameTest
    public static void onlyAPlayerWhoHasTheChunkGetsIt(GameTestHelper helper) {
        WorldServer world = (WorldServer) helper.getWorld();
        TestPos at = helper.absolute(1, 1, 1);
        // The chunk stays queued: the watcher is still loading it, as a fresh player is
        try (Watcher watcher = Watcher.stillLoading(helper, at.x() + 0.5D, at.y(), at.z() + 0.5D)) {
            Packet packet = new S03PacketTimeUpdate(0L, 0L, false);
            assertTrue(watcher.player.loadedChunks.contains(new ChunkCoordIntPair(at.x() >> 4, at.z() >> 4)));
            NetworkUtils.sendToWatchers(packet, world, at.x(), at.z());
            assertTrue("sent ahead of the chunk", watcher.sent.isEmpty());
            watcher.player.loadedChunks.clear();
            NetworkUtils.sendToWatchers(packet, world, at.x(), at.z());
            assertEquals(1, watcher.sent.size());
            // Far away nobody watches
            NetworkUtils.sendToWatchers(packet, world, at.x() + 100000, at.z());
            assertEquals(1, watcher.sent.size());
        }
        helper.succeed();
    }

    @GameTest
    public static void aPacketIsBuiltOnlyForSomeoneAndOnce(GameTestHelper helper) {
        WorldServer world = (WorldServer) helper.getWorld();
        TestPos at = helper.absolute(1, 1, 1);
        int[] built = new int[1];
        Supplier<Packet> packet = () -> {
            built[0]++;
            return new S03PacketTimeUpdate(0L, 0L, false);
        };
        try (Watcher watcher = new Watcher(helper, at.x() + 0.5D, at.y(), at.z() + 0.5D)) {
            NetworkUtils.sendToWatchers(packet, world, at.x() + 100000, at.z());
            assertEquals("built for nobody", 0, built[0]);
            NetworkUtils.sendToWatchers(packet, world, at.x(), at.z());
            assertEquals("built", 1, built[0]);
            assertEquals("sent", 1, watcher.sent.size());
        }
        helper.succeed();
    }
}
