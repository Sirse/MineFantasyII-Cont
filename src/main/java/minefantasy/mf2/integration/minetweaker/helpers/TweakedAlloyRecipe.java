package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.GridMatch;
import minefantasy.mf2.api.refine.Alloy;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;

public class TweakedAlloyRecipe extends Alloy {

    public TweakedAlloyRecipe(IItemStack output, int requiredLevel, List<?> items) {
        super(MineTweakerMC.getItemStack(output), requiredLevel, items);
    }

    /**
     * Carbon sources are interchangeable, so any carbon item satisfies a carbon ingredient
     */
    private boolean matchesCarbon(IIngredient ingred, ItemStack stack) {
        for (IItemStack i : ingred.getItems()) {
            if (areBothCarbon(MineTweakerMC.getItemStack(i), stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean matches(ItemStack[] inv) {
        return assign(inv, null);
    }

    @Override
    public int[] getRequiredAmounts(ItemStack[] inventory) {
        int[] amounts = new int[inventory.length];
        java.util.Arrays.fill(amounts, 1);
        assign(inventory, amounts);
        return amounts;
    }

    /**
     * Pairs ingredients with slots once, trying other pairings when the first fit leaves an ingredient without a slot.
     * Passing an amounts array records how many items each slot owes, so matching and consuming always agree.
     */
    private boolean assign(ItemStack[] inv, int[] amounts) {
        List<Object> ingredients = getIngredients();
        int occupied = 0;
        for (ItemStack stack : inv) {
            if (stack != null) occupied++;
        }
        if (occupied != getIngredients().size()) {
            return false;
        }
        boolean[][] fits = new boolean[getIngredients().size()][inv.length];
        for (int i = 0; i < fits.length; i++) {
            IIngredient ingred = (IIngredient) ingredients.get(i);
            for (int slot = 0; slot < inv.length; slot++) {
                ItemStack stack = inv[slot];
                fits[i][slot] = stack != null
                        && (TweakedIngredients.matches(ingred, stack) || matchesCarbon(ingred, stack));
            }
        }
        int[] owner = GridMatch.pairAll(fits, inv.length);
        if (owner == null) {
            return false;
        }
        if (amounts != null) {
            for (int slot = 0; slot < owner.length && slot < amounts.length; slot++) {
                if (owner[slot] >= 0) {
                    amounts[slot] = Math.max(1, ((IIngredient) ingredients.get(owner[slot])).getAmount());
                }
            }
        }
        return true;
    }

}
