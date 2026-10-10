package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;

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
import minefantasy.mf2.block.decor.BlockTrough;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.fluid.FluidsMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.food.FoodListMF;
import minefantasy.mf2.item.list.ComponentListMF;

/** The trough holds one fluid at a time, water, seed oil or salt water, and salt turns its water to salt water. */
@GameTestHolder("minefantasy2")
public class TroughFluidTest {

    private TroughFluidTest() {}

    private static final ForgeDirection ANY = ForgeDirection.UNKNOWN;

    private static int count(FakePlayer player, net.minecraft.item.Item item) {
        int n = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && stack.getItem() == item) {
                n += stack.stackSize;
            }
        }
        return n;
    }

    @GameTest
    public static void theFluidsAreRegisteredByTheSharedNames(GameTestHelper helper) {
        assertSame(FluidsMF.seedOil, FluidRegistry.getFluid("seedoil"));
        assertSame(FluidsMF.saltWater, FluidRegistry.getFluid("saltwater"));
        FluidStack oil = FluidContainerRegistry.getFluidForFilledItem(new ItemStack(ComponentListMF.plant_oil));
        assertNotNull("a jug of oil is not a Forge container", oil);
        assertSame(FluidsMF.seedOil, oil.getFluid());
        assertEquals(250, oil.amount);
        helper.succeed();
    }

    @GameTest
    public static void oilIsPouredInAndTakenBackByTheJug(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = place(new TileEntityTrough());
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, new ItemStack(ComponentListMF.plant_oil));
            assertTrue(trough.interact(player, player.getHeldItem()));
            assertSame(FluidsMF.seedOil, trough.getFluid());
            assertEquals("a jug of oil is not four units", 4, trough.fill);
            assertSame(FoodListMF.jug_empty, player.getHeldItem().getItem());
            assertTrue(trough.interact(player, player.getHeldItem()));
            assertSame(
                    "the oil came back as something else",
                    ComponentListMF.plant_oil,
                    player.getHeldItem().getItem());
            assertEquals(0, trough.fill);
            assertSame("an emptied trough does not hold water again", FluidRegistry.WATER, trough.getFluid());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** Oil and water never mix: a trough of one takes none of the other, by hand or by pipe. */
    @GameTest
    public static void fluidsDoNotMix(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = place(new TileEntityTrough());
            assertEquals(500, trough.fill(ANY, new FluidStack(FluidsMF.seedOil, 500), true));
            assertEquals("water went into oil", 0, trough.fill(ANY, new FluidStack(FluidRegistry.WATER, 500), true));
            assertFalse(trough.canFill(ANY, FluidRegistry.WATER));
            assertNull("oil was given as water", trough.drain(ANY, new FluidStack(FluidRegistry.WATER, 100), true));
            assertSame(FluidsMF.seedOil, trough.drain(ANY, 100, false).getFluid());
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, new ItemStack(FoodListMF.jug_water));
            assertFalse("a jug of water went into oil", trough.interact(player, player.getHeldItem()));
            assertSame(FoodListMF.jug_water, player.getHeldItem().getItem());
            assertEquals(8, trough.fill);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** Neither the kitchen bench nor a jug drawing water takes oil for it. */
    @GameTest
    public static void oilIsNotWater(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            helper.setBlock(1, 1, 3, BlockListMF.trough_wood);
            TestPos at = helper.absolute(1, 1, 3);
            TileEntityTrough trough = (TileEntityTrough) world().getTileEntity(at.x(), at.y(), at.z());
            trough.fill(ANY, new FluidStack(FluidsMF.seedOil, 1000), true);
            assertFalse(
                    "a jug drew water from oil",
                    TongsHelper.drawWater(world(), at.x(), at.y(), at.z(), FluidsMF.JUG));
            assertEquals(16, trough.fill);
            TileEntityKitchenBench bench = place(new TileEntityKitchenBench());
            bench.setDirtyMax(50F);
            bench.dirtyProgress = 40F;
            assertEquals("oil washed the bench", 0, bench.fill(ANY, new FluidStack(FluidsMF.seedOil, 1000), true));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** One salt for each bucket of water, all at once, the pots handed back. */
    @GameTest
    public static void saltTurnsWaterToSaltWater(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = place(new TileEntityTrough());
            trough.fill(ANY, new FluidStack(FluidRegistry.WATER, 1500), true);
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, new ItemStack(FoodListMF.salt, 1));
            assertFalse("one salt salted a bucket and a half", trough.interact(player, player.getHeldItem()));
            assertEquals(1, player.getHeldItem().stackSize);
            assertSame(FluidRegistry.WATER, trough.getFluid());

            player.setCurrentItemOrArmor(0, new ItemStack(FoodListMF.salt, 3));
            assertTrue(trough.interact(player, player.getHeldItem()));
            assertSame(FluidsMF.saltWater, trough.getFluid());
            assertEquals("not two salts were used", 1, player.getHeldItem().stackSize);
            assertEquals("the pots were not handed back", 2, count(player, ComponentListMF.clay_pot));
            assertEquals("salting changed the amount", 1500, trough.drain(ANY, 5000, false).amount);

            assertFalse("salt water was salted again", trough.interact(player, player.getHeldItem()));
            assertEquals(1, player.getHeldItem().stackSize);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void saltDoesNothingToOilOrAnEmptyTrough(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = place(new TileEntityTrough());
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, new ItemStack(FoodListMF.salt, 4));
            assertFalse("an empty trough took salt", trough.interact(player, player.getHeldItem()));
            trough.fill(ANY, new FluidStack(FluidsMF.seedOil, 500), true);
            assertFalse("oil took salt", trough.interact(player, player.getHeldItem()));
            assertEquals(4, player.getHeldItem().stackSize);
            assertSame(FluidsMF.seedOil, trough.getFluid());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** The fluid is saved; one no mod registers now is kept, and the trough neither takes nor gives it. */
    @GameTest
    public static void theFluidIsSavedEvenWhenUnknown(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTrough trough = place(new TileEntityTrough());
            trough.fill(ANY, new FluidStack(FluidsMF.saltWater, 700), true);
            NBTTagCompound nbt = new NBTTagCompound();
            trough.writeToNBT(nbt);
            TileEntityTrough loaded = place(new TileEntityTrough());
            loaded.readFromNBT(nbt);
            assertSame(FluidsMF.saltWater, loaded.getFluid());
            assertEquals(700, loaded.drain(ANY, 5000, false).amount);

            nbt.setString("fillFluid", "minefantasy_no_such_fluid");
            TileEntityTrough unknown = place(new TileEntityTrough());
            unknown.readFromNBT(nbt);
            assertNull(unknown.drain(ANY, 5000, true));
            assertEquals(
                    "water went into an unknown fluid",
                    0,
                    unknown.fill(ANY, new FluidStack(FluidRegistry.WATER, 100), true));
            NBTTagCompound again = new NBTTagCompound();
            unknown.writeToNBT(again);
            assertEquals("the unknown fluid was lost", "minefantasy_no_such_fluid", again.getString("fillFluid"));
            assertTrue("its amount was lost", again.getInteger("fill") > 0);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** A trough item keeps its fluid when placed, and is not topped up with water while it holds another. */
    @GameTest
    public static void aTroughItemKeepsItsFluid(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            helper.setBlock(1, 1, 3, BlockListMF.trough_wood);
            TestPos at = helper.absolute(1, 1, 3);
            ItemStack item = new ItemStack(BlockListMF.trough_wood);
            item.setTagCompound(new NBTTagCompound());
            item.getTagCompound().setInteger(BlockTrough.NBT_fill, 8);
            item.getTagCompound().setString(BlockTrough.NBT_fluid, FluidsMF.seedOil.getName());
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            BlockListMF.trough_wood.onBlockPlacedBy(world(), at.x(), at.y(), at.z(), player, item);
            TileEntityTrough trough = (TileEntityTrough) world().getTileEntity(at.x(), at.y(), at.z());
            assertSame("the placed trough lost its oil", FluidsMF.seedOil, trough.getFluid());
            assertEquals(8, trough.fill);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
