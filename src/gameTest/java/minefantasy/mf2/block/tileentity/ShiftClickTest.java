package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.container.ContainerAnvilMF;
import minefantasy.mf2.container.ContainerBigFurnace;
import minefantasy.mf2.container.ContainerBlastChamber;
import minefantasy.mf2.container.ContainerBlastHeater;
import minefantasy.mf2.container.ContainerBloomery;
import minefantasy.mf2.container.ContainerCarpenterMF;
import minefantasy.mf2.container.ContainerCrucible;
import minefantasy.mf2.container.ContainerForge;
import minefantasy.mf2.container.ContainerKitchenBench;
import minefantasy.mf2.container.ContainerQuern;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.gametest.TestItems;
import minefantasy.mf2.item.list.ComponentListMF;

/**
 * Shift-clicks through the real containers, the way the client sends them: between the player's inventory and a
 * station, with a full inventory, partial stacks, different materials and hot pieces. After every click no item is made
 * or lost, and the station's slots hold only what they accept, within their limits.
 */
@GameTestHolder("minefantasy2")
public class ShiftClickTest {

    private ShiftClickTest() {}

    // region fixture

    /** A station, its container and the player looking into it. */
    private static final class Bench {

        final IInventory station;
        final Container container;
        final EntityPlayer player;

        Bench(IInventory station, Container container, EntityPlayer player) {
            this.station = station;
            this.container = container;
            this.player = player;
        }

        /** The container slot showing the player's inventory slot (0-8 hotbar, 9-35 main). */
        int playerSlot(int inventoryIndex) {
            for (int i = 0; i < container.inventorySlots.size(); i++) {
                Slot slot = (Slot) container.inventorySlots.get(i);
                if (slot.inventory == player.inventory && slot.getSlotIndex() == inventoryIndex) {
                    return i;
                }
            }
            throw new IllegalArgumentException("no container slot shows inventory slot " + inventoryIndex);
        }

        /** The container slot showing the station's slot. */
        int stationSlot(int stationIndex) {
            for (int i = 0; i < container.inventorySlots.size(); i++) {
                Slot slot = (Slot) container.inventorySlots.get(i);
                if (slot.inventory == station && slot.getSlotIndex() == stationIndex) {
                    return i;
                }
            }
            throw new IllegalArgumentException("no container slot shows station slot " + stationIndex);
        }

        void give(int inventoryIndex, ItemStack stack) {
            player.inventory.setInventorySlotContents(inventoryIndex, stack);
        }

        /** A shift-click, checked: nothing made or lost, the station's slots within their rules. */
        void shift(int containerSlot) {
            Map<String, Integer> before = total();
            container.slotClick(containerSlot, 0, 1, player);
            assertEquals("a shift-click made or lost items", before, total());
            for (Object o : container.inventorySlots) {
                Slot slot = (Slot) o;
                ItemStack stack = slot.getStack();
                if (slot.inventory != station || stack == null) continue;
                int limit = Math.min(stack.getMaxStackSize(), station.getInventoryStackLimit());
                assertTrue(
                        "station slot " + slot.getSlotIndex() + " holds " + stack.stackSize + " over " + limit,
                        stack.stackSize <= limit);
            }
        }

        void shiftFromPlayer(int inventoryIndex) {
            shift(playerSlot(inventoryIndex));
        }

        void shiftFromStation(int stationIndex) {
            shift(stationSlot(stationIndex));
        }

        ItemStack at(int stationIndex) {
            return station.getStackInSlot(stationIndex);
        }

        ItemStack held(int inventoryIndex) {
            return player.inventory.getStackInSlot(inventoryIndex);
        }

        private Map<String, Integer> total() {
            Map<String, Integer> counts = new TreeMap<>();
            add(counts, player.inventory);
            add(counts, station);
            ItemStack cursor = player.inventory.getItemStack();
            if (cursor != null) counts.merge(key(cursor), cursor.stackSize, Integer::sum);
            return counts;
        }

