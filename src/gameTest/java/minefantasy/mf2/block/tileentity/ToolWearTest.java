package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeMetadata;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.CustomToolListMF;

/** Using a station wears the held tool, and only a tool: what has no durability is never destroyed (issue 78). */
@GameTestHolder("minefantasy2")
public class ToolWearTest {

    private ToolWearTest() {}

    /** A kitchen bench whose recipe asks for a spoon, as it stays after its first craft. */
    private static TileEntityKitchenBench spoonBench() {
        reload(
                tx -> tx.add(
                        MFRecipes.KITCHEN,
                        id("kitchen", "spooned"),
                        GridRecipe.shapeless(
                                GridRecipe.Grid.BENCH,
                                new Object[] { new ItemStack(seed) },
                                null,
                                new ItemStack(flour)).tool("spoon", 0).time(100).build(),
                        0));
        TileEntityKitchenBench bench = place(new TileEntityKitchenBench());
        bench.setDirtyMax(50F);
        bench.setInventorySlotContents(0, new ItemStack(seed));
        bench.updateCraftingData();
        return bench;
    }

    @GameTest
    public static void aKitchenBenchNeverDestroysWhatHasNoDurability(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = spoonBench();
            assertEquals("the bench does not ask for a spoon", "spoon", bench.getToolNeeded());
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            for (ItemStack held : new ItemStack[] { new ItemStack(Items.bread, 5), new ItemStack(Items.iron_chestplate),
                    new ItemStack(Items.cake), new ItemStack(Items.glass_bottle, 3) }) {
                ItemStack before = held.copy();
                player.setCurrentItemOrArmor(0, held);
                bench.interact(player);
                ItemStack after = player.getHeldItem();
                assertNotNull(before + " was destroyed", after);
                assertTrue(before + " changed to " + after, ItemStack.areItemStacksEqual(before, after));
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aKitchenBenchWearsItsSpoon(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = spoonBench();
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            ItemStack spoon = new ItemStack(CustomToolListMF.standard_spoon);
            player.setCurrentItemOrArmor(0, spoon);
            bench.interact(player);
            assertSame("the spoon was replaced", spoon, player.getHeldItem());
            assertEquals("the spoon did not wear", 1, spoon.getItemDamage());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** A rack asking for hands, worked with an empty hand, wears nothing and does not crash. */
    @GameTest
    public static void aTanningRackWorkedByHandWearsNothing(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.TANNING,
                            id("tanning", "by_hand"),
                            ProcessRecipe.of(
                                    Input.of(seed),
                                    new ItemStack(flour),
                                    RecipeMetadata.builder().put(MFRecipeKeys.TIME, 50F).put(MFRecipeKeys.TOOL, "hands")
                                            .build()),
                            0));
            TileEntityTanningRack rack = place(new TileEntityTanningRack());
            rack.setInventorySlotContents(0, new ItemStack(seed));
            rack.updateRecipe();
            assertEquals("hands", rack.toolType);
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.setCurrentItemOrArmor(0, null);
            rack.interact(player, false, false);
            assertNull(player.getHeldItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** Water on a clean bench after a craft is kept: a bucket or bottle is neither a spoon nor used up. */
    @GameTest
    public static void waterOnACleanBenchIsKept(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = spoonBench();
            bench.dirtyProgress = 0F;
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            for (ItemStack held : new ItemStack[] { new ItemStack(Items.water_bucket),
                    new ItemStack(Items.potionitem, 3, 0) }) {
                ItemStack before = held.copy();
                player.setCurrentItemOrArmor(0, held);
                bench.interact(player);
                ItemStack after = player.getHeldItem();
                assertNotNull(before + " was destroyed", after);
                assertTrue(before + " changed to " + after, ItemStack.areItemStacksEqual(before, after));
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** Water on a dirty bench washes it and leaves the empty container, never nothing. */
    @GameTest
    public static void waterWashesADirtyBenchAndLeavesTheContainer(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = spoonBench();
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);

            bench.dirtyProgress = 40F;
            player.setCurrentItemOrArmor(0, new ItemStack(Items.water_bucket));
            bench.interact(player);
            assertTrue("the bucket did not wash", bench.dirtyProgress < 40F);
            assertNotNull("the bucket was destroyed", player.getHeldItem());
            assertSame("the bucket was not emptied", Items.bucket, player.getHeldItem().getItem());

            bench.dirtyProgress = 40F;
            player.setCurrentItemOrArmor(0, new ItemStack(Items.potionitem, 2, 0));
            bench.interact(player);
            assertTrue("the bottle did not wash", bench.dirtyProgress < 40F);
            assertEquals("not one bottle was used", 1, player.getHeldItem().stackSize);
            assertTrue("the empty bottle was lost", player.inventory.hasItem(Items.glass_bottle));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
