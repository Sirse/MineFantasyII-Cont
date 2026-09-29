package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

import minefantasy.mf2.block.tileentity.TileEntityCarpenterMF;

public class ContainerCarpenterMF extends ContainerMF {

    private final TileEntityCarpenterMF tile;

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
                this.addSlotToContainer(new SlotFiltered(tile, slot, 44 + x * 18, 54 + y * 18));
            }
        }

        int outputIndex = tile.getSizeInventory() - 5;
        this.addSlotToContainer(new SlotOutput(tile, outputIndex, 174, 80));

        for (int y = 0; y < 4; y++) {
            int slot = tile.getSizeInventory() - 4 + y;
            this.addSlotToContainer(new SlotFiltered(tile, slot, 3, 54 + y * 18));
        }

        if (user != null) {
            shiftClicks(tile.getSizeInventory(), 0, tile.getSizeInventory() - 5);
            this.addPlayerInventory(user, 0, 158);

            trackFloat(() -> tile.progress, value -> tile.progress = value);
            trackFloat(() -> tile.progressMax, value -> tile.progressMax = value);
            trackInt(tile::getToolTierNeeded, tile::setToolTier);
            trackInt(tile::getCarpenterTierNeeded, tile::setRequiredCarpenter);
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return stillUsable(tile, player);
    }
}
