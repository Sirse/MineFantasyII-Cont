package minefantasy.mf2.mechanics;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
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

    public EntityPlayer resolve(World world) {
        if (world == null || world.isRemote) return null;
        if (playerId == null) {
            return legacyName.isEmpty() ? null : world.getPlayerEntityByName(legacyName);
        }
        for (Object value : world.playerEntities) {
            EntityPlayer player = (EntityPlayer) value;
            if (playerId.equals(player.getUniqueID())) return player;
        }
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
