package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

import minefantasy.mf2.block.tileentity.TileEntityQuern;

public class ContainerQuern extends ContainerMF {

    private static final int INPUT_SLOT = 0;
    private static final int POT_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final int QUERN_SLOT_COUNT = 3;

    private final TileEntityQuern tile;

    public ContainerQuern(InventoryPlayer user, TileEntityQuern tile) {
        this.tile = tile;

        this.addSlotToContainer(new SlotFiltered(tile, INPUT_SLOT, 81, 9));
        this.addSlotToContainer(new SlotFiltered(tile, POT_SLOT, 81, 32));
        this.addSlotToContainer(new SlotOutput(tile, OUTPUT_SLOT, 81, 55));

        shiftClicks(QUERN_SLOT_COUNT, INPUT_SLOT, OUTPUT_SLOT);
        this.addPlayerInventory(user, 0, 93);

        trackInt(() -> tile.turnAngle, value -> tile.turnAngle = value);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return stillUsable(tile, player);
    }
}
