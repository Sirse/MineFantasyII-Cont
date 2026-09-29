package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.block.tileentity.TileEntityBombBench;

public class ContainerBombBench extends ContainerMF {

    private static final int CASE_SLOT = 0;
    private static final int POWDER_SLOT = 1;
    private static final int FILLING_SLOT = 2;
    private static final int FUSE_SLOT = 3;
    private static final int OUTPUT_SLOT = 4;
    private static final int MISC_SLOT = 5;
    private static final int BENCH_SLOT_COUNT = 6;

    private final TileEntityBombBench tile;

    public ContainerBombBench(InventoryPlayer playerInv, TileEntityBombBench tile) {
        this.tile = tile;

        addSlotToContainer(new SlotFiltered(tile, CASE_SLOT, 77, 74));
        addSlotToContainer(new SlotFiltered(tile, POWDER_SLOT, 77, 48));
        addSlotToContainer(new SlotFiltered(tile, FILLING_SLOT, 52, 23));
        addSlotToContainer(new SlotFiltered(tile, FUSE_SLOT, 102, 23));

        addSlotToContainer(new SlotOutput(tile, OUTPUT_SLOT, 147, 48));
        addSlotToContainer(new SlotOutput(tile, MISC_SLOT, 147, 75));

        shiftClicks(BENCH_SLOT_COUNT, CASE_SLOT, OUTPUT_SLOT);
        addPlayerInventory(playerInv, 0, 126);

        trackFloat(() -> tile.progress, v -> tile.progress = v);
        trackFloat(() -> tile.maxProgress, v -> tile.maxProgress = v);
        trackInt(() -> tile.hasRecipe ? 1 : 0, v -> tile.hasRecipe = v == 1);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return stillUsable(tile, player);
    }

    @Override
    protected void onPostTransfer(EntityPlayer player, Slot slot, ItemStack moved) {
        tile.onInventoryChanged();
    }
}
