package minefantasy.mf2.api.weapon;

import net.minecraft.item.ItemStack;

/**
 * Marks a weapon that hits further than vanilla allows.
 * <p>
 * This mirrors Battlegear's IExtendedReachWeapon, but lives here so the reach mechanic keeps working when Battlegear is
 * not installed: the Battlegear interface is stripped by @Optional.Interface in that case, while this one is always
 * present. Weapons implement both, and the single method satisfies each.
 */
public interface IExtendedReach {

    /**
     * The distance the weapon will hit, in blocks, on top of the vanilla reach. Only main hand weapons are considered.
     */
    float getReachModifierInBlocks(ItemStack stack);
}
