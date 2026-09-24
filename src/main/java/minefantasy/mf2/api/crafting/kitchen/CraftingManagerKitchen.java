package minefantasy.mf2.api.crafting.kitchen;

import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.crafting.RecipePattern;
import minefantasy.mf2.api.rpg.Skill;

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

    public GridRecipe addRecipe(ItemStack result, Skill skill, String research, String sound, String tool, int toolTier,
            int time, float dirtyAmount, Object... input) {
        RecipePattern pattern = RecipePattern.shaped(input);
        GridRecipe recipe = GridRecipe
                .shaped(GridRecipe.Grid.BENCH, pattern.width, pattern.height, pattern.cells, null, result)
                .tool(tool, toolTier).time(time).research(research).skill(skill).sound(sound).dirtyAmount(dirtyAmount)
                .build();
        NativeRecipes.addGrid(MFRecipes.KITCHEN, recipe.getRecipeOutput(), recipe, 0);
        return recipe;
    }

    public GridRecipe addShapelessRecipe(ItemStack result, Skill skill, String research, String sound, String tool,
            int toolTier, int time, float dirtyAmount, Object... input) {
        GridRecipe recipe = GridRecipe.shapeless(GridRecipe.Grid.BENCH, RecipePattern.shapeless(input), null, result)
                .tool(tool, toolTier).time(time).research(research).skill(skill).sound(sound).dirtyAmount(dirtyAmount)
                .build();
        NativeRecipes.addGrid(MFRecipes.KITCHEN, recipe.getRecipeOutput(), recipe, 0);
        return recipe;
    }

    /** The first recipe the grid holds, with what it makes and on which terms; null for none. */
    public GridRecipe.Match match(InventoryCrafting matrix) {
        for (GridRecipe recipe : MFRecipes.KITCHEN.published().recipes()) {
            GridRecipe.Match match = recipe.match(matrix);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    public List getRecipeList() {
        return MFRecipes.KITCHEN.published().recipes();
    }
}
