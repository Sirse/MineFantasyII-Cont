package minefantasy.mf2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.SlotFurnace;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.block.tileentity.TileEntityBigFurnace;

public class ContainerBigFurnace extends ContainerMF {

    private static final int HEATER_FUEL_SLOT = 0;
    private static final int HEATER_SLOT_COUNT = 1;

    private static final int SMELTER_INPUT_START_INDEX = 0;
    private static final int SMELTER_OUTPUT_START_INDEX = 4;
    private static final int SMELTER_TOTAL_SLOTS = 8;

    private final TileEntityBigFurnace smelter;

    private final int furnaceSlotCount;

    public ContainerBigFurnace(EntityPlayer player, TileEntityBigFurnace tile) {
        this.smelter = tile;
        tile.openChest();

        if (smelter.isHeater()) {
            addSlotToContainer(new SlotFiltered(smelter, HEATER_FUEL_SLOT, 59, 44));
            furnaceSlotCount = HEATER_SLOT_COUNT;
        } else {
            addSlotToContainer(new SlotFiltered(smelter, 0, 36, 26));
            addSlotToContainer(new SlotFiltered(smelter, 1, 54, 26));
            addSlotToContainer(new SlotFiltered(smelter, 2, 36, 44));
            addSlotToContainer(new SlotFiltered(smelter, 3, 54, 44));

            addSlotToContainer(new SlotFurnace(player, smelter, 4, 106, 26));
            addSlotToContainer(new SlotFurnace(player, smelter, 5, 124, 26));
            addSlotToContainer(new SlotFurnace(player, smelter, 6, 106, 44));
            addSlotToContainer(new SlotFurnace(player, smelter, 7, 124, 44));
            furnaceSlotCount = SMELTER_TOTAL_SLOTS;
        }

        shiftClicks(furnaceSlotCount, SMELTER_INPUT_START_INDEX, SMELTER_OUTPUT_START_INDEX);
        this.addPlayerInventory(player.inventory, 0, 84);

        trackInt(() -> smelter.fuel, value -> smelter.fuel = value);
        trackInt(() -> smelter.maxFuel, value -> smelter.maxFuel = value);
        trackInt(() -> smelter.progress, value -> smelter.progress = value);
        trackInt(() -> smelter.doorAngle, value -> smelter.doorAngle = value);
        trackFloat(() -> smelter.heat, value -> smelter.heat = value);
        trackFloat(() -> smelter.maxHeat, value -> smelter.maxHeat = value);
        trackInt(() -> smelter.built ? 1 : 0, value -> smelter.built = value == 1);
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        this.smelter.closeChest();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return stillUsable(smelter, player);
    }

    @Override
    protected boolean moveIntoStation(ItemStack stack) {
        return smelter.isHeater() ? mergeItemStack(stack, HEATER_FUEL_SLOT, HEATER_SLOT_COUNT, false)
                : mergeItemStack(stack, SMELTER_INPUT_START_INDEX, SMELTER_OUTPUT_START_INDEX, false);
    }
}
