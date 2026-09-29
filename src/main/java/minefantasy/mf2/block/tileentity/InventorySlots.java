package minefantasy.mf2.block.tileentity;

import java.util.Random;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

/**
 * The slot handling every station inventory repeats: taking from a slot, emptying it, saving the slots, and throwing
 * items out into the world. Each station still calls its own change hooks around these.
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

    /** Throws out everything in the inventory, as a broken block does, and empties its slots. */
    public static void spill(World world, int x, int y, int z, IInventory inventory) {
        spill(world, x, y, z, inventory, 0, inventory.getSizeInventory());
    }

    /** Throws out the slots from {@code from} up to {@code to}, and empties them. */
    public static void spill(World world, int x, int y, int z, IInventory inventory, int from, int to) {
        for (int slot = from; slot < to; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null) {
                inventory.setInventorySlotContents(slot, null);
                drop(world, x, y, z, stack);
            }
        }
    }

    /**
     * Throws a stack out of the block at a random point inside it, tossed a little upward as vanilla does, in pieces no
     * larger than a stack. The stack given is used up.
     */
    public static void drop(World world, int x, int y, int z, ItemStack stack) {
        if (world == null || world.isRemote || stack == null || stack.getItem() == null) {
            return;
        }
        Random rand = world.rand;
        float dx = rand.nextFloat() * 0.8F + 0.1F;
        float dy = rand.nextFloat() * 0.8F + 0.1F;
        float dz = rand.nextFloat() * 0.8F + 0.1F;
        while (stack.stackSize > 0) {
            // Another mod's item may claim a stack limit of zero or less; a piece of at least one still ends the loop
            ItemStack piece = stack.splitStack(Math.max(1, Math.min(stack.stackSize, stack.getMaxStackSize())));
            EntityItem entity = new EntityItem(world, x + dx, y + dy, z + dz, piece);
            entity.motionX = rand.nextGaussian() * 0.05F;
            entity.motionY = rand.nextGaussian() * 0.05F + 0.2F;
            entity.motionZ = rand.nextGaussian() * 0.05F;
            world.spawnEntityInWorld(entity);
        }
    }
}
