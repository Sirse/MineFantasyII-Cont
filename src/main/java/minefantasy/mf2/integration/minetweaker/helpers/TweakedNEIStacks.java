package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;

/**
 * Turns a script ingredient into the plain stacks NEI can draw.
 * <p>
 * Only touched behind a CraftTweaker presence check, so the NEI handlers keep working when the mod is absent. Every
 * CraftTweaker type stays inside this class for that reason.
 */
public final class TweakedNEIStacks {

    private TweakedNEIStacks() {}

    public static boolean isIngredient(Object entry) {
        return entry instanceof IIngredient;
    }

    /** All stacks the ingredient accepts, each carrying the amount the recipe actually requires. */
    public static List<ItemStack> resolve(Object entry) {
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        if (!(entry instanceof IIngredient)) {
            return stacks;
        }
        IIngredient ingredient = (IIngredient) entry;
        int amount = Math.max(1, ingredient.getAmount());
        for (IItemStack item : ingredient.getItems()) {
            ItemStack stack = MineTweakerMC.getItemStack(item);
            if (stack == null || stack.getItem() == null) {
                continue;
            }
            ItemStack copy = stack.copy();
            copy.stackSize = amount;
            stacks.add(copy);
        }
        return stacks;
    }
}
