package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.heating.ForgeFuel;
import minefantasy.mf2.api.heating.ForgeItemHandler;
import minefantasy.mf2.api.helpers.CustomToolHelper;
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
    public int recipiesPerPage() {
        return 1;
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 0, 11, 176, 89);
        drawSlotFrame(120, 27);
    }

    private void drawSlotFrame(int x, int y) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GuiDraw.changeTexture("minefantasy2:textures/gui/icons.png");
        GuiDraw.drawTexturedModalRect(x - 2, y - 2, 20, 0, 20, 20);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        Map<ItemStack, ItemStack> recipes = BloomRecipe.recipeList;
        for (Entry<ItemStack, ItemStack> recipe : recipes.entrySet()) {
            if (recipe != null && NEIHelper.isValidStack(recipe.getKey())
                    && NEIHelper.isValidStack(recipe.getValue())
                    && CustomToolHelper.areEqual(recipe.getValue(), result)) {
                arecipes.add(new SmeltingPair(recipe.getKey(), recipe.getValue()));
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        ItemStack result = BloomRecipe.getSmeltingResult(ingredient);
        if (NEIHelper.isValidStack(result)) {
            SmeltingPair arecipe = new SmeltingPair(ingredient, result);
            arecipe.setIngredientPermutation(Arrays.asList(arecipe.ingred), ingredient);
            arecipes.add(arecipe);
        }
    }

    private static class FuelPair {

        private PositionedStack stack;

        private FuelPair(ItemStack fuel) {
            this.stack = NEIHelper.positionedStack(fuel, 75, 46, false);
        }
    }

    private class SmeltingPair extends CachedRecipe {

        private PositionedStack ingred;
        private PositionedStack result;

        private SmeltingPair(ItemStack ingred, ItemStack result) {
            this.ingred = NEIHelper.positionedStack(ingred, 75, 8);
            this.result = NEIHelper.positionedStack(result, 120, 27);
        }

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
