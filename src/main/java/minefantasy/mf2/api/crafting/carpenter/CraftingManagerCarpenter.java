package minefantasy.mf2.api.crafting.carpenter;

import java.util.*;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.GridRepair;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.crafting.RecipePattern;
import minefantasy.mf2.api.rpg.Skill;

/**
 * @author AnonymousProductions
 */
public class CraftingManagerCarpenter {

    /**
     * The static instance of this class
     */
    private static final CraftingManagerCarpenter instance = new CraftingManagerCarpenter();

    private CraftingManagerCarpenter() {
        System.out.println("MineFantasy: Anvil recipes initiating");
    }

    /**
     * Returns the static instance of this class
     */
    public static CraftingManagerCarpenter getInstance() {
        return instance;
    }

    /**
     * Adds a recipe. See spreadsheet on first page for details.
     */
    public GridRecipe addRecipe(ItemStack result, Skill skill, String research, String sound, float exp, String tool,
            int hammer, int anvil, int time, Object... input) {
        return addRecipe(result, skill, research, sound, exp, tool, hammer, anvil, time, (byte) 0, input);
    }

    public GridRecipe addToolRecipe(ItemStack result, Skill skill, String research, String sound, float exp,
            String tool, int hammer, int anvil, int time, Object... input) {
        return addRecipe(result, skill, research, sound, exp, tool, hammer, anvil, time, (byte) 1, input);
    }

    /**
     * Adds a recipe. See spreadsheet on first page for details.
     */
    public GridRecipe addRecipe(ItemStack result, Skill skill, String research, String sound, float exp, String tool,
            int hammer, int anvil, int time, byte id, Object... input) {
        RecipePattern pattern = RecipePattern.shaped(input);
        GridRecipe recipe = GridRecipe
                .shaped(GridRecipe.Grid.BENCH, pattern.width, pattern.height, pattern.cells, null, result)
                .tool(tool, hammer).stationTier(anvil).time(time).research(research).skill(skill).sound(sound)
                .experience(exp).tiers(id == (byte) 1 ? GridRecipe.Tiers.MATERIAL : GridRecipe.Tiers.FIXED).build();
        NativeRecipes.addGrid(MFRecipes.CARPENTER, recipe.getRecipeOutput(), recipe, 0);
        return recipe;
    }

    public GridRecipe addShapelessRecipe(ItemStack output, Skill skill, String research, String sound, float experience,
            String tool, int hammer, int anvil, int time, Object... input) {
        GridRecipe recipe = GridRecipe.shapeless(GridRecipe.Grid.BENCH, RecipePattern.shapeless(input), null, output)
                .tool(tool, hammer).stationTier(anvil).time(time).research(research).skill(skill).sound(sound)
                .experience(experience).build();
        NativeRecipes.addGrid(MFRecipes.CARPENTER, recipe.getRecipeOutput(), recipe, 0);
        return recipe;
    }

    /** What the grid makes: a repair, or the first recipe's result; null for nothing. */
    public ItemStack findMatchingRecipe(InventoryCrafting matrix) {
        ItemStack repair = findRepairResult(matrix);
        if (repair != null) {
            return repair;
        }
        GridRecipe.Match match = match(matrix);
        return match == null ? null : match.getResult();
    }

    public ItemStack findRepairResult(InventoryCrafting matrix) {
        return GridRepair.result(matrix);
    }

    private static boolean isRepairPair(InventoryCrafting matrix) {
        return GridRepair.pair(matrix) != null;
    }

    /**
     * The first recipe the grid holds, in lookup order, with what it makes and on which terms; null for none. A repair
     * pair is left to {@link #findRepairResult}.
     */
    public GridRecipe.Match match(InventoryCrafting matrix) {
        if (isRepairPair(matrix)) {
            return null;
        }
        for (GridRecipe recipe : MFRecipes.CARPENTER.published().recipes()) {
            GridRecipe.Match match = recipe.match(matrix);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    /**
     * returns the List<> of all recipes
     */
    public List getRecipeList() {
        return MFRecipes.CARPENTER.published().recipes();
    }
}
