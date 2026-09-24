package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeEntry;

public class RecipeHandlerCooking extends MFNEIRecipeHandler {

    public static final int WIDTH = 166;
    public static final int HEIGHT = 82;

    public RecipeHandlerCooking() {
        super("minefantasy2.cooking");
    }

    /**
     * Every cooking recipe NEI can show: the registered ones, then plain food from the vanilla furnace that a spit
     * cooks when {@link CookRecipe#canCookBasics} allows it and no registered recipe claims the item.
     */
    private static List<CookingPair> allRecipes() {
        List<CookingPair> pairs = new ArrayList<CookingPair>();
        for (RecipeEntry<CookRecipe> entry : MFRecipes.COOKING.published().all()) {
            CookRecipe recipe = entry.getRecipe();
            pairs.add(new CookingPair(recipe.getInput(), null, recipe));
        }
        if (CookRecipe.canCookBasics) {
            for (Object key : FurnaceRecipes.smelting().getSmeltingList().keySet()) {
                if (!(key instanceof ItemStack) || !NEIHelper.isValidStack((ItemStack) key)) {
                    continue;
                }
                ItemStack input = (ItemStack) key;
                CookRecipe recipe = CookRecipe.getResult(input, false);
                if (recipe != null && isBasic(recipe, input)) {
                    pairs.add(new CookingPair(null, input, recipe));
                }
            }
        }
        return pairs;
    }

    /** A recipe built on the fly for vanilla food, rather than a registered one. */
    private static boolean isBasic(CookRecipe recipe, ItemStack input) {
        for (RecipeEntry<CookRecipe> entry : MFRecipes.COOKING.published().candidates(Input.lookupKeys(input))) {
            if (entry.getRecipe() == recipe) {
                return false;
            }
        }
        return true;
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
        if (recipe.getMinTemperature() == recipe.getMaxTemperature()) {
            return recipe.getMinTemperature() + " C";
        }
        return recipe.getMinTemperature() + "-" + recipe.getMaxTemperature() + " C";
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        for (CookingPair pair : allRecipes()) {
            if (CustomToolHelper.areEqual(pair.recipe.getOutput(), result)) {
                arecipes.add(new CachedCookingRecipe(pair));
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        for (CookingPair pair : allRecipes()) {
            if (pair.recipe.getInput().matches(ingredient)) {
                CachedCookingRecipe cachedRecipe = new CachedCookingRecipe(pair);
                cachedRecipe.setIngredientPermutation(cachedRecipe.getIngredients(), ingredient);
                arecipes.add(cachedRecipe);
            }
        }
    }

    private static class CookingPair {

        /** The registered input, or null for a vanilla food shown by its stack. */
        private final Input input;
        private final ItemStack stack;
        private final CookRecipe recipe;

        private CookingPair(Input input, ItemStack stack, CookRecipe recipe) {
            this.input = input;
            this.stack = NEIHelper.validCopy(stack);
            this.recipe = recipe;
        }
    }

    private class CachedCookingRecipe extends CachedRecipe {

        private final CookRecipe recipe;
        private final boolean oven;
        private final PositionedStack input;
        private final PositionedStack output;

        private CachedCookingRecipe(CookingPair recipePair) {
            recipe = recipePair.recipe;
            oven = recipe.isBaking();
            input = recipePair.input != null
                    ? NEIHelper.positionedInput(recipePair.input, NEILayout.COOKING_INPUT.x, NEILayout.COOKING_INPUT.y)
                    : NEILayout.stack(recipePair.stack, NEILayout.COOKING_INPUT);
            output = NEILayout.stack(recipe.getOutput(), NEILayout.COOKING_OUTPUT);
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
