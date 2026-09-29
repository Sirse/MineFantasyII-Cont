package minefantasy.mf2.container;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.archery.AmmoMechanicsMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.CustomToolListMF;

/**
 * The reload window of a bow held in hand. It shows the loaded ammunition, but the bow is the truth: whatever happens
 * to the bow while the window is open, a click never brings back arrows that were already shot or loses ones loaded
 * since.
 */
@GameTestHolder("minefantasy2")
public class ReloadWindowTest {

    private ReloadWindowTest() {}

    private static final int MAIN = 9, AMMO_SLOT = 0, PICK = 0, SHIFT = 1, SWAP = 2;

    private static ItemStack arrows(int count) {
        return new ItemStack(CustomToolListMF.standard_arrow, count);
    }

    private static ItemStack bow(int loaded) {
        ItemStack bow = new ItemStack(CustomToolListMF.standard_bow);
        AmmoMechanicsMF.setAmmo(bow, loaded > 0 ? arrows(loaded) : null);
        return bow;
    }

    private static FakePlayer holding(GameTestHelper helper, ItemStack bow) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.inventory.clearInventory(null, -1);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, bow);
        return player;
    }

    private static int loaded(ItemStack bow) {
        ItemStack ammo = AmmoMechanicsMF.getAmmo(bow);
        return ammo == null ? 0 : ammo.stackSize;
    }

    /** Arrows the player has anywhere: inventory, cursor and loaded in every weapon. */
    private static int arrowsOf(EntityPlayer player) {
        int total = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack == null) continue;
            if (stack.getItem() == CustomToolListMF.standard_arrow) total += stack.stackSize;
            else total += loaded(stack);
        }
        ItemStack cursor = player.inventory.getItemStack();
        return cursor == null ? total : total + cursor.stackSize;
    }

    private static int slotOf(ContainerReload window, EntityPlayer player, int index) {
        for (int i = 0; i < window.inventorySlots.size(); i++) {
            net.minecraft.inventory.Slot slot = (net.minecraft.inventory.Slot) window.inventorySlots.get(i);
            if (slot.inventory == player.inventory && slot.getSlotIndex() == index) return i;
        }
        throw new IllegalArgumentException("no window slot shows inventory slot " + index);
    }

    @GameTest
    public static void loadingAndUnloadingKeepsEveryArrow(GameTestHelper helper) {
        ItemStack bow = bow(0);
        FakePlayer archer = holding(helper, bow);
        archer.inventory.setInventorySlotContents(MAIN, arrows(40));
        ContainerReload window = new ContainerReload(archer.inventory, bow);

        int stack = arrows(1).getMaxStackSize();
        window.slotClick(slotOf(window, archer, MAIN), 0, SHIFT, archer);
        assertEquals("the bow did not take a stack", stack, loaded(bow));
        window.slotClick(AMMO_SLOT, 1, PICK, archer);
        assertEquals("half did not come off", stack / 2, loaded(bow));
        archer.inventory.addItemStackToInventory(archer.inventory.getItemStack());
        archer.inventory.setItemStack(null);
        window.slotClick(AMMO_SLOT, 0, SHIFT, archer);
        assertEquals(0, loaded(bow));
        assertEquals("arrows made or lost", 40, arrowsOf(archer));
        helper.succeed();
    }

    @GameTest
    public static void arrowsShotWhileOpenDoNotComeBack(GameTestHelper helper) {
        ItemStack bow = bow(10);
        FakePlayer archer = holding(helper, bow);
        ContainerReload window = new ContainerReload(archer.inventory, bow);

        // The server takes a shot while the window is open: the bow now holds 9
        AmmoMechanicsMF.setAmmo(bow, arrows(9));
        window.slotClick(AMMO_SLOT, 0, PICK, archer);
        assertEquals("a shot arrow came back", 9, arrowsOf(archer));
        helper.succeed();
    }

    @GameTest
    public static void arrowsLoadedWhileOpenAreNotLost(GameTestHelper helper) {
        ItemStack bow = bow(10);
        FakePlayer archer = holding(helper, bow);
        archer.inventory.setInventorySlotContents(MAIN, arrows(5));
        ContainerReload window = new ContainerReload(archer.inventory, bow);

        // An ammunition box tops the bow up while the window is open
        AmmoMechanicsMF.setAmmo(bow, arrows(14));
        window.slotClick(slotOf(window, archer, MAIN), 0, SHIFT, archer);
        assertEquals("arrows loaded from the box were lost", 19, arrowsOf(archer));
        helper.succeed();
    }

    @GameTest
    public static void theWindowRefusesOnceTheBowLeavesTheHand(GameTestHelper helper) {
        ItemStack bow = bow(10);
        FakePlayer archer = holding(helper, bow);
        ContainerReload window = new ContainerReload(archer.inventory, bow);

        archer.inventory.setInventorySlotContents(0, new ItemStack(Items.stick));
        assertFalse(window.canInteractWith(archer));
        window.slotClick(AMMO_SLOT, 0, PICK, archer);
        assertNull("the ammunition was taken from a bow not in hand", archer.inventory.getItemStack());
        assertEquals(10, loaded(bow));
        helper.succeed();
    }

    /** Death drops the inventory, bow and all, before the window is gone: its ammunition goes with the dropped bow. */
    @GameTest
    public static void aDeadArchersWindowTakesNothing(GameTestHelper helper) {
        ItemStack bow = bow(10);
        FakePlayer archer = holding(helper, bow);
        ContainerReload window = new ContainerReload(archer.inventory, bow);

        archer.inventory.dropAllItems();
        assertFalse(window.canInteractWith(archer));
        window.slotClick(AMMO_SLOT, 0, PICK, archer);
        assertNull("a dead archer took the ammunition", archer.inventory.getItemStack());
        assertEquals("the dropped bow lost its ammunition", 10, loaded(bow));
        helper.succeed();
    }

    @GameTest
    public static void theOpenBowCannotBeSwappedOutByNumberKey(GameTestHelper helper) {
        ItemStack bow = bow(10);
        FakePlayer archer = holding(helper, bow);
        ContainerReload window = new ContainerReload(archer.inventory, bow);

        window.slotClick(slotOf(window, archer, MAIN), 0, SWAP, archer);
        assertSame("the bow left the hand", bow, archer.inventory.getStackInSlot(0));
        assertEquals(10, arrowsOf(archer));
        helper.succeed();
    }
}
