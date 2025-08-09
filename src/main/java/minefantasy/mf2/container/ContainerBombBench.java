package minefantasy.mf2.container;

import minefantasy.mf2.block.tileentity.TileEntityBombBench;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerBombBench extends ContainerMF {
    private static final int CASE_SLOT = 0;
    private static final int POWDER_SLOT = 1;
    private static final int FILLING_SLOT = 2;
    private static final int FUSE_SLOT = 3;
    private static final int OUTPUT_SLOT = 4;
    private static final int MISC_SLOT = 5;
    private static final int BENCH_SLOT_COUNT = 6;

    private final TileEntityBombBench tile;

    private final int playerInventoryStart;

    public ContainerBombBench(InventoryPlayer playerInv, TileEntityBombBench tile) {
        this.tile = tile;

        addSlotToContainer(new SlotFiltered(tile, CASE_SLOT, 77, 74));
        addSlotToContainer(new SlotFiltered(tile, POWDER_SLOT, 77, 48));
        addSlotToContainer(new SlotFiltered(tile, FILLING_SLOT, 52, 23));
        addSlotToContainer(new SlotFiltered(tile, FUSE_SLOT, 102, 23));

        addSlotToContainer(new SlotOutput(tile, OUTPUT_SLOT, 147, 48));
        addSlotToContainer(new SlotOutput(tile, MISC_SLOT, 147, 75));

        playerInventoryStart = BENCH_SLOT_COUNT;
        addPlayerInventory(playerInv, 0, 126);

        trackFloat(() -> tile.progress, v -> tile.progress = v);
        trackFloat(() -> tile.maxProgress, v -> tile.maxProgress = v);
        trackInt(() -> tile.hasRecipe ? 1 : 0, v -> tile.hasRecipe = v == 1);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile != null && !tile.isInvalid() && tile.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return null;

        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;

        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        boolean moved;

        if (index < BENCH_SLOT_COUNT) {
            moved = moveToPlayer(stack, playerInventoryStart);
        } else {
            String type = TileEntityBombBench.getComponentType(stack);
            if (type != null) {
                type = type.toLowerCase();
                switch (type) {
                    case "bombcase":
                    case "minecase":
                    case "arrow":
                    case "bolt":
                        moved = mergeItemStack(stack, CASE_SLOT, CASE_SLOT + 1, false);
                        break;
                    case "powder":
                        moved = mergeItemStack(stack, POWDER_SLOT, POWDER_SLOT + 1, false);
                        break;
                    case "filling":
                        moved = mergeItemStack(stack, FILLING_SLOT, FILLING_SLOT + 1, false);
                        break;
                    case "fuse":
                        moved = mergeItemStack(stack, FUSE_SLOT, FUSE_SLOT + 1, false);
                        break;
                    default:
                        moved = false;
                }
            } else moved = false;
            if (!moved) {
                moved = bounceBetweenMainAndHotbar(stack, playerInventoryStart, index);
            }
        }

        if (!moved) return null;

        if (stack.stackSize == 0) slot.putStack(null);
        else slot.onSlotChanged();

        if (stack.stackSize == copy.stackSize) return null;

        slot.onPickupFromSlot(player, stack);
        onPostTransfer(player, slot, copy);

        return copy;
    }

    @Override
    protected void onPostTransfer(EntityPlayer player, Slot slot, ItemStack moved) {
        tile.onInventoryChanged();
    }
}
