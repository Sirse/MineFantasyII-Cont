package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.heating.AmbientTemperature;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;

/** Issue #79: furnaces smelt only from fuel they burn, and only what they hold. */
@GameTestHolder("minefantasy2")
public class FurnaceIdleTest {

    private FurnaceIdleTest() {}

    /** A heater and a furnace above it, walled in firebrick, facing north. */
    private static TileEntityBigFurnace builtFurnace(GameTestHelper helper) {
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
        AmbientTemperature.setProvider((world, x, y, z) -> y == heaterY ? 25F : 20F);
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
            try {
                TileEntityBigFurnace now = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 2, 2);
                TileEntityBigFurnace heater = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 1, 2);
                assertTrue("the furnace was never built: heater " + heater.built, now.built);
                assertEquals("a cold heater made progress", 0, now.progress);
                return true;
            } finally {
                AmbientTemperature.setProvider(null);
            }
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
}
