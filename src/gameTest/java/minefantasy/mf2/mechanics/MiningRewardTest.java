package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.mechanics.ProtectionFixtures.*;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.config.ConfigHardcore;

/**
 * What mining gives comes only for a block really broken by the player: asking about protection, a refused break or a
 * cancelled one gives nothing, and a stone gives rocks once.
 */
@GameTestHolder("minefantasy2")
public class MiningRewardTest {

    private MiningRewardTest() {}

    @GameTest
    public static void askingAboutStoneGivesNoRocks(GameTestHelper helper) {
        boolean rocks = ConfigHardcore.HCCallowRocks;
        ConfigHardcore.HCCallowRocks = true;
        try {
            helper.setBlock(1, 1, 1, Blocks.stone);
            TestPos at = helper.absolute(1, 1, 1);
            EntityPlayerMP player = unconnected(helper);
            for (int i = 0; i < 5; i++) {
                ProtectionHelper.canBreak(player, helper.getWorld(), at.x(), at.y(), at.z());
            }
            endOfTick(helper);
            assertEquals("the stone is still there", Blocks.stone, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
            assertEquals("asking gave rocks", 0, rocksAround(helper, at));
        } finally {
            ConfigHardcore.HCCallowRocks = rocks;
        }
        helper.succeed();
    }

    @GameTest
    public static void bareHandStoneBreakGivesRocksWithoutHarvestDrops(GameTestHelper helper) {
        boolean rocks = ConfigHardcore.HCCallowRocks;
        ConfigHardcore.HCCallowRocks = true;
        try {
            helper.setBlock(1, 1, 1, Blocks.stone);
            TestPos at = helper.absolute(1, 1, 1);
            EntityPlayerMP player = miner(helper, at);

            assertTrue(
                    "vanilla tryHarvestBlock succeeds",
                    player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
            assertEquals("stone was really removed", Blocks.air, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
            endOfTick(helper);

            int given = rocksAround(helper, at);
            assertTrue(
                    "bare-hand unharvestable stone gives one to three rocks, gave " + given,
                    given >= 1 && given <= 3);
        } finally {
            ConfigHardcore.HCCallowRocks = rocks;
        }
        helper.succeed();
    }

    @GameTest
    public static void aRefusedHandBreakGivesNothing(GameTestHelper helper) {
        boolean rocks = ConfigHardcore.HCCallowRocks;
        ConfigHardcore.HCCallowRocks = true;
        DenyAll deny = new DenyAll();
        MinecraftForge.EVENT_BUS.register(deny);
        try {
            helper.setBlock(1, 1, 1, Blocks.stone);
            TestPos at = helper.absolute(1, 1, 1);
            EntityPlayerMP player = miner(helper, at);
            assertFalse(player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
            endOfTick(helper);
            assertEquals(Blocks.stone, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
            assertEquals("a refused break gives no rocks", 0, rocksAround(helper, at));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
            ConfigHardcore.HCCallowRocks = rocks;
        }
        helper.succeed();
    }

    @GameTest
    public static void aStoneGivesRocksOnceWhoeverElseTries(GameTestHelper helper) {
        boolean rocks = ConfigHardcore.HCCallowRocks;
        ConfigHardcore.HCCallowRocks = true;
        try {
            helper.setBlock(1, 1, 1, Blocks.stone);
            TestPos at = helper.absolute(1, 1, 1);
            EntityPlayerMP first = miner(helper, at);
            EntityPlayerMP second = miner(helper, at);
            // Protection questions from both, then one real break, then the other tries the same spot
            ProtectionHelper.canBreak(second, helper.getWorld(), at.x(), at.y(), at.z());
            ProtectionHelper.canBreak(first, helper.getWorld(), at.x(), at.y(), at.z());
            assertTrue(first.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
            second.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z());
            endOfTick(helper);
            int given = rocksAround(helper, at);
            assertTrue("one stone, one to three rocks, gave " + given, given >= 1 && given <= 3);
            endOfTick(helper);
            assertEquals("nothing more a tick later", given, rocksAround(helper, at));
        } finally {
            ConfigHardcore.HCCallowRocks = rocks;
        }
        helper.succeed();
    }

    @GameTest
    public static void aStoneBrokenWithAPickaxeGivesNoHandRocks(GameTestHelper helper) {
        boolean rocks = ConfigHardcore.HCCallowRocks;
        ConfigHardcore.HCCallowRocks = true;
        try {
            helper.setBlock(1, 1, 1, Blocks.stone);
            TestPos at = helper.absolute(1, 1, 1);
            EntityPlayerMP player = miner(helper, at);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Items.iron_pickaxe));
            assertTrue(player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
            endOfTick(helper);
            assertEquals("rocks are for bare hands only", 0, rocksAround(helper, at));
        } finally {
            ConfigHardcore.HCCallowRocks = rocks;
        }
        helper.succeed();
    }

    /** Cancels every break as the last handler, after any other has had its say. */
    public static final class CancelLate {

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void cancel(BlockEvent.BreakEvent event) {
            event.setCanceled(true);
        }
    }

    @GameTest
    public static void aRefusedBreakGivesNothingWhateverRemovesTheBlockAfter(GameTestHelper helper) {
        boolean rocks = ConfigHardcore.HCCallowRocks;
        ConfigHardcore.HCCallowRocks = true;
        CancelLate late = new CancelLate();
        MinecraftForge.EVENT_BUS.register(late);
        try {
            helper.setBlock(1, 1, 1, Blocks.stone);
            TestPos at = helper.absolute(1, 1, 1);
            EntityPlayerMP player = miner(helper, at);
            assertFalse("the break was refused", player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
            // Something else takes the block away in the same tick: that is no break of the player's
            helper.getWorld().setBlockToAir(at.x(), at.y(), at.z());
            endOfTick(helper);
            assertEquals("a refused break gives no rocks", 0, rocksAround(helper, at));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(late);
            ConfigHardcore.HCCallowRocks = rocks;
        }
        helper.succeed();
    }

    /** Turns the stone being broken into dirt without cancelling, as another mod's break handler might. */
    public static final class StoneToDirt {

        @SubscribeEvent
        public void swap(BlockEvent.BreakEvent event) {
            event.world.setBlock(event.x, event.y, event.z, Blocks.dirt);
        }
    }

    /** Puts a pickaxe in the breaking player's hand without cancelling. */
    public static final class HandAPickaxe {

        @SubscribeEvent
        public void swap(BlockEvent.BreakEvent event) {
            event.getPlayer().inventory.setInventorySlotContents(
                    event.getPlayer().inventory.currentItem,
                    new ItemStack(Items.iron_pickaxe));
        }
    }

    @GameTest
    public static void aBlockChangedDuringTheEventIsRewardedAsWhatWasBroken(GameTestHelper helper) {
        boolean rocks = ConfigHardcore.HCCallowRocks;
        ConfigHardcore.HCCallowRocks = true;
        StoneToDirt swap = new StoneToDirt();
        MinecraftForge.EVENT_BUS.register(swap);
        try {
            helper.setBlock(1, 1, 1, Blocks.stone);
            TestPos at = helper.absolute(1, 1, 1);
            EntityPlayerMP player = miner(helper, at);
            assertTrue(player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
            assertEquals(Blocks.air, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
            assertEquals("dirt was broken, not stone: no rocks", 0, rocksAround(helper, at));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(swap);
            ConfigHardcore.HCCallowRocks = rocks;
        }
        helper.succeed();
    }

    @GameTest
    public static void aToolChangedDuringTheEventIsRewardedAsTheToolUsed(GameTestHelper helper) {
        boolean rocks = ConfigHardcore.HCCallowRocks;
        ConfigHardcore.HCCallowRocks = true;
        HandAPickaxe swap = new HandAPickaxe();
        MinecraftForge.EVENT_BUS.register(swap);
        try {
            helper.setBlock(1, 1, 1, Blocks.stone);
            TestPos at = helper.absolute(1, 1, 1);
            EntityPlayerMP player = miner(helper, at);
            assertTrue(player.theItemInWorldManager.tryHarvestBlock(at.x(), at.y(), at.z()));
            assertEquals("broken with a pickaxe, not by hand: no rocks", 0, rocksAround(helper, at));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(swap);
            ConfigHardcore.HCCallowRocks = rocks;
        }
        helper.succeed();
    }
}
