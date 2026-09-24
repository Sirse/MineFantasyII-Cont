package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.crafting.exotic.SpecialForging;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.food.FoodListMF;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * The stations no other test drives: salvage, special forging, the forge's heat, the paint brush, and the crucible over
 * a fire, each built from its blocks and used the way a player uses it.
 */
@GameTestHolder("minefantasy2")
public class WorldProcessTest {

    private WorldProcessTest() {}

    private static FakePlayer holding(GameTestHelper helper, ItemStack held) {
        FakePlayer player = helper.spawnFakePlayer(Modders.SMITH);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, held);
        return player;
    }

    private static List<Item> items(List<ItemStack> stacks) {
        List<Item> items = new ArrayList<>();
        for (ItemStack stack : stacks) {
            for (int i = 0; i < stack.stackSize; i++) {
                items.add(stack.getItem());
            }
        }
        return items;
    }

    // region salvage

    /** The blade salvages into two bars and an ore; an ornate craft makes flour out of a bar. */
    private static void salvageRecipes() {
        reload(tx -> {
            ItemStack worn = new ItemStack(blade);
            tx.set(
                    MFRecipes.SALVAGE,
                    Salvage.partsId(worn),
                    Salvage.SalvageRecipe.parts(worn, new Object[] { new ItemStack(bar, 2), new ItemStack(ore) }),
                    0);
            SpecialForging.stage(tx, new SpecialForging.SpecialCraft("ornate", blade, flour), 0);
            tx.set(
                    MFRecipes.SALVAGE,
                    Salvage.partsId(new ItemStack(bar)),
                    Salvage.SalvageRecipe.parts(new ItemStack(bar), new Object[] { new ItemStack(ore, 3) }),
                    0);
        });
    }

    @GameTest
    public static void salvageGivesTheRegisteredParts(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            salvageRecipes();
            FakePlayer player = holding(helper, null);
            // A drop rate this high makes every part come out
            List<ItemStack> parts = Salvage.salvage(player, new ItemStack(blade), 2F);
            assertEquals(Arrays.asList(bar, bar, ore), items(parts));
            // The ornate form salvages like its base
            assertEquals(Arrays.asList(bar, bar, ore), items(Salvage.salvage(player, new ItemStack(flour), 2F)));
            assertNull("junk without parts salvages into nothing", Salvage.salvage(player, new ItemStack(junk), 2F));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void salvageBenchTakesTheItemOnTopAndDropsOnlyItsParts(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            salvageRecipes();
            helper.setBlock(1, 1, 1, BlockListMF.salvage_basic);
            TestPos bench = helper.absolute(1, 1, 1);
            EntityItem junkOnTop = new EntityItem(
                    helper.getWorld(),
                    bench.x() + 0.5D,
                    bench.y() + 1.1D,
                    bench.z() + 0.5D,
                    new ItemStack(blade));
            helper.getWorld().spawnEntityInWorld(junkOnTop);
            FakePlayer smith = holding(helper, new ItemStack(ToolListMF.hammerStone));

            assertTrue(
                    "the bench did nothing",
                    BlockListMF.salvage_basic.onBlockActivated(
                            helper.getWorld(),
                            bench.x(),
                            bench.y(),
                            bench.z(),
                            smith,
                            1,
                            0F,
                            0F,
                            0F));
            assertTrue("the salvaged item is still there", junkOnTop.isDead);
            // Each part drops by chance: never more than the item is made of, and nothing else
            assertTrue(dropped(bar) <= 2);
            assertTrue(dropped(ore) <= 1);
            assertEquals(0, dropped(blade));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region special forging

    private static TileEntityAnvilMF anvilWithBarRecipe(GameTestHelper helper, ItemStack inOutput) {
        reload(tx -> {
            tx.add(
                    MFRecipes.ANVIL,
                    id("anvil", "bar"),
                    GridRecipe.shaped(
                            GridRecipe.Grid.ANVIL,
                            1,
                            1,
                            new Object[] { new ItemStack(junk) },
                            null,
                            new ItemStack(bar)).tool("hammer", 0).time(2).build(),
                    0);
            SpecialForging.stage(tx, new SpecialForging.SpecialCraft("ornate", bar, flour), 0);
        });
        helper.setBlock(1, 1, 1, BlockListMF.anvilStone);
        TileEntityAnvilMF anvil = helper.assertTileEntityPresent(TileEntityAnvilMF.class, 1, 1, 1);
        anvil.setInventorySlotContents(0, new ItemStack(junk));
        anvil.setInventorySlotContents(anvil.getSizeInventory() - 1, inOutput);
        anvil.updateCraftingData();
        return anvil;
    }

    private static ItemStack forge(TileEntityAnvilMF anvil, FakePlayer smith) {
        int output = anvil.getSizeInventory() - 1;
        for (int hit = 0; hit < 40 && anvil.getStackInSlot(0) != null; hit++) {
            anvil.tryCraft(smith, hit % 2 == 0);
        }
        return anvil.getStackInSlot(output);
    }

    @GameTest
    public static void ornateDesignTurnsTheResultIntoItsOrnateForm(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityAnvilMF anvil = anvilWithBarRecipe(helper, new ItemStack(ComponentListMF.ornate_items));
            ItemStack made = forge(anvil, holding(helper, new ItemStack(ToolListMF.hammerStone)));
            assertNotNull("the anvil made nothing", made);
            assertEquals("the design takes the ornate form and is used up", flour, made.getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void withoutADesignTheResultIsPlain(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityAnvilMF anvil = anvilWithBarRecipe(helper, null);
            ItemStack made = forge(anvil, holding(helper, new ItemStack(ToolListMF.hammerStone)));
            assertNotNull("the anvil made nothing", made);
            assertEquals(bar, made.getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region forge

    /** A lit forge at the given heat, with the piece in its slot. */
    private static TileEntityForge litForge(GameTestHelper helper, float heat, ItemStack piece) {
        helper.setBlock(1, 1, 1, BlockListMF.forge_active);
        TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 1, 1, 1);
        forge.fuel = 6000;
        forge.fuelTemperature = heat;
        forge.temperature = heat;
        forge.setInventorySlotContents(0, piece);
        return forge;
    }

    /** A heat profile for the item; tests over ticks each heat their own item, as the forge takes the first one. */
    private static void heatProfile(Item item, int work, int unstable, int max) {
        reload(
                tx -> tx.add(
                        MFRecipes.HEATING,
                        id("forge_heat", "piece"),
                        Heatable.of(Input.of(item), work, unstable, max),
                        0));
    }

    @GameTest(timeoutTicks = 400)
    public static void forgeHeatsAPieceUntilItCanBeWorked(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        heatProfile(ore, 100, 500, 900);
        TileEntityForge forge = litForge(helper, 400F, new ItemStack(ore));
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> {
            ItemStack piece = forge.getStackInSlot(0);
            if (piece == null || piece.getItem() != ComponentListMF.hotItem) {
                return false;
            }
            assertEquals("the hot piece carries the ore", ore, Heatable.getItem(piece).getItem());
            return Heatable.getHeatableStage(piece) == 1;
        });
    }

    @GameTest(timeoutTicks = 400)
    public static void forgeRuinsAPieceHeatedPastItsLimit(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        heatProfile(pot, 50, 100, 150);
        TileEntityForge forge = litForge(helper, 800F, new ItemStack(pot));
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> forge.getStackInSlot(0) == null);
    }

    @GameTest(timeoutTicks = 100)
    public static void forgeLeavesItemsWithoutAHeatProfileAlone(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        heatProfile(hammer, 100, 500, 900);
        TileEntityForge forge = litForge(helper, 400F, new ItemStack(seed));
        Stations.keepUntilFinished();
        helper.onEachTick("seed stays a seed", () -> assertEquals(seed, forge.getStackInSlot(0).getItem()));
        helper.succeedAtTimeout();
    }

    // endregion

    // region paint brush

    @GameTest
    public static void paintBrushTurnsTheBlockWithOil(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.nailed_planks);
        FakePlayer painter = holding(helper, new ItemStack(ToolListMF.paint_brush));
        painter.inventory.setInventorySlotContents(1, new ItemStack(ComponentListMF.plant_oil));
        ResearchLogic.forceUnlock(painter, ResearchLogic.getResearch("paint_brush"));
        TestPos block = helper.absolute(1, 1, 1);

        painter.getHeldItem().getItem().onItemUse(
                painter.getHeldItem(),
                painter,
                helper.getWorld(),
                block.x(),
                block.y(),
                block.z(),
                1,
                0.5F,
                1F,
                0.5F);
        helper.assertBlockPresent(BlockListMF.refined_planks, 1, 1, 1);
        assertFalse("the oil is used", painter.inventory.hasItem(ComponentListMF.plant_oil));
        assertTrue("the empty jug comes back", painter.inventory.hasItem(FoodListMF.jug_empty));
        helper.succeed();
    }

    @GameTest
    public static void paintBrushWithoutOilDoesNothing(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.nailed_planks);
        FakePlayer painter = holding(helper, new ItemStack(ToolListMF.paint_brush));
        ResearchLogic.forceUnlock(painter, ResearchLogic.getResearch("paint_brush"));
        TestPos block = helper.absolute(1, 1, 1);
        painter.getHeldItem().getItem().onItemUse(
                painter.getHeldItem(),
                painter,
                helper.getWorld(),
                block.x(),
                block.y(),
                block.z(),
                1,
                0.5F,
                1F,
                0.5F);
        helper.assertBlockPresent(BlockListMF.nailed_planks, 1, 1, 1);
        helper.succeed();
    }

    // endregion

    // region crucible

    @GameTest(timeoutTicks = 600)
    public static void crucibleOverLavaAlloysItsGrid(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        reload(
                tx -> tx.add(
                        MFRecipes.ALLOY,
                        id("alloy", "bar"),
                        new Alloy(new ItemStack(bar), 0, Arrays.asList(new ItemStack(ore), new ItemStack(seed))),
                        0));
        // Lava in a stone basin, the crucible on top taking its heat
        helper.setBlock(1, 0, 1, Blocks.stone);
        for (int[] side : new int[][] { { 0, 1 }, { 2, 1 }, { 1, 0 }, { 1, 2 } }) {
            helper.setBlock(side[0], 1, side[1], Blocks.stone);
        }
        helper.setBlock(1, 1, 1, Blocks.lava);
        helper.setBlock(1, 2, 1, BlockListMF.crucible);
        TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 2, 1);
        crucible.setInventorySlotContents(0, new ItemStack(ore));
        crucible.setInventorySlotContents(1, new ItemStack(seed));
        Stations.keepUntilFinished();
        int output = crucible.getSizeInventory() - 1;
        helper.succeedWhen(() -> {
            ItemStack made = crucible.getStackInSlot(output);
            if (made == null) {
                return false;
            }
            assertEquals(bar, made.getItem());
            assertNull("the grid paid its ore", crucible.getStackInSlot(0));
            assertNull("the grid paid its seed", crucible.getStackInSlot(1));
            return true;
        });
    }

    @GameTest(timeoutTicks = 60)
    public static void crucibleWithoutHeatDoesNotSmelt(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        reload(
                tx -> tx.add(
                        MFRecipes.ALLOY,
                        id("alloy", "cold"),
                        new Alloy(new ItemStack(flour), 0, Arrays.asList(new ItemStack(junk))),
                        0));
        helper.setBlock(1, 1, 1, BlockListMF.crucible);
        TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 1, 1);
        crucible.setInventorySlotContents(0, new ItemStack(junk));
        crucible.progress = crucible.progressMax - 2;
        Stations.keepUntilFinished();
        helper.onEachTick("nothing smelts", () -> assertNull(crucible.getStackInSlot(crucible.getSizeInventory() - 1)));
        helper.succeedAtTimeout();
    }

    // endregion
}
