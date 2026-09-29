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

    public ContainerCrossbowBench(InventoryPlayer user, TileEntityCrossbowBench tile) {
        this.tile = tile;

        this.addSlotToContainer(new SlotFiltered(tile, STOCK_SLOT, 77, 74));
        this.addSlotToContainer(new SlotFiltered(tile, MECHANISM_SLOT, 77, 48));
        this.addSlotToContainer(new SlotFiltered(tile, MOD_SLOT, 52, 48));
        this.addSlotToContainer(new SlotFiltered(tile, MUZZLE_SLOT, 100, 30));

        this.addSlotToContainer(new SlotOutput(tile, OUTPUT_SLOT, 147, 48));

        shiftClicks(BENCH_SLOT_COUNT, STOCK_SLOT, OUTPUT_SLOT);
        this.addPlayerInventory(user, 0, 126);

        trackFloat(() -> tile.progress, value -> tile.progress = value);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return stillUsable(tile, player);
    }

    @Override
    protected void onPostTransfer(EntityPlayer player, Slot slot, ItemStack moved) {
        tile.markDirty();
    }

}
