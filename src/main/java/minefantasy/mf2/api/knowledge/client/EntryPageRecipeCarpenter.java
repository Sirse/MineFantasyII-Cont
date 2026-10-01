package minefantasy.mf2.api.knowledge.client;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.opengl.GL11;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.kitchen.CraftingManagerKitchen;
import minefantasy.mf2.api.helpers.GuiHelper;

public class EntryPageRecipeCarpenter extends EntryPageRecipe {

    private static final int CELL = 23;
    private final GridRecipe[] recipes;
    /** Station label/icon key. Null means it is derived from where the recipe is registered. */
    private final String station;

    public EntryPageRecipeCarpenter(List<GridRecipe> recipes) {
        this(recipes.toArray(new GridRecipe[0]));
    }

    public EntryPageRecipeCarpenter(GridRecipe... recipes) {
        this(null, recipes);
    }

    /**
     * @param station tool type key used for the station label and icon, e.g. "carpenter" or "kitchenbench"
     */
    public EntryPageRecipeCarpenter(String station, GridRecipe... recipes) {
        super("carpenterGrid");
        this.recipes = recipes;
        this.station = station;
    }

    @Override
    protected int variantCount() {
        return recipes.length;
    }

    @Override
    protected int stationY() {
        return 175;
    }

    @Override
    protected String station() {
        return recipes.length == 0 ? "carpenter" : stationOf(shown());
    }

    /** The page holds the recipe registered by the mod; a script may have replaced or removed it since. */
    private GridRecipe shown() {
        GridRecipe recipe = recipes[variant()];
        // Recipes registered with the kitchen manager belong to the kitchen bench; everything else is a carpenter one
        return MFRecipes.KITCHEN.idOf(recipe) != null ? MFRecipes.KITCHEN.current(recipe)
                : MFRecipes.CARPENTER.current(recipe);
    }

    /**
     * Derived from where the recipe is registered, which keeps the page correct when the kitchen bench is disabled and
     * its recipes fall back to the carpenter bench.
     */
    private String stationOf(GridRecipe recipe) {
        if (station != null) {
            return station;
        }
        if (recipe != null && CraftingManagerKitchen.getInstance().getRecipeList().contains(recipe)) {
            return "kitchenbench";
        }
        return "carpenter";
    }

    @Override
    protected void drawRecipe(GuiScreen parent, int posX, int posY, int mx, int my) {
        if (recipes.length == 0) {
            return;
        }
        GridRecipe recipe = shown();
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
        GuiHelper.renderToolIcon(parent, stationOf(recipe), recipe.getAnvil(), posX + 124, posY + 51, true, true);
        GridPages.forEachEntry(
                recipe,
                (x, y, stack) -> drawItem(stack, posX + x * CELL + 46, posY + y * CELL + 80, mx, my));
        drawItem(recipe.getRecipeOutput(), posX + 80, posY + 42, mx, my);
    }
}
