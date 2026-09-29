package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

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
            // The player's items go to the grid and the surplus slots; the output slot turns them away
            shiftClicks(tile.getSizeInventory(), 0, tile.getSizeInventory());

            trackFloat(() -> tile.progress, value -> tile.progress = value);
            trackFloat(() -> tile.progressMax, value -> tile.progressMax = value);
            trackFloat(() -> tile.dirtyProgress, value -> tile.dirtyProgress = value);
            trackFloat(tile::getDirtyMax, tile::setDirtyMax);
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return stillUsable(tile, player);
    }
}
