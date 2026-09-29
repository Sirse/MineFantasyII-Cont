package minefantasy.mf2.mechanics;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.world.BlockEvent;

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
        return breakExperience(player, world, x, y, z) >= 0;
    }

    /**
     * The experience breaking the block would give, as the break event settles it (none under silk touch, and as other
     * mods change it), or -1 when a protection plugin or mod denies the break.
     */
    public static int breakExperience(EntityPlayer player, World world, int x, int y, int z) {
        if (world == null || world.isRemote || player == null) {
            return 0;
        }
        if (MineFantasyII.isBukkitServer() && BukkitUtils.cantBreakBlock(player, x, y, z)) {
            return -1;
        }
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            if (playerMP.playerNetServerHandler != null && playerMP.theItemInWorldManager != null) {
                WorldSettings.GameType gameType = playerMP.theItemInWorldManager.getGameType();
                BlockEvent.BreakEvent event = ForgeHooks.onBlockBreakEvent(world, gameType, playerMP, x, y, z);
                return event.isCanceled() ? -1 : event.getExpToDrop();
            }
        }
        // Fake or partially initialised player: nothing to ask, and the hook would need a live connection; the
        // experience is worked out as the event would
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        if (EnchantmentHelper.getSilkTouchModifier(player) && block.canSilkHarvest(world, player, x, y, z, meta)) {
            return 0;
        }
        return block.getExpDrop(world, meta, EnchantmentHelper.getFortuneModifier(player));
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
