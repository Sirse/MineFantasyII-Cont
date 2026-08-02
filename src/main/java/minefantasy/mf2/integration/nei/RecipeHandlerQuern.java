package minefantasy.mf2.integration.nei;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.refine.QuernRecipes;
import minefantasy.mf2.api.helpers.CustomToolHelper;
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
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        for (QuernRecipes recipe : QuernRecipes.recipeList) {
            if (isValidRecipe(recipe) && CustomToolHelper.areEqual(recipe.result, result)) {
                CachedQuernRecipe cachedRecipe = new CachedQuernRecipe(recipe);
                arecipes.add(cachedRecipe);
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (NEIHelper.isValidStack(ingredient)) {
            if (ingredient.getItem().equals(ComponentListMF.clay_pot)) {
                for (QuernRecipes recipe : QuernRecipes.recipeList) {
                    if (!isValidRecipe(recipe)) {
                        continue;
                    }
                    CachedQuernRecipe cachedRecipe = new CachedQuernRecipe(recipe);
                    arecipes.add(cachedRecipe);
                }
                return;
            }

            for (QuernRecipes output : QuernRecipes.recipeList) {
                if (isValidRecipe(output) && NEIHelper.matchesItemDamage(output.input, ingredient)) {
                    CachedQuernRecipe recipe = new CachedQuernRecipe(output);
                    arecipes.add(recipe);
                }
            }
        }
    }

    private boolean isValidRecipe(QuernRecipes recipe) {
        return recipe != null && NEIHelper.isValidStack(recipe.input) && NEIHelper.isValidStack(recipe.result);
    }

    private class CachedQuernRecipe extends CachedRecipe {

        private ItemStack input, output;
        private boolean consumePot;

        private CachedQuernRecipe(QuernRecipes recipe) {
            input = NEIHelper.validCopy(recipe.input);
            output = NEIHelper.validCopy(recipe.result);
            consumePot = recipe.consumePot;
        }

        @Override
        public PositionedStack getIngredient() {
            return NEIHelper.positionedStack(input, 76, 9);
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
