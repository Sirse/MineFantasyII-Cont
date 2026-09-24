package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.helpers.TextureHelperMF;

/**
 * The carpenter's and kitchen bench pages: the bench table with its 4x4 grid on the left, the tool, output and bench on
 * the right. Both show {@link GridRecipe}s, native or scripted, from their entries.
 */
public abstract class RecipeHandlerBench extends MFNEIRecipeHandler {

    /**
     * Fill order for shapeless recipes across the 4x4 grid. The first nine entries keep the old ordering so existing
     * recipes look unchanged; the rest cover the fourth column and row.
     */
    private static final int[][] SHAPELESS_ORDER = new int[][] { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 }, { 0, 2 },
            { 1, 2 }, { 2, 0 }, { 2, 1 }, { 2, 2 }, { 3, 0 }, { 3, 1 }, { 3, 2 }, { 0, 3 }, { 1, 3 }, { 2, 3 },
            { 3, 3 } };

    protected RecipeHandlerBench(String handlerId) {
        super(handlerId);
    }

    /** The published recipes of the bench, in lookup order. */
    protected abstract List<GridRecipe> recipes();

    /** The benches that can make the recipe, shown below the output. */
    protected abstract List<ItemStack> stations(GridRecipe recipe);

    /** Whether heatable ingredients and hot results get a flame mark. */
    protected boolean marksHeat() {
        return false;
    }

    @Override
    public int recipiesPerPage() {
        return 1;
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
        for (GridRecipe recipe : recipes()) {
            if (NEIHelper.isValidStack(recipe.getRecipeOutput()) && matchesOutput(recipe.getRecipeOutput(), result)
                    && NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, recipe.getResearch())) {
                arecipes.add(new CachedBenchRecipe(recipe));
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        for (GridRecipe recipe : recipes()) {
            if (!NEIHelper.isValidStack(recipe.getRecipeOutput())
                    || !NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, recipe.getResearch())) {
                continue;
            }
            CachedBenchRecipe cached = new CachedBenchRecipe(recipe);
            if (cached.contains(cached.ingredients, ingredient.getItem())
                    && cached.contains(cached.ingredients, ingredient)) {
                cached.setIngredientPermutation(cached.ingredients, ingredient);
                arecipes.add(cached);
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
        ((CachedBenchRecipe) arecipes.get(recipe)).drawSlotFrames();
    }

    @Override
    public void drawExtras(int recipe) {
        if (marksHeat()) {
            ((CachedBenchRecipe) arecipes.get(recipe)).drawHeatMarks();
        }
    }

    private static void drawHeatMark(int x, int y) {
        GL11.glPushMatrix();
        GL11.glColor3f(1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager()
                .bindTexture(TextureHelperMF.getResource("textures/gui/knowledge/anvilGrid.png"));
        GuiDraw.drawTexturedModalRect(x, y, 248, 0, 8, 8);
        GL11.glPopMatrix();
    }

    class CachedBenchRecipe extends CachedRecipe {

        private final GridRecipe recipe;
        private final ArrayList<PositionedStack> ingredients = new ArrayList<PositionedStack>();
        private final ArrayList<int[]> hotCells = new ArrayList<int[]>();
        private final PositionedStack result;
        private PositionedStack toolSlot;
        private PositionedStack stationSlot;
        private boolean slotsBuilt;

        CachedBenchRecipe(GridRecipe recipe) {
            this.recipe = recipe;
            this.result = NEILayout.stack(recipe.getRecipeOutput(), NEILayout.BENCH_OUTPUT);
            List<Object> entries = recipe.getEntries();
            int shapeless = 0;
            for (int i = 0; i < entries.size(); i++) {
                Object entry = entries.get(i);
                if (entry == null) {
                    continue;
                }
                int col;
                int row;
                if (recipe.isShaped()) {
                    col = i % recipe.getWidth();
                    row = i / recipe.getWidth();
                } else {
                    if (shapeless >= SHAPELESS_ORDER.length) {
                        break;
                    }
                    col = SHAPELESS_ORDER[shapeless][0];
                    row = SHAPELESS_ORDER[shapeless][1];
                    shapeless++;
                }
                addIngredient(
                        NEIHelper.resolveEntry(entry),
                        NEILayout.BENCH_GRID_X + col * NEILayout.BENCH_CELL,
                        NEILayout.BENCH_GRID_Y + row * NEILayout.BENCH_CELL);
            }
            for (PositionedStack stack : ingredients) {
                stack.generatePermutations();
            }
        }

        private void addIngredient(List<ItemStack> stacks, int x, int y) {
            if (stacks.isEmpty()) {
                return;
            }
            MFPositionedStack stack = NEIHelper.mfPositionedStack(stacks, x, y, false);
            if (stack == null) {
                return;
            }
            stack.setMaxSize(1);
            ingredients.add(stack);
            for (ItemStack item : stacks) {
                if (Heatable.canHeatItem(item)) {
                    hotCells.add(new int[] { x, y });
                    break;
                }
            }
        }

        /** Built on first use: the tool list walks the item registry once per tool type and tier */
        private void buildStationSlots() {
            if (slotsBuilt) {
                return;
            }
            slotsBuilt = true;
            toolSlot = NEIStationSlots.slot(
                    NEIStationSlots.tools(recipe.getToolType(), recipe.getRecipeHammer()),
                    NEILayout.BENCH_TOOL.x - 2,
                    NEILayout.BENCH_TOOL.y - 2);
            stationSlot = NEIStationSlots
                    .slot(stations(recipe), NEILayout.BENCH_STATION.x - 2, NEILayout.BENCH_STATION.y - 2);
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

        private void drawHeatMarks() {
            if (result != null && recipe.outputHot()) {
                drawHeatMark(NEILayout.BENCH_OUTPUT.x, NEILayout.BENCH_OUTPUT.y);
            }
            for (int[] cell : hotCells) {
                drawHeatMark(cell[0], cell[1]);
            }
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return getCycledIngredients(cycleticks / 20, ingredients);
        }

        @Override
        public PositionedStack getResult() {
            return result;
        }
    }
}
