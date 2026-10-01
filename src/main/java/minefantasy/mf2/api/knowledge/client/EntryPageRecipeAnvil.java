package minefantasy.mf2.api.knowledge.client;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.helpers.GuiHelper;
import minefantasy.mf2.api.helpers.TextureHelperMF;

public class EntryPageRecipeAnvil extends EntryPageRecipe {

    private static final int CELL = 18;
    private static final ResourceLocation GRID = TextureHelperMF.getResource("textures/gui/knowledge/anvilGrid.png");
    private final GridRecipe[] recipes;

    public EntryPageRecipeAnvil(List<GridRecipe> recipes) {
        this(recipes.toArray(new GridRecipe[0]));
    }

    public EntryPageRecipeAnvil(GridRecipe... recipes) {
        super("anvilGrid");
        this.recipes = recipes;
    }

    @Override
    protected int variantCount() {
        return recipes.length;
    }

    @Override
    protected boolean isPresent(int variant) {
        return MFRecipes.ANVIL.current(recipes[variant]) != null;
    }

    @Override
    protected String station() {
        return "anvil";
    }

    @Override
    protected int stationY() {
        return 150;
    }

    @Override
    protected void drawRecipe(GuiScreen parent, int posX, int posY, int mx, int my) {
        if (recipes.length == 0) {
            return;
        }
        // The page holds the recipe registered by the mod; a script may have replaced or removed it since
        GridRecipe recipe = MFRecipes.ANVIL.current(recipes[variant()]);
        if (recipe == null) {
            return;
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GuiHelper.renderToolIcon(
                parent,
                recipe.getToolType(),
                recipe.getRecipeHammer(),
                posX + 34,
                posY + 51,
                true,
                true);
        GuiHelper.renderToolIcon(parent, "anvil", recipe.getAnvil(), posX + 124, posY + 51, true, true);
        GridPages.forEachEntry(recipe, (x, y, stack) -> {
            int itemX = posX + x * CELL + 36, itemY = posY + y * CELL + 76;
            if (Heatable.canHeatItem(stack)) {
                heatMark(parent, itemX, itemY);
            }
            drawItem(stack, itemX, itemY, mx, my);
        });
        if (recipe.outputHot()) {
            heatMark(parent, posX + 80, posY + 42);
        }
        drawItem(recipe.getRecipeOutput(), posX + 80, posY + 42, mx, my);
    }

    /** The small mark in a cell's corner: this goes in, or comes out, hot. */
    private void heatMark(GuiScreen parent, int x, int y) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(GRID);
        parent.drawTexturedModalRect(x, y, 248, 0, 8, 8);
    }
}
