package minefantasy.mf2.container;

import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerBlastChamber extends ContainerMF {
    private static final int CARBON_SLOT = 0;
    private static final int INPUT_SLOT = 1;
    private static final int CHAMBER_SLOT_COUNT = 2;

    private final TileEntityBlastFC tile;

    private final int playerInventoryStartIndex;

    public ContainerBlastChamber(InventoryPlayer user, TileEntityBlastFC tile) {
        this.tile = tile;

        this.addSlotToContainer(new SlotFiltered(tile, CARBON_SLOT, 80, 30));
        this.addSlotToContainer(new SlotFiltered(tile, INPUT_SLOT, 80, 68));

        this.playerInventoryStartIndex = CHAMBER_SLOT_COUNT;
        this.addPlayerInventory(user, 0, 126);

        trackInt(() -> tile.fireTime, value -> tile.fireTime = value);
        trackInt(() -> tile.isBuilt ? 1 : 0, value -> tile.isBuilt = value == 1);
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

        if (slotIndex < CHAMBER_SLOT_COUNT) {
            if (this.moveToPlayer(stackInSlot, playerInventoryStartIndex)) {
                merged = true;
            }
        } else {
            if (tile.isItemValidForSlot(CARBON_SLOT, stackInSlot)) {
                if (this.mergeItemStack(stackInSlot, CARBON_SLOT, CARBON_SLOT + 1, false)) {
                    merged = true;
                }
            }

            if (!merged && tile.isItemValidForSlot(INPUT_SLOT, stackInSlot)) {
                if (this.mergeItemStack(stackInSlot, INPUT_SLOT, INPUT_SLOT + 1, false)) {
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