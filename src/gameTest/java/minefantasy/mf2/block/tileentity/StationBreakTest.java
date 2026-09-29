package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.recipe.CraftInventory;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeMetadata;
import minefantasy.mf2.api.recipe.Usage;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * A station broken in the middle of its work and right after it: what lands on the ground is exactly what it held, so
 * ingredients, results and returned containers are neither doubled nor lost. The multiblock furnaces lose one part at a
 * time.
 */
@GameTestHolder("minefantasy2")
public class StationBreakTest {

    private StationBreakTest() {}

    // region fixture

    private static String key(ItemStack stack) {
        String name = Item.itemRegistry.getNameForObject(stack.getItem()) + "@" + stack.getItemDamage();
        return stack.hasTagCompound() ? name + stack.getTagCompound() : name;
    }

    /** The stacks as counts by item, metadata and tag. */
    private static Map<String, Integer> items(ItemStack... stacks) {
        Map<String, Integer> counts = new TreeMap<>();
        for (ItemStack stack : stacks) {
            if (stack != null && stack.stackSize > 0) {
                counts.merge(key(stack), stack.stackSize, Integer::sum);
            }
        }
        return counts;
    }

    /** Everything lying around the test's cell, taken up so the next count starts empty. */
    private static Map<String, Integer> pickUp(GameTestHelper helper) {
        TestPos low = helper.absolute(-2, 0, -2);
        TestPos high = helper.absolute(7, 7, 7);
        List<?> entities = helper.getWorld().getEntitiesWithinAABB(
                EntityItem.class,
                AxisAlignedBB.getBoundingBox(low.x(), low.y(), low.z(), high.x(), high.y(), high.z()));
        List<ItemStack> stacks = new ArrayList<>();
        for (Object o : entities) {
            EntityItem entity = (EntityItem) o;
            if (!entity.isDead) {
                stacks.add(entity.getEntityItem());
                entity.setDead();
            }
        }
        return items(stacks.toArray(new ItemStack[0]));
    }

    /** Breaks the block the way removing it from the world does: its inventory drops, the block itself does not. */
    private static void breakAt(GameTestHelper helper, int x, int y, int z) {
        TestPos at = helper.absolute(x, y, z);
        helper.getWorld().setBlockToAir(at.x(), at.y(), at.z());
    }

