package minefantasy.mf2.integration.nei;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.item.list.ComponentListMF;

public class RecipeHandlerQuern extends MFNEIRecipeHandler {

    public RecipeHandlerQuern() {
        super("minefantasy2.quern");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.quern");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/quern.png";
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 0, 122, 80);
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
        for (ProcessRecipe recipe : recipesMaking(MFRecipes.QUERN, result)) {
            arecipes.add(new CachedQuernRecipe(recipe));
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        if (ingredient.getItem() == ComponentListMF.clay_pot) {
            for (ProcessRecipe recipe : recipesMaking(MFRecipes.QUERN, null)) {
                if (recipe.get(MFRecipeKeys.CONSUME_POT, true)) {
                    arecipes.add(new CachedQuernRecipe(recipe));
                }
            }
            return;
        }
        for (ProcessRecipe recipe : recipesUsing(MFRecipes.QUERN, ingredient)) {
            arecipes.add(new CachedQuernRecipe(recipe));
        }
    }

    private class CachedQuernRecipe extends CachedRecipe {

        private final PositionedStack input;
        private final ItemStack output;
        private final boolean consumePot;

        private CachedQuernRecipe(ProcessRecipe recipe) {
            input = NEIHelper.positionedInput(recipe.getInput(), 76, 9);
            output = recipe.getOutput();
            consumePot = recipe.get(MFRecipeKeys.CONSUME_POT, true);
        }

        @Override
        public PositionedStack getIngredient() {
            return input;
        }

        @Override
        public PositionedStack getOtherStack() {
            if (consumePot) {
                return NEIHelper.positionedStack(new ItemStack(ComponentListMF.clay_pot), 76, 32);
            }
            return null;
        }

        @Override
        public PositionedStack getResult() {
            return NEIHelper.positionedStack(output, 76, 55);
        }
    }
}
