package minefantasy.mf2.gametest;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.mojang.authlib.GameProfile;

/** Real server players for tests, not fake ones. */
public final class Players {

    private Players() {}

    /** A server player with no connection, as automation has; its inventory starts empty. */
    public static EntityPlayerMP unconnected(GameTestHelper helper, String name) {
        WorldServer world = (WorldServer) helper.getWorld();
        EntityPlayerMP player = new EntityPlayerMP(
                MinecraftServer.getServer(),
                world,
                new GameProfile(UUID.randomUUID(), name),
                new ItemInWorldManager(world));
        player.inventory.clearInventory(null, -1);
        return player;
    }

    /**
     * A connected survival player, so vanilla's paths that need a connection run. What the server sends it goes to the
     * sink, or nowhere when the sink is null.
     */
    public static EntityPlayerMP connected(GameTestHelper helper, String name, Consumer<Packet> sink) {
        EntityPlayerMP player = unconnected(helper, name);
        boolean[] ready = { false };
        new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), player) {

            @Override
            public void sendPacket(Packet packet) {
                if (ready[0] && sink != null) sink.accept(packet);
            }
        };
        player.theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
        // What setting it up sent is not the test's
        ready[0] = true;
        return player;
    }
}
