package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.container.ContainerAnvilMF;
import minefantasy.mf2.container.ContainerBigFurnace;
import minefantasy.mf2.container.ContainerCarpenterMF;
import minefantasy.mf2.container.ContainerCrucible;
import minefantasy.mf2.container.ContainerForge;
import minefantasy.mf2.gametest.Modders;

/**
 * Two players in the windows of one station, clicking in turns. Whatever they do, every item exists exactly once:
 * across both inventories, both cursors and the station.
 */
@GameTestHolder("minefantasy2")
public class TwoPlayersTest {

    private TwoPlayersTest() {}

    private static final class Pair<T extends IInventory> {

        final T station;
        final FakePlayer a, b;
        final Container windowA, windowB;

        Pair(T station, FakePlayer a, FakePlayer b, Container windowA, Container windowB) {
            this.station = station;
            this.a = a;
            this.b = b;
            this.windowA = windowA;
            this.windowB = windowB;
        }

        /** A click by one of them, checked: nothing made or lost, no station slot over its limit. */
        void click(EntityPlayer who, int slot, int button, int mode) {
            Container window = who == a ? windowA : windowB;
            Map<String, Integer> before = total();
            window.slotClick(slot, button, mode, who);
            assertEquals("a click made or lost items", before, total());
            for (int i = 0; i < station.getSizeInventory(); i++) {
                ItemStack stack = station.getStackInSlot(i);
                if (stack != null) {
                    assertTrue(
                            "station slot " + i + " over its limit",
                            stack.stackSize <= Math.min(stack.getMaxStackSize(), station.getInventoryStackLimit()));
                }
            }
        }

        /** The window slot that shows station slot i; the same index in both windows. */
        int stationSlot(int index) {
            for (int i = 0; i < windowA.inventorySlots.size(); i++) {
                Slot slot = (Slot) windowA.inventorySlots.get(i);
                if (slot.inventory == station && slot.getSlotIndex() == index) return i;
            }
            throw new IllegalArgumentException("no window slot shows station slot " + index);
        }

        int playerSlot(EntityPlayer who, int index) {
            Container window = who == a ? windowA : windowB;
            for (int i = 0; i < window.inventorySlots.size(); i++) {
                Slot slot = (Slot) window.inventorySlots.get(i);
                if (slot.inventory == who.inventory && slot.getSlotIndex() == index) return i;
            }
            throw new IllegalArgumentException("no window slot shows inventory slot " + index);
        }

        private Map<String, Integer> total() {
            Map<String, Integer> counts = new TreeMap<>();
            add(counts, station);
            for (EntityPlayer who : new EntityPlayer[] { a, b }) {
                add(counts, who.inventory);
                ItemStack cursor = who.inventory.getItemStack();
                if (cursor != null) counts.merge(key(cursor), cursor.stackSize, Integer::sum);
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
            String name = Item.itemRegistry.getNameForObject(stack.getItem()) + "@" + stack.getItemDamage();
            return stack.hasTagCompound() ? name + stack.getTagCompound() : name;
        }
    }

    private static FakePlayer player(GameTestHelper helper, String name, TestPos at) {
        FakePlayer player = Modders.fresh(helper, name);
        player.inventory.clearInventory(null, -1);
        player.setPosition(at.x() + 0.5, at.y() + 1, at.z() + 0.5);
        return player;
    }

    private static <T extends TileEntity & IInventory> Pair<T> pair(GameTestHelper helper, Block block, Class<T> type,
            BiFunction<FakePlayer, T, Container> open) {
        helper.setBlock(1, 1, 1, block);
        T station = helper.assertTileEntityPresent(type, 1, 1, 1);
        TestPos at = helper.absolute(1, 1, 1);
        FakePlayer a = player(helper, Modders.SMITH, at);
        FakePlayer b = player(helper, Modders.SCHOLAR, at);
        return new Pair<>(station, a, b, open.apply(a, station), open.apply(b, station));
    }

    private static final int PICK = 0, SHIFT = 1;

    /** Both reach for the same stack; one gets it, the other nothing. Then both shift-click against each other. */
    private static <T extends TileEntity & IInventory> void contend(Pair<T> p, Item item) {
        int slot = p.stationSlot(0);
        p.station.setInventorySlotContents(0, new ItemStack(item, Math.min(40, p.station.getInventoryStackLimit())));

        p.click(p.a, slot, 0, PICK);
        p.click(p.b, slot, 0, PICK);
        assertNotNull("the first hand came away empty", p.a.inventory.getItemStack());
        assertNull("the second hand took a stack that was gone", p.b.inventory.getItemStack());

        // The first puts it back half at a time while the second takes one at a time
        p.click(p.a, slot, 1, PICK);
        p.click(p.b, slot, 1, PICK);
        p.click(p.a, slot, 0, PICK);
        p.click(p.b, slot, 0, PICK);
        assertNull(p.a.inventory.getItemStack());

        // Each empties the cursor into their inventory, then they shift the coal back and forth
        p.b.inventory.addItemStackToInventory(p.b.inventory.getItemStack());
        p.b.inventory.setItemStack(null);
        p.a.inventory.setInventorySlotContents(9, new ItemStack(item, 64));
        for (int round = 0; round < 4; round++) {
            p.click(p.a, p.playerSlot(p.a, 9), 0, SHIFT);
            p.click(p.b, slot, 0, SHIFT);
            p.click(p.a, slot, 0, SHIFT);
            p.click(p.b, p.playerSlot(p.b, 9), 0, SHIFT);
        }
    }

    @GameTest
    public static void anvil(GameTestHelper helper) {
        contend(
                pair(
                        helper,
                        BlockListMF.anvilStone,
                        TileEntityAnvilMF.class,
                        (p, t) -> new ContainerAnvilMF(p.inventory, t)),
                Items.coal);
        helper.succeed();
    }

    @GameTest
    public static void carpenter(GameTestHelper helper) {
        contend(
                pair(
                        helper,
                        BlockListMF.carpenter,
                        TileEntityCarpenterMF.class,
                        (p, t) -> new ContainerCarpenterMF(p.inventory, t)),
                Items.coal);
        helper.succeed();
    }

    @GameTest
    public static void crucible(GameTestHelper helper) {
        contend(
                pair(
                        helper,
                        BlockListMF.crucible,
                        TileEntityCrucible.class,
                        (p, t) -> new ContainerCrucible(p.inventory, t)),
                Items.coal);
        helper.succeed();
    }

    @GameTest
    public static void forge(GameTestHelper helper) {
        contend(
                pair(helper, BlockListMF.forge, TileEntityForge.class, (p, t) -> new ContainerForge(p.inventory, t)),
                Items.coal);
        helper.succeed();
    }

    @GameTest
    public static void bigFurnace(GameTestHelper helper) {
        contend(
                pair(
                        helper,
                        BlockListMF.furnace_stone,
                        TileEntityBigFurnace.class,
                        (p, t) -> new ContainerBigFurnace(p, t)),
                Item.getItemFromBlock(Blocks.iron_ore));
        helper.succeed();
    }
}
