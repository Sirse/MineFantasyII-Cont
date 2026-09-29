package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.Usage;

/**
 * Recipes driven through the real stations: what the inventory holds after a craft, a reload in the middle of the work,
 * a save loaded in the middle of the work, and returned containers with no room.
 */
@GameTestHolder("minefantasy2")
public class StationBoundaryTest {

    private static int size(ItemStack stack) {
        return stack == null ? 0 : stack.stackSize;
    }

    // region quern

    private static TileEntityQuern quern(ItemStack input, ItemStack pot, ItemStack output) {
        reload(tx -> {
            tx.add(MFRecipes.QUERN, id("quern", "flour"), ProcessRecipe.of(Input.of(seed), new ItemStack(flour)), 0);
            tx.add(
                    MFRecipes.QUERN,
                    id("quern", "pair"),
                    ProcessRecipe.of(Input.of(ore).amount(2), new ItemStack(bar)),
                    0);
        });
        TileEntityQuern quern = new TileEntityQuern();
        place(quern);
        quern.setInventorySlotContents(0, input);
        quern.setInventorySlotContents(1, pot);
        quern.setInventorySlotContents(2, output);
        return quern;
    }

    @GameTest
    public static void quernUsesOnePotPerGrindNotTheStack(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityQuern quern = quern(new ItemStack(seed, 3), new ItemStack(pot, 16), null);
            assertTrue(quern.onRevolutionComplete());
            assertEquals(2, size(quern.getStackInSlot(0)));
            assertEquals(15, size(quern.getStackInSlot(1)));
            assertEquals(1, size(quern.getStackInSlot(2)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void quernTakesTheRecipeAmount(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityQuern quern = quern(new ItemStack(ore, 3), new ItemStack(pot, 4), null);
            assertTrue(quern.onRevolutionComplete());
            assertEquals(1, size(quern.getStackInSlot(0)));
            assertEquals(3, size(quern.getStackInSlot(1)));
            assertEquals(bar, quern.getStackInSlot(2).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void quernWithAFullOutputChangesNothing(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityQuern quern = quern(new ItemStack(seed, 3), new ItemStack(pot, 16), new ItemStack(flour, 64));
            assertFalse(quern.onRevolutionComplete());
            assertEquals(3, size(quern.getStackInSlot(0)));
            assertEquals(16, size(quern.getStackInSlot(1)));
            assertEquals(64, size(quern.getStackInSlot(2)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void quernWithoutAPotChangesNothing(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityQuern quern = quern(new ItemStack(seed, 3), null, null);
            assertFalse(quern.onRevolutionComplete());
            assertEquals(3, size(quern.getStackInSlot(0)));
            assertNull(quern.getStackInSlot(2));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region bloomery

    private static void bloomRecipe(ItemStack output) {
        reload(tx -> tx.add(MFRecipes.BLOOMERY, id("bloomery", "bar"), BloomRecipe.of(Input.of(ore), output), 0));
    }

    private static TileEntityBloomery litBloomery() {
        bloomRecipe(new ItemStack(bar));
        TileEntityBloomery bloomery = new TileEntityBloomery();
        place(bloomery);
        bloomery.setInventorySlotContents(0, new ItemStack(ore, 4));
        bloomery.setInventorySlotContents(1, new ItemStack(carbon, 1));
        assertTrue(
                "the bloomery needs the sky above it",
                world().canBlockSeeTheSky(bloomery.xCoord, bloomery.yCoord + 1, bloomery.zCoord));
        minefantasy.mf2.api.recipe.CheckResult check = bloomery.check(null);
        assertTrue("the bloomery refused the smelt: " + check.getReason(), check.isSuccess());
        assertTrue(bloomery.light(null));
        assertTrue(bloomery.isActive);
        return bloomery;
    }

    @GameTest
    public static void bloomerySmeltsTheWholeStackWithExactCarbon(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityBloomery bloomery = litBloomery();
            bloomery.smeltItem();
            assertNull(bloomery.getStackInSlot(0));
            assertNull(bloomery.getStackInSlot(1));
            assertEquals(bar, bloomery.getStackInSlot(2).getItem());
            assertEquals(4, size(bloomery.getStackInSlot(2)));
            assertFalse(bloomery.isActive);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void reloadMidSmeltPutsTheFireOutAndKeepsTheItems(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityBloomery bloomery = litBloomery();
            bloomRecipe(new ItemStack(flour));
            bloomery.smeltItem();
            assertEquals(4, size(bloomery.getStackInSlot(0)));
            assertEquals(1, size(bloomery.getStackInSlot(1)));
            assertNull(bloomery.getStackInSlot(2));
            assertFalse(bloomery.isActive);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void savedSmeltFinishesAfterLoading(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityBloomery bloomery = litBloomery();
            NBTTagCompound nbt = new NBTTagCompound();
            bloomery.writeToNBT(nbt);

            TileEntityBloomery loaded = new TileEntityBloomery();
            loaded.readFromNBT(nbt);
            place(loaded);
            assertTrue(loaded.isActive);
            loaded.smeltItem();
            assertEquals(4, size(loaded.getStackInSlot(2)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void smeltFromAnOldSaveRestarts(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityBloomery bloomery = litBloomery();
            NBTTagCompound nbt = new NBTTagCompound();
            bloomery.writeToNBT(nbt);
            nbt.removeTag("RecipeFormat");
            nbt.removeTag("Project");

            TileEntityBloomery loaded = new TileEntityBloomery();
            loaded.readFromNBT(nbt);
            assertFalse(loaded.isActive);
            assertEquals(4, size(loaded.getStackInSlot(0)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region big furnace

    private static TileEntityBigFurnace furnace(ItemStack input) throws Exception {
        reload(
                tx -> tx.add(
                        MFRecipes.BIG_FURNACE,
                        id("big_furnace", "jars"),
                        ProcessRecipe.of(Input.of(jar).amount(2).usage(Usage.CONTAINER), new ItemStack(bar)),
                        0));
        TileEntityBigFurnace furnace = new TileEntityBigFurnace();
        place(furnace);
        setField(furnace, "built", true);
        furnace.setInventorySlotContents(0, input);
        return furnace;
    }

    private static void smelt(TileEntityBigFurnace furnace) throws Exception {
        Object plan = call(furnace, "planFor", new Class<?>[] { int.class, int.class }, 0, 4);
        assertTrue("the furnace found nothing to smelt", plan != null);
        // The same path updateEntity takes once the heat is up
        java.util.List<ItemStack> spill = new java.util.ArrayList<>();
        ((minefantasy.mf2.api.recipe.CraftPlan) plan)
                .apply(minefantasy.mf2.api.recipe.CraftInventory.of(furnace), spill);
        for (ItemStack stack : spill) {
            InventorySlots.drop(furnace.getWorldObj(), furnace.xCoord, furnace.yCoord, furnace.zCoord, stack);
        }
    }

    @GameTest
    public static void furnaceReturnsContainersIntoTheEmptiedSlot(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityBigFurnace furnace = furnace(new ItemStack(jar, 2));
            smelt(furnace);
            assertEquals(emptyJar, furnace.getStackInSlot(0).getItem());
            assertEquals(2, size(furnace.getStackInSlot(0)));
            assertEquals(bar, furnace.getStackInSlot(4).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void furnaceDropsContainersWithoutRoomInsteadOfStalling(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityBigFurnace furnace = furnace(new ItemStack(jar, 3));
            smelt(furnace);
            assertEquals(1, size(furnace.getStackInSlot(0)));
            assertEquals(bar, furnace.getStackInSlot(4).getItem());
            assertEquals(2, dropped(emptyJar));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region carpenter

    private static void carpenterRecipe(int time) {
        reload(
                tx -> tx.add(
                        MFRecipes.CARPENTER,
                        id("carpenter", "jar_bar"),
                        GridRecipe.shapeless(
                                GridRecipe.Grid.BENCH,
                                new Object[] { new ItemStack(jar) },
                                null,
                                new ItemStack(bar)).time(time).build(),
                        0));
    }

    private static TileEntityCarpenterMF carpenter(int time) {
        carpenterRecipe(time);
        TileEntityCarpenterMF bench = new TileEntityCarpenterMF();
        place(bench);
        bench.setInventorySlotContents(0, new ItemStack(jar, 4));
        bench.updateCraftingData();
        return bench;
    }

    /** The end of the last hit: the bench re-reads its grid and pays out, as tryCraft does at full progress. */
    private static void finish(TileEntityCarpenterMF bench) throws Exception {
        bench.progress = bench.progressMax;
        call(bench, "craftItem", new Class<?>[] { net.minecraft.entity.player.EntityPlayer.class }, (Object) null);
    }

    private static int output(TileEntityCarpenterMF bench) {
        return bench.getSizeInventory() - 5;
    }

    @GameTest
    public static void carpenterDropsContainersWhenReturnSlotsAreFull(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityCarpenterMF bench = carpenter(1);
            for (int slot = output(bench) + 1; slot < bench.getSizeInventory(); slot++) {
                bench.setInventorySlotContents(slot, new ItemStack(junk, 64));
            }
            bench.updateCraftingData();
            finish(bench);
            assertEquals(bar, bench.getStackInSlot(output(bench)).getItem());
            assertEquals(3, size(bench.getStackInSlot(0)));
            assertEquals(1, dropped(emptyJar));
            assertEquals(64, size(bench.getStackInSlot(bench.getSizeInventory() - 1)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void carpenterReturnsTheContainerToTheSlotItEmptied(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            carpenterRecipe(1);
            TileEntityCarpenterMF bench = new TileEntityCarpenterMF();
            place(bench);
            bench.setInventorySlotContents(0, new ItemStack(jar, 1));
            bench.updateCraftingData();
            finish(bench);
            assertEquals(emptyJar, bench.getStackInSlot(0).getItem());
            assertEquals(0, dropped(emptyJar));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void reloadChangingTheTimeRestartsTheWork(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityCarpenterMF bench = carpenter(100);
            bench.progress = 10;
            carpenterRecipe(200);
            bench.updateCraftingData();
            assertEquals(0F, bench.progress, 0F);
            assertEquals(4, size(bench.getStackInSlot(0)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void savedWorkSurvivesLoadingUnlessTheRecipeChanged(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityCarpenterMF bench = carpenter(100);
            bench.progress = 10;
            float progress = bench.progress;
            NBTTagCompound nbt = new NBTTagCompound();
            bench.writeToNBT(nbt);

            TileEntityCarpenterMF same = new TileEntityCarpenterMF();
            same.readFromNBT(nbt);
            place(same);
            same.updateCraftingData();
            assertEquals(progress, same.progress, 0F);

            carpenterRecipe(200);
            TileEntityCarpenterMF changed = new TileEntityCarpenterMF();
            changed.readFromNBT(nbt);
            place(changed);
            changed.updateCraftingData();
            assertEquals(0F, changed.progress, 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region tanning rack

    private static void tanningRecipe(float time) {
        reload(
                tx -> tx.add(
                        MFRecipes.TANNING,
                        id("tanning", "jars"),
                        ProcessRecipe.of(
                                Input.of(jar).amount(2).usage(Usage.CONTAINER),
                                new ItemStack(bar),
                                minefantasy.mf2.api.recipe.RecipeMetadata.builder()
                                        .put(minefantasy.mf2.api.crafting.MFRecipeKeys.TIME, time).build()),
                        0));
    }

    private static TileEntityTanningRack rack() {
        TileEntityTanningRack rack = new TileEntityTanningRack();
        place(rack);
        rack.setInventorySlotContents(0, new ItemStack(jar, 2));
        rack.updateRecipe();
        return rack;
    }

    private static Object currentPlan(TileEntityTanningRack rack) throws Exception {
        return call(rack, "currentPlan", new Class<?>[0]);
    }

    @GameTest
    public static void rackPaysTheRecipeAmountAndDropsTheContainers(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            tanningRecipe(10F);
            TileEntityTanningRack rack = rack();
            Object plan = currentPlan(rack);
            assertTrue(plan != null);
            call(rack, "finish", new Class<?>[] { minefantasy.mf2.api.recipe.CraftPlan.class }, plan);
            assertEquals(bar, rack.getStackInSlot(0).getItem());
            assertEquals(1, size(rack.getStackInSlot(0)));
            assertEquals(2, dropped(emptyJar));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void rackRestartsWhenAReloadChangesTheRecipe(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            tanningRecipe(10F);
            TileEntityTanningRack rack = rack();
            rack.progress = 5;
            tanningRecipe(20F);
            assertNull(currentPlan(rack));
            assertEquals(0F, rack.progress, 0F);
            assertEquals(2, size(rack.getStackInSlot(0)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void rackWorkSurvivesSaving(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            tanningRecipe(10F);
            TileEntityTanningRack rack = rack();
            NBTTagCompound nbt = new NBTTagCompound();
            rack.writeToNBT(nbt);
            TileEntityTanningRack loaded = new TileEntityTanningRack();
            loaded.readFromNBT(nbt);
            place(loaded);
            assertTrue(currentPlan(loaded) != null);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region crucible

    @GameTest
    public static void crucibleReturnsContainersToTheSlotTheyEmptied(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.ALLOY,
                            id("alloy", "jar"),
                            new minefantasy.mf2.api.refine.Alloy(
                                    new ItemStack(bar),
                                    0,
                                    Arrays.asList(new ItemStack(jar))),
                            0));
            TileEntityCrucible crucible = new TileEntityCrucible();
            place(crucible);
            crucible.setInventorySlotContents(0, new ItemStack(jar, 1));
            call(crucible, "updateCachedRecipe", new Class<?>[0]);
            crucible.temperature = 500;
            crucible.smeltItem();
            assertEquals(bar, crucible.getStackInSlot(9).getItem());
            assertEquals(emptyJar, crucible.getStackInSlot(0).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region review fixes: products larger than the station's slot limit, cooking after a reload

    @GameTest
    public static void rackLeavesAProductLargerThanItsSlotLimit(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            // A medium hide gives three leather: the rack's one-item slot limit is for hands and hoppers only
            reload(
                    tx -> tx.add(
                            MFRecipes.TANNING,
                            id("tanning", "hide"),
                            ProcessRecipe.of(Input.of(seed), new ItemStack(flour, 3)),
                            0));
            TileEntityTanningRack rack = place(new TileEntityTanningRack());
            rack.setInventorySlotContents(0, new ItemStack(seed));
            rack.updateRecipe();
            Object plan = currentPlan(rack);
            assertNotNull("the rack found no work", plan);
            call(rack, "finish", new Class<?>[] { minefantasy.mf2.api.recipe.CraftPlan.class }, plan);
            assertEquals(flour, rack.getStackInSlot(0).getItem());
            assertEquals(3, size(rack.getStackInSlot(0)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    private static void cookingRecipe(ItemStack output, int time) {
        reload(
                tx -> tx.add(
                        MFRecipes.COOKING,
                        id("cooking", "seed"),
                        CookRecipe.builder(Input.of(seed), output).temperature(100, 500).time(time)
                                .burnt(new ItemStack(junk)).build(),
                        0));
    }

    /** A spit with a seed cooking on it. */
    private static TileEntityRoast spit() {
        TileEntityRoast spit = place(new TileEntityRoast());
        spit.setInventorySlotContents(0, new ItemStack(seed));
        spit.updateRecipe();
        return spit;
    }

    private static void cook(TileEntityRoast spit, int temperature) throws Exception {
        call(spit, "cook", new Class<?>[] { int.class }, temperature);
    }

    @GameTest
    public static void hotSpitWithNothingToCookStaysIdle(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityRoast spit = place(new TileEntityRoast());
            spit.setInventorySlotContents(0, new ItemStack(junk));
            spit.updateRecipe();
            // A recipe lookup restarting the work would zero this
            spit.progress = 5;
            cook(spit, 200);
            assertEquals("food nothing cooks restarts every step", 5F, spit.progress, 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void spitLeavesAProductLargerThanItsSlotLimit(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            cookingRecipe(new ItemStack(flour, 2), 10);
            TileEntityRoast spit = spit();
            spit.progress = 10;
            cook(spit, 200);
            assertEquals(flour, spit.getStackInSlot(0).getItem());
            assertEquals(2, size(spit.getStackInSlot(0)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void spitDoesNotFinishARecipeAReloadRemoved(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            cookingRecipe(new ItemStack(flour), 10);
            TileEntityRoast spit = spit();
            spit.progress = 10;
            reload(tx -> {});
            cook(spit, 200);
            assertEquals("the food stays raw", seed, spit.getStackInSlot(0).getItem());
            // Nor does it burn by the removed recipe
            cook(spit, 900);
            assertEquals(seed, spit.getStackInSlot(0).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void spitRestartsOnARecipeAReloadReplaced(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            cookingRecipe(new ItemStack(flour), 10);
            TileEntityRoast spit = spit();
            spit.progress = 10;
            cookingRecipe(new ItemStack(bar), 10);
            cook(spit, 200);
            assertEquals("the old result is not paid out", seed, spit.getStackInSlot(0).getItem());
            assertEquals(0F, spit.progress, 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void savedCookingRestartsWhenTheTimeChanged(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            cookingRecipe(new ItemStack(flour), 100);
            TileEntityRoast spit = spit();
            spit.progress = 50;
            NBTTagCompound nbt = new NBTTagCompound();
            spit.writeToNBT(nbt);

            TileEntityRoast same = place(new TileEntityRoast());
            same.readFromNBT(nbt);
            same.restoreRecipe();
            cook(same, 200);
            assertEquals("unchanged terms keep the progress", 52F, same.progress, 0.01F);

            cookingRecipe(new ItemStack(flour), 10);
            TileEntityRoast changed = place(new TileEntityRoast());
            changed.readFromNBT(nbt);
            changed.restoreRecipe();
            cook(changed, 200);
            assertEquals("a changed time starts over", 0F, changed.progress, 0F);
            assertEquals(seed, changed.getStackInSlot(0).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion
}
