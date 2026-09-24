package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityHopper;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;

/**
 * Vanilla hoppers feeding and emptying stations: each station takes only what belongs in a slot and gives out only its
 * products, whatever side the hopper is on.
 */
@GameTestHolder("minefantasy2")
public class AutomationTest {

    /** Hopper metadata: pointing down, or sideways towards +x. */
    private static final int DOWN = 0;
    private static final int EAST = 5;

    private AutomationTest() {}

    private static TileEntityHopper hopper(GameTestHelper helper, int x, int y, int z, int facing,
            ItemStack... contents) {
        helper.setBlock(x, y, z, Blocks.hopper, facing);
        TileEntityHopper hopper = helper.assertTileEntityPresent(TileEntityHopper.class, x, y, z);
        for (int i = 0; i < contents.length; i++) {
            hopper.setInventorySlotContents(i, contents[i]);
        }
        return hopper;
    }

    private static int count(net.minecraft.inventory.IInventory inventory, net.minecraft.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && stack.getItem() == item) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    // region crucible

    @GameTest(timeoutTicks = 100)
    public static void hopperFeedsTheCrucibleGridNotItsOutput(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.crucible);
        TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 1, 1);
        hopper(helper, 1, 2, 1, DOWN, new ItemStack(ore, 2));
        int output = crucible.getSizeInventory() - 1;
        helper.succeedWhen(() -> {
            if (count(crucible, ore) < 2) {
                return false;
            }
            assertNull("the hopper filled the output", crucible.getStackInSlot(output));
            return true;
        });
    }

    @GameTest(timeoutTicks = 60)
    public static void hopperCannotEmptyAManualCrucible(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 2, 1, BlockListMF.crucible);
        TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 2, 1);
        crucible.setInventorySlotContents(crucible.getSizeInventory() - 1, new ItemStack(bar));
        TileEntityHopper below = hopper(helper, 1, 1, 1, EAST);
        helper.onEachTick("the bar stays", () -> assertEquals(0, count(below, bar)));
        helper.succeedAtTimeout();
    }

    @GameTest(timeoutTicks = 100)
    public static void hopperEmptiesAnAutomaticCrucible(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 2, 1, BlockListMF.crucibleauto);
        TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 2, 1);
        crucible.setInventorySlotContents(crucible.getSizeInventory() - 1, new ItemStack(bar));
        crucible.setInventorySlotContents(0, new ItemStack(ore));
        TileEntityHopper below = hopper(helper, 1, 1, 1, EAST);
        helper.succeedWhen(() -> {
            if (count(below, bar) < 1) {
                return false;
            }
            assertEquals("the grid stays", 0, count(below, ore));
            return true;
        });
    }

    // endregion

    // region big furnace

    private static TileEntityBigFurnace bigFurnace(GameTestHelper helper) {
        reload(
                tx -> tx.add(
                        MFRecipes.BIG_FURNACE,
                        id("big_furnace", "seed"),
                        ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                        0));
        helper.setBlock(2, 1, 2, BlockListMF.furnace_heater, 0);
        helper.setBlock(2, 2, 2, BlockListMF.furnace_stone, 0);
        return helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 2, 2);
    }

    @GameTest(timeoutTicks = 100)
    public static void hopperFeedsTheBigFurnaceOnlyWhatItSmelts(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        TileEntityBigFurnace furnace = bigFurnace(helper);
        TileEntityHopper hopper = hopper(helper, 2, 3, 2, DOWN, new ItemStack(junk), new ItemStack(seed, 2));
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> {
            if (count(furnace, seed) < 2) {
                return false;
            }
            assertEquals("the furnace took what it cannot smelt", 1, count(hopper, junk));
            for (int slot = 4; slot < 8; slot++) {
                assertNull("the hopper filled an output", furnace.getStackInSlot(slot));
            }
            return true;
        });
    }

    @GameTest(timeoutTicks = 100)
    public static void bigFurnaceHeaterTakesOnlyFuel(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        bigFurnace(helper);
        TileEntityBigFurnace heater = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 1, 2);
        assertTrue("coal is not fuel for the heater", heater.isItemFuel(new ItemStack(Items.coal)));
        TileEntityHopper hopper = hopper(helper, 1, 1, 2, EAST, new ItemStack(seed), new ItemStack(Items.coal));
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> {
            if (count(hopper, Items.coal) > 0) {
                return false;
            }
            assertEquals("the heater took what does not burn", 1, count(hopper, seed));
            return true;
        });
    }

    // endregion

    // region blast furnace

    @GameTest(timeoutTicks = 100)
    public static void hopperFeedsTheBlastChamberCarbonAndOre(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        reload(
                tx -> tx.add(
                        MFRecipes.BLAST_FURNACE,
                        id("blast_furnace", "ore"),
                        ProcessRecipe.of(Input.of(ore), new ItemStack(bar)),
                        0));
        helper.setBlock(1, 1, 1, BlockListMF.blast_chamber);
        TileEntityBlastFC chamber = helper.assertTileEntityPresent(TileEntityBlastFC.class, 1, 1, 1);
        TileEntityHopper hopper = hopper(
                helper,
                1,
                2,
                1,
                DOWN,
                new ItemStack(junk),
                new ItemStack(carbon),
                new ItemStack(ore));
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> {
            if (chamber.getStackInSlot(0) == null || chamber.getStackInSlot(1) == null) {
                return false;
            }
            assertEquals("carbon goes to the carbon slot", carbon, chamber.getStackInSlot(0).getItem());
            assertEquals("ore goes to the input slot", ore, chamber.getStackInSlot(1).getItem());
            assertEquals("the chamber took what it cannot use", 1, count(hopper, junk));
            return true;
        });
    }

    @GameTest(timeoutTicks = 60)
    public static void nothingComesOutOfTheBlastFurnace(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 2, 1, BlockListMF.blast_chamber);
        TileEntityBlastFC chamber = helper.assertTileEntityPresent(TileEntityBlastFC.class, 1, 2, 1);
        chamber.setInventorySlotContents(0, new ItemStack(carbon));
        TileEntityHopper below = hopper(helper, 1, 1, 1, EAST);
        helper.onEachTick("the carbon stays", () -> assertEquals(0, count(below, carbon)));
        helper.succeedAtTimeout();
    }

    @GameTest(timeoutTicks = 100)
    public static void blastHeaterTakesFuelFromTheSideOnly(GameTestHelper helper) throws Exception {
        helper.setBlock(2, 1, 1, BlockListMF.blast_heater);
        TileEntityBlastFH heater = helper.assertTileEntityPresent(TileEntityBlastFH.class, 2, 1, 1);
        assertTrue("coal is not fuel for the blast heater", TileEntityBlastFH.isFuel(new ItemStack(Items.coal)));
        TileEntityHopper side = hopper(helper, 1, 1, 1, EAST, new ItemStack(seed), new ItemStack(Items.coal));
        helper.succeedWhen(() -> {
            if (count(side, Items.coal) > 0) {
                return false;
            }
            assertEquals("the heater took what does not burn", 1, count(side, seed));
            return true;
        });
    }

    // endregion
}
