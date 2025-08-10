package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;

public class ContainerBlastHeater extends ContainerMF {

    private static final int FUEL_SLOT = 0;
    private static final int HEATER_SLOT_COUNT = 1;

    private final TileEntityBlastFH tile;

    private final int playerInventoryStartIndex;

    public ContainerBlastHeater(InventoryPlayer user, TileEntityBlastFH tile) {
        this.tile = tile;
        this.addSlotToContainer(new SlotFiltered(tile, FUEL_SLOT, 80, 76));

        this.playerInventoryStartIndex = HEATER_SLOT_COUNT;
        this.addPlayerInventory(user, 0, 126);

        trackInt(() -> tile.fuel, value -> tile.fuel = value);
        trackInt(() -> tile.maxFuel, value -> tile.maxFuel = value);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.tile != null && !this.tile.isInvalid() && this.tile.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.inventorySlots.size()) {
            return null;
        }
        Slot slot = (Slot) this.inventorySlots.get(slotIndex);
        if (slot == null || !slot.getHasStack()) {
            return null;
        }

        ItemStack stackInSlot = slot.getStack();
        ItemStack originalStack = stackInSlot.copy();
        boolean merged = false;

        if (slotIndex < HEATER_SLOT_COUNT) {
            if (this.moveToPlayer(stackInSlot, playerInventoryStartIndex)) {
                merged = true;
            }
        } else {
            if (tile.isItemValidForSlot(FUEL_SLOT, stackInSlot)) {
                if (this.mergeItemStack(stackInSlot, FUEL_SLOT, HEATER_SLOT_COUNT, false)) {
                    merged = true;
                }
            }
            if (!merged) {
                int mainStart = playerInventoryStartIndex;
                int mainEnd = mainStart + 27;
                int hotbarEnd = this.inventorySlots.size();

                if (slotIndex >= mainStart && slotIndex < mainEnd) {
                    merged = this.mergeItemStack(stackInSlot, mainEnd, hotbarEnd, false);
                } else if (slotIndex >= mainEnd && slotIndex < hotbarEnd) {
                    merged = this.mergeItemStack(stackInSlot, mainStart, mainEnd, false);
                }
            }
        }

        if (!merged) {
            return null;
        }

        if (stackInSlot.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }

        if (stackInSlot.stackSize == originalStack.stackSize) {
            return null;
        }
        slot.onPickupFromSlot(player, stackInSlot);
        return originalStack;
    }
}
