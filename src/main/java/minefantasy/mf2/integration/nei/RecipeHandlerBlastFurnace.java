package minefantasy.mf2.integration.nei;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.knowledge.KnowledgeListMF;

public class RecipeHandlerBlastFurnace extends MFNEIRecipeHandler {

    public RecipeHandlerBlastFurnace() {
        super("minefantasy2.blast_furnace");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.blastfurnace");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/blast_chamber.png";
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 0, 133, 112);
    }

    @Override
    public int recipiesPerPage() {
        return 1;
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
        if (NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, KnowledgeListMF.blastfurn)) {
            for (ProcessRecipe recipe : recipesMaking(MFRecipes.BLAST_FURNACE, result)) {
                arecipes.add(new CachedBlastFurnaceRecipe(recipe));
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, KnowledgeListMF.blastfurn)) {
            for (ProcessRecipe recipe : recipesUsing(MFRecipes.BLAST_FURNACE, ingredient)) {
                arecipes.add(new CachedBlastFurnaceRecipe(recipe));
            }
        }
    }

    private class CachedBlastFurnaceRecipe extends CachedRecipe {

        private PositionedStack input;
        private PositionedStack output;

        private CachedBlastFurnaceRecipe(ProcessRecipe recipe) {
            input = NEIHelper.positionedInput(recipe.getInput(), 75, 30);
            output = NEIHelper.positionedStack(recipe.getOutput(), 75, 68);
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
