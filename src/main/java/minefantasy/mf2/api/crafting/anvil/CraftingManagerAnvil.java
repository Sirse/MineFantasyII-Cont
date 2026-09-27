package minefantasy.mf2.api.crafting.anvil;

import java.util.*;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.GridRepair;
import minefantasy.mf2.api.crafting.MFRecipes;

/**
 * @author AnonymousProductions
 */
public class CraftingManagerAnvil {

    /**
     * The static instance of this class
     */
    private static final CraftingManagerAnvil instance = new CraftingManagerAnvil();

    private CraftingManagerAnvil() {
        System.out.println("MineFantasy: Anvil recipes initiating");
    }

    /**
     * Returns the static instance of this class
     */
    public static CraftingManagerAnvil getInstance() {
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
        return GridRecipe.find(MFRecipes.ANVIL, matrix);
    }

    /**
     * returns the List<> of all recipes
     */
    public List getRecipeList() {
        return MFRecipes.ANVIL.published().recipes();
    }

    /**
     * Shaped recipes before shapeless ones, larger before smaller: the order the anvil has always tried them in, so a
     * small pattern cannot steal the items of a larger one.
     */
    public static int priorityOf(GridRecipe recipe) {
        return (recipe.isShaped() ? 1000 : 0) + recipe.getRecipeSize();
    }
}
