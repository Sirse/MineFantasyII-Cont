package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.block.tileentity.TileEntityCarpenterMF;

public class ContainerCarpenterMF extends ContainerMF {

    private final TileEntityCarpenterMF tile;
    private int playerInventoryStartIndex;

    public ContainerCarpenterMF(InventoryPlayer user, TileEntityCarpenterMF tile) {
        this.tile = tile;
        init(user);
    }

    public ContainerCarpenterMF(TileEntityCarpenterMF tile) {
        this.tile = tile;
        init(null);
    }

    private void init(InventoryPlayer user) {
        int width = tile.width;
        int height = tile.height;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int slot = y * width + x;
                this.addSlotToContainer(new Slot(tile, slot, 44 + x * 18, 54 + y * 18));
            }
        }

        int outputIndex = tile.getSizeInventory() - 5;
        this.addSlotToContainer(new SlotOutput(tile, outputIndex, 174, 80));

        for (int y = 0; y < 4; y++) {
            int slot = tile.getSizeInventory() - 4 + y;
            this.addSlotToContainer(new Slot(tile, slot, 3, 54 + y * 18));
        }

        if (user != null) {
            this.playerInventoryStartIndex = tile.getSizeInventory();
            this.addPlayerInventory(user, 0, 158);

            trackFloat(() -> tile.progress, value -> tile.progress = value);
            trackFloat(() -> tile.progressMax, value -> tile.progressMax = value);
            trackInt(tile::getToolTierNeeded, tile::setToolTier);
            trackInt(tile::getCarpenterTierNeeded, tile::setRequiredCarpenter);
        }
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

        int tileSlotCount = tile.getSizeInventory();
        int gridEndExclusive = tileSlotCount - 5;

        if (slotIndex < tileSlotCount) {
            if (!this.moveToPlayer(stackInSlot, playerInventoryStartIndex)) {
                return null;
            }
        } else {
            boolean moved = this.mergeItemStack(stackInSlot, 0, gridEndExclusive, false);

            if (!moved && !this.bounceBetweenMainAndHotbar(stackInSlot, playerInventoryStartIndex, slotIndex))
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
