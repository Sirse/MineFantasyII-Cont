package minefantasy.mf2.api.crafting.carpenter;

import java.util.*;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.GridRepair;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.util.MFLogUtil;

/**
 * @author AnonymousProductions
 */
public class CraftingManagerCarpenter {

    /**
     * The static instance of this class
     */
    private static final CraftingManagerCarpenter instance = new CraftingManagerCarpenter();

    private CraftingManagerCarpenter() {
        MFLogUtil.logDebug("Carpenter recipes initiating");
    }

    /**
     * Returns the static instance of this class
     */
    public static CraftingManagerCarpenter getInstance() {
        return instance;
    }

    /** What the grid makes: a repair, or the first recipe's result; null for nothing. */
    public ItemStack findMatchingRecipe(InventoryCrafting matrix) {
        ItemStack repair = findRepairResult(matrix);
        if (repair != null) {
            return repair;
        }
        GridRecipe.Found found = find(matrix);
        return found == null ? null : found.getMatch().getResult();
    }

    public ItemStack findRepairResult(InventoryCrafting matrix) {
        return GridRepair.result(matrix);
    }

    private static boolean isRepairPair(InventoryCrafting matrix) {
        return GridRepair.pair(matrix) != null;
    }

    /**
     * The first recipe the grid holds, in lookup order, with its id and what the grid makes by it; null for none. A
     * repair pair is left to {@link #findRepairResult}.
     */
    public GridRecipe.Found find(InventoryCrafting matrix) {
        if (isRepairPair(matrix)) {
            return null;
        }
        return GridRecipe.find(MFRecipes.CARPENTER, matrix);
    }

    /**
     * returns the List<> of all recipes
     */
    public List getRecipeList() {
        return MFRecipes.CARPENTER.published().recipes();
    }
}
