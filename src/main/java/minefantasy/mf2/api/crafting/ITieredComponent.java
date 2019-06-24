package minefantasy.mf2.api.crafting;

import net.minecraft.item.ItemStack;

public interface ITieredComponent {
    /**
     * is it made of "wood", "metal", etc
     */
    String getMaterialType(ItemStack item);
}
