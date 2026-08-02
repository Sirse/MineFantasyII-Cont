package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.engineer.ICrossbowPart;
import minefantasy.mf2.item.gadget.ItemCrossbow;
import minefantasy.mf2.item.list.ComponentListMF;

public class RecipeHandlerCrossbowBench extends MFNEIRecipeHandler {

    private static final int WIDTH = 166;
    // Only the crafting panel; the player-inventory portion of the texture (y >= HEIGHT) is cropped out.
    private static final int HEIGHT = 115;
    // Whole panel is shifted left by this amount so the output slot clears NEI's side buttons.
    private static final int X_SHIFT = 18;

    private static final int STOCK_X = 77 - X_SHIFT;
    private static final int STOCK_Y = 74;
    private static final int MECHANISM_X = 77 - X_SHIFT;
    private static final int MECHANISM_Y = 48;
    private static final int MOD_X = 52 - X_SHIFT;
    private static final int MOD_Y = 48;
    private static final int MUZZLE_X = 102 - X_SHIFT;
    private static final int MUZZLE_Y = 30;
    private static final int OUTPUT_X = 147 - X_SHIFT;
    private static final int OUTPUT_Y = 48;

    private static final ItemStack[] STOCKS = new ItemStack[] { new ItemStack(ComponentListMF.crossbow_handle_wood),
            new ItemStack(ComponentListMF.crossbow_stock_wood), new ItemStack(ComponentListMF.crossbow_stock_iron) };

    private static final ItemStack[] MECHANISMS = new ItemStack[] { new ItemStack(ComponentListMF.cross_arms_basic),
            new ItemStack(ComponentListMF.cross_arms_light), new ItemStack(ComponentListMF.cross_arms_heavy),
            new ItemStack(ComponentListMF.cross_arms_advanced) };

    private static final ItemStack[] MODS = new ItemStack[] { null, new ItemStack(ComponentListMF.cross_ammo),
            new ItemStack(ComponentListMF.cross_scope) };

    private static final ItemStack[] MUZZLES = new ItemStack[] { null, new ItemStack(ComponentListMF.cross_bayonet) };

    public RecipeHandlerCrossbowBench() {
        super("minefantasy2.crossbow_bench");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.crossbow_bench");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/crossbowCraft.png";
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    public static int getWidth() {
        return WIDTH;
    }

    public static int getHeight() {
        return HEIGHT;
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, X_SHIFT, 0, WIDTH, HEIGHT);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        for (ItemStack stock : STOCKS) {
            ArrayList<Combo> combos = buildCombos(stock);
            ArrayList<Combo> matching = new ArrayList<Combo>();
            for (Combo combo : combos) {
                if (combo != null && NEIHelper.matchesCrafting(combo.output, result)) {
                    matching.add(combo);
                }
            }
            if (!matching.isEmpty()) {
                arecipes.add(new CachedCrossbowRecipe(stock, matching));
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        for (ItemStack stock : STOCKS) {
            ArrayList<Combo> combos = buildCombos(stock);
            ArrayList<Combo> matching = new ArrayList<Combo>();
            for (Combo combo : combos) {
                if (combo != null && combo.contains(ingredient)) {
                    matching.add(combo);
                }
            }
            if (!matching.isEmpty()) {
                arecipes.add(new CachedCrossbowRecipe(stock, matching));
            }
        }
    }

    private ArrayList<Combo> buildCombos(ItemStack stock) {
        ArrayList<Combo> combos = new ArrayList<Combo>();
        for (ItemStack mechanism : MECHANISMS) {
            for (ItemStack mod : MODS) {
                for (ItemStack muzzle : MUZZLES) {
                    ItemStack output = buildResult(stock, mechanism, mod, muzzle);
                    if (output != null) {
                        combos.add(new Combo(stock, mechanism, mod, muzzle, output));
                    }
                }
            }
        }
        return combos;
    }

    private ItemStack buildResult(ItemStack stock, ItemStack mechanism, ItemStack mod, ItemStack muzzle) {
        if (!NEIHelper.isValidStack(stock) || !NEIHelper.isValidStack(mechanism)) {
            return null;
        }
        ItemCrossbow crossbow = minefantasy.mf2.item.list.ToolListMF.crossbow_custom;
        if (crossbow == null) {
            return null;
        }
        ItemStack result = crossbow.constructCrossbow(
                (ICrossbowPart) stock.getItem(),
                (ICrossbowPart) mechanism.getItem(),
                mod != null ? (ICrossbowPart) mod.getItem() : null,
                muzzle != null ? (ICrossbowPart) muzzle.getItem() : null);
        return NEIHelper.validCopy(result);
    }

    private class Combo {

        private final ItemStack stock;
        private final ItemStack mechanism;
        private final ItemStack mod;
        private final ItemStack muzzle;
        private final ItemStack output;

        private Combo(ItemStack stock, ItemStack mechanism, ItemStack mod, ItemStack muzzle, ItemStack output) {
            this.stock = NEIHelper.validCopy(stock);
            this.mechanism = NEIHelper.validCopy(mechanism);
            this.mod = NEIHelper.validCopy(mod);
            this.muzzle = NEIHelper.validCopy(muzzle);
            this.output = NEIHelper.validCopy(output);
        }

        private boolean contains(ItemStack ingredient) {
            return matches(stock, ingredient) || matches(mechanism, ingredient)
                    || matches(mod, ingredient)
                    || matches(muzzle, ingredient);
        }

        private boolean matches(ItemStack a, ItemStack b) {
            return a != null && b != null && NEIHelper.matchesCrafting(a, b);
        }
    }

    private class CachedCrossbowRecipe extends CachedRecipe {

        private final ArrayList<Combo> combos;

        private CachedCrossbowRecipe(ItemStack stock, List<Combo> combos) {
            this.combos = new ArrayList<Combo>(combos);
        }

        private Combo current() {
            if (combos.isEmpty()) {
                return null;
            }
            return combos.get((cycleticks / 20) % combos.size());
        }

        @Override
        public List<PositionedStack> getIngredients() {
            ArrayList<PositionedStack> ingredients = new ArrayList<PositionedStack>();
            Combo combo = current();
            if (combo == null) {
                return ingredients;
            }
            addStack(ingredients, combo.stock, STOCK_X, STOCK_Y);
            addStack(ingredients, combo.mechanism, MECHANISM_X, MECHANISM_Y);
            if (combo.mod != null) {
                addStack(ingredients, combo.mod, MOD_X, MOD_Y);
            }
            if (combo.muzzle != null) {
                addStack(ingredients, combo.muzzle, MUZZLE_X, MUZZLE_Y);
            }
            return ingredients;
        }

        @Override
        public PositionedStack getResult() {
            Combo combo = current();
            return combo == null ? null : NEILayout.stack(combo.output, OUTPUT_X, OUTPUT_Y);
        }

        private void addStack(List<PositionedStack> ingredients, ItemStack stack, int x, int y) {
            PositionedStack positionedStack = NEILayout.stack(stack, x, y);
            if (positionedStack != null) {
                ingredients.add(positionedStack);
            }
        }
    }
}
