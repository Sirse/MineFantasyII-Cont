package minefantasy.mf2.mechanics;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

import cpw.mods.fml.common.eventhandler.Event;

/**
 * Permission checks and block changes for code that changes blocks outside vanilla's break/place flow. Only Forge
 * events are raised; hybrid servers (Thermos, Crucible) bridge them to Bukkit plugins themselves. Setting fire is
 * placing a fire block, as flint and steel goes in Forge.
 */
public class ProtectionHelper {

    /** Set while a placement waits on its event and check; a placement asked for meanwhile is refused. */
    private static boolean deciding;

    /** Set while a break event is raised only to ask about protection, so mining effects can tell it from a break. */
    private static boolean querying;

    private ProtectionHelper() {}

    /**
     * Returns false when a protection plugin or mod denies this player breaking the block.
     * <p>
     * Uses the Forge adapter, so handlers that cancel {@code BlockEvent.BreakEvent} are honoured and adventure/creative
     * restrictions apply. Note this also mirrors vanilla's behaviour of telling the client the block is gone and
     * restoring it when the break is denied.
     */
    public static boolean canBreak(EntityPlayer player, World world, int x, int y, int z) {
        return breakExperience(player, world, x, y, z) >= 0;
    }

    /**
     * The experience breaking the block would give, as the break event settles it (none under silk touch, and as other
     * mods change it), or -1 when a protection plugin or mod denies the break.
     */
    public static int breakExperience(EntityPlayer player, World world, int x, int y, int z) {
        if (world == null || player == null) return -1;
        if (world.isRemote) {
            return 0;
        }
        if (!world.blockExists(x, y, z)) return -1;
        // The adapter uses vanilla's hook for connected players and still asks Forge listeners for fake/unconnected
        // players without granting permission just because no connection exists.
        BlockEvent.BreakEvent event = breakEvent(player, world, x, y, z);
        return event == null || event.isCanceled() ? -1 : event.getExpToDrop();
    }

    /**
     * Removes the block once the break event allows it and the block is still the one asked about. Gives no drops: the
     * caller gives what it should, and only when this returns true.
     */
    public static boolean breakBlock(EntityPlayer player, World world, int x, int y, int z) {
        if (world == null || player == null || world.isRemote || !world.blockExists(x, y, z)) return false;
        Block original = world.getBlock(x, y, z);
        int originalMeta = world.getBlockMetadata(x, y, z);
        if (original.isAir(world, x, y, z) || !canBreak(player, world, x, y, z)) return false;
        if (!stands(world, x, y, z, original, originalMeta)) return false;
        return world.setBlockToAir(x, y, z);
    }

    /** Places a block, such as fire, where the player may place it. See {@link #replaceBlock}. */
    public static boolean placeBlock(EntityPlayer player, World world, int x, int y, int z, Block block, int meta) {
        return place(player, world, x, y, z, block, meta, null);
    }

    /** Replaces a block where the player may both break the old one and place the new one. */
    public static boolean replaceBlock(EntityPlayer player, World world, int x, int y, int z, Block block, int meta) {
        return replaceBlock(player, world, x, y, z, block, meta, null);
    }

    /**
     * As {@link #replaceBlock(EntityPlayer, World, int, int, int, Block, int)}, with a last check run after the events
     * allowed the change and before it stands: it rechecks inputs and takes payment, and the block is put back when it
     * returns false.
     */
    public static boolean replaceBlock(EntityPlayer player, World world, int x, int y, int z, Block block, int meta,
            CommitCheck check) {
        if (world == null || player == null || world.isRemote || !world.blockExists(x, y, z)) return false;
        Block original = world.getBlock(x, y, z);
        int originalMeta = world.getBlockMetadata(x, y, z);
        if (!canBreak(player, world, x, y, z)) return false;
        if (!stands(world, x, y, z, original, originalMeta)) return false;
        return place(player, world, x, y, z, block, meta, check);
    }

    /**
     * Places the block as Forge's item placement does: the change is captured, the place event sees it in the world (as
     * Bukkit's place event needs), and the captured snapshot is put back when the event or the check refuses. Only the
     * block itself is put back, not what other handlers did meanwhile. Blocks with a tile entity are never replaced, so
     * no inventory can be lost; nor is a placement made inside another capture or while another waits on its event.
     */
    private static boolean place(EntityPlayer player, World world, int x, int y, int z, Block block, int meta,
            CommitCheck check) {
        if (world == null || player == null || block == null || world.isRemote) return false;
        if (!world.blockExists(x, y, z) || world.captureBlockSnapshots || deciding) return false;
        Block original = world.getBlock(x, y, z);
        if (original.hasTileEntity(world.getBlockMetadata(x, y, z))) return false;

        ArrayList<BlockSnapshot> outer = world.capturedBlockSnapshots;
        ArrayList<BlockSnapshot> snapshots = new ArrayList<BlockSnapshot>();
        world.capturedBlockSnapshots = snapshots;
        world.captureBlockSnapshots = true;
        boolean changed;
        try {
            changed = world.setBlock(x, y, z, block, meta, 3);
        } finally {
            world.captureBlockSnapshots = false;
            world.capturedBlockSnapshots = outer;
        }
        if (!changed || snapshots.isEmpty()) {
            restore(world, snapshots);
            return false;
        }
        boolean allowed;
        deciding = true;
        try {
            allowed = stands(world, x, y, z, block, meta) && !placeEvent(snapshots.get(0), player)
                    && stands(world, x, y, z, block, meta)
                    && (check == null || check.beforeCommit());
        } finally {
            deciding = false;
        }
        if (!allowed) {
            restore(world, snapshots);
            return false;
        }
        // What setBlock held back while capturing: client updates and neighbour notifications
        for (BlockSnapshot snapshot : snapshots) {
            world.markAndNotifyBlock(
                    snapshot.x,
                    snapshot.y,
                    snapshot.z,
                    null,
                    snapshot.getReplacedBlock(),
                    world.getBlock(snapshot.x, snapshot.y, snapshot.z),
                    snapshot.flag);
        }
        return true;
    }

