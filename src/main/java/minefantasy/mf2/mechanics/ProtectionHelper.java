package minefantasy.mf2.mechanics;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.ForgeHooks;

import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.util.BukkitUtils;

/**
 * Central permission check for code paths that modify blocks outside the vanilla break/place flow (AoE tools,
 * transformations). On Bukkit servers the registered protection plugins are asked directly; on the Forge side the real
 * block-break event is raised, which is what region mods listen to.
 */
public class ProtectionHelper {

    private ProtectionHelper() {}

    /**
     * Returns false when a protection plugin or mod denies this player breaking the block.
     * <p>
     * Uses {@link ForgeHooks#onBlockBreakEvent}, so handlers that cancel {@code BlockEvent.BreakEvent} are honoured and
     * adventure/creative restrictions apply. Note this also mirrors vanilla's behaviour of telling the client the block
     * is gone and restoring it when the break is denied.
     */
    public static boolean canBreak(EntityPlayer player, World world, int x, int y, int z) {
        if (world == null || world.isRemote || player == null) {
            return true;
        }
        if (MineFantasyII.isBukkitServer() && BukkitUtils.cantBreakBlock(player, x, y, z)) {
            return false;
        }
        if (!(player instanceof EntityPlayerMP)) {
            return true;
        }
        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        if (playerMP.playerNetServerHandler == null || playerMP.theItemInWorldManager == null) {
            // Fake or partially initialised player: nothing to ask, and the hook would need a live connection
            return true;
        }
        WorldSettings.GameType gameType = playerMP.theItemInWorldManager.getGameType();
        return !ForgeHooks.onBlockBreakEvent(world, gameType, playerMP, x, y, z).isCanceled();
    }

    /**
     * Returns false when a protection plugin or mod denies this player interacting with the block. Raises the same
     * PlayerInteractEvent the vanilla right-click path would, so region protection applies to packet-driven
     * interactions too.
     */
    public static boolean canInteract(EntityPlayer player, World world, int x, int y, int z) {
        if (world == null || world.isRemote || player == null) {
            return true;
        }
        if (MineFantasyII.isBukkitServer() && BukkitUtils.cantBreakBlock(player, x, y, z)) {
            return false;
        }
        net.minecraftforge.event.entity.player.PlayerInteractEvent event = new net.minecraftforge.event.entity.player.PlayerInteractEvent(
                player,
                net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
                x,
                y,
                z,
                1,
                world);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled() && event.useBlock != cpw.mods.fml.common.eventhandler.Event.Result.DENY;
    }
}
