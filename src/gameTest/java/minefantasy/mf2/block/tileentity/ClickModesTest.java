package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.container.ContainerAnvilMF;
import minefantasy.mf2.container.ContainerBigFurnace;
import minefantasy.mf2.container.ContainerCrucible;
import minefantasy.mf2.container.ContainerQuern;
import minefantasy.mf2.gametest.Modders;

/**
 * The clicks besides picking up and shift-clicking: double-click to collect, throwing, number keys, dragging and the
 * creative middle click. None makes or loses items, and none reaches into an output slot, which only gives out its
 * contents through its own pickup: a double-click that swept it would skip that.
 */
@GameTestHolder("minefantasy2")
public class ClickModesTest {

    private ClickModesTest() {}

    private static final int PICK = 0, SWAP = 2, CLONE = 3, THROW = 4, DRAG = 5, COLLECT = 6;
    private static final int OUTSIDE = -999;

    private static final class Window<T extends TileEntity & IInventory> {

        final GameTestHelper helper;
        final T station;
        final FakePlayer player;
        final Container container;
        final int output;

        Window(GameTestHelper helper, T station, FakePlayer player, Container container, int output) {
            this.helper = helper;
            this.station = station;
            this.player = player;
            this.container = container;
            this.output = output;
        }

        int stationSlot(int index) {
            return slotOf(station, index);
        }

        int playerSlot(int index) {
            return slotOf(player.inventory, index);
        }

        private int slotOf(IInventory inventory, int index) {
            for (int i = 0; i < container.inventorySlots.size(); i++) {
                Slot slot = (Slot) container.inventorySlots.get(i);
                if (slot.inventory == inventory && slot.getSlotIndex() == index) return i;
            }
            throw new IllegalArgumentException("no window slot shows " + index);
        }

        /** A click, checked: nothing made or lost counting what was thrown, the output never given anything. */
        void click(int slot, int button, int mode) {
            Map<String, Integer> before = total();
            ItemStack out = station.getStackInSlot(output);
            int outBefore = out == null ? 0 : out.stackSize;
            container.slotClick(slot, button, mode, player);
            assertEquals("mode " + mode + " made or lost items", before, total());
            out = station.getStackInSlot(output);
            assertTrue("mode " + mode + " put into the output", (out == null ? 0 : out.stackSize) <= outBefore);
            for (int i = 0; i < station.getSizeInventory(); i++) {
                ItemStack stack = station.getStackInSlot(i);
                if (stack != null) {
                    assertTrue(
                            "mode " + mode + " overfilled slot " + i,
                            stack.stackSize <= Math.min(stack.getMaxStackSize(), station.getInventoryStackLimit()));
                }
            }
        }

        private Map<String, Integer> total() {
            Map<String, Integer> counts = new TreeMap<>();
            add(counts, station);
            add(counts, player.inventory);
            ItemStack cursor = player.inventory.getItemStack();
            if (cursor != null) counts.merge(key(cursor), cursor.stackSize, Integer::sum);
            TestPos at = helper.absolute(1, 1, 1);
            List<?> dropped = helper.getWorld().getEntitiesWithinAABB(
                    EntityItem.class,
                    AxisAlignedBB
                            .getBoundingBox(at.x() - 4, at.y() - 4, at.z() - 4, at.x() + 5, at.y() + 5, at.z() + 5));
            for (Object o : dropped) {
                ItemStack stack = ((EntityItem) o).getEntityItem();
                counts.merge(key(stack), stack.stackSize, Integer::sum);
            }
            return counts;
        }

