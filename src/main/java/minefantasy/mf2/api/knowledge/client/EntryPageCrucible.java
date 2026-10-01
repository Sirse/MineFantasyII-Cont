package minefantasy.mf2.api.knowledge.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.refine.Alloy;

public class EntryPageCrucible extends EntryPageRecipe {

    private static final int CELL = 29;
    private final Alloy[] recipes;

    public EntryPageCrucible(Alloy... recipes) {
        super("crucible");
        this.recipes = recipes;
    }

    @Override
    protected int variantCount() {
        return recipes.length;
    }

    @Override
    protected int stationY() {
        return 175;
    }

    /** The crucible good enough for the alloy shown. */
    @Override
    protected String station() {
        int tier = recipes.length == 0 ? 0 : recipes[variant()].getLevel();
        return tier == 3 ? "crucibleT4" : tier == 2 ? "crucibleT3" : tier == 1 ? "crucibleT2" : "crucible";
    }

    @Override
    protected void drawRecipe(GuiScreen parent, int posX, int posY, int mx, int my) {
        if (recipes.length == 0) {
            return;
        }
        Alloy recipe = recipes[variant()];
        // Filled from the bottom row up, three to a row
        int count = Math.min(9, recipe.getIngredients().size());
        for (int i = 0; i < count; i++) {
            int column = i % 3, row = 2 - i / 3;
            drawItem(
                    (ItemStack) recipe.getIngredients().get(i),
                    posX + column * CELL + 51,
                    posY + row * CELL + 42,
                    mx,
                    my);
        }
        drawItem(recipe.getRecipeOutput(), posX + 80, posY + 144, mx, my);
    }
}