    private static FakePlayer holding(GameTestHelper helper, ItemStack held) {
        FakePlayer player = helper.spawnFakePlayer(Modders.SMITH);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, held);
        return player;
    }

    /** A bench or anvil recipe making a bar from one jar, which gives its empty jar back. */
    private static GridRecipe jarRecipe(GridRecipe.Grid grid, int time) {
        return GridRecipe.shapeless(grid, new Object[] { new ItemStack(jar) }, null, new ItemStack(bar)).time(time)
                .build();
    }

    // endregion

    // region grid stations

    @GameTest
    public static void anvil(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(tx -> tx.add(MFRecipes.ANVIL, id("anvil", "jar"), jarRecipe(GridRecipe.Grid.ANVIL, 100), 0));
            for (boolean finished : new boolean[] { false, true }) {
                helper.setBlock(1, 1, 1, BlockListMF.anvilStone);
                TileEntityAnvilMF anvil = helper.assertTileEntityPresent(TileEntityAnvilMF.class, 1, 1, 1);
                anvil.setInventorySlotContents(0, new ItemStack(jar, 3));
                anvil.updateCraftingData();
                assertTrue("the anvil found no project", anvil.hasProject());
                anvil.progress = anvil.progressMax / 2;
                if (finished) {
                    anvil.progress = anvil.progressMax;
                    call(
                            anvil,
                            "craftItem",
                            new Class<?>[] { EntityPlayer.class },
                            holding(helper, new ItemStack(ToolListMF.hammerStone)));
                }
                Map<String, Integer> spilled = pickUp(helper);
                breakAt(helper, 1, 1, 1);
                Map<String, Integer> expected = finished
                        ? items(new ItemStack(jar, 2), new ItemStack(emptyJar), new ItemStack(bar))
                        : items(new ItemStack(jar, 3));
                assertEquals(finished ? "after the work" : "during the work", expected, merge(spilled, pickUp(helper)));
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void benches(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(tx -> {
                tx.add(MFRecipes.CARPENTER, id("carpenter", "jar"), jarRecipe(GridRecipe.Grid.BENCH, 100), 0);
                tx.add(MFRecipes.KITCHEN, id("kitchen", "jar"), jarRecipe(GridRecipe.Grid.BENCH, 100), 0);
            });
            for (boolean finished : new boolean[] { false, true }) {
                helper.setBlock(1, 1, 1, BlockListMF.carpenter);
                TileEntityCarpenterMF carpenter = helper.assertTileEntityPresent(TileEntityCarpenterMF.class, 1, 1, 1);
                carpenter.setInventorySlotContents(0, new ItemStack(jar, 3));
                carpenter.updateCraftingData();
                carpenter.progress = carpenter.progressMax / 2;
                helper.setBlock(3, 1, 1, BlockListMF.kitchenBench);
                TileEntityKitchenBench kitchen = helper.assertTileEntityPresent(TileEntityKitchenBench.class, 3, 1, 1);
                kitchen.setInventorySlotContents(0, new ItemStack(jar, 3));
                kitchen.updateCraftingData();
                kitchen.progress = kitchen.progressMax / 2;
                if (finished) {
                    FakePlayer cook = holding(helper, null);
                    carpenter.progress = carpenter.progressMax;
                    call(carpenter, "craftItem", new Class<?>[] { EntityPlayer.class }, cook);
                    kitchen.progress = kitchen.progressMax;
                    call(kitchen, "craftItem", new Class<?>[] { EntityPlayer.class }, cook);
                }
                Map<String, Integer> spilled = pickUp(helper);
                breakAt(helper, 1, 1, 1);
                breakAt(helper, 3, 1, 1);
                ItemStack[] one = finished
                        ? new ItemStack[] { new ItemStack(jar, 2), new ItemStack(emptyJar), new ItemStack(bar) }
                        : new ItemStack[] { new ItemStack(jar, 3) };
                List<ItemStack> both = new ArrayList<>();
                for (ItemStack stack : one) {
                    both.add(stack.copy());
                    both.add(stack.copy());
                }
                assertEquals(
                        finished ? "after the work" : "during the work",
                        items(both.toArray(new ItemStack[0])),
                        merge(spilled, pickUp(helper)));
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region one-slot stations

    @GameTest
    public static void quern(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.QUERN,
                            id("quern", "seed"),
                            ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                            0));
            for (boolean finished : new boolean[] { false, true }) {
                helper.setBlock(1, 1, 1, BlockListMF.quern);
                TileEntityQuern quern = helper.assertTileEntityPresent(TileEntityQuern.class, 1, 1, 1);
                quern.setInventorySlotContents(0, new ItemStack(seed, 3));
                quern.setInventorySlotContents(1, new ItemStack(pot, 4));
                quern.onUse(holding(helper, null));
                if (finished) {
                    assertTrue("the quern did not grind", quern.onRevolutionComplete());
                }
                breakAt(helper, 1, 1, 1);
                Map<String, Integer> expected = finished
                        ? items(new ItemStack(seed, 2), new ItemStack(pot, 3), new ItemStack(flour))
                        : items(new ItemStack(seed, 3), new ItemStack(pot, 4));
                assertEquals(finished ? "after the work" : "during the work", expected, pickUp(helper));
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void tanningRack(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.TANNING,
                            id("tanning", "jars"),
                            ProcessRecipe.of(
                                    Input.of(jar).amount(2).usage(Usage.CONTAINER),
                                    new ItemStack(bar),
                                    RecipeMetadata.builder().put(MFRecipeKeys.TIME, 10F).build()),
                            0));
            for (boolean finished : new boolean[] { false, true }) {
                helper.setBlock(1, 1, 1, BlockListMF.tanner);
                TileEntityTanningRack rack = helper.assertTileEntityPresent(TileEntityTanningRack.class, 1, 1, 1);
                rack.setInventorySlotContents(0, new ItemStack(jar, 2));
                rack.updateRecipe();
                assertNotNull("the rack shows no result", rack.getStackInSlot(1));
                rack.progress = 5;
                if (finished) {
                    Object plan = call(rack, "currentPlan", new Class<?>[0]);
                    assertNotNull("the rack found no work", plan);
                    call(rack, "finish", new Class<?>[] { CraftPlan.class }, plan);
                }
                Map<String, Integer> spilled = pickUp(helper);
                breakAt(helper, 1, 1, 1);
                // The rack shows its result in a second slot: it is a picture, not an item, and must not drop
                Map<String, Integer> expected = finished ? items(new ItemStack(bar), new ItemStack(emptyJar, 2))
                        : items(new ItemStack(jar, 2));
                assertEquals(finished ? "after the work" : "during the work", expected, merge(spilled, pickUp(helper)));
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void spit(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.COOKING,
                            id("cooking", "seed"),
                            CookRecipe.builder(Input.of(seed), new ItemStack(flour)).temperature(100, 500).time(10)
                                    .burnt(new ItemStack(junk)).build(),
                            0));
            for (boolean finished : new boolean[] { false, true }) {
                helper.setBlock(1, 1, 1, BlockListMF.roast);
                TileEntityRoast spit = helper.assertTileEntityPresent(TileEntityRoast.class, 1, 1, 1);
                spit.setInventorySlotContents(0, new ItemStack(seed));
                spit.updateRecipe();
                spit.progress = finished ? 10 : 5;
                call(spit, "cook", new Class<?>[] { int.class }, 200);
                breakAt(helper, 1, 1, 1);
                Map<String, Integer> expected = items(new ItemStack(finished ? flour : seed));
                assertEquals(finished ? "after the work" : "during the work", expected, pickUp(helper));
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region crucible and bloomery: what the research book says is lost

    @GameTest
    public static void crucible(GameTestHelper helper) throws Exception {
        boolean reduce = ConfigHardcore.HCCreduceIngots;
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.ALLOY,
                            id("alloy", "jar"),
                            new Alloy(new ItemStack(bar), 0, Arrays.asList(new ItemStack(jar))),
                            0));
            // Hardcore Ingots: breaking a crucible loses its output, as the book warns
            for (boolean hardcore : new boolean[] { false, true }) {
                ConfigHardcore.HCCreduceIngots = hardcore;
                for (boolean finished : new boolean[] { false, true }) {
                    helper.setBlock(1, 1, 1, BlockListMF.crucible);
                    TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 1, 1);
                    crucible.setInventorySlotContents(0, new ItemStack(jar));
                    call(crucible, "updateCachedRecipe", new Class<?>[0]);
                    crucible.temperature = 500;
                    crucible.progress = 1;
                    if (finished) {
                        crucible.smeltItem();
                        assertEquals(bar, crucible.getStackInSlot(crucible.getSizeInventory() - 1).getItem());
                    }
                    breakAt(helper, 1, 1, 1);
                    Map<String, Integer> expected = !finished ? items(new ItemStack(jar))
                            : hardcore ? items(new ItemStack(emptyJar))
                                    : items(new ItemStack(emptyJar), new ItemStack(bar));
                    assertEquals(
                            (hardcore ? "hardcore, " : "") + (finished ? "after the work" : "during the work"),
                            expected,
                            pickUp(helper));
                }
            }
        } finally {
            ConfigHardcore.HCCreduceIngots = reduce;
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void bloomery(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.BLOOMERY,
                            id("bloomery", "ore"),
                            BloomRecipe.of(Input.of(ore), new ItemStack(bar)),
                            0));
            for (boolean finished : new boolean[] { false, true }) {
                helper.setBlock(1, 1, 1, BlockListMF.bloomery);
                TileEntityBloomery bloomery = helper.assertTileEntityPresent(TileEntityBloomery.class, 1, 1, 1);
                bloomery.setInventorySlotContents(0, new ItemStack(ore, 4));
                bloomery.setInventorySlotContents(1, new ItemStack(carbon));
                assertTrue("the bloomery did not light", bloomery.light(holding(helper, null)));
                if (finished) {
                    bloomery.smeltItem();
                    assertEquals("no bloom", 4, bloomery.getStackInSlot(2).stackSize);
                }
                breakAt(helper, 1, 1, 1);
                // BlockBloomery drops no bloom on purpose, so it cannot be taken without hammering. The book does not
                // say so yet and whether to keep it is undecided: this pins the current rule until then.
                Map<String, Integer> expected = finished ? items()
                        : items(new ItemStack(ore, 4), new ItemStack(carbon));
                assertEquals(finished ? "after the work" : "during the work", expected, pickUp(helper));
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region multiblock furnaces

    /** Two lists of drops as one. */
    private static Map<String, Integer> merge(Map<String, Integer> a, Map<String, Integer> b) {
        Map<String, Integer> all = new TreeMap<>(a);
        b.forEach((k, v) -> all.merge(k, v, Integer::sum));
        return all;
    }

    /** Heater, furnace on it, firebricks on their sides and back and over the furnace. */
    private static TileEntityBigFurnace bigFurnace(GameTestHelper helper) throws Exception {
        helper.setBlock(2, 1, 2, BlockListMF.furnace_heater, 0);
        helper.setBlock(2, 2, 2, BlockListMF.furnace_stone, 0);
        for (int y = 1; y <= 2; y++) {
            helper.setBlock(1, y, 2, BlockListMF.firebricks);
            helper.setBlock(3, y, 2, BlockListMF.firebricks);
            helper.setBlock(2, y, 3, BlockListMF.firebricks);
        }
        helper.setBlock(2, 3, 2, BlockListMF.firebricks);
        TileEntityBigFurnace furnace = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 2, 2);
        furnace.setInventorySlotContents(0, new ItemStack(seed, 2));
        helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 1, 2)
                .setInventorySlotContents(0, new ItemStack(Items.coal, 3));
        return furnace;
    }

    /** One smelt of the furnace, the way its update does it once the heat is up. */
    private static void smeltOnce(TileEntityBigFurnace furnace) throws Exception {
        setField(furnace, "built", true);
        CraftPlan plan = (CraftPlan) call(furnace, "planFor", new Class<?>[] { int.class, int.class }, 0, 4);
        assertNotNull("the furnace found nothing to smelt", plan);
        assertTrue(plan.apply(CraftInventory.of(furnace), new ArrayList<>()));
    }

    @GameTest
    public static void bigFurnaceLosesAPartAtATime(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.BIG_FURNACE,
                            id("big_furnace", "seed"),
                            ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                            0));
            TileEntityBigFurnace furnace = bigFurnace(helper);
            furnace.progress = 50;
            breakAt(helper, 2, 3, 2);
            assertEquals("a wall dropped items", items(), pickUp(helper));
            assertEquals("the furnace lost its load with a wall", 2, furnace.getStackInSlot(0).stackSize);
            breakAt(helper, 2, 1, 2);
            assertEquals("the heater drops its fuel alone", items(new ItemStack(Items.coal, 3)), pickUp(helper));
            breakAt(helper, 2, 2, 2);
            assertEquals("during the work", items(new ItemStack(seed, 2)), pickUp(helper));

            furnace = bigFurnace(helper);
            smeltOnce(furnace);
            breakAt(helper, 2, 2, 2);
            assertEquals("after the work", items(new ItemStack(seed), new ItemStack(flour)), pickUp(helper));
            breakAt(helper, 2, 1, 2);
            assertEquals(items(new ItemStack(Items.coal, 3)), pickUp(helper));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** Crucible, heater with firebricks on its corners, chamber with firebricks on its sides. */
    private static TileEntityBlastFH blastFurnace(GameTestHelper helper) {
        helper.setBlock(2, 1, 2, BlockListMF.crucible);
        helper.setBlock(2, 2, 2, BlockListMF.blast_heater);
        helper.setBlock(2, 3, 2, BlockListMF.blast_chamber);
        for (int[] corner : new int[][] { { 1, 1 }, { 3, 1 }, { 1, 3 }, { 3, 3 } }) {
            helper.setBlock(corner[0], 2, corner[1], BlockListMF.firebricks);
        }
        for (int[] side : new int[][] { { 1, 2 }, { 3, 2 }, { 2, 1 }, { 2, 3 } }) {
            helper.setBlock(side[0], 3, side[1], BlockListMF.firebricks);
        }
        TileEntityBlastFH heater = helper.assertTileEntityPresent(TileEntityBlastFH.class, 2, 2, 2);
        TileEntityBlastFC chamber = helper.assertTileEntityPresent(TileEntityBlastFC.class, 2, 3, 2);
        heater.updateBuild();
        chamber.updateBuild();
        assertTrue("the heater is not built", heater.isBuilt);
        assertTrue("the chamber is not built", chamber.isBuilt);
        chamber.setInventorySlotContents(0, new ItemStack(carbon));
        chamber.setInventorySlotContents(1, new ItemStack(ore));
        heater.setInventorySlotContents(0, new ItemStack(Items.coal, 2));
        return heater;
    }

    private static TileEntityCrucible catcher(GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileEntityCrucible.class, 2, 1, 2);
    }

    @GameTest
    public static void blastFurnaceLosesAPartAtATime(GameTestHelper helper) throws Exception {
        boolean reduce = ConfigHardcore.HCCreduceIngots;
        Stations.begin(helper);
        try {
            ConfigHardcore.HCCreduceIngots = false;
            reload(
                    tx -> tx.add(
                            MFRecipes.BLAST_FURNACE,
                            id("blast_furnace", "ore"),
                            ProcessRecipe.of(Input.of(ore), new ItemStack(bar)),
                            0));
            // During the work: the chamber goes, then the heater smelts what is left above it, which is nothing
            TileEntityBlastFH heater = blastFurnace(helper);
            breakAt(helper, 2, 3, 2);
            assertEquals(
                    "the chamber drops its load",
                    items(new ItemStack(carbon), new ItemStack(ore)),
                    pickUp(helper));
            call(heater, "smeltItem", new Class<?>[0]);
            assertNull("a smelt without a chamber made something", catcher(helper).getStackInSlot(9));
            assertEquals(items(), pickUp(helper));
            breakAt(helper, 2, 2, 2);
            assertEquals("the heater drops its fuel", items(new ItemStack(Items.coal, 2)), pickUp(helper));
            breakAt(helper, 2, 1, 2);
            assertEquals(items(), pickUp(helper));

            // After the work: the ore and a use of the carbon are spent, the bar sits in the crucible
            heater = blastFurnace(helper);
            call(heater, "smeltItem", new Class<?>[0]);
            breakAt(helper, 2, 3, 2);
            breakAt(helper, 2, 2, 2);
            breakAt(helper, 2, 1, 2);
            assertEquals("after the work", items(new ItemStack(Items.coal, 2), new ItemStack(bar)), pickUp(helper));
        } finally {
            ConfigHardcore.HCCreduceIngots = reduce;
            Stations.end();
        }
        helper.succeed();
    }

    // endregion
}
