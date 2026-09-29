package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

import minefantasy.mf2.block.tileentity.TileEntityForge;

public class ContainerForge extends ContainerMF {

    private static final int FORGE_SLOT = 0;
    private static final int FORGE_SLOT_COUNT = 1;
    private final TileEntityForge tile;

    public ContainerForge(InventoryPlayer user, TileEntityForge tile) {
        this.tile = tile;

        this.addSlotToContainer(new SlotFiltered(tile, FORGE_SLOT, 88, 32));

        shiftClicks(FORGE_SLOT_COUNT, FORGE_SLOT, FORGE_SLOT_COUNT);
        this.addPlayerInventory(user, 0, 93);

        trackFloat(() -> tile.temperature, value -> tile.temperature = value);
        trackFloat(() -> tile.fuel, value -> tile.fuel = value);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return stillUsable(tile, player);
    }
}