    private static boolean stands(World world, int x, int y, int z, Block block, int meta) {
        return world.getBlock(x, y, z) == block && world.getBlockMetadata(x, y, z) == meta;
    }

    private static void restore(World world, ArrayList<BlockSnapshot> snapshots) {
        // Latest first, so a block changed twice ends as it began
        world.restoringBlockSnapshots = true;
        try {
            for (int i = snapshots.size() - 1; i >= 0; i--) snapshots.get(i).restore(true, false);
        } finally {
            world.restoringBlockSnapshots = false;
        }
    }

    /**
     * Whether whoever is behind an action may change blocks at all: a player always (protection then weighs each
     * block), a creature while mobGriefing allows; with no one behind it never, since no one's rights can be asked.
     */
    public static boolean griefs(Entity actor, World world) {
        if (actor == null || world == null || world.isRemote) return false;
        return actor instanceof EntityPlayer || world.getGameRules().getGameRuleBooleanValue("mobGriefing");
    }

    /**
     * {@link #canBreak(EntityPlayer, World, int, int, int)} for whoever is behind an action, such as a projectile's
     * shooter. Forge has no event for creatures changing blocks, so for them mobGriefing alone decides.
     */
    public static boolean canBreak(Entity actor, World world, int x, int y, int z) {
        if (actor instanceof EntityPlayer) return canBreak((EntityPlayer) actor, world, x, y, z);
        return griefs(actor, world) && world.blockExists(x, y, z);
    }

    /** {@link #breakBlock(EntityPlayer, World, int, int, int)} for whoever is behind an action. */
    public static boolean breakBlock(Entity actor, World world, int x, int y, int z) {
        if (actor instanceof EntityPlayer) return breakBlock((EntityPlayer) actor, world, x, y, z);
        return canBreak(actor, world, x, y, z) && world.setBlockToAir(x, y, z);
    }

    /** {@link #placeBlock(EntityPlayer, World, int, int, int, Block, int)} for whoever is behind an action. */
    public static boolean placeBlock(Entity actor, World world, int x, int y, int z, Block block, int meta) {
        if (actor instanceof EntityPlayer) return placeBlock((EntityPlayer) actor, world, x, y, z, block, meta);
        return canBreak(actor, world, x, y, z) && world.setBlock(x, y, z, block, meta, 3);
    }

    /** The last check of a placement, after protection allowed it; false puts the block back. */
    public interface CommitCheck {

        boolean beforeCommit();
    }

    /**
     * Returns false when a protection plugin or mod denies this player interacting with the block. Raises the same
     * PlayerInteractEvent the vanilla right-click path would, so region protection applies to packet-driven
     * interactions too.
     */
    public static boolean canInteract(EntityPlayer player, World world, int x, int y, int z) {
        if (world == null || player == null) return false;
        if (world.isRemote) return true;
        if (!world.blockExists(x, y, z)) return false;
        return interactEvent(player, world, x, y, z);
    }

    /** Whether the break event being handled was raised by a protection question rather than by a break. */
    static boolean isProtectionQuery() {
        return querying;
    }

    /**
     * The break event as Forge settles it, or null for a player that is not a server player, who is allowed nothing. A
     * connected player goes through vanilla's hook; one with no connection gets the event posted directly, which Forge
     * listeners weigh all the same.
     */
    private static BlockEvent.BreakEvent breakEvent(EntityPlayer player, World world, int x, int y, int z) {
        if (!(player instanceof EntityPlayerMP)) return null;
        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        boolean previous = querying;
        querying = true;
        try {
            if (playerMP.playerNetServerHandler != null && playerMP.theItemInWorldManager != null) {
                WorldSettings.GameType gameType = playerMP.theItemInWorldManager.getGameType();
                return ForgeHooks.onBlockBreakEvent(world, gameType, playerMP, x, y, z);
            }
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(
                    x,
                    y,
                    z,
                    world,
                    world.getBlock(x, y, z),
                    world.getBlockMetadata(x, y, z),
                    player);
            MinecraftForge.EVENT_BUS.post(event);
            return event;
        } finally {
            querying = previous;
        }
    }

    /** Whether the place event was cancelled; always so for a player that is not a server player. */
    private static boolean placeEvent(BlockSnapshot snapshot, EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return true;
        Block against = snapshot.y > 0 && snapshot.world.blockExists(snapshot.x, snapshot.y - 1, snapshot.z)
                ? snapshot.world.getBlock(snapshot.x, snapshot.y - 1, snapshot.z)
                : Blocks.air;
        return MinecraftForge.EVENT_BUS.post(new BlockEvent.PlaceEvent(snapshot, against, player));
    }

    private static boolean interactEvent(EntityPlayer player, World world, int x, int y, int z) {
        PlayerInteractEvent event = ForgeEventFactory
                .onPlayerInteract(player, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, x, y, z, 1, world);
        return !event.isCanceled() && event.useBlock != Event.Result.DENY;
    }
}
