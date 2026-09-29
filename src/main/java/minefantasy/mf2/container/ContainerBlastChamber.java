package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;

public class ContainerBlastChamber extends ContainerMF {

    private static final int CARBON_SLOT = 0;
    private static final int INPUT_SLOT = 1;
    private static final int CHAMBER_SLOT_COUNT = 2;

    private final TileEntityBlastFC tile;

    public ContainerBlastChamber(InventoryPlayer user, TileEntityBlastFC tile) {
        this.tile = tile;

        this.addSlotToContainer(new SlotFiltered(tile, CARBON_SLOT, 80, 30));
        this.addSlotToContainer(new SlotFiltered(tile, INPUT_SLOT, 80, 68));

        shiftClicks(CHAMBER_SLOT_COUNT, 0, CHAMBER_SLOT_COUNT);
        this.addPlayerInventory(user, 0, 126);

        trackInt(() -> tile.fireTime, value -> tile.fireTime = value);
        trackInt(() -> tile.isBuilt ? 1 : 0, value -> tile.isBuilt = value == 1);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return stillUsable(tile, player);
    }
}
