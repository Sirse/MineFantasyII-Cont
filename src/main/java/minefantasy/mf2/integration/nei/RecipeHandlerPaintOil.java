package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
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
        return "minefantasy2:textures/gui/icons.png";
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @Override
    public void drawBackground(int recipe) {
        // Oiling happens in hand, with no GUI to borrow, so draw bare slot frames with an arrow to the result
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GuiDraw.changeTexture(getGuiTexture());
        drawSlotFrame(NEILayout.PAINT_OIL_INPUT);
        drawSlotFrame(NEILayout.PAINT_OIL_OIL);
        drawSlotFrame(NEILayout.PAINT_OIL_OUTPUT);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.65F, 0.65F, 0.65F, 0.9F);
        GL11.glLineWidth(2F);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(74, 39);
        GL11.glVertex2f(94, 39);
        GL11.glVertex2f(90, 35);
        GL11.glVertex2f(94, 39);
        GL11.glVertex2f(90, 43);
        GL11.glVertex2f(94, 39);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private void drawSlotFrame(NEILayout.Slot slot) {
        GuiDraw.drawTexturedModalRect(slot.x - 2, slot.y - 2, 20, 0, 20, 20);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        for (ProcessRecipe recipe : recipesMaking(MFRecipes.PAINT_OIL, null)) {
            ItemStack input = displayInput(recipe);
            if (input != null && NEIHelper.matchesCrafting(materializeOutput(recipe.getOutput(), result), result)) {
                arecipes.add(new CachedPaintOilRecipe(input, materializeOutput(recipe.getOutput(), result), result));
            }
        }
    }

    @Override
    protected void loadAllRecipes() {
        for (ProcessRecipe recipe : recipesMaking(MFRecipes.PAINT_OIL, null)) {
            ItemStack input = displayInput(recipe);
            if (input != null) {
                arecipes.add(new CachedPaintOilRecipe(input, materializeOutput(recipe.getOutput(), input)));
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        if (ingredient.getItem() == ComponentListMF.plant_oil) {
            loadAllRecipes();
            return;
        }
        for (ProcessRecipe recipe : recipesUsing(MFRecipes.PAINT_OIL, ingredient)) {
            CachedPaintOilRecipe cachedRecipe = new CachedPaintOilRecipe(
                    ingredient,
                    materializeOutput(recipe.getOutput(), ingredient),
                    ingredient);
            cachedRecipe.setIngredientPermutation(cachedRecipe.getIngredients(), ingredient);
            arecipes.add(cachedRecipe);
        }
    }

    private static ItemStack displayInput(ProcessRecipe recipe) {
        List<ItemStack> examples = recipe.getInput().examples();
        return examples.isEmpty() ? null : examples.get(0);
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
