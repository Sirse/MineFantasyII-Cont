package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.block.tileentity.TileEntityCrossbowBench;

public class ContainerCrossbowBench extends ContainerMF {

    private static final int STOCK_SLOT = 0;
    private static final int MECHANISM_SLOT = 1;
    private static final int MOD_SLOT = 2;
    private static final int MUZZLE_SLOT = 3;
    private static final int OUTPUT_SLOT = 4;
    private static final int BENCH_SLOT_COUNT = 5;

    private final TileEntityCrossbowBench tile;

    private final int playerInventoryStartIndex;

    public ContainerCrossbowBench(InventoryPlayer user, TileEntityCrossbowBench tile) {
        this.tile = tile;

        this.addSlotToContainer(new SlotFiltered(tile, STOCK_SLOT, 77, 74));
        this.addSlotToContainer(new SlotFiltered(tile, MECHANISM_SLOT, 77, 48));
        this.addSlotToContainer(new SlotFiltered(tile, MOD_SLOT, 52, 48));
        this.addSlotToContainer(new SlotFiltered(tile, MUZZLE_SLOT, 100, 30));

        this.addSlotToContainer(new SlotOutput(tile, OUTPUT_SLOT, 147, 48));

        this.playerInventoryStartIndex = BENCH_SLOT_COUNT;
        this.addPlayerInventory(user, 0, 126);

        trackFloat(() -> tile.progress, value -> tile.progress = value);
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

        if (slotIndex < BENCH_SLOT_COUNT) {
            if (this.moveToPlayer(stackInSlot, playerInventoryStartIndex)) {
                merged = true;
            }
        } else {
            if (TileEntityCrossbowBench.isMatch(stackInSlot, "stock")) {
                if (this.mergeItemStack(stackInSlot, STOCK_SLOT, STOCK_SLOT + 1, false)) {
                    merged = true;
                }
            } else if (TileEntityCrossbowBench.isMatch(stackInSlot, "mechanism")) {
                if (this.mergeItemStack(stackInSlot, MECHANISM_SLOT, MECHANISM_SLOT + 1, false)) {
                    merged = true;
                }
            } else if (TileEntityCrossbowBench.isMatch(stackInSlot, "mod")) {
                if (this.mergeItemStack(stackInSlot, MOD_SLOT, MOD_SLOT + 1, false)) {
                    merged = true;
                }
            } else if (TileEntityCrossbowBench.isMatch(stackInSlot, "muzzle")) {
                if (this.mergeItemStack(stackInSlot, MUZZLE_SLOT, MUZZLE_SLOT + 1, false)) {
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
        onPostTransfer(player, slot, originalStack);
        return originalStack;
    }

    @Override
    protected void onPostTransfer(EntityPlayer player, Slot slot, ItemStack moved) {
        tile.markDirty();
    }

}
