package minefantasy.mf2.api.recipe;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/** The slots a plan reads and writes. */
public interface CraftInventory {

    int size();

    /** The stack in the slot, or null. Callers must not modify it. */
    ItemStack get(int slot);

    void set(int slot, ItemStack stack);

    /** Largest stack the slot takes for the given item. */
    default int limit(int slot, ItemStack stack) {
        return stack.getMaxStackSize();
    }

    static CraftInventory of(IInventory inventory) {
        return of(inventory, inventory.getInventoryStackLimit());
    }

    /**
     * The inventory with its own stack limit for what the station places itself. A one-item slot for hoppers and hands
     * (the tanning rack, the spit) still holds what the recipe takes and makes there.
     */
    static CraftInventory of(IInventory inventory, int stackLimit) {
        return new CraftInventory() {

            @Override
            public int size() {
                return inventory.getSizeInventory();
            }

            @Override
            public ItemStack get(int slot) {
                return inventory.getStackInSlot(slot);
            }

            @Override
            public void set(int slot, ItemStack stack) {
                inventory.setInventorySlotContents(slot, stack);
            }

            @Override
            public int limit(int slot, ItemStack stack) {
                return Math.min(stack.getMaxStackSize(), stackLimit);
            }
        };
    }

    /** A plain array, for tests and simulations. */
    static CraftInventory of(ItemStack[] slots) {
        return new CraftInventory() {

            @Override
            public int size() {
                return slots.length;
            }

            @Override
            public ItemStack get(int slot) {
                return slots[slot];
            }

            @Override
            public void set(int slot, ItemStack stack) {
                slots[slot] = stack;
            }
        };
    }
}
