package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.heating.ForgeFuel;
import minefantasy.mf2.api.heating.ForgeItemHandler;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;

public class RecipeHandlerBloom extends MFNEIRecipeHandler {

    public RecipeHandlerBloom() {
        super("minefantasy2.bloomery");
    }

    private static ArrayList<FuelPair> afuels;

    private static void findFuels() {
        afuels = new ArrayList<FuelPair>();
        for (ForgeFuel fuel : ForgeItemHandler.forgeFuel) {
            if (fuel == null || !NEIHelper.isValidStack(fuel.fuel)) {
                continue;
            }
            ItemStack item = fuel.fuel;
            if (TileEntityBlastFC.isCarbon(item) && MineFantasyFuels.getCarbon(item) > 0) {
                afuels.add(new FuelPair(item.copy()));
            }
        }
    }

    @Override
    public codechicken.nei.recipe.TemplateRecipeHandler newInstance() {
        if (afuels == null || afuels.isEmpty()) {
            findFuels();
        }
        return super.newInstance();
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.bloomery");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/bloomery.png";
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getGuiTexture());
        // Only the furnace shape (u 53-123, v 20-98) is drawn, centred in the recipe area; the container's slots
        // (80,30 and 80,68) move by the same offset
        GuiDraw.drawTexturedModalRect(48, 5, 53, 20, 70, 78);
        drawSlotFrame(134, 34);
    }

    private void drawSlotFrame(int x, int y) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GuiDraw.changeTexture("minefantasy2:textures/gui/icons.png");
        GuiDraw.drawTexturedModalRect(x - 2, y - 2, 20, 0, 20, 20);
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
        for (RecipeEntry<BloomRecipe> entry : MFRecipes.BLOOMERY.published().all()) {
            if (matchesOutput(entry.getRecipe().getOutput(), result)) {
                addRecipe(entry.getRecipe(), null);
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        for (RecipeEntry<BloomRecipe> entry : MFRecipes.BLOOMERY.published().candidates(Input.lookupKeys(ingredient))) {
            if (entry.getRecipe().getInput().matches(ingredient)) {
                addRecipe(entry.getRecipe(), ingredient);
            }
        }
    }

    private void addRecipe(BloomRecipe recipe, ItemStack shownInput) {
        SmeltingPair pair = new SmeltingPair(recipe);
        if (pair.ingred == null || pair.result == null) {
            return;
        }
        if (shownInput != null) {
            pair.setIngredientPermutation(Arrays.asList(pair.ingred), shownInput);
        }
        arecipes.add(pair);
    }

    private static class FuelPair {

        private PositionedStack stack;

        private FuelPair(ItemStack fuel) {
            this.stack = NEIHelper.positionedStack(fuel, 75, 53, false);
        }
    }

    private class SmeltingPair extends CachedRecipe {

        private PositionedStack ingred;
        private PositionedStack result;

        private SmeltingPair(BloomRecipe recipe) {
            this.ingred = NEIHelper.positionedInput(recipe.getInput(), 75, 15);
            this.result = NEIHelper.positionedStack(recipe.getOutput(), 134, 34);
        }

        // Deprecated in GTNH NEI without a replacement; its own handlers still cycle ingredients this way
        @SuppressWarnings("deprecation")
        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 48, Arrays.asList(ingred));
        }

        @Override
        public PositionedStack getResult() {
            return result;
        }

        @Override
        public PositionedStack getOtherStack() {
            if (afuels == null || afuels.isEmpty()) {
                return null;
            }
            return afuels.get((cycleticks / 48) % afuels.size()).stack;
        }
    }
}
