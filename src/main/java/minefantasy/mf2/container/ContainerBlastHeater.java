package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;

public class ContainerBlastHeater extends ContainerMF {

    private static final int FUEL_SLOT = 0;
    private static final int HEATER_SLOT_COUNT = 1;

    private final TileEntityBlastFH tile;

    public ContainerBlastHeater(InventoryPlayer user, TileEntityBlastFH tile) {
        this.tile = tile;
        this.addSlotToContainer(new SlotFiltered(tile, FUEL_SLOT, 80, 76));

        shiftClicks(HEATER_SLOT_COUNT, FUEL_SLOT, HEATER_SLOT_COUNT);
        this.addPlayerInventory(user, 0, 126);

        trackInt(() -> tile.fuel, value -> tile.fuel = value);
        trackInt(() -> tile.maxFuel, value -> tile.maxFuel = value);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return stillUsable(tile, player);
    }
}
