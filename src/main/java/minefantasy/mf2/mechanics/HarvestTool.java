package minefantasy.mf2.mechanics;

import net.minecraft.item.ItemStack;

/** A player's harvest in progress: the tool as it was held before it wore, for drops paid after it broke. */
public interface HarvestTool {

    /** A copy of the tool of the innermost harvest past the break, or null when none is. */
    ItemStack mf2$harvestTool();
}
