package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.TileEntityBombBench;
import minefantasy.mf2.item.gadget.ItemBomb;
import minefantasy.mf2.item.gadget.ItemExplodingArrow;
import minefantasy.mf2.item.gadget.ItemExplodingBolt;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.ToolListMF;

public class RecipeHandlerBombBench extends MFNEIRecipeHandler {

    private static final int WIDTH = 166;
    // Only the crafting panel; the player-inventory portion of the texture (y >= HEIGHT) is cropped out.
    private static final int HEIGHT = 115;
    // Whole panel is shifted left by this amount so the output slot clears NEI's side buttons.
    private static final int X_SHIFT = 18;

    private static final ItemStack[] CASES = new ItemStack[] { new ItemStack(ComponentListMF.bomb_casing),
            new ItemStack(ComponentListMF.bomb_casing_iron), new ItemStack(ComponentListMF.bomb_casing_obsidian),
            new ItemStack(ComponentListMF.bomb_casing_crystal), new ItemStack(ComponentListMF.mine_casing),
            new ItemStack(ComponentListMF.mine_casing_iron), new ItemStack(ComponentListMF.mine_casing_obsidian),
            new ItemStack(ComponentListMF.mine_casing_crystal), new ItemStack(ComponentListMF.bomb_casing_arrow),
            new ItemStack(ComponentListMF.bomb_casing_bolt) };

    private static final ItemStack[] POWDERS = new ItemStack[] { new ItemStack(ComponentListMF.blackpowder),
            new ItemStack(ComponentListMF.blackpowder_advanced) };

    private static final ItemStack[] FILLINGS = new ItemStack[] { null, new ItemStack(ComponentListMF.shrapnel),
            new ItemStack(ComponentListMF.magma_cream_refined) };

    private static final ItemStack[] FUSES = new ItemStack[] { new ItemStack(ComponentListMF.bomb_fuse),
            new ItemStack(ComponentListMF.bomb_fuse_long) };

