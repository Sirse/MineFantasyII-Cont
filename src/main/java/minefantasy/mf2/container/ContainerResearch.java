package minefantasy.mf2.container;

import minefantasy.mf2.block.tileentity.TileEntityResearch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerResearch extends ContainerMF {
    private static final int INPUT_SLOT = 0;
    private static final int RESEARCH_SLOT_COUNT = 1;

    private final TileEntityResearch tile;

    private final int playerInventoryStartIndex;

    public ContainerResearch(InventoryPlayer playerInventory, TileEntityResearch tile) {
        this.tile = tile;

        addSlotToContainer(new Slot(tile, INPUT_SLOT, 83, 40));

        this.playerInventoryStartIndex = RESEARCH_SLOT_COUNT;
        addPlayerMainInventory(playerInventory, 2, 76);
        addPlayerHotbar(playerInventory, 2, 134);

        trackInt(() -> (int) tile.progress, v -> tile.progress = v);
        trackInt(() -> (int) tile.maxProgress, v -> tile.maxProgress = v);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.tile != null && !this.tile.isInvalid() && this.tile.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= inventorySlots.size()) return null;
        Slot slot = (Slot) inventorySlots.get(slotIndex);
        if (slot == null || !slot.getHasStack()) return null;

        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        boolean moved = false;
        if (slotIndex < RESEARCH_SLOT_COUNT) {
            moved = mergeItemStack(stack, playerInventoryStartIndex, inventorySlots.size(), true);
        } else {
            if (TileEntityResearch.canAccept(stack)) {
                moved = mergeItemStack(stack, INPUT_SLOT, INPUT_SLOT + 1, false);
            }
            if (!moved) {
                int mainStart = playerInventoryStartIndex;
                int mainEnd = mainStart + 27;
                int hotbarEnd = inventorySlots.size();

                if (slotIndex >= mainStart && slotIndex < mainEnd) {
                    moved = mergeItemStack(stack, mainEnd, hotbarEnd, false);
                } else if (slotIndex >= mainEnd && slotIndex < hotbarEnd) {
                    moved = mergeItemStack(stack, mainStart, mainEnd, false);
                }
            }
        }

        if (!moved) return null;

        if (stack.stackSize == 0) slot.putStack(null);
        else slot.onSlotChanged();

        if (stack.stackSize == copy.stackSize) return null;

        slot.onPickupFromSlot(player, stack);
        return copy;
    }
}