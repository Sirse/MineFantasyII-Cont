package minefantasy.mf2.integration.nei;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;

public class RecipeHandlerBigFurnace extends MFNEIRecipeHandler {

    public RecipeHandlerBigFurnace() {
        super("minefantasy2.big_furnace");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("nei.method.big_furnace");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/furnace_top.png";
    }

    /** NEI scrolls the page, so several recipes share it; the handler info carries the same value. */
    static final int RECIPES_PER_PAGE = 5;

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 11, 166, 63);
    }

    @Override
    public void drawExtras(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getGuiTexture());
        int progress = cycleticks % 24;
        GuiDraw.drawTexturedModalRect(71, 23, 176, 0, progress + 1, 16);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (NEIHelper.isValidStack(result)) {
            loadRecipesFor(result);
        }
    }

    @Override
    protected void loadAllRecipes() {
        loadRecipesFor(null);
    }

    private void loadRecipesFor(ItemStack result) {
        for (ProcessRecipe recipe : recipesMaking(MFRecipes.BIG_FURNACE, result)) {
            arecipes.add(new BigFurnaceRecipe(recipe));
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        for (ProcessRecipe recipe : recipesUsing(MFRecipes.BIG_FURNACE, ingredient)) {
            arecipes.add(new BigFurnaceRecipe(recipe));
        }
    }

    private class BigFurnaceRecipe extends CachedRecipe {

        private PositionedStack input;
        private PositionedStack output;

        private BigFurnaceRecipe(ProcessRecipe recipe) {
            input = NEIHelper.positionedInput(recipe.getInput(), 31, 15);
            output = NEIHelper.positionedStack(recipe.getOutput(), 102, 16); // Hell of a perfectionist
        }

        @Override
        public PositionedStack getOtherStack() {
            return input;
        }

        @Override
        public PositionedStack getResult() {
            return output;
        }
    }
}
