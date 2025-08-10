package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.block.tileentity.TileEntityAnvilMF;

public class ContainerAnvilMF extends ContainerMF {

    private static final int ANVIL_GRID_WIDTH = 6;
    private static final int ANVIL_GRID_HEIGHT = 4;
    private static final int ANVIL_GRID_SLOT_COUNT = ANVIL_GRID_WIDTH * ANVIL_GRID_HEIGHT;

    private final TileEntityAnvilMF tile;

    private final int anvilSlotCount;
    private final int anvilOutputSlotIndex;
    private final int playerInventoryStartIndex;

    public ContainerAnvilMF(InventoryPlayer user, TileEntityAnvilMF tile) {
        this.tile = tile;
        this.anvilSlotCount = tile.getSizeInventory();
        this.anvilOutputSlotIndex = this.anvilSlotCount - 1;
        this.playerInventoryStartIndex = this.anvilSlotCount;

        init(user, true);
    }

    public ContainerAnvilMF(TileEntityAnvilMF tile) {
        this.tile = tile;
        this.anvilSlotCount = tile.getSizeInventory();
        this.anvilOutputSlotIndex = this.anvilSlotCount - 1;
        this.playerInventoryStartIndex = this.anvilSlotCount;
        init(null, false);
    }

    private void init(InventoryPlayer user, boolean addPlayerSlots) {
        int anvilGridX = 44;
        int anvilGridY = 39;
        int outputSlotX = 214;
        int outputSlotY = 66;

        int slotsToAdd = Math.min(anvilSlotCount - 1, ANVIL_GRID_SLOT_COUNT);
        for (int i = 0; i < slotsToAdd; i++) {
            int x = i % ANVIL_GRID_WIDTH;
            int y = i / ANVIL_GRID_WIDTH;
            this.addSlotToContainer(new SlotFiltered(tile, i, anvilGridX + x * 18, anvilGridY + y * 18));
        }

        if (anvilSlotCount > 0) {
            this.addSlotToContainer(new SlotOutput(tile, anvilOutputSlotIndex, outputSlotX, outputSlotY));
        }

        if (addPlayerSlots && user != null) {
            addPlayerInventory(user, 28, 128);
            tile.openInventory();

            trackInt(tile::getRecipeHammer, tile::setHammerUsed);
            trackInt(tile::getRecipeAnvil, tile::setRequiredAnvil);
            trackInt(() -> tile.isOutputHot() ? 1 : 0, v -> tile.setHotOutput(v == 1));
            trackFloat(() -> tile.progress, v -> tile.progress = v);
            trackFloat(() -> tile.progressMax, v -> tile.progressMax = v);
            trackFloat(() -> tile.qualityBalance, v -> tile.qualityBalance = v);
            trackFloat(() -> tile.thresholdPosition, v -> tile.thresholdPosition = v);
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
        boolean merged = false;

        if (slotIndex < anvilSlotCount) {
            if (this.moveToPlayer(stackInSlot, playerInventoryStartIndex)) {
                merged = true;
            }
        } else {
            if (this.mergeItemStack(stackInSlot, 0, anvilOutputSlotIndex, false)) {
                merged = true;
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
        onPostTransfer(player, slot, originalStack);
        return originalStack;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (tile != null && !tile.isInvalid()) {
            tile.closeInventory();
        }
    }

    @Override
    protected void onPostTransfer(EntityPlayer player, Slot slot, ItemStack moved) {
        tile.onInventoryChanged();
    }
}
