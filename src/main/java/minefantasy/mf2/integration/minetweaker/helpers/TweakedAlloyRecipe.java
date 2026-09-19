package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

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
        // One list of outstanding requirements: search and removal must happen in the same list, otherwise an
        // already-satisfied ingredient stays a candidate for the next stack and the recipe accepts extra items.
        ArrayList<IIngredient> remaining = new ArrayList<IIngredient>(recipeItems);
        for (ItemStack stack : inv) {
            if (stack != null) {
                IIngredient matched = null;
                for (IIngredient ingred : remaining) {
                    if (TweakedIngredients.matches(ingred, stack) || matchesCarbon(ingred, stack)) {
                        matched = ingred;
                        break;
                    }
                }
                if (matched == null) {
                    return false;
                }
                remaining.remove(matched);
            }
        }
        return remaining.isEmpty();
    }

}
