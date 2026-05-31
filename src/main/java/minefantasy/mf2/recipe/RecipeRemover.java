package minefantasy.mf2.recipe;

import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.util.MFLogUtil;

public class RecipeRemover {

    public static void removeRecipes() {
        MFLogUtil.log("MineFantasy: Removing replaced recipes...");
        List recipeList = CraftingManager.getInstance().getRecipeList();
        for (int a = recipeList.size() - 1; a >= 0; a--) {
            Object entry = recipeList.get(a);
            if (!(entry instanceof IRecipe)) {
                continue;
            }
            IRecipe rec = (IRecipe) entry;
            if (rec.getRecipeOutput() != null && willRemoveItem(rec.getRecipeOutput(), ConfigHardcore.HCCRemoveCraft)) {
                recipeList.remove(a);
            }
        }
    }

    private static boolean willRemoveItem(ItemStack item, boolean HCC) {
        if (item.getItem() == Items.stick) return true;

        if (HCC) {
            return item.getItem() == Items.bread || item.getItem() == Items.pumpkin_pie
                    || item.getItem() == Items.cake
                    || item.getItem() == Items.flint_and_steel
                    || item.getItem() == Items.bucket;
        }
        return false;
    }
}