        private static void add(Map<String, Integer> counts, IInventory inventory) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack != null) counts.merge(key(stack), stack.stackSize, Integer::sum);
            }
        }

        private static String key(ItemStack stack) {
            String name = Item.itemRegistry.getNameForObject(stack.getItem()) + "@" + stack.getItemDamage();
            return stack.hasTagCompound() ? name + stack.getTagCompound() : name;
        }
    }

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = helper.spawnFakePlayer(Modders.SMITH);
        player.inventory.clearInventory(null, -1);
        return player;
    }

    private static int size(ItemStack stack) {
        return stack == null ? 0 : stack.stackSize;
    }

    /** The first main inventory slot and the first hotbar slot. */
    private static final int MAIN = 9, HOTBAR = 0;

    // endregion

    // region grids: anvil, carpenter, kitchen

    @GameTest
    public static void anvilTakesIntoItsGridNeverItsOutputAndKeepsPiecesApart(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.anvilStone);
        TileEntityAnvilMF anvil = helper.assertTileEntityPresent(TileEntityAnvilMF.class, 1, 1, 1);
        FakePlayer smith = player(helper);
        Bench bench = new Bench(anvil, new ContainerAnvilMF(smith.inventory, anvil), smith);
        int output = anvil.getSizeInventory() - 1;

        // A steel bar on the grid does not take a bronze one, and neither goes to the output
        anvil.setInventorySlotContents(0, of(bar, "steel", 1));
        bench.give(MAIN, of(bar, "bronze", 5));
        bench.shiftFromPlayer(MAIN);
        assertNull("the bronze stayed in the inventory", bench.held(MAIN));
        assertEquals("steel and bronze stacked together", 1, size(anvil.getStackInSlot(0)));
        assertNull("something went to the output", anvil.getStackInSlot(output));

        // Hot pieces of different heat are different stacks
        bench.give(MAIN, heated(new ItemStack(ore), 300));
        bench.shiftFromPlayer(MAIN);
        bench.give(MAIN, heated(new ItemStack(ore), 400));
        bench.shiftFromPlayer(MAIN);
        int hot = 0;
        for (int slot = 0; slot < output; slot++) {
            if (anvil.getStackInSlot(slot) != null && anvil.getStackInSlot(slot).getItem() == TestItems.hot) hot++;
        }
        assertEquals("pieces of different heat merged", 2, hot);
        assertNull(anvil.getStackInSlot(output));

        // The result goes back to the player whole
        anvil.setInventorySlotContents(output, new ItemStack(flour, 3));
        bench.shiftFromStation(output);
        assertNull("the output kept part of the result", anvil.getStackInSlot(output));
        helper.succeed();
    }

    /** A stack of the item made of the material. */
    private static ItemStack of(Item item, String material, int count) {
        ItemStack stack = Stations.of(item, minefantasy.mf2.api.material.CustomMaterial.getMaterial(material));
        stack.stackSize = count;
        return stack;
    }

    @GameTest
    public static void carpenterFillsPartialStacksThenEmptySlots(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.carpenter);
        TileEntityCarpenterMF carpenter = helper.assertTileEntityPresent(TileEntityCarpenterMF.class, 1, 1, 1);
        FakePlayer maker = player(helper);
        Bench bench = new Bench(carpenter, new ContainerCarpenterMF(maker.inventory, carpenter), maker);

        carpenter.setInventorySlotContents(0, new ItemStack(seed, 60));
        bench.give(MAIN, new ItemStack(seed, 10));
        bench.shiftFromPlayer(MAIN);
        assertEquals("the partial stack was not topped up", 64, size(carpenter.getStackInSlot(0)));
        int rest = 0;
        for (int slot = 1; slot < carpenter.getSizeInventory() - 5; slot++) {
            rest += size(carpenter.getStackInSlot(slot));
        }
        assertEquals("the rest did not go to another grid slot", 6, rest);
        assertNull(bench.held(MAIN));
        assertNull(carpenter.getStackInSlot(carpenter.getSizeInventory() - 5));
        helper.succeed();
    }

    @GameTest
    public static void kitchenBenchNeverFillsItsOutput(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.kitchenBench);
        TileEntityKitchenBench kitchen = helper.assertTileEntityPresent(TileEntityKitchenBench.class, 1, 1, 1);
        FakePlayer cook = player(helper);
        Bench bench = new Bench(kitchen, new ContainerKitchenBench(cook.inventory, kitchen), cook);
        int output = kitchen.getSizeInventory() - 5;
        // A full grid: what does not fit goes to the surplus slots, never the output
        for (int slot = 0; slot < output; slot++) {
            kitchen.setInventorySlotContents(slot, new ItemStack(junk, 64));
        }
        bench.give(MAIN, new ItemStack(seed, 20));
        bench.shiftFromPlayer(MAIN);
        assertNull("the output took the shift-click", kitchen.getStackInSlot(output));
        assertEquals(20, size(kitchen.getStackInSlot(output + 1)));
        helper.succeed();
    }

    // endregion

    // region filtered stations

    @GameTest
    public static void quernSortsInputAndPotAndRefusesTheRest(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.QUERN,
                            id("quern", "seed"),
                            ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                            0));
            helper.setBlock(1, 1, 1, BlockListMF.quern);
            TileEntityQuern quern = helper.assertTileEntityPresent(TileEntityQuern.class, 1, 1, 1);
            FakePlayer miller = player(helper);
            Bench bench = new Bench(quern, new ContainerQuern(miller.inventory, quern), miller);

            bench.give(MAIN, new ItemStack(seed, 5));
            bench.shiftFromPlayer(MAIN);
            bench.give(MAIN + 1, new ItemStack(ComponentListMF.clay_pot, 3));
            bench.shiftFromPlayer(MAIN + 1);
            bench.give(MAIN + 2, new ItemStack(junk, 7));
            bench.shiftFromPlayer(MAIN + 2);
            assertEquals(seed, quern.getStackInSlot(0).getItem());
            assertEquals(ComponentListMF.clay_pot, quern.getStackInSlot(1).getItem());
            assertNull("the output took an item", quern.getStackInSlot(2));
            assertEquals("junk went to the hotbar", 7, size(bench.held(HOTBAR)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void bloomerySortsOreAndCarbon(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.BLOOMERY,
                            id("bloomery", "ore"),
                            BloomRecipe.of(Input.of(ore), new ItemStack(bar)),
                            0));
            helper.setBlock(1, 1, 1, BlockListMF.bloomery);
            TileEntityBloomery bloomery = helper.assertTileEntityPresent(TileEntityBloomery.class, 1, 1, 1);
            FakePlayer smelter = player(helper);
            Bench bench = new Bench(bloomery, new ContainerBloomery(smelter.inventory, bloomery), smelter);
            bench.give(HOTBAR, new ItemStack(carbon, 2));
            bench.shiftFromPlayer(HOTBAR);
            bench.give(HOTBAR + 1, new ItemStack(ore, 4));
            bench.shiftFromPlayer(HOTBAR + 1);
            bench.give(HOTBAR + 2, new ItemStack(junk));
            bench.shiftFromPlayer(HOTBAR + 2);
            assertEquals(ore, bloomery.getStackInSlot(0).getItem());
            assertEquals(carbon, bloomery.getStackInSlot(1).getItem());
            assertNull("the bloom slot took an item", bloomery.getStackInSlot(2));
            assertEquals("junk went to the main inventory", 1, size(bench.held(MAIN)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void blastFurnacePartsTakeOnlyTheirOwn(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.BLAST_FURNACE,
                            id("blast_furnace", "ore"),
                            ProcessRecipe.of(Input.of(ore), new ItemStack(bar)),
                            0));
            helper.setBlock(1, 1, 1, BlockListMF.blast_chamber);
            helper.setBlock(3, 1, 1, BlockListMF.blast_heater);
            TileEntityBlastFC chamber = helper.assertTileEntityPresent(TileEntityBlastFC.class, 1, 1, 1);
            TileEntityBlastFH heater = helper.assertTileEntityPresent(TileEntityBlastFH.class, 3, 1, 1);
            FakePlayer smelter = player(helper);

            Bench top = new Bench(chamber, new ContainerBlastChamber(smelter.inventory, chamber), smelter);
            top.give(MAIN, new ItemStack(ore, 3));
            top.shiftFromPlayer(MAIN);
            top.give(MAIN + 1, new ItemStack(carbon, 2));
            top.shiftFromPlayer(MAIN + 1);
            assertEquals(carbon, chamber.getStackInSlot(0).getItem());
            assertEquals(ore, chamber.getStackInSlot(1).getItem());

            Bench bottom = new Bench(heater, new ContainerBlastHeater(smelter.inventory, heater), smelter);
            bottom.give(MAIN + 2, new ItemStack(junk, 5));
            bottom.shiftFromPlayer(MAIN + 2);
            assertNull("the heater took something that does not burn", heater.getStackInSlot(0));
            bottom.give(MAIN + 3, new ItemStack(Items.coal, 5));
            bottom.shiftFromPlayer(MAIN + 3);
            assertEquals(Items.coal, heater.getStackInSlot(0).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void bigFurnaceFillsItsInputsNeverItsOutputs(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.BIG_FURNACE,
                            id("big_furnace", "seed"),
                            ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                            0));
            helper.setBlock(2, 1, 2, BlockListMF.furnace_heater, 0);
            helper.setBlock(2, 2, 2, BlockListMF.furnace_stone, 0);
            TileEntityBigFurnace furnace = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 2, 2);
            FakePlayer smelter = player(helper);
            Bench bench = new Bench(furnace, new ContainerBigFurnace(smelter, furnace), smelter);
            // More than four stacks: the inputs fill, the rest stays with the player
            for (int i = 0; i < 5; i++) {
                bench.give(MAIN + i, new ItemStack(seed, 64));
                bench.shiftFromPlayer(MAIN + i);
            }
            for (int slot = 0; slot < 4; slot++) {
                assertEquals("input " + slot, 64, size(furnace.getStackInSlot(slot)));
            }
            for (int slot = 4; slot < 8; slot++) {
                assertNull("output " + slot + " took an item", furnace.getStackInSlot(slot));
            }
            bench.give(MAIN + 5, new ItemStack(junk, 3));
            bench.shiftFromPlayer(MAIN + 5);
            for (int slot = 0; slot < 8; slot++) {
                ItemStack stack = furnace.getStackInSlot(slot);
                assertTrue("junk went into the furnace", stack == null || stack.getItem() != junk);
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void crucibleFillsItsGridNeverItsOutput(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.crucible);
        TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 1, 1);
        FakePlayer smelter = player(helper);
        Bench bench = new Bench(crucible, new ContainerCrucible(smelter.inventory, crucible), smelter);
        int output = crucible.getSizeInventory() - 1;
        for (int i = 0; i < 10; i++) {
            bench.give(MAIN + i, of(bar, i % 2 == 0 ? "copper" : "tin", 64));
            bench.shiftFromPlayer(MAIN + i);
        }
        assertNull("the output took an item", crucible.getStackInSlot(output));
        // Nine grid slots: the tenth stack finds no room and moves to the hotbar instead
        assertNull(bench.held(MAIN + 9));
        assertEquals("the tenth stack went into the crucible", 64, size(bench.held(HOTBAR)));

        crucible.setInventorySlotContents(output, new ItemStack(flour, 5));
        bench.shiftFromStation(output);
        assertNull("the output kept part of the result", crucible.getStackInSlot(output));
        helper.succeed();
    }

    @GameTest
    public static void forgeTakesAHotPieceAndGivesItBack(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.forge);
        TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 1, 1, 1);
        FakePlayer smith = player(helper);
        Bench bench = new Bench(forge, new ContainerForge(smith.inventory, forge), smith);
        bench.give(MAIN, heated(new ItemStack(ore, 3), 300));
        bench.shiftFromPlayer(MAIN);
        assertEquals(3, size(forge.getStackInSlot(0)));
        bench.shiftFromStation(0);
        assertNull(forge.getStackInSlot(0));
        helper.succeed();
    }

    // endregion

    // region the player's side

    @GameTest
    public static void aFullInventoryKeepsTheStationsItems(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.carpenter);
        TileEntityCarpenterMF carpenter = helper.assertTileEntityPresent(TileEntityCarpenterMF.class, 1, 1, 1);
        FakePlayer maker = player(helper);
        Bench bench = new Bench(carpenter, new ContainerCarpenterMF(maker.inventory, carpenter), maker);
        for (int i = 0; i < 36; i++) {
            bench.give(i, new ItemStack(junk, 64));
        }
        carpenter.setInventorySlotContents(0, new ItemStack(seed, 10));
        bench.shiftFromStation(0);
        assertEquals("the grid lost items into a full inventory", 10, size(carpenter.getStackInSlot(0)));

        // A partly free stack takes what fits and the rest stays
        bench.give(0, new ItemStack(seed, 60));
        bench.shiftFromStation(0);
        assertEquals(64, size(bench.held(0)));
        assertEquals(6, size(carpenter.getStackInSlot(0)));
        helper.succeed();
    }

    // endregion

}
