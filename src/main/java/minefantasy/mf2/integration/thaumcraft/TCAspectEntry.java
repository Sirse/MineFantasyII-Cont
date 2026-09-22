package minefantasy.mf2.integration.thaumcraft;

import net.minecraft.item.ItemStack;

import thaumcraft.api.aspects.AspectList;

/**
 * One row of the aspect table: the exact Item plus damage Thaumcraft will key on, and what it is worth.
 */
public class TCAspectEntry {

    public final ItemStack stack;
    public final AspectList aspects;

    public TCAspectEntry(ItemStack stack, AspectList aspects) {
        this.stack = stack;
        this.aspects = aspects;
    }
}
