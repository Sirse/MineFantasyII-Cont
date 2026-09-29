package minefantasy.mf2.container;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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

    /**
     * S31PacketWindowProperty writes the value with writeShort, so a progress bar update only carries 16 bits. Each
     * tracked field therefore occupies two consecutive ids: the low half at index*2 and the high half at index*2+1.
     */
    private void sendTracked(ICrafting crafter, int index, int intValue) {
        crafter.sendProgressBarUpdate(this, index * 2, intValue & 0xFFFF);
        crafter.sendProgressBarUpdate(this, index * 2 + 1, (intValue >>> 16) & 0xFFFF);
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        for (int i = 0; i < trackedData.size(); i++) {
            sendTracked(crafter, i, trackedData.get(i).getIntValue());
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
                    sendTracked((ICrafting) crafterObj, i, intValue);
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        int index = id >> 1;
        if (index < 0 || index >= trackedData.size()) {
            return;
        }
        // readShort sign-extends, so mask before reassembling the 32-bit value
        if ((id & 1) == 0) {
            trackedData.get(index).setLowHalf(value & 0xFFFF);
        } else {
            trackedData.get(index).applyHighHalf(value & 0xFFFF);
        }
    }

    /**
     * Adds the player's main inventory and hotbar slots to the container. This method is intended to be called from
     * subclasses.
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
     * Try moving between main (27 slots) and hotbar (9 slots) depending on where the item currently is. fromIndex is
     * the slot index in this container.
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
     * The station still stands and the player can use it from the world they are in. Coordinates alone are not enough:
     * the same position in another dimension is just as near.
     */
    protected static <T extends TileEntity & IInventory> boolean stillUsable(T station, EntityPlayer player) {
        return station != null && !station.isInvalid()
                && station.getWorldObj() == player.worldObj
                && station.isUseableByPlayer(player);
    }

    /**
     * The window closes on the player's next tick once {@link #canInteractWith} fails, but a click can arrive in the
     * same tick, after a broken station has already dropped its contents. That click would take them a second time.
     */
    @Override
    public ItemStack slotClick(int slotId, int mouseButton, int modifier, EntityPlayer player) {
        if (!canInteractWith(player)) {
            return null;
        }
        return super.slotClick(slotId, mouseButton, modifier, player);
    }

    /**
     * A double-click gathers matching items from every slot of the window. An output slot gives its contents out only
     * through its own pickup (a furnace pays its experience there), so it is left out, like the result of a vanilla
     * crafting table: only slots that would take the item back are swept.
     */
    @Override
    public boolean func_94530_a(ItemStack stack, Slot slot) {
        return slot.inventory instanceof InventoryPlayer || slot.isItemValid(stack);
    }

    /**
     * Vanilla merging ignores what a slot accepts and how much it holds, so every container had to check both before
     * calling it. Here a slot that refuses the stack is skipped and each slot takes at most its own limit.
     */
    @Override
    protected boolean mergeItemStack(ItemStack stack, int start, int end, boolean reverse) {
        boolean merged = false;
        if (stack.isStackable()) {
            for (int i = reverse ? end - 1 : start; stack.stackSize > 0 && i >= start
                    && i < end; i += reverse ? -1 : 1) {
                Slot slot = (Slot) inventorySlots.get(i);
                ItemStack held = slot.getStack();
                if (held == null || !slot.isItemValid(stack)
                        || held.getItem() != stack.getItem()
                        || stack.getHasSubtypes() && stack.getItemDamage() != held.getItemDamage()
                        || !ItemStack.areItemStackTagsEqual(stack, held)) {
                    continue;
                }
                int moved = Math.min(stack.stackSize, limit(slot, stack) - held.stackSize);
                if (moved > 0) {
                    held.stackSize += moved;
                    stack.stackSize -= moved;
                    slot.onSlotChanged();
                    merged = true;
                }
            }
        }
        for (int i = reverse ? end - 1 : start; stack.stackSize > 0 && i >= start && i < end; i += reverse ? -1 : 1) {
            Slot slot = (Slot) inventorySlots.get(i);
            if (slot.getStack() != null || !slot.isItemValid(stack)) {
                continue;
            }
            ItemStack placed = stack.splitStack(Math.min(stack.stackSize, limit(slot, stack)));
            slot.putStack(placed);
            merged = true;
        }
        return merged;
    }

    private static int limit(Slot slot, ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), slot.getSlotStackLimit());
    }

    /**
     * Optional hook invoked by containers after a successful transfer. Default is no-op; override in subclasses that
     * need to notify their tile.
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
        /**
         * Low 16 bits received so far; the value is applied once its high half arrives
         */
        private int pendingLowHalf;

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
         * Buffers the low half of a split value until the high half arrives
         */
        void setLowHalf(int low) {
            pendingLowHalf = low;
        }

        /**
         * Reassembles and applies the value once both halves have been received
         */
        void applyHighHalf(int high) {
            setter.accept(fromInt.apply((high << 16) | pendingLowHalf));
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
