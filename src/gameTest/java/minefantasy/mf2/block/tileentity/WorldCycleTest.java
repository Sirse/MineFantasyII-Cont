package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.config.ConfigKitchen;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * The whole working cycle of stations the other tests drive a step at a time: lit and ticking, turned, struck and
 * washed by a player.
 */
@GameTestHolder("minefantasy2")
public class WorldCycleTest {

    private WorldCycleTest() {}

    private static FakePlayer holding(GameTestHelper helper, ItemStack held) {
        FakePlayer player = helper.spawnFakePlayer(Modders.SMITH);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, held);
        return player;
    }

    // region bloomery

    private static TileEntityBloomery litBloomery(GameTestHelper helper) {
        reload(
                tx -> tx.add(
                        MFRecipes.BLOOMERY,
                        id("bloomery", "ore"),
                        BloomRecipe.of(Input.of(ore), new ItemStack(bar)),
                        0));
        helper.setBlock(1, 1, 1, BlockListMF.bloomery);
        TileEntityBloomery bloomery = helper.assertTileEntityPresent(TileEntityBloomery.class, 1, 1, 1);
        bloomery.setInventorySlotContents(0, new ItemStack(ore, 4));
        bloomery.setInventorySlotContents(1, new ItemStack(carbon));
        assertTrue("the bloomery did not light", bloomery.light(holding(helper, null)));
        // Four items burn for 1200 ticks: skip most of it, the last ticks run for real
        bloomery.progress = bloomery.progressMax - 10;
        return bloomery;
    }

    @GameTest(timeoutTicks = 100)
    public static void bloomeryUnderTheSkySmeltsItsStack(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        TileEntityBloomery bloomery = litBloomery(helper);
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> {
            ItemStack bloom = bloomery.getStackInSlot(2);
            if (bloom == null) {
                return false;
            }
            assertEquals(bar, bloom.getItem());
            assertEquals("a bloom per ore", 4, bloom.stackSize);
            assertNull("the ore is smelted", bloomery.getStackInSlot(0));
            assertNull("the carbon is burnt", bloomery.getStackInSlot(1));
            assertFalse("the fire is out", bloomery.isActive);
            return true;
        });
    }

    @GameTest(timeoutTicks = 60)
    public static void roofOverTheBloomeryPutsTheFireOut(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        TileEntityBloomery bloomery = litBloomery(helper);
        helper.setBlock(1, 3, 1, Blocks.stone);
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> {
            if (bloomery.isActive) {
                return false;
            }
            assertNull("nothing was smelted", bloomery.getStackInSlot(2));
            assertEquals("the ore stays", 4, bloomery.getStackInSlot(0).stackSize);
            assertEquals("the carbon stays", 1, bloomery.getStackInSlot(1).stackSize);
            return true;
        });
    }

    // endregion

    // region big furnace

    /** A heater with a furnace on it, firebricks on their sides and back and over the furnace, the front open. */
    private static TileEntityBigFurnace bigFurnace(GameTestHelper helper, boolean walls) {
        reload(
                tx -> tx.add(
                        MFRecipes.BIG_FURNACE,
                        id("big_furnace", "seed"),
                        ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                        0));
        // Metadata 0 faces the front north (towards -z)
        helper.setBlock(2, 1, 2, BlockListMF.furnace_heater, 0);
        helper.setBlock(2, 2, 2, BlockListMF.furnace_stone, 0);
        if (walls) {
            for (int y = 1; y <= 2; y++) {
                helper.setBlock(1, y, 2, BlockListMF.firebricks);
                helper.setBlock(3, y, 2, BlockListMF.firebricks);
                helper.setBlock(2, y, 3, BlockListMF.firebricks);
            }
            helper.setBlock(2, 3, 2, BlockListMF.firebricks);
        }
        TileEntityBigFurnace furnace = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 2, 2);
        assertTrue("the lower block is not the heater", heater(helper).isHeater());
        furnace.setInventorySlotContents(0, new ItemStack(seed, 2));
        return furnace;
    }

    private static TileEntityBigFurnace heater(GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 1, 2);
    }

    /**
     * Feeds the heater to full heat once it finds its walls itself: an unbuilt heater puts its fire out. The long
     * warming up is skipped, the smelt runs for real.
     */
    private static void stokeOnceBuilt(TileEntityBigFurnace heater) {
        if (heater.built && heater.fuel <= 0) {
            heater.fuel = 100000;
            heater.maxHeat = 2000;
            heater.heat = 2000;
        }
    }

    @GameTest(timeoutTicks = 200)
    public static void builtBigFurnaceSmeltsOverItsHeater(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        TileEntityBigFurnace furnace = bigFurnace(helper, true);
        TileEntityBigFurnace heater = heater(helper);
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> {
            stokeOnceBuilt(heater);
            ItemStack made = furnace.getStackInSlot(4);
            if (made == null) {
                return false;
            }
            assertEquals(flour, made.getItem());
            assertEquals("one seed is smelted at a time", 1, furnace.getStackInSlot(0).stackSize);
            return true;
        });
    }

    @GameTest(timeoutTicks = 60)
    public static void bigFurnaceWithoutWallsGetsNoHeat(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        TileEntityBigFurnace furnace = bigFurnace(helper, false);
        TileEntityBigFurnace heater = heater(helper);
        Stations.keepUntilFinished();
        helper.onEachTick("nothing smelts", () -> {
            stokeOnceBuilt(heater);
            assertFalse("a heater without walls counts as built", heater.built);
            assertNull(furnace.getStackInSlot(4));
        });
        helper.succeedAtTimeout();
    }

    // endregion

    // region quern

    @GameTest(timeoutTicks = 100)
    public static void aTurnOfTheQuernGrindsOnce(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        reload(
                tx -> tx.add(
                        MFRecipes.QUERN,
                        id("quern", "seed"),
                        ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                        0));
        helper.setBlock(1, 1, 1, BlockListMF.quern);
        TileEntityQuern quern = helper.assertTileEntityPresent(TileEntityQuern.class, 1, 1, 1);
        quern.setInventorySlotContents(0, new ItemStack(seed, 3));
        quern.setInventorySlotContents(1, new ItemStack(ComponentListMF.clay_pot, 4));
        quern.onUse(holding(helper, null));
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> {
            ItemStack flourOut = quern.getStackInSlot(2);
            if (flourOut == null) {
                return false;
            }
            assertEquals(flour, flourOut.getItem());
            assertEquals("one grind per quarter turn", 1, flourOut.stackSize);
            assertEquals(2, quern.getStackInSlot(0).stackSize);
            assertEquals("the pot is used", 3, quern.getStackInSlot(1).stackSize);
            return true;
        });
    }

    // endregion

    // region carpenter

    private static TileEntityCarpenterMF carpenter(GameTestHelper helper, int hammerTier) {
        reload(
                tx -> tx.add(
                        MFRecipes.CARPENTER,
                        id("carpenter", "seed"),
                        GridRecipe.shapeless(
                                GridRecipe.Grid.BENCH,
                                new Object[] { new ItemStack(seed) },
                                null,
                                new ItemStack(flour)).tool("hammer", hammerTier).time(2).build(),
                        0));
        helper.setBlock(1, 1, 1, BlockListMF.carpenter);
        TileEntityCarpenterMF bench = helper.assertTileEntityPresent(TileEntityCarpenterMF.class, 1, 1, 1);
        bench.setInventorySlotContents(0, new ItemStack(seed, 2));
        bench.updateCraftingData();
        return bench;
    }

    private static int output(TileEntityCarpenterMF bench) {
        return bench.getSizeInventory() - 5;
    }

    @GameTest
    public static void carpenterCraftsUnderTheToolAndWearsIt(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityCarpenterMF bench = carpenter(helper, 0);
            FakePlayer carpenter = holding(helper, new ItemStack(ToolListMF.hammerStone));
            int hits = 0;
            while (hits < 20 && bench.getStackInSlot(output(bench)) == null) {
                bench.tryCraft(carpenter);
                hits++;
            }
            assertNotNull("the bench made nothing", bench.getStackInSlot(output(bench)));
            assertEquals(flour, bench.getStackInSlot(output(bench)).getItem());
            assertEquals("the grid paid one seed", 1, bench.getStackInSlot(0).stackSize);
            assertEquals("every hit wears the tool", hits, carpenter.getHeldItem().getItemDamage());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void carpenterRefusesAToolBelowTheRecipeTier(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityCarpenterMF bench = carpenter(helper, 5);
            FakePlayer carpenter = holding(helper, new ItemStack(ToolListMF.hammerStone));
            for (int hit = 0; hit < 20; hit++) {
                bench.tryCraft(carpenter);
            }
            assertNull("a stone hammer made a tier 5 recipe", bench.getStackInSlot(output(bench)));
            assertEquals(2, bench.getStackInSlot(0).stackSize);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aWornOutToolBreaksOnTheBench(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityCarpenterMF bench = carpenter(helper, 0);
            ItemStack worn = new ItemStack(ToolListMF.hammerStone);
            worn.setItemDamage(worn.getMaxDamage() - 1);
            FakePlayer carpenter = holding(helper, worn);
            bench.tryCraft(carpenter);
            assertNull("the worn out hammer is still in hand", carpenter.getHeldItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region kitchen

    @GameTest
    public static void waterWashesTheKitchenBench(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.kitchenBench);
        TileEntityKitchenBench bench = helper.assertTileEntityPresent(TileEntityKitchenBench.class, 1, 1, 1);
        bench.dirtyProgress = ConfigKitchen.dirtyProgressMax;
        assertTrue(bench.isDirty());
        FakePlayer cook = holding(helper, new ItemStack(Items.water_bucket));
        bench.interact(cook);
        float washed = ConfigKitchen.dirtyProgressMax * ConfigKitchen.washStrengthFraction;
        assertEquals(ConfigKitchen.dirtyProgressMax - washed, bench.dirtyProgress, 0.01F);
        assertFalse("half a wash leaves the bench usable", bench.isDirty());
        assertTrue("the bucket comes back empty", cook.inventory.hasItem(Items.bucket));
        assertFalse(cook.inventory.hasItem(Items.water_bucket));
        helper.succeed();
    }

    // endregion
}
