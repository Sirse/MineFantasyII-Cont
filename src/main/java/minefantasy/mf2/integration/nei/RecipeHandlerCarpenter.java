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
import minefantasy.mf2.api.crafting.carpenter.CraftingManagerCarpenter;
import minefantasy.mf2.api.crafting.carpenter.ICarpenterRecipe;
import minefantasy.mf2.api.crafting.carpenter.ShapedCarpenterRecipes;
import minefantasy.mf2.api.crafting.carpenter.ShapelessCarpenterRecipes;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.TextureHelperMF;

public class RecipeHandlerCarpenter extends MFNEIRecipeHandler {

    public int[][] stackorder = new int[][] { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 }, { 0, 2 }, { 1, 2 }, { 2, 0 },
            { 2, 1 }, { 2, 2 } };

    private static final int TOOL_ICON_X = 10;
    private static final int STATION_ICON_X = 32;
    private static final int ICON_Y = 6;

    public RecipeHandlerCarpenter() {
        super("minefantasy2.carpenter");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.carpenter");
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
        for (ICarpenterRecipe irecipe : (List<ICarpenterRecipe>) CraftingManagerCarpenter.getInstance()
                .getRecipeList()) {
            if (irecipe != null && NEIHelper.isValidStack(irecipe.getRecipeOutput())
                    && CustomToolHelper.areEqual(irecipe.getRecipeOutput(), result)
                    && NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, irecipe.getResearch())) {
                CachedCarpenterRecipe recipe = handleRecipe(irecipe);

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
        for (ICarpenterRecipe irecipe : (List<ICarpenterRecipe>) CraftingManagerCarpenter.getInstance()
                .getRecipeList()) {

            if (irecipe == null || !NEIHelper.isValidStack(irecipe.getRecipeOutput())) {
                continue;
            }
            if (!NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, irecipe.getResearch())) {
                continue;
            }

            CachedCarpenterRecipe recipe = handleRecipe(irecipe);

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
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 33, 166, 171);
    }

    @Override
    public void drawExtras(int recipe) {
        CachedCarpenterRecipe cachedRecipe = (CachedCarpenterRecipe) arecipes.get(recipe);
        cachedRecipe.drawHotOverlays();
        cachedRecipe.drawToolIcon();
    }

    private CachedCarpenterRecipe handleRecipe(ICarpenterRecipe irecipe) {
        if (irecipe instanceof ShapedCarpenterRecipes) {
            return new CachedCarpenterRecipe((ShapedCarpenterRecipes) irecipe);
        } else if (irecipe instanceof ShapelessCarpenterRecipes) {
            return new CachedCarpenterRecipe((ShapelessCarpenterRecipes) irecipe);
        }
        return null;
    }

    class CachedCarpenterRecipe extends CachedRecipe {

        ICarpenterRecipe carpRecipe;
        ArrayList<PositionedStack> ingredients = new ArrayList<PositionedStack>();
        PositionedStack result;
        ArrayList<int[]> hotSlots = new ArrayList<int[]>();
        String toolType;
        int toolTier;
        int benchTier;

        public CachedCarpenterRecipe(ShapedCarpenterRecipes recipe) {
            carpRecipe = recipe;
            toolType = recipe.getToolType();
            toolTier = recipe.getRecipeHammer();
            benchTier = recipe.getAnvil();
            result = NEIHelper.positionedStack(recipe.getRecipeOutput(), 75, 8);
            setIngredients(recipe.recipeWidth, recipe.recipeHeight, recipe.recipeItems);
        }

        @SuppressWarnings("unchecked")
        public CachedCarpenterRecipe(ShapelessCarpenterRecipes recipe) {
            carpRecipe = recipe;
            toolType = recipe.getToolType();
            toolTier = recipe.getRecipeHammer();
            benchTier = recipe.getAnvil();
            result = NEIHelper.positionedStack(recipe.getRecipeOutput(), 75, 8);
            setIngredients(recipe.recipeItems);
        }

        public void setIngredients(int width, int height, Object[] items) {
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
                    if (minefantasy.mf2.api.heating.Heatable.canHeatItem((ItemStack) items[y * width + x])) {
                        hotSlots.add(new int[] { 41 + x * 23, 47 + y * 23 });
                    }
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
                if (minefantasy.mf2.api.heating.Heatable.canHeatItem((ItemStack) item)) {
                    hotSlots.add(new int[] { 41 + stackorder[ingred][0] * 23, 47 + stackorder[ingred][1] * 23 });
                }
            }
        }

        @Override
        public PositionedStack getResult() {
            return result;
        }

        private void drawHotOverlays() {
            if (result != null && carpRecipe.outputHot()) {
                drawHeatOverlay(75, 8);
            }
            for (int[] hotSlot : hotSlots) {
                drawHeatOverlay(hotSlot[0], hotSlot[1]);
            }
        }

        private void drawToolIcon() {
            // On the output row (output at y=8), in the free space left of the grid.
            if (toolType != null) {
                drawIcon(toolType, toolTier, TOOL_ICON_X, ICON_Y);
            }
            drawIcon("carpenter", benchTier, STATION_ICON_X, ICON_Y);
        }

        private void drawIcon(String type, int tier, int x, int y) {
            GL11.glPushMatrix();
            GL11.glColor3f(1F, 1F, 1F);
            GuiDraw.changeTexture("minefantasy2:textures/gui/icons.png");
            int[] icon = minefantasy.mf2.api.helpers.GuiHelper.getToolTypeIcon(type);
            GuiDraw.drawTexturedModalRect(x, y, 20, 0, 20, 20);
            GuiDraw.drawTexturedModalRect(x, y, icon[0], icon[1] + 20, 20, 20);
            if (tier > -1) {
                GuiDraw.drawString("" + tier, x + 4, y + 10, -1, true);
            }
            GL11.glPopMatrix();
        }

        private void drawHeatOverlay(int x, int y) {
            GL11.glPushMatrix();
            GL11.glColor3f(1F, 1F, 1F);
            Minecraft.getMinecraft().getTextureManager()
                    .bindTexture(TextureHelperMF.getResource("textures/gui/knowledge/anvilGrid.png"));
            GuiDraw.drawTexturedModalRect(x, y, 248, 0, 8, 8);
            GL11.glPopMatrix();
        }
    }
}
