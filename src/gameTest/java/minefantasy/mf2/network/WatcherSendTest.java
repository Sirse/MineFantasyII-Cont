package minefantasy.mf2.network;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.mojang.authlib.GameProfile;

/** A packet for a chunk's watchers reaches a player who has the chunk, and not one still waiting for it. */
@GameTestHolder("minefantasy2")
public class WatcherSendTest {

    private WatcherSendTest() {}

    @GameTest
    public static void onlyAPlayerWhoHasTheChunkGetsIt(GameTestHelper helper) {
        WorldServer world = (WorldServer) helper.getWorld();
        TestPos at = helper.absolute(1, 1, 1);
        EntityPlayerMP player = new EntityPlayerMP(
                MinecraftServer.getServer(),
                world,
                new GameProfile(UUID.randomUUID(), "mf2_watcher"),
                new ItemInWorldManager(world));
        final List<Packet> sent = new ArrayList<Packet>();
        new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), player) {

            @Override
            public void sendPacket(Packet packet) {
                sent.add(packet);
            }
        };
        player.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        world.playerEntities.add(player);
        world.getPlayerManager().addPlayer(player);
        try {
            Packet packet = new S03PacketTimeUpdate(0L, 0L, false);
            // Newly watching: the chunk itself is still queued for this player
            assertTrue(player.loadedChunks.contains(new ChunkCoordIntPair(at.x() >> 4, at.z() >> 4)));
            NetworkUtils.sendToWatchers(packet, world, at.x(), at.z());
            assertTrue("sent ahead of the chunk", sent.isEmpty());
            player.loadedChunks.clear();
            NetworkUtils.sendToWatchers(packet, world, at.x(), at.z());
            assertEquals(1, sent.size());
            // Far away nobody watches
            NetworkUtils.sendToWatchers(packet, world, at.x() + 100000, at.z());
            assertEquals(1, sent.size());
        } finally {
            world.getPlayerManager().removePlayer(player);
            world.playerEntities.remove(player);
        }
        helper.succeed();
    }
}
