package minefantasy.mf2.mechanics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.api.helpers.Heading;
import minefantasy.mf2.config.ConfigTools;

/**
 * Heavy tools breaking more than the block they hit: which face was hit, which blocks around go with it, and breaking
 * each of those as a player would, so protection, tool rules, drops and experience all apply. Sneaking breaks only the
 * one block.
 */
public class HeavyHarvest {

    /** A block much softer than the one hit, beyond this ratio, is left alone, as ore around stone is not. */
    private static final float MAX_STRENGTH_RATIO = 10F;

    private static final Map<EntityPlayer, Integer> sideHit = new WeakHashMap<>();

    /** Remembers the face a player starts to break, since the block is gone by the time more is broken. */
    @SubscribeEvent
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK && !event.entityPlayer.worldObj.isRemote) {
            sideHit.put(event.entityPlayer, event.face);
        }
    }

    /** The face the player hit last, or the one facing them along their look when none was seen. */
    public static ForgeDirection sideHit(EntityPlayer player) {
        Integer side = sideHit.get(player);
        return side != null && side >= 0 && side < 6 ? ForgeDirection.getOrientation(side)
                : Heading.look(player).getOpposite();
    }

    /** Whether the player breaks more than one block now: on the server, standing, not sneaking. */
    public static boolean breaksMore(EntityPlayer player) {
        return player != null && !player.worldObj.isRemote && !player.isSneaking();
    }

    /** The blocks in a flat square of the given reach around the hit block, across the face that was hit. */
    public static List<int[]> square(EntityPlayer player, int x, int y, int z, int reach) {
        ForgeDirection side = sideHit(player);
        List<int[]> blocks = new ArrayList<>();
        for (int a = -reach; a <= reach; a++) {
            for (int b = -reach; b <= reach; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                if (side.offsetY != 0) {
                    blocks.add(new int[] { x + a, y, z + b });
                } else if (side.offsetZ != 0) {
                    blocks.add(new int[] { x + a, y + b, z });
                } else {
                    blocks.add(new int[] { x, y + a, z + b });
                }
            }
        }
        return blocks;
    }

    /**
     * The logs joined to the hit one, of the same block and kind, found outward from it; the hit log itself is not
     * among them.
     */
    public static List<int[]> tree(World world, int x, int y, int z, int limit) {
        Block log = world.getBlock(x, y, z);
        int kind = world.getBlockMetadata(x, y, z) & 3;
        List<int[]> logs = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { x, y, z });
        seen.add(key(x, y, z));
        while (!queue.isEmpty() && logs.size() < limit) {
            int[] at = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int nx = at[0] + dx, ny = at[1] + dy, nz = at[2] + dz;
                        if (logs.size() >= limit || !seen.add(key(nx, ny, nz))) {
                            continue;
                        }
                        if (world.getBlock(nx, ny, nz) == log && (world.getBlockMetadata(nx, ny, nz) & 3) == kind) {
                            int[] found = { nx, ny, nz };
                            logs.add(found);
                            queue.add(found);
                        }
                    }
                }
            }
        }
        return logs;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | ((long) z & 0x3FFFFFF);
    }

    /** How hard the block at the spot is for the player to break. */
    public static float strength(EntityPlayer player, World world, int x, int y, int z) {
        return ForgeHooks.blockStrength(world.getBlock(x, y, z), player, world, x, y, z);
    }

    /**
     * Breaks one more block as the player would, if they may and it is not much softer than the block hit. A heavy tool
     * may crumble what it breaks, leaving nothing. Returns whether the block broke; the tool wears by one.
     */
    public static boolean breakExtra(ItemStack tool, EntityPlayer player, World world, int x, int y, int z,
            float hitStrength) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        if (block.isAir(world, x, y, z) || player.getHeldItem() != tool) {
            return false;
        }
        float strength = strength(player, world, x, y, z);
        if (strength <= 0F || hitStrength / strength > MAX_STRENGTH_RATIO) {
            return false;
        }
        if (!ForgeHooks.canHarvestBlock(block, player, meta) || !ProtectionHelper.canBreak(player, world, x, y, z)) {
            return false;
        }
        block.onBlockHarvested(world, x, y, z, meta, player);
        if (!block.removedByPlayer(world, player, x, y, z, true)) {
            return false;
        }
        block.onBlockDestroyedByPlayer(world, x, y, z, meta);
        world.playAuxSFX(2001, x, y, z, Block.getIdFromBlock(block) + (meta << 12));
        if (!player.capabilities.isCreativeMode) {
            if (world.rand.nextFloat() * 100F >= ConfigTools.hvyDropChance) {
                block.harvestBlock(world, player, x, y, z, meta);
                block.dropXpOnBlockBreak(
                        world,
                        x,
                        y,
                        z,
                        block.getExpDrop(world, meta, EnchantmentHelper.getFortuneModifier(player)));
            }
            tool.damageItem(1, player);
            if (tool.stackSize <= 0) {
                player.destroyCurrentEquippedItem();
            }
        }
        return true;
    }
}
