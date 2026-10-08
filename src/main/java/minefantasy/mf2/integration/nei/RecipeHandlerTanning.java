package minefantasy.mf2.integration.nei;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;

public class RecipeHandlerTanning extends MFNEIRecipeHandler {

    public RecipeHandlerTanning() {
        super("minefantasy2.tanning");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("tile.tannerStrong.name");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/icons.png";
    }

    @Override
    public void drawBackground(int recipe) {
        // The rack has no GUI of its own, so draw bare slot frames with an arrow between them
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(48, 18, 20, 0, 20, 20);
        GuiDraw.drawTexturedModalRect(98, 18, 20, 0, 20, 20);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.65F, 0.65F, 0.65F, 0.9F);
        GL11.glLineWidth(2F);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(72, 28);
        GL11.glVertex2f(92, 28);
        GL11.glVertex2f(88, 24);
        GL11.glVertex2f(92, 28);
        GL11.glVertex2f(88, 32);
        GL11.glVertex2f(92, 28);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        for (ProcessRecipe recipe : recipesMaking(MFRecipes.TANNING, result)) {
            arecipes.add(new TanningPair(recipe));
        }
    }

    @Override
    protected void loadAllRecipes() {
        for (ProcessRecipe recipe : recipesMaking(MFRecipes.TANNING, null)) {
            arecipes.add(new TanningPair(recipe));
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        for (ProcessRecipe recipe : recipesUsing(MFRecipes.TANNING, ingredient)) {
            TanningPair cachedRecipe = new TanningPair(recipe);
            cachedRecipe.setIngredientPermutation(Arrays.asList(cachedRecipe.input), ingredient);
            arecipes.add(cachedRecipe);
        }
    }

    @Override
    public void drawExtras(int recipe) {
        TanningPair cachedRecipe = (TanningPair) this.arecipes.get(recipe);
        GuiDraw.drawString(
                String.format(
                        "%s: %s",
                        StatCollector.translateToLocal("nei.method.tanning.tool"),
                        cachedRecipe.toolType),
                10,
                85,
                -16777216,
                false);
    }

    private class TanningPair extends CachedRecipe {

        private PositionedStack input;
        private PositionedStack output;
        private String toolType;

        private TanningPair(ProcessRecipe recipe) {
            input = NEIHelper.positionedInput(recipe.getInput(), 50, 20);
            output = NEIHelper.positionedStack(recipe.getOutput(), 100, 20);
            toolType = recipe.get(MFRecipeKeys.TOOL, "knife");
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
