package minefantasy.mf2.container;

import minefantasy.mf2.block.tileentity.TileEntityCrucible;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerCrucible extends ContainerMF {
    private static final int GRID_START_INDEX = 0;
    private static final int GRID_SLOT_COUNT = 9;
    private static final int GRID_END_INDEX = GRID_START_INDEX + GRID_SLOT_COUNT;
    private static final int OUTPUT_SLOT_INDEX = GRID_END_INDEX;
    private static final int CRUCIBLE_SLOT_COUNT = OUTPUT_SLOT_INDEX + 1;

    private final TileEntityCrucible tile;

    private final int playerInventoryStartIndex;

    public ContainerCrucible(InventoryPlayer user, TileEntityCrucible tile) {
        this.tile = tile;

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int slot = y * 3 + x;
                this.addSlotToContainer(new SlotFiltered(tile, slot, 62 + x * 18, 14 + y * 18));
            }
        }

        this.addSlotToContainer(new SlotOutput(tile, OUTPUT_SLOT_INDEX, 129, 32));

        this.playerInventoryStartIndex = CRUCIBLE_SLOT_COUNT;
        this.addPlayerInventory(user, 0, 104);

        trackFloat(() -> tile.progress, value -> tile.progress = value);
        trackFloat(() -> tile.progressMax, value -> tile.progressMax = value);
        trackFloat(() -> tile.temperature, value -> tile.temperature = value);
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

        if (slotIndex < CRUCIBLE_SLOT_COUNT) {
            if (this.moveToPlayer(stackInSlot, playerInventoryStartIndex)) {
                merged = true;
            }
        } else {
            if (this.mergeItemStack(stackInSlot, GRID_START_INDEX, GRID_END_INDEX, false)) {
                merged = true;
            } else {
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
        onPostTransfer(player, slot, originalStack);

        return originalStack;
    }

    @Override
    protected void onPostTransfer(EntityPlayer player, Slot slot, ItemStack moved) {
        if (tile != null) tile.markDirty();
    }
}