        private static void add(Map<String, Integer> counts, IInventory inventory) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack != null) counts.merge(key(stack), stack.stackSize, Integer::sum);
            }
        }

        private static String key(ItemStack stack) {
            return Item.itemRegistry.getNameForObject(stack.getItem()) + "@" + stack.getItemDamage();
        }
    }

    private static <T extends TileEntity & IInventory> Window<T> open(GameTestHelper helper, Block block, Class<T> type,
            int output, BiFunction<EntityPlayer, T, Container> open) {
        helper.setBlock(1, 1, 1, block);
        T station = helper.assertTileEntityPresent(type, 1, 1, 1);
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        TestPos at = helper.absolute(1, 1, 1);
        player.setPosition(at.x() + 0.5, at.y() + 1, at.z() + 0.5);
        player.inventory.clearInventory(null, -1);
        return new Window<>(helper, station, player, open.apply(player, station), output);
    }

    /**
     * Every mode against the station: the output holds some of an item, an input slot some more, the player the rest.
     */
    private static <T extends TileEntity & IInventory> void everyMode(Window<T> w, ItemStack item, int input) {
        w.station.setInventorySlotContents(w.output, copy(item, 5));
        w.station.setInventorySlotContents(input, copy(item, 3));
        w.player.inventory.setInventorySlotContents(9, copy(item, 1));
        w.player.inventory.setInventorySlotContents(0, copy(item, 10));

        // Double-click: the cursor gathers from the window, but not from the output
        w.click(w.playerSlot(9), 0, PICK);
        w.click(w.playerSlot(9), 0, COLLECT);
        ItemStack left = w.station.getStackInSlot(w.output);
        assertEquals("a double-click swept the output", 5, left == null ? 0 : left.stackSize);
        w.click(w.playerSlot(9), 0, PICK);

        // Throwing one and a whole stack out of the window
        w.click(w.stationSlot(input), 0, THROW);
        w.click(w.stationSlot(w.output), 1, THROW);

        // Number keys: a hotbar stack goes nowhere near the output
        w.click(w.stationSlot(w.output), 0, SWAP);
        w.click(w.stationSlot(w.output), 1, SWAP);

        // Dragging a stack over an input and the output
        w.click(w.playerSlot(0), 0, PICK);
        w.click(OUTSIDE, 0, DRAG);
        w.click(w.stationSlot(input), 1, DRAG);
        w.click(w.stationSlot(w.output), 1, DRAG);
        w.click(OUTSIDE, 2, DRAG);
        if (w.player.inventory.getItemStack() != null) w.click(w.playerSlot(0), 0, PICK);

        // The creative middle click from a survival player
        w.click(w.stationSlot(input), 2, CLONE);
        assertNull("a survival player cloned a stack", w.player.inventory.getItemStack());
    }

    private static ItemStack copy(ItemStack item, int count) {
        ItemStack stack = item.copy();
        stack.stackSize = count;
        return stack;
    }

    @GameTest
    public static void anvil(GameTestHelper helper) {
        Window<TileEntityAnvilMF> w = open(
                helper,
                BlockListMF.anvilStone,
                TileEntityAnvilMF.class,
                new TileEntityAnvilMF().getSizeInventory() - 1,
                (p, t) -> new ContainerAnvilMF(p.inventory, t));
        everyMode(w, new ItemStack(Items.iron_ingot), 0);
        helper.succeed();
    }

    @GameTest
    public static void crucible(GameTestHelper helper) {
        Window<TileEntityCrucible> w = open(
                helper,
                BlockListMF.crucible,
                TileEntityCrucible.class,
                new TileEntityCrucible().getSizeInventory() - 1,
                (p, t) -> new ContainerCrucible(p.inventory, t));
        everyMode(w, new ItemStack(Items.iron_ingot), 0);
        helper.succeed();
    }

    @GameTest
    public static void bigFurnace(GameTestHelper helper) {
        Window<TileEntityBigFurnace> w = open(
                helper,
                BlockListMF.furnace_stone,
                TileEntityBigFurnace.class,
                4,
                (p, t) -> new ContainerBigFurnace(p, t));
        everyMode(w, new ItemStack(net.minecraft.init.Blocks.iron_ore), 0);
        helper.succeed();
    }

    @GameTest
    public static void quern(GameTestHelper helper) {
        Window<TileEntityQuern> w = open(
                helper,
                BlockListMF.quern,
                TileEntityQuern.class,
                2,
                (p, t) -> new ContainerQuern(p.inventory, t));
        everyMode(w, new ItemStack(minefantasy.mf2.item.list.ComponentListMF.clay_pot), 1);
        helper.succeed();
    }
}
