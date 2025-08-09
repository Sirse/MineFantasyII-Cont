package minefantasy.mf2.container;

import minefantasy.mf2.block.tileentity.TileEntityTanningRack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerTanner extends Container {
    private static final int TANNER_SLOT = 0;

    private final TileEntityTanningRack tile;

    public ContainerTanner(TileEntityTanningRack tile) {
        this.tile = tile;
        this.addSlotToContainer(new SlotTanner(tile, TANNER_SLOT, 80, 35));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
        return null;
    }

    private static class SlotTanner extends Slot {
        private final TileEntityTanningRack tile;

        public SlotTanner(TileEntityTanningRack inventory, int id, int x, int y) {
            super(inventory, id, x, y);
            this.tile = inventory;
        }

        @Override
        public boolean isItemValid(ItemStack item) {
            return tile.isItemValidForSlot(TANNER_SLOT, item);
        }
    }
}