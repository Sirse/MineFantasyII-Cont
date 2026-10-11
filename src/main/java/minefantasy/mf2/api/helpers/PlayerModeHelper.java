package minefantasy.mf2.api.helpers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.FakePlayer;

import minefantasy.mf2.MineFantasyII;

/**
 * Recognises the spectator gamemode, which Et Futurum Requiem adds to 1.7.10 as a "spectator"
 * {@link WorldSettings.GameType} constant registered through EnumHelper while mods load. Found by the constant's name,
 * so Et Futurum stays an optional dependency: without it, nobody is a spectator.
 *
 * A spectator is neither survival nor creative (isCreativeMode is false, and no creative exemption applies): an
 * invulnerable, editing-forbidden, no-clip observer who may open inventories to look, but never act. Every MineFantasy
 * action a client can request and every effect laid on a player's body checks here.
 */
public class PlayerModeHelper {

    /** How often to look for the constant before giving up: Et Futurum registers it while mods load. */
    private static final int MAX_RESOLUTION_ATTEMPTS = 64;

    private static volatile WorldSettings.GameType spectatorType;
    private static WorldSettings.GameType testType;
    private static int resolutionAttempts;

    /**
     * Whether the entity is a player in spectator mode, on the server as the world manager holds it, on the client as
     * the player controller does.
     */
    public static boolean isSpectator(Entity entity) {
        if (!(entity instanceof EntityPlayer) || entity instanceof FakePlayer) {
            return false;
        }
        WorldSettings.GameType spectator = spectatorGameType();
        if (spectator == null) {
            return false;
        }
        if (entity instanceof EntityPlayerMP) {
            return ((EntityPlayerMP) entity).theItemInWorldManager.getGameType() == spectator;
        }
        // The client's own player is no EntityPlayerMP and has no world manager: the proxy reads the gametype off its
        // player controller. Other players seen on a client keep whatever the server gave them.
        return entity.worldObj.isRemote && entity == MineFantasyII.proxy.getClientPlayer()
                && MineFantasyII.proxy.clientIsInGameType(spectator);
    }

    /**
     * The gametype spectators play in, or null while no spectator mode is registered. The client compares the player
     * controller's current gametype with this through the proxy.
     */
    public static WorldSettings.GameType spectatorGameType() {
        if (testType != null) {
            return testType;
        }
        if (spectatorType == null && resolutionAttempts < MAX_RESOLUTION_ATTEMPTS) {
            resolutionAttempts++;
            spectatorType = resolveSpectatorType();
        }
        return spectatorType;
    }

    /** Treats the given gametype as spectator from now on, for tests without Et Futurum; null clears it. */
    public static void setSpectatorGameTypeForTest(WorldSettings.GameType type) {
        testType = type;
    }

    private static WorldSettings.GameType resolveSpectatorType() {
        for (WorldSettings.GameType type : WorldSettings.GameType.values()) {
            if ("spectator".equals(type.name())) {
                return type;
            }
        }
        return null;
    }
}
