package minefantasy.mf2.block.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

/**
 * A station with an inventory. The station keeps its slots in an array of its own and names it through
 * {@link #slots()}; this handles the slots and calls {@link #onInventoryChanged()} after each change made through the
 * inventory, so the station can take up the work its slots now make. What automation may do with each slot follows from
 * its {@link Role}.
 */
public abstract class TileEntityStation extends TileEntityShown implements ISidedInventory {

    /** What a slot is for, as far as hoppers and other automation are concerned. */
    public enum Role {

        /** Takes items, as the station's filter allows; they are not taken back out. */
        INPUT,
        /** Gives out what the station makes or returns; nothing is put in. */
        OUTPUT,
        /** An item worked in place, such as a piece heating or food cooking: it goes in and comes back out. */
        WORK,
        /** Not for automation at all, such as a result the station only shows. */
        NONE;

        boolean takesIn() {
            return this == INPUT || this == WORK;
        }

        boolean givesOut() {
            return this == OUTPUT || this == WORK;
        }
    }

    /** The array the station keeps its slots in. */
    protected abstract ItemStack[] slots();

    /** What the slot is for; every slot takes input unless the station says otherwise. */
    public Role role(int slot) {
        return Role.INPUT;
    }

    /** Whether the item may go into the slot; by default whatever the slot's role lets in. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return role(slot).takesIn();
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        int count = 0;
        for (int slot = 0; slot < getSizeInventory(); slot++) {
            if (role(slot) != Role.NONE) count++;
        }
        int[] open = new int[count];
        for (int slot = 0, i = 0; slot < getSizeInventory(); slot++) {
            if (role(slot) != Role.NONE) open[i++] = slot;
        }
        return open;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack item, int side) {
        return role(slot).takesIn() && isItemValidForSlot(slot, item);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack item, int side) {
        return role(slot).givesOut();
    }

    /** Called after a slot changed through the inventory; nothing by default. */
    public void onInventoryChanged() {}

    @Override
    public int getSizeInventory() {
        return slots().length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slots()[slot];
    }

    /**
     * Marks the chunk for saving after state changed in place, such as fuel burned or an item heated, without the
     * neighbour and comparator updates of {@link #markDirty()}: cheap enough for every tick.
     */
    protected void saveLater() {
        if (worldObj != null) {
            worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this);
        }
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack taken = InventorySlots.take(slots(), slot, amount);
        if (taken != null) {
            onInventoryChanged();
            markDirty();
        }
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return InventorySlots.takeAll(slots(), slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        slots()[slot] = stack;
        onInventoryChanged();
        // Marked for saving here, whoever changed the slot: a window, a hopper or the station itself
        markDirty();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    /** The station still stands here, and the player is within reach of it. */
    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
                && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64D;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}
}
