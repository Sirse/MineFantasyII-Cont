package minefantasy.mf2.container;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ContainerMF extends Container {

    private final List<TrackedData<?>> trackedData = new ArrayList<>();

    /**
     * Registers an integer value to be automatically synchronized with the client.
     *
     * @param getter A supplier function that returns the current server-side value.
     * @param setter A consumer function that sets the client-side value.
     */
    protected void trackInt(Supplier<Integer> getter, Consumer<Integer> setter) {
        this.trackedData.add(new TrackedData<>(getter, setter, val -> val, val -> val));
    }

    /**
     * Registers a float value to be automatically synchronized with the client.
     *
     * @param getter A supplier function that returns the current server-side value.
     * @param setter A consumer function that sets the client-side value.
     */
    protected void trackFloat(Supplier<Float> getter, Consumer<Float> setter) {
        this.trackedData.add(new TrackedData<>(getter, setter, Float::floatToIntBits, Float::intBitsToFloat));
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        for (int i = 0; i < trackedData.size(); i++) {
            TrackedData<?> tracked = trackedData.get(i);
            crafter.sendProgressBarUpdate(this, i, tracked.getIntValue());
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (int i = 0; i < trackedData.size(); i++) {
            TrackedData<?> tracked = trackedData.get(i);
            if (tracked.hasChanged()) {
                int intValue = tracked.getIntValue();
                for (Object crafterObj : this.crafters) {
                    ((ICrafting) crafterObj).sendProgressBarUpdate(this, i, intValue);
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        if (id >= 0 && id < trackedData.size()) {
            trackedData.get(id).setFromInt(value);
        }
    }

    /**
     * Adds the player's main inventory and hotbar slots to the container.
     * This method is intended to be called from subclasses.
     *
     * @param playerInventory The player's inventory.
     * @param xOffset         The horizontal offset for the slots.
     * @param yOffset         The vertical offset for the slots.
     */
    protected void addPlayerInventory(InventoryPlayer playerInventory, int xOffset, int yOffset) {
        addPlayerMainInventory(playerInventory, xOffset, yOffset);
        addPlayerHotbar(playerInventory, xOffset, yOffset + 58);
    }

    /**
     * Adds only the player's main inventory (3x9) to the container.
     */
    protected void addPlayerMainInventory(InventoryPlayer playerInventory, int xOffset, int yOffset) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9 + 9;
                int x = 8 + col * 18 + xOffset;
                int y = yOffset + row * 18;
                this.addSlotToContainer(new Slot(playerInventory, index, x, y));
            }
        }
    }

    /**
     * Adds the player's hotbar (9 slots) to the container at the given y position.
     */
    protected void addPlayerHotbar(InventoryPlayer playerInventory, int xOffset, int y) {
        for (int col = 0; col < 9; ++col) {
            int x = 8 + col * 18 + xOffset;
            this.addSlotToContainer(new Slot(playerInventory, col, x, y));
        }
    }

    /**
     * Adds the player's hotbar except for a specific slot index (0-8).
     */
    protected void addPlayerHotbarExcept(InventoryPlayer playerInventory, int xOffset, int y, int excludeHotbarIndex) {
        for (int col = 0; col < 9; ++col) {
            if (col == excludeHotbarIndex) continue;
            int x = 8 + col * 18 + xOffset;
            this.addSlotToContainer(new Slot(playerInventory, col, x, y));
        }
    }

    /**
     * Move the given stack into the player's inventory (both main+hotbar), preferring reverse order.
     */
    protected boolean moveToPlayer(ItemStack stack, int playerInventoryStartIndex) {
        return this.mergeItemStack(stack, playerInventoryStartIndex, this.inventorySlots.size(), true);
    }

    /**
     * Try moving between main (27 slots) and hotbar (9 slots) depending on where the item currently is.
     * fromIndex is the slot index in this container.
     */
    protected boolean bounceBetweenMainAndHotbar(ItemStack stack, int mainStart, int fromIndex) {
        int mainEnd = mainStart + 27; // exclusive
        int hotbarEnd = this.inventorySlots.size(); // exclusive

        if (fromIndex >= mainStart && fromIndex < mainEnd) {
            return this.mergeItemStack(stack, mainEnd, hotbarEnd, false);
        }
        if (fromIndex >= mainEnd && fromIndex < hotbarEnd) {
            return this.mergeItemStack(stack, mainStart, mainEnd, false);
        }
        // If origin unknown/not in player inventory, try main then hotbar
        if (this.mergeItemStack(stack, mainStart, mainEnd, false)) return true;
        return this.mergeItemStack(stack, mainEnd, hotbarEnd, false);
    }

    /**
     * Optional hook invoked by containers after a successful transfer.
     * Default is no-op; override in subclasses that need to notify their tile.
     */
    protected void onPostTransfer(EntityPlayer player, Slot slot, ItemStack moved) {
        // no-op by default
    }

    /**
     * Represents a piece of data that is synchronized between server and client.
     *
     * @param <T> The type of the data.
     */
    private static class TrackedData<T> {
        private final Supplier<T> getter;
        private final Consumer<T> setter;
        private final Function<T, Integer> toInt;
        private final Function<Integer, T> fromInt;
        private T lastValue;

        TrackedData(Supplier<T> getter, Consumer<T> setter, Function<T, Integer> toInt, Function<Integer, T> fromInt) {
            this.getter = getter;
            this.setter = setter;
            this.toInt = toInt;
            this.fromInt = fromInt;
            this.lastValue = getter.get();
        }

        /**
         * Checks if the value has changed since the last check.
         *
         * @return true if the value has changed, false otherwise.
         */
        boolean hasChanged() {
            T currentValue = getter.get();
            if (!Objects.equals(currentValue, lastValue)) {
                lastValue = currentValue;
                return true;
            }
            return false;
        }

        /**
         * Gets the current value, converted to an integer for network transport.
         */
        int getIntValue() {
            return toInt.apply(lastValue);
        }

        /**
         * Sets the value from an integer received from the network.
         */
        void setFromInt(int value) {
            setter.accept(fromInt.apply(value));
        }
    }

    /**
     * A generic output-only slot: forbids placing any items into it.
     */
    public static class SlotOutput extends Slot {
        public SlotOutput(IInventory inventory, int id, int x, int y) {
            super(inventory, id, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }
    }

    /**
     * A generic slot that defers validity checks to the backing IInventory's isItemValidForSlot.
     */
    public static class SlotFiltered extends Slot {
        private final IInventory backing;

        public SlotFiltered(IInventory inventory, int id, int x, int y) {
            super(inventory, id, x, y);
            this.backing = inventory;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return backing.isItemValidForSlot(getSlotIndex(), stack);
        }
    }
}
