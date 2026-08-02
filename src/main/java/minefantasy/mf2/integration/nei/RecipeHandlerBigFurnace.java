package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.ItemList;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.refine.BigFurnaceRecipes;

public class RecipeHandlerBigFurnace extends MFNEIRecipeHandler {

    public RecipeHandlerBigFurnace() {
        super("minefantasy2.big_furnace");
    }

    private static ArrayList<RecipePair> recipeList;
    private static List<ItemStack> itemSnapshot;

    @Override
    public codechicken.nei.recipe.TemplateRecipeHandler newInstance() {
        if (recipeList == null || recipeList.isEmpty()) {
            fillRecipeList();
        }

        return super.newInstance();
    }

    private void fillRecipeList() {
        recipeList = new ArrayList<RecipePair>();
        for (ItemStack item : snapshotItemList()) {
            if (!NEIHelper.isValidStack(item)) {
                continue;
            }
            BigFurnaceRecipes tempRecipe = BigFurnaceRecipes.getResult(item);
            if (tempRecipe != null && NEIHelper.isValidStack(tempRecipe.result)) {
                recipeList.add(new RecipePair(item, tempRecipe.result));
            }
        }
    }

    private List<ItemStack> snapshotItemList() {
        if (itemSnapshot != null) {
            return itemSnapshot;
        }

        ArrayList<ItemStack> snapshot = new ArrayList<ItemStack>();
        for (ItemStack item : ItemList.items) {
            if (item != null) {
                snapshot.add(item);
            }
        }
        itemSnapshot = snapshot;
        return snapshot;
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("nei.method.big_furnace");
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
    }

    @Override
    public void drawExtras(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getGuiTexture());
        int progress = cycleticks % 24;
        GuiDraw.drawTexturedModalRect(71, 23, 176, 0, progress + 1, 16);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        if (recipeList == null) {
            fillRecipeList();
        }
        for (RecipePair recipePair : recipeList) {
            if (recipePair != null && CustomToolHelper.areEqual(recipePair.outputStack, result)) {
                BigFurnaceRecipe cachedRecipe = new BigFurnaceRecipe(recipePair.inputStack, recipePair.outputStack);
                arecipes.add(cachedRecipe);
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        BigFurnaceRecipes recipe = BigFurnaceRecipes.getResult(ingredient);
        if (recipe != null && NEIHelper.isValidStack(recipe.result)) {
            BigFurnaceRecipe cachedRecipe = new BigFurnaceRecipe(ingredient, recipe.result);
            arecipes.add(cachedRecipe);
        }
    }

    private class RecipePair {

        private ItemStack inputStack;
        private ItemStack outputStack;

        private RecipePair(ItemStack input, ItemStack output) {
            inputStack = input;
            outputStack = output;
        }
    }

    private class BigFurnaceRecipe extends CachedRecipe {

        private PositionedStack input;
        private PositionedStack output;

        private BigFurnaceRecipe(ItemStack inputStack, ItemStack outputStack) {
            input = NEIHelper.positionedStack(inputStack, 31, 15);
            output = NEIHelper.positionedStack(outputStack, 102, 16); // Hell of a perfectionist
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
