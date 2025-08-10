package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.block.tileentity.TileEntityQuern;

public class ContainerQuern extends ContainerMF {

    private static final int INPUT_SLOT = 0;
    private static final int POT_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final int QUERN_SLOT_COUNT = 3;

    private final TileEntityQuern tile;

    private final int playerInventoryStartIndex;

    public ContainerQuern(InventoryPlayer user, TileEntityQuern tile) {
        this.tile = tile;

        this.addSlotToContainer(new SlotFiltered(tile, INPUT_SLOT, 81, 9));
        this.addSlotToContainer(new SlotFiltered(tile, POT_SLOT, 81, 32));
        this.addSlotToContainer(new SlotFiltered(tile, OUTPUT_SLOT, 81, 55));

        this.playerInventoryStartIndex = QUERN_SLOT_COUNT;
        this.addPlayerInventory(user, 0, 93);

        trackInt(() -> tile.turnAngle, value -> tile.turnAngle = value);
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

        if (slotIndex < QUERN_SLOT_COUNT) {
            if (this.moveToPlayer(stackInSlot, playerInventoryStartIndex)) {
                merged = true;
            }
        } else {
            if (TileEntityQuern.isInput(stackInSlot)) {
                if (this.mergeItemStack(stackInSlot, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                    merged = true;
                }
            } else if (TileEntityQuern.isPot(stackInSlot)) {
                if (this.mergeItemStack(stackInSlot, POT_SLOT, POT_SLOT + 1, false)) {
                    merged = true;
                }
            }
            if (!merged) {
                merged = this.bounceBetweenMainAndHotbar(stackInSlot, playerInventoryStartIndex, slotIndex);
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
