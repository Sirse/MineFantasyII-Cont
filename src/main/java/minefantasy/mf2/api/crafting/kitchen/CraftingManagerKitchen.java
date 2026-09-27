package minefantasy.mf2.api.crafting.kitchen;

import java.util.List;

import net.minecraft.inventory.InventoryCrafting;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;

/**
 * Recipe manager for the kitchen bench. Reuses carpenter recipe types so knowledge pages and NEI handlers keep working,
 * but keeps food recipes in a separate registry and feeds the dirty amount back to the bench.
 */
public class CraftingManagerKitchen {

    private static final CraftingManagerKitchen instance = new CraftingManagerKitchen();

    private CraftingManagerKitchen() {}

    public static CraftingManagerKitchen getInstance() {
        return instance;
    }

    /**
     * The first recipe the grid holds, in lookup order, with its id and what the grid makes by it; null for none.
     */
    public GridRecipe.Found find(InventoryCrafting matrix) {
        return GridRecipe.find(MFRecipes.KITCHEN, matrix);
    }

    public List getRecipeList() {
        return MFRecipes.KITCHEN.published().recipes();
    }
}
