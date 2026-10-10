package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.gametest.Modders;

/**
 * Water from any container Forge knows, on the kitchen bench and the trough: registry pairs emptied whole, tanks that
 * keep their own fluid drained by the millibucket, one item of a stack at a time, nothing used up in creative mode.
 */
@GameTestHolder("minefantasy2")
public class FluidContainerTest {

    private FluidContainerTest() {}

    private static ItemStack tank(int water) {
        ItemStack tank = new ItemStack(minefantasy.mf2.gametest.TestItems.tank);
        if (water > 0) {
            ((IFluidContainerItem) tank.getItem()).fill(tank, new FluidStack(FluidRegistry.WATER, water), true);
        }
        return tank;
    }

    private static int water(ItemStack tank) {
        FluidStack inside = ((IFluidContainerItem) tank.getItem()).getFluid(tank);
        return inside == null ? 0 : inside.amount;
    }

    private static TileEntityKitchenBench dirtyBench() {
        TileEntityKitchenBench bench = place(new TileEntityKitchenBench());
        bench.setDirtyMax(50F);
        bench.dirtyProgress = 40F;
        return bench;
    }

    private static int count(FakePlayer player, net.minecraft.item.Item item) {
        int n = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && stack.getItem() == item) {
                n += stack.stackSize;
            }
        }
        return n;
    }

    // region kitchen bench

    @GameTest
    public static void aTankGivesTheBenchWhatCleansIt(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = dirtyBench();
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, tank(4000));
            bench.interact(player);
            assertEquals("the tank did not clean the bench", 0F, bench.dirtyProgress, 0F);
            assertEquals("the tank did not give one wash, 250 mB", 3750, water(player.getHeldItem()));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aNearlyEmptyTankDoesNotWash(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = dirtyBench();
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, tank(100));
            bench.interact(player);
            assertEquals("a trickle washed the bench", 40F, bench.dirtyProgress, 0F);
            assertEquals("the trickle was taken", 100, water(player.getHeldItem()));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** One tank of a stack is drained and handed back; the rest stay full in the hand. */
    @GameTest
    public static void oneTankOfAStackIsDrained(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = dirtyBench();
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            ItemStack stack = tank(2000);
            stack.stackSize = 2;
            player.setCurrentItemOrArmor(0, stack);
            bench.interact(player);
            assertEquals("not one tank was taken off the stack", 1, player.getHeldItem().stackSize);
            assertEquals("the tank left in hand was drained", 2000, water(player.getHeldItem()));
            boolean handedBack = false;
            for (int i = 0; i < player.inventory.mainInventory.length; i++) {
                ItemStack other = player.inventory.mainInventory[i];
                if (i != player.inventory.currentItem && other != null && other.getItem() == stack.getItem()) {
                    handedBack = water(other) == 1750;
                }
            }
            assertTrue("the drained tank was not handed back with 1750 mB", handedBack);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** A registry cell is emptied whole; from a stack, the empty cell goes to the inventory. */
    @GameTest
    public static void aRegistryCellIsEmptiedWhole(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = dirtyBench();
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, new ItemStack(cell, 3));
            bench.interact(player);
            assertTrue("the cell did not wash", bench.dirtyProgress < 40F);
            assertEquals(2, player.getHeldItem().stackSize);
            assertEquals("the empty cell was lost", 1, count(player, emptyCell));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void creativeWaterIsNotUsedUp(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = dirtyBench();
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.capabilities.isCreativeMode = true;
            player.setCurrentItemOrArmor(0, tank(4000));
            bench.interact(player);
            assertTrue("creative water did not wash", bench.dirtyProgress < 40F);
            assertEquals("creative water was used up", 4000, water(player.getHeldItem()));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region trough

    private static TileEntityTrough trough(int fill) {
        TileEntityTrough trough = place(new TileEntityTrough());
        trough.fill = fill;
        return trough;
    }

    @GameTest
    public static void aTankPoursABucketsWorthAsSixteenUnits(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = trough(0);
            int room = trough.getCapacity();
            assertTrue("the trough holds less than a bucket", room >= 16);
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, tank(1000));
            assertTrue(trough.interact(player, player.getHeldItem()));
            assertEquals("a bucket's worth is not 16 units", 16, trough.fill);
            assertEquals("the tank kept water", 0, water(player.getHeldItem()));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aTankIsFilledFromTheTrough(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = trough(32);
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, tank(0));
            assertTrue(trough.interact(player, player.getHeldItem()));
            assertEquals("the trough was not emptied into the tank", 0, trough.fill);
            assertEquals("32 units are not two buckets", 2000, water(player.getHeldItem()));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aRegistryCellIsFilledFromTheTrough(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = trough(16);
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, new ItemStack(emptyCell));
            assertTrue(trough.interact(player, player.getHeldItem()));
            assertSame(cell, player.getHeldItem().getItem());
            assertEquals("500 mB is not 8 units", 8, trough.fill);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** The old measures hold: a bucket pours 16 units and leaves an empty bucket. */
    @GameTest
    public static void aBucketStillPoursSixteenUnits(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = trough(0);
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, new ItemStack(Items.water_bucket));
            assertTrue(trough.interact(player, player.getHeldItem()));
            assertEquals(16, trough.fill);
            assertSame(Items.bucket, player.getHeldItem().getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    /** Water carried back and forth through the trough is neither made nor lost, a drop short of a unit included. */
    @GameTest
    public static void theTroughKeepsWaterExact(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = trough(0);
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, tank(1000));
            trough.interact(player, player.getHeldItem());
            player.setCurrentItemOrArmor(0, new ItemStack(Items.bucket));
            assertTrue("a bucket's worth does not fill a bucket", trough.interact(player, player.getHeldItem()));
            assertSame(Items.water_bucket, player.getHeldItem().getItem());
            assertEquals("water was made", 0, trough.fill);
            player.setCurrentItemOrArmor(0, tank(0));
            assertFalse("water was made from nothing", trough.interact(player, player.getHeldItem()));

            player.setCurrentItemOrArmor(0, tank(100));
            trough.interact(player, player.getHeldItem());
            assertEquals("100 mB is not one unit and a remainder", 1, trough.fill);
            player.setCurrentItemOrArmor(0, tank(0));
            trough.interact(player, player.getHeldItem());
            assertEquals("the remainder was lost or made", 100, water(player.getHeldItem()));
            assertEquals(0, trough.fill);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** A bucket that would overflow the trough is refused, not partly lost. */
    @GameTest
    public static void aBucketThatDoesNotFitIsRefused(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = place(new TileEntityTrough());
            trough.fill = trough.getCapacity() - 1;
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, new ItemStack(Items.water_bucket));
            assertFalse(trough.interact(player, player.getHeldItem()));
            assertSame(Items.water_bucket, player.getHeldItem().getItem());
            assertEquals(trough.getCapacity() - 1, trough.fill);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
