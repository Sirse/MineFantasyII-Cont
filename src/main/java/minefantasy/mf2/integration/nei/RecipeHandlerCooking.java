package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.ItemList;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.util.MFLogUtil;

public class RecipeHandlerCooking extends MFNEIRecipeHandler {

    public static final int WIDTH = 166;
    public static final int HEIGHT = 82;

    public RecipeHandlerCooking() {
        super("minefantasy2.cooking");
    }

    private static ArrayList<CookingPair> recipeList;
    private static boolean recipeListBuilt;

    @Override
    public codechicken.nei.recipe.TemplateRecipeHandler newInstance() {
        if (!recipeListBuilt) {
            fillRecipeList();
        }
        return super.newInstance();
    }

    private static void fillRecipeList() {
        recipeList = new ArrayList<CookingPair>();
        Set<String> addedRecipes = new HashSet<String>();
        ArrayList<ItemStack> items = snapshotItemList();
        if (items == null) {
            return;
        }
        for (ItemStack item : items) {
            if (!NEIHelper.isValidStack(item)) {
                continue;
            }
            addRecipe(item, false, addedRecipes);
            addRecipe(item, true, addedRecipes);
        }
        recipeListBuilt = true;
    }

    private static ArrayList<ItemStack> snapshotItemList() {
        try {
            return new ArrayList<ItemStack>(ItemList.items);
        } catch (ConcurrentModificationException e) {
            MFLogUtil.warnOnce("nei-cooking-itemlist-cme", "NEI item list changed while building cooking recipes", e);
            return null;
        }
    }

    private static void addRecipe(ItemStack input, boolean oven, Set<String> addedRecipes) {
        CookRecipe recipe = CookRecipe.getResult(input, oven);
        if (recipe == null || !NEIHelper.isValidStack(recipe.output)) {
            return;
        }
        String recipeKey = getRecipeKey(input, recipe.output, oven);
        if (!addedRecipes.add(recipeKey)) {
            return;
        }
        recipeList.add(new CookingPair(input, recipe, oven));
    }

    private static String getRecipeKey(ItemStack input, ItemStack output, boolean oven) {
        return CustomToolHelper.getReferenceName(input) + ">" + CustomToolHelper.getReferenceName(output) + ">" + oven;
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.cooking");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/furnace_top.png";
    }

    @Override
    public int recipiesPerPage() {
        return 2;
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 11, 166, 63);

        int arrow = (cycleticks % 24) + 1;
        GuiDraw.drawTexturedModalRect(71, 24, 176, 0, arrow, 16);
    }

    @Override
    public void drawExtras(int recipe) {
        CachedCookingRecipe cachedRecipe = (CachedCookingRecipe) arecipes.get(recipe);
        String temperature = formatTemperature(cachedRecipe.recipe);
        int temperatureX = (WIDTH - GuiDraw.getStringWidth(temperature)) / 2;
        GuiDraw.drawString(temperature, temperatureX, 66, -16777216, false);
        if (cachedRecipe.oven) {
            String oven = StatCollector.translateToLocal("method.oven");
            int ovenX = (WIDTH - GuiDraw.getStringWidth(oven)) / 2;
            GuiDraw.drawString(oven, ovenX, 75, -16777216, false);
        }
    }

    private String formatTemperature(CookRecipe recipe) {
        if (recipe.minTemperature == recipe.maxTemperature) {
            return recipe.minTemperature + " C";
        }
        return recipe.minTemperature + "-" + recipe.maxTemperature + " C";
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        ensureRecipeList();
        for (CookingPair recipePair : recipeList) {
            if (recipePair != null && CustomToolHelper.areEqual(recipePair.recipe.output, result)) {
                arecipes.add(new CachedCookingRecipe(recipePair));
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        ensureRecipeList();
        for (CookingPair recipePair : recipeList) {
            if (recipePair != null && CustomToolHelper.areEqual(recipePair.input, ingredient)) {
                CachedCookingRecipe cachedRecipe = new CachedCookingRecipe(recipePair);
                cachedRecipe.setIngredientPermutation(cachedRecipe.getIngredients(), ingredient);
                arecipes.add(cachedRecipe);
            }
        }
    }

    private void ensureRecipeList() {
        if (!recipeListBuilt) {
            fillRecipeList();
        }
    }

    private static class CookingPair {

        private final ItemStack input;
        private final CookRecipe recipe;
        private final boolean oven;

        private CookingPair(ItemStack input, CookRecipe recipe, boolean oven) {
            this.input = NEIHelper.validCopy(input);
            this.recipe = recipe;
            this.oven = oven;
        }
    }

    private class CachedCookingRecipe extends CachedRecipe {

        private final CookRecipe recipe;
        private final boolean oven;
        private final PositionedStack input;
        private final PositionedStack output;

        private CachedCookingRecipe(CookingPair recipePair) {
            recipe = recipePair.recipe;
            oven = recipePair.oven;
            input = NEILayout.stack(recipePair.input, NEILayout.COOKING_INPUT);
            output = NEILayout.stack(recipe.output, NEILayout.COOKING_OUTPUT);
        }

        @Override
        public List<PositionedStack> getIngredients() {
            ArrayList<PositionedStack> ingredients = new ArrayList<PositionedStack>();
            if (input != null) {
                ingredients.add(input);
            }
            return ingredients;
        }

        @Override
        public PositionedStack getResult() {
            return output;
        }
    }
}
