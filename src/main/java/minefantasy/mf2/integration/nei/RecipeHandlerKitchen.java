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
import minefantasy.mf2.block.list.BlockListMF;
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
        return "minefantasy2:textures/gui/kitchen.png";
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @SuppressWarnings("unchecked")
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
        for (ICarpenterRecipe irecipe : (List<ICarpenterRecipe>) CraftingManagerKitchen.getInstance().getRecipeList()) {
            if (irecipe != null && NEIHelper.isValidStack(irecipe.getRecipeOutput())
                    && matchesOutput(irecipe.getRecipeOutput(), result)
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
        NEILayout.drawBenchBackground();
        GL11.glDisable(GL11.GL_BLEND);
        ((CachedKitchenRecipe) arecipes.get(recipe)).drawSlotFrames();
    }

    @Override
    public void drawExtras(int recipe) {}

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

                    MFPositionedStack stack = NEIHelper.mfPositionedStack(
                            stacks,
                            NEILayout.BENCH_GRID_X + x * NEILayout.BENCH_CELL,
                            NEILayout.BENCH_GRID_Y + y * NEILayout.BENCH_CELL,
                            false);
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

                MFPositionedStack stack = NEIHelper.mfPositionedStack(
                        stacks,
                        NEILayout.BENCH_GRID_X + stackorder[slot][0] * NEILayout.BENCH_CELL,
                        NEILayout.BENCH_GRID_Y + stackorder[slot][1] * NEILayout.BENCH_CELL,
                        false);
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

                    MFPositionedStack stack = NEIHelper.mfPositionedStack(
                            items[y * width + x],
                            NEILayout.BENCH_GRID_X + x * NEILayout.BENCH_CELL,
                            NEILayout.BENCH_GRID_Y + y * NEILayout.BENCH_CELL,
                            false);
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

        private PositionedStack toolSlot;
        private PositionedStack stationSlot;
        private boolean slotsBuilt;

        /** Built on first use: the tool list walks the item registry once per tool type and tier */
        private void buildStationSlots() {
            if (slotsBuilt) {
                return;
            }
            slotsBuilt = true;
            toolSlot = NEIStationSlots.slot(NEIStationSlots.tools(toolType, toolTier), TOOL_ICON_X, ICON_Y);
            stationSlot = NEIStationSlots
                    .slot(NEIStationSlots.single(BlockListMF.kitchenBench), STATION_ICON_X, ICON_Y);
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            buildStationSlots();
            ArrayList<PositionedStack> stacks = new ArrayList<PositionedStack>();
            for (PositionedStack slot : new PositionedStack[] { toolSlot, stationSlot }) {
                if (slot != null) {
                    NEIStationSlots.cycle(slot, cycleticks);
                    stacks.add(slot);
                }
            }
            return stacks;
        }

        private void drawSlotFrames() {
            buildStationSlots();
            NEIStationSlots.drawFrame(toolSlot);
            NEIStationSlots.drawFrame(stationSlot);
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
                MFPositionedStack stack = NEIHelper.mfPositionedStack(
                        item,
                        NEILayout.BENCH_GRID_X + stackorder[ingred][0] * NEILayout.BENCH_CELL,
                        NEILayout.BENCH_GRID_Y + stackorder[ingred][1] * NEILayout.BENCH_CELL);
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

    }
}
