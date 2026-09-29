package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityHopper;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeMetadata;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.item.list.ComponentListMF;

/**
 * Hoppers, through the vanilla code: one above a station puts only into the slots that take input, and one below takes
 * only what the station gives out, never its inputs, and never a result it only shows.
 */
@GameTestHolder("minefantasy2")
public class HopperTest {

    private HopperTest() {}

    private static <T extends TileEntity & IInventory> T station(GameTestHelper helper, Block block, Class<T> type) {
        helper.setBlock(1, 2, 1, block);
        return helper.assertTileEntityPresent(type, 1, 2, 1);
    }

    /** A hopper under the station; returns what it pulled in one go, or null. */
    private static ItemStack pull(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.hopper);
        TileEntityHopper hopper = helper.assertTileEntityPresent(TileEntityHopper.class, 1, 1, 1);
        TileEntityHopper.func_145891_a(hopper);
        for (int i = 0; i < hopper.getSizeInventory(); i++) {
            if (hopper.getStackInSlot(i) != null) return hopper.getStackInSlot(i);
        }
        return null;
    }

    /** A hopper above puts the stack in; returns what did not fit. */
    private static ItemStack push(IInventory station, ItemStack stack) {
        return TileEntityHopper.func_145889_a(station, stack, 0);
    }

    /** Fills every input slot, then pushes one more: it must not land in the output. */
    private static void pushNeverReachesTheOutput(IInventory station, int output, int inputs) {
        for (int slot = 0; slot < inputs; slot++) {
            station.setInventorySlotContents(slot, new ItemStack(Blocks.stone, 64));
        }
        push(station, new ItemStack(Items.coal, 3));
        assertNull(station.getClass().getSimpleName() + ": a hopper filled the output", station.getStackInSlot(output));
    }

    /** An input and an output are both filled; a hopper below takes the output and leaves the input. */
    private static void pullTakesOnlyTheOutput(GameTestHelper helper, IInventory station, int output) {
        for (int slot = 0; slot < station.getSizeInventory(); slot++) {
            station.setInventorySlotContents(slot, null);
        }
        station.setInventorySlotContents(0, new ItemStack(Blocks.stone, 5));
        station.setInventorySlotContents(output, new ItemStack(bar, 2));
        ItemStack pulled = pull(helper);
        assertNotNull(station.getClass().getSimpleName() + ": the output was not taken", pulled);
        assertEquals(station.getClass().getSimpleName() + ": the input was taken", bar, pulled.getItem());
        assertEquals(5, station.getStackInSlot(0).stackSize);
    }

    @GameTest
    public static void anvil(GameTestHelper helper) {
        TileEntityAnvilMF anvil = station(helper, BlockListMF.anvilStone, TileEntityAnvilMF.class);
        int output = anvil.getSizeInventory() - 1;
        pushNeverReachesTheOutput(anvil, output, output);
        pullTakesOnlyTheOutput(helper, anvil, output);
        helper.succeed();
    }

    @GameTest
    public static void carpenter(GameTestHelper helper) {
        TileEntityCarpenterMF bench = station(helper, BlockListMF.carpenter, TileEntityCarpenterMF.class);
        int output = bench.getSizeInventory() - 5;
        pushNeverReachesTheOutput(bench, output, output);
        pullTakesOnlyTheOutput(helper, bench, output);
        helper.succeed();
    }

    @GameTest
    public static void kitchenBench(GameTestHelper helper) {
        TileEntityKitchenBench bench = station(helper, BlockListMF.kitchenBench, TileEntityKitchenBench.class);
        int output = bench.getSizeInventory() - 5;
        pushNeverReachesTheOutput(bench, output, output);
        pullTakesOnlyTheOutput(helper, bench, output);
        helper.succeed();
    }

    @GameTest
    public static void quern(GameTestHelper helper) {
        TileEntityQuern quern = station(helper, BlockListMF.quern, TileEntityQuern.class);
        quern.setInventorySlotContents(0, null);
        push(quern, new ItemStack(ComponentListMF.clay_pot, 2));
        assertNull("a hopper filled the output", quern.getStackInSlot(2));
        assertNotNull("the pot did not go in", quern.getStackInSlot(1));
        quern.setInventorySlotContents(2, new ItemStack(bar));
        ItemStack pulled = pull(helper);
        assertEquals("the pot was taken instead of the output", bar, pulled.getItem());
        helper.succeed();
    }

    /** The rack shows the result of the hide it works in slot 1; that is no item, and no hopper may take it. */
    @GameTest
    public static void aTanningRackGivesNoShownResult(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.TANNING,
                            id("tanning", "hopper"),
                            ProcessRecipe.of(
                                    Input.of(seed),
                                    new ItemStack(flour),
                                    RecipeMetadata.builder().put(MFRecipeKeys.TIME, 50F).build()),
                            0));
            TileEntityTanningRack rack = station(helper, BlockListMF.tanner, TileEntityTanningRack.class);
            rack.setInventorySlotContents(0, new ItemStack(seed));
            rack.updateRecipe();
            assertNotNull("the rack shows no result", rack.getStackInSlot(1));
            for (int pull = 0; pull < 3; pull++) {
                ItemStack pulled = pull(helper);
                assertTrue("a hopper took the shown result", pulled == null || pulled.getItem() != flour);
                rack.updateRecipe();
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
