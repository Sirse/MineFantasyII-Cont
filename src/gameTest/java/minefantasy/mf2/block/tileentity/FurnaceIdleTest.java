package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;
import static minefantasy.mf2.gametest.Ticks.tick;
import static minefantasy.mf2.gametest.Ticks.tickUntil;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minefantasy.mf2.api.heating.AmbientTemperature;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.gametest.Overrides;

/** Issue #79: furnaces smelt only from fuel they burn, and only what they hold. */
@GameTestHolder("minefantasy2")
public class FurnaceIdleTest {

    private FurnaceIdleTest() {}

    /** A heater and a furnace above it, walled in firebrick, facing north. */
    static TileEntityBigFurnace builtFurnace(GameTestHelper helper) {
        for (int y = 1; y <= 2; y++) {
            helper.setBlock(1, y, 2, BlockListMF.firebricks);
            helper.setBlock(3, y, 2, BlockListMF.firebricks);
            helper.setBlock(2, y, 3, BlockListMF.firebricks);
        }
        helper.setBlock(2, 3, 2, BlockListMF.firebricks);
        helper.setBlock(2, 1, 2, BlockListMF.furnace_heater, 0);
        helper.setBlock(2, 2, 2, BlockListMF.furnace_stone, 0);
        return helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 2, 2);
    }

    /**
     * The air by a heater is warmer than up at the furnace, as altitude makes it: an unlit heater still smelts nothing.
     */
    @GameTest(timeoutTicks = 100)
    public static void aColdHeaterDoesNotSmelt(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        int heaterY = helper.absolute(2, 1, 2).y();
        // The air stays overridden while the furnace ticks between polls; the close runs when the test ends.
        Overrides overrides = new Overrides().air((world, x, y, z) -> y == heaterY ? 25F : 20F);
        helper.afterTest(overrides::close);
        reload(
                tx -> tx.add(
                        MFRecipes.BIG_FURNACE,
                        id("big_furnace", "seed"),
                        ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                        0));
        TileEntityBigFurnace furnace = builtFurnace(helper);
        furnace.setInventorySlotContents(0, new ItemStack(seed));
        Stations.keepUntilFinished();
        int[] ticks = { 0 };
        helper.succeedWhen(() -> {
            if (++ticks[0] < 60) return false;
            TileEntityBigFurnace now = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 2, 2);
            TileEntityBigFurnace heater = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 1, 2);
            assertTrue("the furnace was never built: heater " + heater.built, now.built);
            assertEquals("a cold heater made progress", 0, now.progress);
            return true;
        });
    }

    /** With nothing to smelt above, the blast heater keeps its fuel and makes no progress. */
    @GameTest(timeoutTicks = 100)
    public static void anEmptyBlastFurnaceKeepsItsFuel(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.firebricks);
        helper.setBlock(3, 1, 1, BlockListMF.firebricks);
        helper.setBlock(1, 1, 3, BlockListMF.firebricks);
        helper.setBlock(3, 1, 3, BlockListMF.firebricks);
        helper.setBlock(2, 1, 2, BlockListMF.blast_heater);
        TileEntityBlastFH heater = helper.assertTileEntityPresent(TileEntityBlastFH.class, 2, 1, 2);
        heater.updateBuild();
        heater.setInventorySlotContents(0, new ItemStack(Items.coal, 4));
        int[] ticks = { 0 };
        helper.succeedWhen(() -> {
            if (++ticks[0] < 60) return false;
            TileEntityBlastFH now = helper.assertTileEntityPresent(TileEntityBlastFH.class, 2, 1, 2);
            assertTrue("the heater was never built", now.isBuilt);
            assertEquals("fuel burned with nothing to smelt", 4, now.getStackInSlot(0).stackSize);
            assertEquals(0F, now.progress, 0F);
            return true;
        });
    }

    /** Lava burned in the heater leaves its bucket in the fuel slot, where automation may take it out. */
    @GameTest
    public static void theHeaterGivesBackTheBucketOfItsFuel(GameTestHelper helper) {
        MineFantasyFuels.Fuel lava = MineFantasyFuels.BLAST.addScript(new ItemStack(Items.lava_bucket), 40);
        try {
            helper.setBlock(1, 1, 1, BlockListMF.firebricks);
            helper.setBlock(3, 1, 1, BlockListMF.firebricks);
            helper.setBlock(1, 1, 3, BlockListMF.firebricks);
            helper.setBlock(3, 1, 3, BlockListMF.firebricks);
            helper.setBlock(2, 1, 2, BlockListMF.blast_heater);
            helper.setBlock(1, 2, 2, BlockListMF.firebricks);
            helper.setBlock(3, 2, 2, BlockListMF.firebricks);
            helper.setBlock(2, 2, 1, BlockListMF.firebricks);
            helper.setBlock(2, 2, 3, BlockListMF.firebricks);
            helper.setBlock(2, 2, 2, BlockListMF.blast_chamber);
            TileEntityBlastFH heater = helper.assertTileEntityPresent(TileEntityBlastFH.class, 2, 1, 2);
            TileEntityBlastFC chamber = helper.assertTileEntityPresent(TileEntityBlastFC.class, 2, 2, 2);
            chamber.setInventorySlotContents(0, new ItemStack(Items.coal));
            ItemStack ore = MFRecipes.BLAST_FURNACE.published().all().get(0).getRecipe().getInput().examples().get(0);
            chamber.setInventorySlotContents(1, ore.copy());
            heater.updateBuild();
            chamber.updateBuild();
            heater.setInventorySlotContents(0, new ItemStack(Items.lava_bucket));
            heater.updateEntity();
            assertEquals("the lava was not burned", 40, heater.maxFuel);
            ItemStack left = heater.getStackInSlot(0);
            assertNotNull("the bucket was destroyed", left);
            assertSame(Items.bucket, left.getItem());
            assertTrue("automation may not take the bucket", heater.canExtractItem(0, left, 1));
        } finally {
            MineFantasyFuels.BLAST.removeScript(lava);
        }
        helper.succeed();
    }

    /** Once the fire goes out with no fuel left, progress starts over instead of waiting for the next fuel. */
    @GameTest(timeoutTicks = 100)
    public static void blastProgressStartsOverWhenTheFireDies(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.firebricks);
        helper.setBlock(3, 1, 1, BlockListMF.firebricks);
        helper.setBlock(1, 1, 3, BlockListMF.firebricks);
        helper.setBlock(3, 1, 3, BlockListMF.firebricks);
        helper.setBlock(2, 1, 2, BlockListMF.blast_heater);
        helper.setBlock(1, 2, 2, BlockListMF.firebricks);
        helper.setBlock(3, 2, 2, BlockListMF.firebricks);
        helper.setBlock(2, 2, 1, BlockListMF.firebricks);
        helper.setBlock(2, 2, 3, BlockListMF.firebricks);
        helper.setBlock(2, 2, 2, BlockListMF.blast_chamber);
        TileEntityBlastFH heater = helper.assertTileEntityPresent(TileEntityBlastFH.class, 2, 1, 2);
        TileEntityBlastFC chamber = helper.assertTileEntityPresent(TileEntityBlastFC.class, 2, 2, 2);
        chamber.setInventorySlotContents(0, new ItemStack(Items.coal));
        ItemStack ore = MFRecipes.BLAST_FURNACE.published().all().get(0).getRecipe().getInput().examples().get(0);
        chamber.setInventorySlotContents(1, ore.copy());
        heater.updateBuild();
        chamber.updateBuild();
        heater.fuel = 5;
        heater.progress = 50;
        int[] ticks = { 0 };
        helper.succeedWhen(() -> {
            if (++ticks[0] < 20) return false;
            TileEntityBlastFH now = helper.assertTileEntityPresent(TileEntityBlastFH.class, 2, 1, 2);
            assertEquals("progress waited for the next fuel", 0F, now.progress, 0F);
            return true;
        });
    }

    @GameTest
    public static void advancedForgeCoolsAfterFuelEnds(GameTestHelper helper) {
        try (Overrides overrides = new Overrides().air((world, x, y, z) -> 20F)) {
            helper.setBlock(2, 1, 2, BlockListMF.forge_metal_active);
            TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 2, 1, 2);
            forge.temperature = 120F;
            forge.fuel = 0;
            tick(forge, 60);
            assertEquals("an empty advanced forge kept its heat", 20F, forge.temperature, 0F);
            assertEquals(0, forge.getHeat());
            forge.fuel = 10;
            forge.fuelTemperature = 600F;
            tick(forge, 15);
            assertTrue("a fueled forge must still heat up", forge.temperature > 20F);
        }
        helper.succeed();
    }

    @GameTest
    public static void crucibleUsesHeatAboveTheSourcesAir(GameTestHelper helper) {
        Stations.begin(helper);
        int forgeY = helper.absolute(2, 1, 2).y();
        try (Overrides overrides = new Overrides().air((world, x, y, z) -> y == forgeY ? 25F : 20F)) {
            reload(
                    tx -> tx.add(
                            MFRecipes.ALLOY,
                            id("alloy", "air"),
                            new minefantasy.mf2.api.refine.Alloy(
                                    new ItemStack(flour),
                                    0,
                                    java.util.Arrays.asList(new ItemStack(seed))),
                            0));
            helper.setBlock(2, 1, 2, BlockListMF.forge_metal_active);
            helper.setBlock(2, 2, 2, BlockListMF.crucible);
            TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 2, 1, 2);
            TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 2, 2, 2);
            forge.temperature = 25F;
            forge.fuel = 0;
            crucible.setInventorySlotContents(0, new ItemStack(seed));
            tick(60, forge, crucible);
            assertEquals(20F, crucible.temperature, 0F);
            assertEquals("air made smelting progress", 0F, crucible.progress, 0F);
            assertTrue(crucible.getStackInSlot(9) == null);
            assertEquals(1, crucible.getStackInSlot(0).stackSize);
            forge.temperature = 600F;
            forge.fuelTemperature = 600F;
            forge.fuel = 1000;
            tickUntil(() -> crucible.getStackInSlot(9) != null, 2000, forge, crucible);
            assertTrue("a hot forge must still smelt", crucible.getStackInSlot(9) != null);
            assertSame(flour, crucible.getStackInSlot(9).getItem());
            assertTrue(crucible.getStackInSlot(0) == null);
            AmbientTemperature.setProvider((world, x, y, z) -> -15F);
            helper.setBlock(2, 1, 2, BlockListMF.forge_metal);
            forge = helper.assertTileEntityPresent(TileEntityForge.class, 2, 1, 2);
            forge.temperature = -15F;
            crucible.setInventorySlotContents(9, null);
            crucible.setInventorySlotContents(0, new ItemStack(seed));
            tick(20, forge, crucible);
            assertEquals("an unlit forge warmed a freezing crucible", -15F, crucible.temperature, 0F);
            assertEquals(0F, crucible.progress, 0F);
            assertTrue(crucible.getStackInSlot(9) == null);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void bigHeaterLightsOnlyForWorkThatFits(GameTestHelper helper) {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.BIG_FURNACE,
                            id("big_furnace", "idle"),
                            ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                            0));
            TileEntityBigFurnace furnace = builtFurnace(helper);
            TileEntityBigFurnace heater = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 1, 2);
            heater.setInventorySlotContents(0, new ItemStack(Items.coal, 4));
            tick(30, furnace, heater);
            assertTrue(furnace.built && heater.built);
            assertEquals("empty furnace burned fuel", 4, heater.getStackInSlot(0).stackSize);
            furnace.setInventorySlotContents(0, new ItemStack(Items.stick));
            heater.updateEntity();
            assertEquals("unusable input burned fuel", 4, heater.getStackInSlot(0).stackSize);
            furnace.setInventorySlotContents(0, new ItemStack(seed));
            furnace.setInventorySlotContents(4, new ItemStack(flour, 64));
            heater.updateEntity();
            assertEquals("blocked output burned fuel", 4, heater.getStackInSlot(0).stackSize);
            furnace.setInventorySlotContents(4, null);
            heater.updateEntity();
            assertEquals(3, heater.getStackInSlot(0).stackSize);
            assertTrue(heater.fuel > 0);
            heater.fuel = 0;
            furnace.setInventorySlotContents(0, null);
            heater.updateEntity();
            assertEquals("changed input lit more fuel", 3, heater.getStackInSlot(0).stackSize);
            furnace.setInventorySlotContents(0, new ItemStack(seed));
            heater.updateEntity();
            assertEquals(2, heater.getStackInSlot(0).stackSize);
            heater.heat = heater.maxHeat = 5000;
            tickUntil(() -> furnace.getStackInSlot(4) != null, 100, heater, furnace);
            assertTrue("valid work must still pay out", furnace.getStackInSlot(4) != null);
            assertSame(flour, furnace.getStackInSlot(4).getItem());
            assertTrue(furnace.getStackInSlot(0) == null);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void neighbouringForgesExchangeOnlyExcessHeat(GameTestHelper helper) {
        int sourceX = helper.absolute(1, 1, 1).x();
        try (Overrides overrides = new Overrides().air((world, x, y, z) -> x == sourceX ? 20F : 40F)) {
            helper.setBlock(1, 1, 1, BlockListMF.forge_metal_active);
            helper.setBlock(2, 1, 1, BlockListMF.forge_metal);
            TileEntityForge source = helper.assertTileEntityPresent(TileEntityForge.class, 1, 1, 1);
            TileEntityForge other = helper.assertTileEntityPresent(TileEntityForge.class, 2, 1, 1);
            source.temperature = source.fuelTemperature = 60F;
            source.fuel = 100;
            other.temperature = 40F;
            tick(source, 10);
            assertEquals("sharing reset a warm neighbour", 40F, other.temperature, 0F);
            assertEquals(2F, other.getWorkingHeat(), 0F);
            AmbientTemperature.setProvider((world, x, y, z) -> x == sourceX ? 20F : -15F);
            source.temperature = 20F;
            source.fuelTemperature = 0F;
            other.temperature = -15F;
            tick(source, 10);
            assertEquals("air generated heat in a freezing neighbour", -15F, other.temperature, 0F);
        }
        helper.succeed();
    }

    @GameTest
    public static void bellowsWorkOnALitForgeBelowZeroButNotAnUnlitOne(GameTestHelper helper) {
        try (Overrides overrides = new Overrides().air((world, x, y, z) -> -15F)) {
            helper.setBlock(2, 1, 2, BlockListMF.forge_metal_active);
            TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 2, 1, 2);
            forge.temperature = -15F;
            forge.fuelTemperature = 600F;
            forge.fuel = 100;
            forge.onUsedWithBellows(1F);
            assertTrue("a lit freezing forge rejected bellows", forge.temperature > -15F);
            helper.setBlock(2, 1, 2, BlockListMF.forge_metal);
            forge = helper.assertTileEntityPresent(TileEntityForge.class, 2, 1, 2);
            forge.temperature = -15F;
            forge.fuelTemperature = 600F;
            forge.fuel = 100;
            forge.onUsedWithBellows(1F);
            assertEquals("unlit stored fuel became heat", -15F, forge.temperature, 0F);
        }
        helper.succeed();
    }

}
