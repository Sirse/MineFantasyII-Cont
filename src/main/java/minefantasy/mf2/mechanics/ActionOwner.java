package minefantasy.mf2.mechanics;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.mojang.authlib.GameProfile;

/** Persistent owner identity for world mechanics that must act for a player who may be offline. */
public final class ActionOwner {

    private static final Map<WorldServer, Map<UUID, FakePlayer>> PROXIES = new IdentityHashMap<WorldServer, Map<UUID, FakePlayer>>();

    private String legacyName = "";
    private UUID playerId;

    public void set(EntityPlayer player) {
        legacyName = player == null ? "" : player.getCommandSenderName();
        playerId = player == null ? null : player.getUniqueID();
    }

    /**
     * The player acting at this block: the owner when online, otherwise a stand-in with the owner's profile, standing
     * at the block so that protection weighing where the actor stands sees the owner's own place. Null when no owner is
     * known.
     */
    public EntityPlayer resolve(World world, int x, int y, int z) {
        EntityPlayer player = resolve(world);
        if (player instanceof FakePlayer) player.setPosition(x + 0.5D, y, z + 0.5D);
        return player;
    }

    /**
     * The owner when online, in this world or another, otherwise a stand-in with the owner's profile; null when none is
     * known. An online owner always acts as themselves, so no stand-in shares their identity while they play.
     */
    public EntityPlayer resolve(World world) {
        if (world == null || world.isRemote) return null;
        if (playerId == null) {
            return legacyName.isEmpty() ? null : online(world, null, legacyName);
        }
        EntityPlayer online = online(world, playerId, null);
        if (online != null) return online;
        if (!(world instanceof WorldServer)) return null;
        return proxy((WorldServer) world, playerId, legacyName);
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString("Owner", legacyName == null ? "" : legacyName);
        if (playerId != null) nbt.setString("OwnerId", playerId.toString());
        else nbt.removeTag("OwnerId");
    }

    public void readFromNBT(NBTTagCompound nbt) {
        boolean hasOwnerId = nbt.hasKey("OwnerId");
        legacyName = nbt.hasKey("Owner", 8) ? nbt.getString("Owner") : "";
        playerId = null;
        if (hasOwnerId) {
            try {
                playerId = nbt.hasKey("OwnerId", 8) ? UUID.fromString(nbt.getString("OwnerId")) : null;
            } catch (IllegalArgumentException ignored) {
                // A malformed modern identity must not fall back to a possibly reused player name.
                legacyName = "";
            }
            if (playerId == null) legacyName = "";
        }
    }

    /** The player with this id, or else this name, in this world first and then anywhere on the server. */
    private static EntityPlayer online(World world, UUID id, String name) {
        EntityPlayer found = find(world.playerEntities, id, name);
        if (found != null) return found;
        MinecraftServer server = MinecraftServer.getServer();
        return server == null ? null : find(server.getConfigurationManager().playerEntityList, id, name);
    }

    private static EntityPlayer find(List<?> players, UUID id, String name) {
        for (Object value : players) {
            EntityPlayer player = (EntityPlayer) value;
            if (player instanceof FakePlayer) continue;
            if (id != null ? id.equals(player.getUniqueID()) : name.equals(player.getCommandSenderName()))
                return player;
        }
        return null;
    }

    public static FakePlayer proxy(WorldServer world, UUID playerId, String name) {
        if (world == null || playerId == null) return null;
        Map<UUID, FakePlayer> worldProxies = PROXIES.get(world);
        if (worldProxies == null) {
            worldProxies = new HashMap<UUID, FakePlayer>();
            PROXIES.put(world, worldProxies);
        }
        FakePlayer proxy = worldProxies.get(playerId);
        if (proxy == null) {
            proxy = new FakePlayer(world, new GameProfile(playerId, name));
            worldProxies.put(playerId, proxy);
        }
        return proxy;
    }

    public static void unload(WorldServer world) {
        PROXIES.remove(world);
    }
}
