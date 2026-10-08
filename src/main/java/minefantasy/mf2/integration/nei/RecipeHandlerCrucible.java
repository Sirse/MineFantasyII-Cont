package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.Requirements;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.api.refine.AlloyRecipes;
import minefantasy.mf2.block.refining.BlockCrucible;

public class RecipeHandlerCrucible extends MFNEIRecipeHandler {

    public RecipeHandlerCrucible() {
        super("minefantasy2.crucible");
    }

    private String recipeName = "method.crucible";

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal(recipeName);
    }

    @Override
    public String getGuiTexture() {
        return getTexture(0);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (NEIHelper.isValidStack(result)) {
            loadRecipesFor(result);
        }
    }

    @Override
    protected boolean madeOn(ItemStack station, CachedRecipe recipe) {
        int tier = stationTier(station, block -> block instanceof BlockCrucible ? ((BlockCrucible) block).tier : -1);
        return tier < 0 || Requirements.CRUCIBLE.stationFits(tier, ((CachedAlloyRecipe) recipe).tier);
    }

    @Override
    protected void loadAllRecipes() {
        loadRecipesFor(null);
    }

    private void loadRecipesFor(ItemStack result) {
        for (Alloy alloy : AlloyRecipes.alloys()) {
            if (alloy != null && NEIHelper.isValidStack(alloy.getRecipeOutput())
                    && matchesOutput(alloy.getRecipeOutput(), result)) {
                CachedAlloyRecipe recipe = new CachedAlloyRecipe(alloy);
                arecipes.add(recipe);
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        for (Alloy alloy : AlloyRecipes.alloys()) {
            if (alloy == null || !NEIHelper.isValidStack(alloy.getRecipeOutput())) {
                continue;
            }
            boolean used = false;
            for (Object object : alloy.getIngredients()) {
                for (ItemStack recipeIngredient : NEIHelper.resolveEntry(object)) {
                    if (NEIHelper.isValidStack(recipeIngredient)
                            && CustomToolHelper.areEqual(recipeIngredient, ingredient)) {
                        used = true;
                        break;
                    }
                }
                if (used) {
                    break;
                }
            }
            if (used) {
                arecipes.add(new CachedAlloyRecipe(alloy));
            }
        }
    }

    @Override
    public void drawBackground(int recipe) {
        CachedAlloyRecipe cachedRecipe = (CachedAlloyRecipe) arecipes.get(recipe);
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getTexture(cachedRecipe.tier));
        GuiDraw.drawTexturedModalRect(0, 0, 5, 0, 151, 94);
    }

    private String getTexture(int tier) {
        if (tier == 1) {
            return "minefantasy2:textures/gui/crucible_advanced.png";
        }
        if (tier >= 2) {
            return "minefantasy2:textures/gui/crucible_mythic.png";
        }
        return "minefantasy2:textures/gui/crucible.png";
    }

    private class CachedAlloyRecipe extends CachedRecipe {

        private ArrayList<PositionedStack> ingredients = new ArrayList<PositionedStack>();
        private PositionedStack output;
        private int tier;

        @SuppressWarnings("unchecked")
        private CachedAlloyRecipe(Alloy alloy) {
            setIngridients(alloy.getIngredients());
            output = NEIHelper.positionedStack(alloy.getRecipeOutput(), 124, 32);
            tier = alloy.level;
        }

        private void setIngridients(List<Object> recipeItems) {
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    int index = y * 3 + x;
                    if (recipeItems.size() > index) {
                        PositionedStack stack = NEIHelper
                                .positionedEntry(recipeItems.get(index), 57 + x * 18, 14 + y * 18);
                        if (stack == null) {
                            continue;
                        }
                        ingredients.add(stack);
                    }
                }
            }
        }

        // Deprecated in GTNH NEI without a replacement; its own handlers still cycle ingredients this way
        @SuppressWarnings("deprecation")
        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 20, ingredients);
        }

        @Override
        public PositionedStack getResult() {
            return output;
        }
    }
}
