package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

import minefantasy.mf2.block.tileentity.TileEntityBloomery;

public class ContainerBloomery extends ContainerMF {

    private static final int INPUT_SLOT = 0;
    private static final int FUEL_SLOT = 1;
    private static final int BLOOMERY_SLOT_COUNT = 2;

    private final TileEntityBloomery tile;

    public ContainerBloomery(InventoryPlayer user, TileEntityBloomery tile) {
        this.tile = tile;

        this.addSlotToContainer(new SlotFiltered(tile, INPUT_SLOT, 80, 30)); // Input/Ore
        this.addSlotToContainer(new SlotFiltered(tile, FUEL_SLOT, 80, 68)); // Fuel

        shiftClicks(BLOOMERY_SLOT_COUNT, 0, BLOOMERY_SLOT_COUNT);
        this.addPlayerInventory(user, 0, 126);

        trackFloat(() -> tile.progress, value -> tile.progress = value);
        trackFloat(() -> tile.progressMax, value -> tile.progressMax = value);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return stillUsable(tile, player);
    }
}
