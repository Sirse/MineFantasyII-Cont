package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.refine.PaintOilRecipe;
import minefantasy.mf2.item.list.ComponentListMF;

public class RecipeHandlerPaintOil extends MFNEIRecipeHandler {

    public RecipeHandlerPaintOil() {
        super("minefantasy2.paint_oil");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.paintOil");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/knowledge/carpenterGrid.png";
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 33, 166, 90);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        for (Entry<ItemStack, ItemStack> recipe : PaintOilRecipe.recipeList.entrySet()) {
            if (isValidRecipe(recipe)
                    && NEIHelper.matchesCrafting(materializeOutput(recipe.getValue(), result), result)) {
                arecipes.add(
                        new CachedPaintOilRecipe(
                                recipe.getKey(),
                                materializeOutput(recipe.getValue(), result),
                                result));
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        if (ingredient.getItem() == ComponentListMF.plant_oil) {
            for (Entry<ItemStack, ItemStack> recipe : PaintOilRecipe.recipeList.entrySet()) {
                if (isValidRecipe(recipe)) {
                    arecipes.add(
                            new CachedPaintOilRecipe(
                                    recipe.getKey(),
                                    materializeOutput(recipe.getValue(), recipe.getKey())));
                }
            }
            return;
        }
        for (Entry<ItemStack, ItemStack> recipe : PaintOilRecipe.recipeList.entrySet()) {
            if (isValidRecipe(recipe) && NEIHelper.matchesCrafting(recipe.getKey(), ingredient)) {
                CachedPaintOilRecipe cachedRecipe = new CachedPaintOilRecipe(
                        recipe.getKey(),
                        materializeOutput(recipe.getValue(), ingredient),
                        ingredient);
                cachedRecipe.setIngredientPermutation(cachedRecipe.getIngredients(), ingredient);
                arecipes.add(cachedRecipe);
            }
        }
    }

    private boolean isValidRecipe(Entry<ItemStack, ItemStack> recipe) {
        return recipe != null && NEIHelper.isValidStack(recipe.getKey()) && NEIHelper.isValidStack(recipe.getValue());
    }

    private ItemStack materializeOutput(ItemStack output, ItemStack source) {
        if (!NEIHelper.isValidStack(output)) {
            return null;
        }
        ItemStack copy = output.copy();
        if (copy.getItemDamage() == OreDictionary.WILDCARD_VALUE && source != null) {
            copy.setItemDamage(source.getItemDamage());
        }
        return copy;
    }

    private class CachedPaintOilRecipe extends CachedRecipe {

        private final PositionedStack input;
        private final PositionedStack oil;
        private final PositionedStack output;

        private CachedPaintOilRecipe(ItemStack inputStack, ItemStack outputStack) {
            this(inputStack, outputStack, inputStack);
        }

        private CachedPaintOilRecipe(ItemStack inputStack, ItemStack outputStack, ItemStack resultSource) {
            input = NEILayout.stack(inputStack, NEILayout.PAINT_OIL_INPUT);
            oil = NEILayout.stack(new ItemStack(ComponentListMF.plant_oil), NEILayout.PAINT_OIL_OIL);
            output = NEILayout.stack(materializeOutput(outputStack, resultSource), NEILayout.PAINT_OIL_OUTPUT);
        }

        @Override
        public List<PositionedStack> getIngredients() {
            ArrayList<PositionedStack> ingredients = new ArrayList<PositionedStack>();
            if (input != null) {
                ingredients.add(input);
            }
            if (oil != null) {
                ingredients.add(oil);
            }
            return ingredients;
        }

        @Override
        public PositionedStack getResult() {
            return output;
        }
    }
}
