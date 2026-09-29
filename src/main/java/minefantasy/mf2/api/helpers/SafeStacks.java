package minefantasy.mf2.api.helpers;

import net.minecraft.item.ItemStack;

/** Stacks the mod did not make itself, brought within what an item can be. */
public final class SafeStacks {

    private SafeStacks() {}

    /**
     * A stack read from a tag the mod may not have written: nothing when it is empty or negative, at most a full stack.
     */
    public static ItemStack withinAStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return null;
        }
        stack.stackSize = Math.min(stack.stackSize, stack.getMaxStackSize());
        return stack;
    }

    /**
     * The container one used item leaves behind, as a stack of its own. Another mod's item may return null, an empty or
     * oversized stack, or a stack it keeps and hands out again; whatever it does, one used item gives back one
     * container the caller owns.
     */
    public static ItemStack containerOf(ItemStack used) {
        if (used == null || used.getItem() == null) {
            return null;
        }
        ItemStack single = used.copy();
        single.stackSize = 1;
        ItemStack container = single.getItem().getContainerItem(single);
        if (container == null || container.getItem() == null) {
            return null;
        }
        container = container.copy();
        container.stackSize = 1;
        return container;
    }
}
