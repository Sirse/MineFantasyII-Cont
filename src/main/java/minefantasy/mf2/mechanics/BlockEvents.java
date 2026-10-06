package minefantasy.mf2.mechanics;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.world.BlockEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.api.helpers.Drops;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.config.ConfigStamina;
import minefantasy.mf2.farming.FarmingHelper;
import minefantasy.mf2.integration.CustomStone;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.weapon.ItemWeaponMF;
import minefantasy.mf2.util.XSTRandom;

/** Hoeing, harvesting and mining: failed tilling, trampled farmland, rocks and sticks by hand, tiring work. */
public class BlockEvents {

    private static final XSTRandom random = new XSTRandom();
    /** Breaks noted this tick in each world, by position: the block, and who broke it holding what. */
    private static final Map<World, Map<ChunkCoordinates, Mined>> pendingBreaks = new WeakHashMap<World, Map<ChunkCoordinates, Mined>>();

    /**
     * Notes a player's break as it is about to happen; protection questions and fake players are not breaks. A break
     * raises no Forge event once it succeeds (stone by hand drops nothing), so it is settled at the end of the tick.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void breaking(BlockEvent.BreakEvent event) {
        World world = event.world;
        EntityPlayer player = event.getPlayer();
        if (world.isRemote || player == null || player instanceof FakePlayer || ProtectionHelper.isProtectionQuery())
            return;
        // Nothing to mine, and it must not take the place of a real break at the same spot this tick
        if (event.block == null || event.block.isAir(world, event.x, event.y, event.z)) return;
        Map<ChunkCoordinates, Mined> here = pendingBreaks.get(world);
        if (here == null) {
            here = new HashMap<ChunkCoordinates, Mined>();
            pendingBreaks.put(world, here);
        }
        ItemStack held = player.getHeldItem();
        here.put(
                new ChunkCoordinates(event.x, event.y, event.z),
                new Mined(event.block, event.blockMetadata, player, held == null ? null : held.copy(), event));
    }

    @SubscribeEvent
    public void useHoe(UseHoeEvent event) {
        Block block = event.world.getBlock(event.x, event.y, event.z);
        if (block != Blocks.farmland && FarmingHelper.didHoeFail(event.current, event.world, block == Blocks.grass)) {
            event.entityPlayer.swingItem();
            event.world.playAuxSFXAtEntity(
                    event.entityPlayer,
                    2001,
                    event.x,
                    event.y,
                    event.z,
                    Block.getIdFromBlock(block) + (event.world.getBlockMetadata(event.x, event.y, event.z) << 12));
            event.setCanceled(true);
        }
    }

    /**
     * Pays what the breaks noted this tick give, for each block gone by now. Known limit: if the player's break failed
     * and something else removed the block in the same tick, the player is still rewarded. Exact settling would need a
     * hook on tryHarvestBlock.
     */
    @SubscribeEvent
    public void settleBreaks(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        Map<ChunkCoordinates, Mined> here = pendingBreaks.remove(event.world);
        if (here == null) return;
        for (Map.Entry<ChunkCoordinates, Mined> entry : here.entrySet()) {
            ChunkCoordinates at = entry.getKey();
            BlockEvent.BreakEvent asked = entry.getValue().event;
            // A handler after this one may have cancelled the break: then whatever removed the block, it was not this
            if (asked != null && asked.isCanceled()) continue;
            if (event.world.blockExists(at.posX, at.posY, at.posZ)
                    && event.world.isAirBlock(at.posX, at.posY, at.posZ)) {
                minedBlock(event.world, at.posX, at.posY, at.posZ, entry.getValue());
            }
        }
    }

    /** Settles MineFantasy mining effects after a caller has confirmed its own block removal. */
    public static void successfulPlayerBreak(World world, int x, int y, int z, Block block, int meta,
            EntityPlayer player, ItemStack held) {
        if (world == null || world.isRemote) return;
        minedBlock(world, x, y, z, new Mined(block, meta, player, held == null ? null : held.copy(), null));
    }

    /** A break as it was asked about: the block, and who broke it holding what. */
    private static final class Mined {

        final Block block;
        final int meta;
        final EntityPlayer player;
        final ItemStack held;
        /** The break event it was noted from, if any: a handler after this one may still cancel it. */
        final BlockEvent.BreakEvent event;

        Mined(Block block, int meta, EntityPlayer player, ItemStack held, BlockEvent.BreakEvent event) {
            this.block = block;
            this.meta = meta;
            this.player = player;
            this.held = held;
            this.event = event;
        }
    }

    /** A block really broken: farmland under it may be ruined, and a player mining by hand gets rocks and tires. */
    private static void minedBlock(World world, int x, int y, int z, Mined mined) {
        EntityPlayer player = mined.player;
        if (player != null && y > 0
                && world.getBlock(x, y - 1, z) == Blocks.farmland
                && FarmingHelper.didHarvestRuinBlock(world, false)) {
            ProtectionHelper.replaceBlock(player, world, x, y - 1, z, Blocks.dirt, 0);
        }
        if (player != null && !player.capabilities.isCreativeMode && !(player instanceof FakePlayer)) {
            playerMineBlock(world, x, y, z, mined);
        }
    }

    private static void playerMineBlock(World world, int x, int y, int z, Mined mined) {
        EntityPlayer player = mined.player;
        ItemStack held = mined.held;
        Block broken = mined.block;

        if (broken != null && ConfigHardcore.HCCallowRocks) {
            if (held == null && CustomStone.isStone(broken, mined.meta)) {
                Drops.fromBlock(world, x, y, z, new ItemStack(ComponentListMF.sharp_rock, random.nextInt(3) + 1));
            }
            if (held != null && held.getItem() == ComponentListMF.sharp_rock && broken instanceof BlockLeavesBase) {
                if (random.nextInt(5) == 0) {
                    Drops.fromBlock(world, x, y, z, new ItemStack(Items.stick, random.nextInt(3) + 1));
                }
                if (random.nextInt(3) == 0) {
                    Drops.fromBlock(world, x, y, z, new ItemStack(ComponentListMF.vine, random.nextInt(3) + 1));
                }
            }
        }

        if (StaminaBar.isSystemActive && ConfigStamina.affectMining && StaminaBar.doesAffectEntity(player)) {
            float points = 2.0F * ConfigStamina.miningSpeed;
            ItemWeaponMF.applyFatigue(player, points, 20F);

            if (points > 0 && !StaminaBar.isAnyStamina(player, false)) {
                player.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, 100, 1));
            }
        }
    }
}