    public RecipeHandlerBombBench() {
        super("minefantasy2.bomb_bench");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.bomb_bench");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/bombCraft.png";
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
        for (ItemStack caseStack : CASES) {
            ArrayList<Combo> combos = buildCombosForCase(caseStack);
            ArrayList<Combo> matching = new ArrayList<Combo>();
            for (Combo combo : combos) {
                if (combo != null && matchesBombOutput(combo.output, result)) {
                    matching.add(combo);
                }
            }
            if (!matching.isEmpty()) {
                arecipes.add(new CachedBombRecipe(matching));
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        String ingredientType = TileEntityBombBench.getComponentType(ingredient);
        for (ItemStack caseStack : CASES) {
            ArrayList<Combo> combos = buildCombosForCase(caseStack);
            ArrayList<Combo> matching = new ArrayList<Combo>();
            if (ingredientType != null
                    && ("bombcase".equalsIgnoreCase(ingredientType) || "minecase".equalsIgnoreCase(ingredientType)
                            || "arrow".equalsIgnoreCase(ingredientType)
                            || "bolt".equalsIgnoreCase(ingredientType))) {
                if (NEIHelper.matchesCrafting(caseStack, ingredient)) {
                    arecipes.add(new CachedBombRecipe(combos));
                }
                continue;
            }
            for (Combo combo : combos) {
                if (combo != null && combo.contains(ingredient)) {
                    matching.add(combo);
                }
            }
            if (!matching.isEmpty()) {
                arecipes.add(new CachedBombRecipe(matching));
            }
        }
    }

    private boolean matchesBombOutput(ItemStack recipeOutput, ItemStack requested) {
        if (!NEIHelper.matchesCrafting(recipeOutput, requested)) {
            return false;
        }
        return ItemBomb.getCasing(recipeOutput) == ItemBomb.getCasing(requested)
                && ItemBomb.getPowder(recipeOutput) == ItemBomb.getPowder(requested)
                && ItemBomb.getFilling(recipeOutput) == ItemBomb.getFilling(requested)
                && ItemBomb.getFuse(recipeOutput) == ItemBomb.getFuse(requested);
    }

    private ArrayList<Combo> buildCombosForCase(ItemStack caseStack) {
        ArrayList<Combo> combos = new ArrayList<Combo>();
        String type = TileEntityBombBench.getComponentType(caseStack);
        if (type == null) {
            return combos;
        }
        type = type.toLowerCase();

        if ("arrow".equals(type) || "bolt".equals(type)) {
            for (ItemStack powder : POWDERS) {
                for (ItemStack filling : getOptionalFillings()) {
                    ItemStack output = buildResult(caseStack, powder, filling, null);
                    if (output != null) {
                        combos.add(new Combo(caseStack, powder, filling, null, output));
                    }
                }
            }
            return combos;
        }

        for (ItemStack powder : POWDERS) {
            for (ItemStack filling : getOptionalFillings()) {
                for (ItemStack fuse : FUSES) {
                    ItemStack output = buildResult(caseStack, powder, filling, fuse);
                    if (output != null) {
                        combos.add(new Combo(caseStack, powder, filling, fuse, output));
                    }
                }
            }
        }
        return combos;
    }

    private List<ItemStack> getOptionalFillings() {
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        for (ItemStack filling : FILLINGS) {
            list.add(filling);
        }
        return list;
    }

    private ItemStack buildResult(ItemStack caseStack, ItemStack powder, ItemStack filling, ItemStack fuse) {
        if (!NEIHelper.isValidStack(caseStack) || !NEIHelper.isValidStack(powder)) {
            return null;
        }

        String type = TileEntityBombBench.getComponentType(caseStack);
        if (type == null) {
            return null;
        }
        type = type.toLowerCase();

        byte powderTier = TileEntityBombBench.getComponentTier(powder);
        byte fillingTier = filling == null ? 0 : TileEntityBombBench.getComponentTier(filling);

        Item design = getDesignCrafted(type);
        if (design == null) {
            return null;
        }

        if ("arrow".equals(type) || "bolt".equals(type)) {
            if (!NEIHelper.isValidStack(powder)) {
                return null;
            }
            if ("arrow".equals(type)) {
                return ItemExplodingArrow.createBombArrow(design, powderTier, fillingTier);
            }
            return ItemExplodingBolt.createBombArrow(powderTier, fillingTier);
        }

        if (fuse == null) {
            return null;
        }
        byte caseTier = TileEntityBombBench.getComponentTier(caseStack);
        byte fuseTier = TileEntityBombBench.getComponentTier(fuse);
        return ItemBomb.createExplosive(design, caseTier, fillingTier, fuseTier, powderTier, 1, false);
    }

    private Item getDesignCrafted(String type) {
        if ("bombcase".equalsIgnoreCase(type)) {
            return ToolListMF.bomb_custom;
        }
        if ("minecase".equalsIgnoreCase(type)) {
            return ToolListMF.mine_custom;
        }
        if ("arrow".equalsIgnoreCase(type)) {
            return ToolListMF.exploding_arrow;
        }
        if ("bolt".equalsIgnoreCase(type)) {
            return ToolListMF.exploding_bolt;
        }
        return null;
    }

    private static class Combo {

        private final ItemStack caseStack;
        private final ItemStack powder;
        private final ItemStack filling;
        private final ItemStack fuse;
        private final ItemStack output;

        private Combo(ItemStack caseStack, ItemStack powder, ItemStack filling, ItemStack fuse, ItemStack output) {
            this.caseStack = NEIHelper.validCopy(caseStack);
            this.powder = NEIHelper.validCopy(powder);
            this.filling = NEIHelper.validCopy(filling);
            this.fuse = NEIHelper.validCopy(fuse);
            this.output = NEIHelper.validCopy(output);
        }

        private boolean contains(ItemStack ingredient) {
            return matches(caseStack, ingredient) || matches(powder, ingredient)
                    || matches(filling, ingredient)
                    || matches(fuse, ingredient);
        }

        private boolean matches(ItemStack a, ItemStack b) {
            return a != null && b != null && NEIHelper.matchesCrafting(a, b);
        }
    }

    private class CachedBombRecipe extends CachedRecipe {

        private final PositionedStack pressCatalyst;
        private final ArrayList<Combo> combos;

        private CachedBombRecipe(List<Combo> combos) {
            this.combos = new ArrayList<Combo>(combos);
            this.pressCatalyst = NEILayout.stack(new ItemStack(BlockListMF.bombPress), 110, 20);
        }

        private int index() {
            if (combos.isEmpty()) {
                return 0;
            }
            return (cycleticks / 20) % combos.size();
        }

        private Combo current() {
            if (combos.isEmpty()) {
                return null;
            }
            return combos.get(index());
        }

        @Override
        public List<PositionedStack> getIngredients() {
            ArrayList<PositionedStack> ingredients = new ArrayList<PositionedStack>();
            Combo combo = current();
            if (combo == null) {
                return ingredients;
            }
            addStack(ingredients, combo.caseStack, NEILayout.BOMB_BENCH_CASE);
            addStack(ingredients, combo.powder, NEILayout.BOMB_BENCH_POWDER);
            if (combo.filling != null) {
                addStack(ingredients, combo.filling, NEILayout.BOMB_BENCH_FILLING);
            }
            if (combo.fuse != null) {
                addStack(ingredients, combo.fuse, NEILayout.BOMB_BENCH_FUSE);
            }
            return ingredients;
        }

        @Override
        public PositionedStack getResult() {
            Combo combo = current();
            return combo == null ? null : NEILayout.stack(combo.output, NEILayout.BOMB_BENCH_OUTPUT);
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            ArrayList<PositionedStack> otherStacks = new ArrayList<PositionedStack>();
            if (pressCatalyst != null) {
                otherStacks.add(pressCatalyst);
            }
            return otherStacks;
        }

        private void addStack(List<PositionedStack> ingredients, ItemStack stack, NEILayout.Slot slot) {
            PositionedStack positionedStack = NEILayout.stack(stack, slot);
            if (positionedStack != null) {
                ingredients.add(positionedStack);
            }
        }
    }
}
