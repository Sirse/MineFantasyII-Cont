package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.block.tileentity.TileEntityKitchenBench;

public class ContainerKitchenBench extends ContainerMF {

    private final TileEntityKitchenBench tile;

    public ContainerKitchenBench(InventoryPlayer user, TileEntityKitchenBench tile) {
        this.tile = tile;
        init(user);
    }

    public ContainerKitchenBench(TileEntityKitchenBench tile) {
        this.tile = tile;
        init(null);
    }

    private void init(InventoryPlayer user) {
        for (int x = 0; x < tile.width; x++) {
            for (int y = 0; y < tile.height; y++) {
                int slot = y * tile.width + x;
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
            addPlayerInventory(user, 0, 158);

            trackFloat(() -> tile.progress, value -> tile.progress = value);
            trackFloat(() -> tile.progressMax, value -> tile.progressMax = value);
            trackFloat(() -> tile.dirtyProgress, value -> tile.dirtyProgress = value);
            trackInt(tile::getToolTierNeeded, tile::setToolTier);
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

        if (slotIndex == tileSlotCount - 5) {
            if (!this.mergeItemStack(stackInSlot, tileSlotCount, this.inventorySlots.size(), true)) {
                return null;
            }
        } else if (slotIndex < tileSlotCount) {
            if (!this.mergeItemStack(stackInSlot, tileSlotCount, this.inventorySlots.size(), true)) {
                return null;
            }
        } else {
            if (!this.mergeItemStack(stackInSlot, 0, tileSlotCount - 5, false)
                    && !this.mergeItemStack(stackInSlot, tileSlotCount - 5, tileSlotCount, false)) {
                return null;
            }
        }

        if (stackInSlot.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        return originalStack;
    }
}
