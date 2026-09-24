package minefantasy.mf2.block.tileentity;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * The slot handling every station inventory repeats: taking from a slot, emptying it, and saving the slots. Each
 * station still calls its own change hooks around these.
 */
public final class InventorySlots {

    private static final int COMPOUND = 10;

    private InventorySlots() {}

    /** Takes up to the given amount from the slot, emptying it when nothing is left; null for an empty slot. */
    public static ItemStack take(ItemStack[] slots, int slot, int amount) {
        ItemStack stack = slots[slot];
        if (stack == null) {
            return null;
        }
        if (stack.stackSize <= amount) {
            slots[slot] = null;
            return stack;
        }
        ItemStack taken = stack.splitStack(amount);
        if (stack.stackSize == 0) {
            slots[slot] = null;
        }
        return taken;
    }

    /** Empties the slot, returning what it held. */
    public static ItemStack takeAll(ItemStack[] slots, int slot) {
        ItemStack stack = slots[slot];
        slots[slot] = null;
        return stack;
    }

    /** Saves the filled slots under the key, each with its slot number. */
    public static void write(NBTTagCompound nbt, String key, ItemStack[] slots) {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                NBTTagCompound saved = new NBTTagCompound();
                saved.setByte("Slot", (byte) i);
                slots[i].writeToNBT(saved);
                list.appendTag(saved);
            }
        }
        nbt.setTag(key, list);
    }

    /** Loads slots saved by {@link #write} into a new array of the given size; slots outside it are dropped. */
    public static ItemStack[] read(NBTTagCompound nbt, String key, int size) {
        ItemStack[] slots = new ItemStack[size];
        NBTTagList list = nbt.getTagList(key, COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound saved = list.getCompoundTagAt(i);
            byte slot = saved.getByte("Slot");
            if (slot >= 0 && slot < size) {
                slots[slot] = ItemStack.loadItemStackFromNBT(saved);
            }
        }
        return slots;
    }
}
