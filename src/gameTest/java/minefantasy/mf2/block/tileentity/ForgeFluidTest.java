package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.heating.TongsHelper;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.fluid.FluidsMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.food.FoodListMF;

/**
 * The trough and the kitchen bench as Forge tanks, met the way pipes meet them, and water drawn and quenched from any
 * tank or a cauldron.
 */
@GameTestHolder("minefantasy2")
public class ForgeFluidTest {

    private ForgeFluidTest() {}

    private static final ForgeDirection ANY = ForgeDirection.UNKNOWN;

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.WATER, amount);
    }

    private static int amount(FluidStack stack) {
        return stack == null ? 0 : stack.amount;
    }

    private static TileEntityTrough placedTrough(GameTestHelper helper) {
        helper.setBlock(1, 1, 3, BlockListMF.trough_wood);
        TestPos at = helper.absolute(1, 1, 3);
        return (TileEntityTrough) world().getTileEntity(at.x(), at.y(), at.z());
    }

    // region trough

    @GameTest
    public static void aPipeFillsAndDrainsTheTroughExactly(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = place(new TileEntityTrough());
            assertEquals("a simulated fill was not offered", 100, trough.fill(ANY, water(100), false));
            assertNull("a simulated fill changed the trough", trough.getTankInfo(ANY)[0].fluid);
            for (int i = 0; i < 10; i++) {
                assertEquals(10, trough.fill(ANY, water(10), true));
            }
            assertEquals("100 mB is not one unit and a remainder", 1, trough.fill);
            assertEquals(100, amount(trough.getTankInfo(ANY)[0].fluid));
            assertEquals("a simulated drain", 100, amount(trough.drain(ANY, 1000, false)));
            assertEquals(100, amount(trough.drain(ANY, water(1000), true)));
            assertNull("an empty trough gave water", trough.drain(ANY, 1, true));
            assertEquals(0, trough.fill);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void theTroughTakesAndGivesOnlyWater(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = place(new TileEntityTrough());
            trough.fill = 16;
            assertEquals("lava was taken", 0, trough.fill(ANY, new FluidStack(FluidRegistry.LAVA, 1000), true));
            assertNull("water was given as lava", trough.drain(ANY, new FluidStack(FluidRegistry.LAVA, 1000), true));
            assertEquals(0, trough.fill(ANY, null, true));
            assertEquals(0, trough.fill(ANY, water(-5), true));
            assertNull(trough.drain(ANY, -5, true));
            assertFalse(trough.canFill(ANY, FluidRegistry.LAVA));
            assertEquals(16, trough.fill);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aFullTroughTakesOnlyItsRoom(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = place(new TileEntityTrough());
            int capacity = trough.getTankInfo(ANY)[0].capacity;
            assertEquals(capacity, trough.fill(ANY, water(capacity + 500), true));
            assertEquals(trough.getCapacity(), trough.fill);
            assertEquals("a full trough took water", 0, trough.fill(ANY, water(1), true));
            assertEquals("a full trough is not at 15", 15, trough.comparatorSignal());
            trough.drain(ANY, capacity, true);
            assertEquals("an empty trough gives a signal", 0, trough.comparatorSignal());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** Water a pipe poured in marks the chunk for saving and is saved to the drop. */
    @GameTest
    public static void pipedWaterIsSaved(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = placedTrough(helper);
            world().getChunkFromBlockCoords(trough.xCoord, trough.zCoord).isModified = false;
            trough.fill(ANY, water(1100), true);
            assertTrue(
                    "piped water did not mark the chunk for saving",
                    world().getChunkFromBlockCoords(trough.xCoord, trough.zCoord).isModified);
            NBTTagCompound nbt = new NBTTagCompound();
            trough.writeToNBT(nbt);
            TileEntityTrough loaded = new TileEntityTrough();
            loaded.readFromNBT(nbt);
            assertEquals(1100, amount(loaded.getTankInfo(ANY)[0].fluid));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** A jug is a Forge container of 250 mB: four trough units, as four jugs are filled from one bucket. */
    @GameTest
    public static void aJugIsAQuarterBucket(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            FluidStack inside = FluidContainerRegistry.getFluidForFilledItem(new ItemStack(FoodListMF.jug_water));
            assertNotNull("the jug is not a Forge container", inside);
            assertEquals(250, inside.amount);
            TileEntityTrough trough = place(new TileEntityTrough());
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, new ItemStack(FoodListMF.jug_water));
            assertTrue(trough.interact(player, player.getHeldItem()));
            assertEquals("a jug is not four units", 4, trough.fill);
            assertSame(FoodListMF.jug_empty, player.getHeldItem().getItem());
            assertTrue(trough.interact(player, player.getHeldItem()));
            assertSame(FoodListMF.jug_water, player.getHeldItem().getItem());
            assertEquals(0, trough.fill);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region kitchen bench

    @GameTest
    public static void aPipeWashesTheBenchOnlyWhileDirty(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = place(new TileEntityKitchenBench());
            bench.setDirtyMax(50F);
            assertEquals("a clean bench took water", 0, bench.fill(ANY, water(1000), true));
            bench.dirtyProgress = 80F;
            assertEquals("a simulated fill", 500, bench.fill(ANY, water(1000), false));
            assertEquals("a simulated fill washed", 80F, bench.dirtyProgress, 0F);
            assertEquals("a partial wash was refused", 249, bench.fill(ANY, water(249), true));
            assertEquals("a partial wash cleaned the bench", 80F, bench.dirtyProgress, 0F);
            assertEquals("lava washed", 0, bench.fill(ANY, new FluidStack(FluidRegistry.LAVA, 1000), true));
            assertEquals("more than cleans it was taken", 251, bench.fill(ANY, water(1000), true));
            assertEquals(0F, bench.dirtyProgress, 0F);
            assertNull("the bench gave water", bench.drain(ANY, 1000, true));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    @GameTest
    public static void smallPipedPortionsAccumulateAndSave(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = place(new TileEntityKitchenBench());
            world().setTileEntity(bench.xCoord, bench.yCoord, bench.zCoord, bench);
            bench.dirtyProgress = 80F;
            world().getChunkFromBlockCoords(bench.xCoord, bench.zCoord).isModified = false;
            assertEquals(100, bench.fill(ANY, water(100), false));
            assertNull(bench.getTankInfo(ANY)[0].fluid);
            assertFalse(world().getChunkFromBlockCoords(bench.xCoord, bench.zCoord).isModified);
            assertEquals(100, bench.fill(ANY, water(100), true));
            assertTrue(world().getChunkFromBlockCoords(bench.xCoord, bench.zCoord).isModified);
            NBTTagCompound saved = new NBTTagCompound();
            bench.writeToNBT(saved);
            TileEntityKitchenBench loaded = place(new TileEntityKitchenBench());
            loaded.readFromNBT(saved);
            assertEquals(100, amount(loaded.getTankInfo(ANY)[0].fluid));
            assertEquals(100, loaded.fill(ANY, water(100), true));
            assertEquals(80F, loaded.dirtyProgress, 0F);
            assertEquals(100, loaded.fill(ANY, water(100), true));
            assertTrue("accumulated water did not wash", loaded.dirtyProgress < 80F);
            int accepted = 300;
            for (int i = 0; i < 100; i++) {
                accepted += loaded.fill(ANY, water(100), true);
            }
            assertEquals("the pipe overpaid", 500, accepted);
            assertEquals(0F, loaded.dirtyProgress, 0F);
            assertNull(loaded.getTankInfo(ANY)[0].fluid);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // region drawing and quenching

    @GameTest
    public static void aJugDrawsAQuarterBucketFromATank(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = placedTrough(helper);
            trough.fill = 6;
            int x = trough.xCoord, y = trough.yCoord, z = trough.zCoord;
            assertTrue(TongsHelper.drawWater(world(), x, y, z, FluidsMF.JUG));
            assertEquals("a jug did not take four units", 2, trough.fill);
            assertFalse("a jug was filled from too little", TongsHelper.drawWater(world(), x, y, z, 250));
            assertEquals(2, trough.fill);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aCauldronIsNotEndless(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TestPos at = helper.absolute(2, 1, 3);
            world().setBlock(at.x(), at.y(), at.z(), Blocks.cauldron, 3, 2);
            assertTrue(TongsHelper.drawWater(world(), at.x(), at.y(), at.z(), 250));
            assertEquals("drawing left the cauldron full", 2, world().getBlockMetadata(at.x(), at.y(), at.z()));
            assertTrue(TongsHelper.getWaterSource(world(), at.x(), at.y(), at.z()) >= 0);
            assertEquals("quenching left the cauldron as it was", 1, world().getBlockMetadata(at.x(), at.y(), at.z()));
            TongsHelper.getWaterSource(world(), at.x(), at.y(), at.z());
            assertFalse("an empty cauldron gave water", TongsHelper.drawWater(world(), at.x(), at.y(), at.z(), 250));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** A jug filled from a water block leaves it, as a bottle does; quenching still boils it away. */
    @GameTest
    public static void aJugLeavesTheWaterBlock(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TestPos at = helper.absolute(3, 1, 3);
            world().setBlock(at.x(), at.y(), at.z(), Blocks.water, 0, 2);
            assertTrue(TongsHelper.drawWater(world(), at.x(), at.y(), at.z(), 250));
            assertSame("the jug took the water block", Blocks.water, world().getBlock(at.x(), at.y(), at.z()));
            assertTrue(TongsHelper.getWaterSource(world(), at.x(), at.y(), at.z()) >= 0);
            assertSame(Blocks.air, world().getBlock(at.x(), at.y(), at.z()));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion
}
