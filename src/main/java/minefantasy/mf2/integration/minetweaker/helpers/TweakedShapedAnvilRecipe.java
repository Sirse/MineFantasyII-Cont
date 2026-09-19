package minefantasy.mf2.integration.minetweaker.helpers;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.anvil.AnvilCraftMatrix;
import minefantasy.mf2.api.crafting.anvil.IAnvilRecipe;
import minefantasy.mf2.api.crafting.anvil.ShapelessAnvilRecipes;
import minefantasy.mf2.api.rpg.Skill;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;

public class TweakedShapedAnvilRecipe implements IAnvilRecipe {

    private int hammer, anvil, time, recipeWidth, recipeHeight;
    private float exp;
    private IItemStack result;
    private IIngredient[][] ingredients;
    private Skill skill;
    private String research, tool;
    private boolean hot;

    public TweakedShapedAnvilRecipe(IIngredient[][] input, IItemStack output, String tool, int time, int hammer,
            int anvil, boolean hot, String research, Skill skill) {
        this.ingredients = input;
        this.result = output;
        this.tool = tool;
        this.hammer = hammer;
        this.anvil = anvil;
        this.hot = hot;
        this.research = research;
        this.skill = skill;
        this.time = time;
        calculateRecipeSize();
    }

    @Override
    public int getAnvil() {
        return anvil;
    }

    @Override
    public int getCraftTime() {
        return time;
    }

    @Override
    public ItemStack getCraftingResult(AnvilCraftMatrix arg0) {
        return this.getRecipeOutput().copy();
    }

    @Override
    public int getRecipeHammer() {
        return hammer;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return MineTweakerMC.getItemStack(result).copy();
    }

    @Override
    public int getRecipeSize() {
        return this.recipeWidth * this.recipeHeight;
    }

    @Override
    public String getResearch() {
        return research;
    }

    @Override
    public Skill getSkill() {
        return this.skill;
    }

    @Override
    public String getToolType() {
        return tool;
    }

    @Override
    public boolean matches(AnvilCraftMatrix matrix) {
        // Walk the whole grid, not just the pattern: a cell outside it must be empty, otherwise a 1x1 recipe would
        // match with a stray item elsewhere and consumeResources would then eat that stack too.
        for (int y = 0; y < ShapelessAnvilRecipes.globalHeight; ++y) {
            for (int x = 0; x < ShapelessAnvilRecipes.globalWidth; ++x) {
                ItemStack inputItem = matrix.getStackInRowAndColumn(x, y);
                IIngredient ingredient = x < this.recipeWidth && y < this.recipeHeight
                        ? TweakedIngredients.gridCell(ingredients, y, x)
                        : null;

                if (inputItem == null && ingredient == null) {
                    continue;
                }
                if (inputItem == null || ingredient == null) {
                    return false;
                }
                if (!TweakedIngredients.matches(ingredient, inputItem)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void calculateRecipeSize() {
        this.recipeHeight = this.ingredients.length;
        for (int x = 0; x < this.recipeHeight; x++) {
            if (this.ingredients[x] != null && this.ingredients[x].length > this.recipeWidth) {
                this.recipeWidth = this.ingredients[x].length;
            }
        }
    }

    @Override
    public boolean outputHot() {
        return hot;
    }

    @Override
    public boolean useCustomTiers() {
        return false;
    }

    public IIngredient[][] getIngredients() {
        return ingredients;
    }

}
