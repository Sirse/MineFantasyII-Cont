package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.carpenter.ICarpenterRecipe;
import minefantasy.mf2.api.crafting.carpenter.ShapedCarpenterRecipes;
import minefantasy.mf2.api.crafting.carpenter.ShapelessCarpenterRecipes;
import minefantasy.mf2.api.crafting.kitchen.CraftingManagerKitchen;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedShapedCBRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedShapelessCBRecipes;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;

public class RecipeHandlerKitchen extends MFNEIRecipeHandler {

    public int[][] stackorder = new int[][] { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 }, { 0, 2 }, { 1, 2 }, { 2, 0 },
            { 2, 1 }, { 2, 2 }, { 3, 0 }, { 3, 1 }, { 3, 2 }, { 0, 3 }, { 1, 3 }, { 2, 3 }, { 3, 3 } };

    private static final int TOOL_ICON_X = 10;
    private static final int STATION_ICON_X = 32;
    private static final int ICON_Y = 6;

    public RecipeHandlerKitchen() {
        super("minefantasy2.kitchen");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.kitchenbench");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/knowledge/carpenterGrid.png";
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        for (ICarpenterRecipe irecipe : (List<ICarpenterRecipe>) CraftingManagerKitchen.getInstance().getRecipeList()) {
            if (irecipe != null && NEIHelper.isValidStack(irecipe.getRecipeOutput())
                    && CustomToolHelper.areEqual(irecipe.getRecipeOutput(), result)
                    && NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, irecipe.getResearch())) {
                CachedKitchenRecipe recipe = handleRecipe(irecipe);

                if (recipe == null) continue;

                recipe.computeVisuals();
                arecipes.add(recipe);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        for (ICarpenterRecipe irecipe : (List<ICarpenterRecipe>) CraftingManagerKitchen.getInstance().getRecipeList()) {

            if (irecipe == null || !NEIHelper.isValidStack(irecipe.getRecipeOutput())) {
                continue;
            }
            if (!NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, irecipe.getResearch())) {
                continue;
            }

            CachedKitchenRecipe recipe = handleRecipe(irecipe);

            if (recipe == null || !recipe.contains(recipe.ingredients, ingredient.getItem())) continue;

            recipe.computeVisuals();
            if (recipe.contains(recipe.ingredients, ingredient)) {
                recipe.setIngredientPermutation(recipe.ingredients, ingredient);
                arecipes.add(recipe);
            }
        }
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 33, 166, 171);
        GL11.glDisable(GL11.GL_BLEND);
    }

    @Override
    public void drawExtras(int recipe) {
        ((CachedKitchenRecipe) arecipes.get(recipe)).drawToolIcon();
    }

    private CachedKitchenRecipe handleRecipe(ICarpenterRecipe irecipe) {
        if (irecipe instanceof ShapedCarpenterRecipes) {
            return new CachedKitchenRecipe((ShapedCarpenterRecipes) irecipe);
        } else if (irecipe instanceof ShapelessCarpenterRecipes) {
            return new CachedKitchenRecipe((ShapelessCarpenterRecipes) irecipe);
        } else if (irecipe instanceof TweakedShapedCBRecipes) {
            return new CachedKitchenRecipe((TweakedShapedCBRecipes) irecipe);
        } else if (irecipe instanceof TweakedShapelessCBRecipes) {
            return new CachedKitchenRecipe((TweakedShapelessCBRecipes) irecipe);
        }
        return null;
    }

    private static List<ItemStack> resolveIngredient(IIngredient ingredient) {
        ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
        if (ingredient == null) {
            return stacks;
        }
        for (IItemStack item : ingredient.getItems()) {
            ItemStack stack = MineTweakerMC.getItemStack(item);
            if (NEIHelper.isValidStack(stack)) {
                stacks.add(stack.copy());
            }
        }
        return stacks;
    }

    class CachedKitchenRecipe extends CachedRecipe {

        ICarpenterRecipe kitchenRecipe;
        ArrayList<PositionedStack> ingredients = new ArrayList<PositionedStack>();
        PositionedStack result;
        String toolType;
        int toolTier;

        public CachedKitchenRecipe(ShapedCarpenterRecipes recipe) {
            kitchenRecipe = recipe;
            toolType = recipe.getToolType();
            toolTier = recipe.getRecipeHammer();
            result = NEIHelper.positionedStack(recipe.getRecipeOutput(), 75, 8);
            setIngredients(recipe.recipeWidth, recipe.recipeHeight, recipe.recipeItems);
        }

        public CachedKitchenRecipe(ShapelessCarpenterRecipes recipe) {
            kitchenRecipe = recipe;
            toolType = recipe.getToolType();
            toolTier = recipe.getRecipeHammer();
            result = NEIHelper.positionedStack(recipe.getRecipeOutput(), 75, 8);
            setIngredients(recipe.recipeItems);
        }

        public CachedKitchenRecipe(TweakedShapedCBRecipes recipe) {
            kitchenRecipe = recipe;
            toolType = recipe.getToolType();
            toolTier = recipe.getRecipeHammer();
            result = NEIHelper.positionedStack(recipe.getRecipeOutput(), 75, 8);
            setIngredients(recipe.getIngredients());
        }

        public CachedKitchenRecipe(TweakedShapelessCBRecipes recipe) {
            kitchenRecipe = recipe;
            toolType = recipe.getToolType();
            toolTier = recipe.getRecipeHammer();
            result = NEIHelper.positionedStack(recipe.getRecipeOutput(), 75, 8);
            setIngredients(recipe.getIngredients());
        }

        public void setIngredients(IIngredient[][] ingreds) {
            for (int y = 0; y < ingreds.length && y < 4; y++) {
                IIngredient[] row = ingreds[y];
                if (row == null) continue;

                for (int x = 0; x < row.length && x < 4; x++) {
                    List<ItemStack> stacks = resolveIngredient(row[x]);
                    if (stacks.isEmpty()) continue;

                    MFPositionedStack stack = NEIHelper.mfPositionedStack(stacks, 41 + x * 23, 47 + y * 23, false);
                    if (stack == null) {
                        continue;
                    }
                    stack.setMaxSize(1);
                    ingredients.add(stack);
                }
            }
        }

        public void setIngredients(IIngredient[] ingreds) {
            int slot = 0;
            for (IIngredient ingredient : ingreds) {
                if (ingredient == null || slot >= stackorder.length) continue;

                List<ItemStack> stacks = resolveIngredient(ingredient);
                if (stacks.isEmpty()) continue;

                MFPositionedStack stack = NEIHelper
                        .mfPositionedStack(stacks, 41 + stackorder[slot][0] * 23, 47 + stackorder[slot][1] * 23, false);
                if (stack == null) {
                    continue;
                }
                stack.setMaxSize(1);
                ingredients.add(stack);
                slot++;
            }
        }

        public void setIngredients(int width, int height, ItemStack[] items) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (items[y * width + x] == null) continue;

                    MFPositionedStack stack = NEIHelper
                            .mfPositionedStack(items[y * width + x], 41 + x * 23, 47 + y * 23, false);
                    if (stack == null) {
                        continue;
                    }
                    stack.setMaxSize(1);
                    ingredients.add(stack);
                }
            }
        }

        public void computeVisuals() {
            for (PositionedStack p : ingredients) {
                p.generatePermutations();
            }
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 20, ingredients);
        }

        public void setIngredients(List<?> items) {
            ingredients.clear();
            for (int ingred = 0; ingred < items.size() && ingred < stackorder.length; ingred++) {
                Object item = items.get(ingred);
                if (item == null) {
                    continue;
                }
                MFPositionedStack stack = NEIHelper
                        .mfPositionedStack(item, 41 + stackorder[ingred][0] * 23, 47 + stackorder[ingred][1] * 23);
                if (stack == null) {
                    continue;
                }
                stack.setMaxSize(1);
                ingredients.add(stack);
            }
        }

        @Override
        public PositionedStack getResult() {
            return result;
        }

        private void drawToolIcon() {
            if (toolType != null) {
                drawIcon(toolType, toolTier, TOOL_ICON_X, ICON_Y);
            }
            drawIcon("kitchenbench", -1, STATION_ICON_X, ICON_Y);
        }

        private void drawIcon(String type, int tier, int x, int y) {
            GL11.glPushMatrix();
            GL11.glColor3f(1F, 1F, 1F);
            GuiDraw.changeTexture("minefantasy2:textures/gui/icons.png");
            int[] icon = minefantasy.mf2.api.helpers.GuiHelper.getToolTypeIcon(type);
            GuiDraw.drawTexturedModalRect(x, y, 20, 0, 20, 20);
            GuiDraw.drawTexturedModalRect(x, y, icon[0], icon[1] + 20, 20, 20);
            if (tier > 0) { // Tier 0 accepts anything, so only a real requirement is shown
                GuiDraw.drawString("" + tier, x + 4, y + 10, -1, true);
            }
            GL11.glPopMatrix();
        }
    }
}
