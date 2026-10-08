package minefantasy.mf2.api.weapon;

import net.minecraft.item.ItemStack;

/**
 * Marks a weapon that hits further than vanilla allows.
 */
public interface IExtendedReach {

    /**
     * The distance the weapon will hit, in blocks, on top of the vanilla reach. Only main hand weapons are considered.
     */
    float getReachModifierInBlocks(ItemStack stack);
}
