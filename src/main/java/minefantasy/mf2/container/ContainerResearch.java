package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

import minefantasy.mf2.block.tileentity.TileEntityResearch;

public class ContainerResearch extends ContainerMF {

    private static final int INPUT_SLOT = 0;
    private static final int RESEARCH_SLOT_COUNT = 1;

    private final TileEntityResearch tile;

    public ContainerResearch(InventoryPlayer playerInventory, TileEntityResearch tile) {
        this.tile = tile;

        addSlotToContainer(new SlotFiltered(tile, INPUT_SLOT, 83, 40));

        shiftClicks(RESEARCH_SLOT_COUNT, INPUT_SLOT, INPUT_SLOT + 1);
        addPlayerMainInventory(playerInventory, 2, 76);
        addPlayerHotbar(playerInventory, 2, 134);

        trackInt(() -> (int) tile.progress, v -> tile.progress = v);
        trackInt(() -> (int) tile.maxProgress, v -> tile.maxProgress = v);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return stillUsable(tile, player);
    }
}
