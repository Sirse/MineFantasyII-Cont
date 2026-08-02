package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.crafting.Salvage.SalvageRecipe;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.block.list.BlockListMF;

public class RecipeHandlerSalvage extends MFNEIRecipeHandler {

    public RecipeHandlerSalvage() {
        super("minefantasy2.salvage");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.salvage");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/icons.png";
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        drawSlotFrame(NEILayout.SALVAGE_INPUT);
        drawSlotFrame(NEILayout.SALVAGE_STATION);
        for (NEILayout.Slot slot : NEILayout.SALVAGE_OUTPUTS) {
            drawSlotFrame(slot);
        }
        drawArrow();
    }

    private void drawSlotFrame(NEILayout.Slot slot) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GuiDraw.changeTexture("minefantasy2:textures/gui/icons.png");
        GuiDraw.drawTexturedModalRect(slot.x - 2, slot.y - 2, 20, 0, 20, 20);
    }

    private void drawArrow() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.65F, 0.65F, 0.65F, 0.9F);
        GL11.glLineWidth(2F);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(52, 51);
        GL11.glVertex2f(68, 51);
        GL11.glVertex2f(64, 47);
        GL11.glVertex2f(68, 51);
        GL11.glVertex2f(64, 55);
        GL11.glVertex2f(68, 51);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        for (SalvageRecipe recipe : Salvage.displayList) {
            CachedSalvageRecipe cachedRecipe = createRecipe(recipe);
            if (cachedRecipe != null && cachedRecipe.hasOutput(result)) {
                arecipes.add(cachedRecipe);
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        for (SalvageRecipe recipe : Salvage.displayList) {
            if (recipe != null && NEIHelper.matchesCrafting(recipe.input, ingredient)) {
                CachedSalvageRecipe cachedRecipe = createRecipe(recipe);
                if (cachedRecipe != null) {
                    cachedRecipe.setIngredientPermutation(cachedRecipe.getIngredients(), ingredient);
                    arecipes.add(cachedRecipe);
                }
            }
        }
    }

    @Override
    public List<String> handleItemTooltip(codechicken.nei.recipe.GuiRecipe<?> gui, ItemStack stack,
            List<String> currenttip, int recipe) {
        if (recipe >= 0 && recipe < arecipes.size() && ((CachedSalvageRecipe) arecipes.get(recipe)).hasOutput(stack)) {
            currenttip.add(StatCollector.translateToLocal("nei.minefantasy2.salvage.durability_note"));
        }
        return currenttip;
    }

    private CachedSalvageRecipe createRecipe(SalvageRecipe recipe) {
        if (recipe == null || !NEIHelper.isValidStack(recipe.input) || recipe.outputs == null) {
            return null;
        }
        CachedSalvageRecipe cachedRecipe = new CachedSalvageRecipe(recipe);
        return cachedRecipe.outputs.isEmpty() ? null : cachedRecipe;
    }

    private class CachedSalvageRecipe extends CachedRecipe {

        private final PositionedStack input;
        private final PositionedStack station;
        private final ArrayList<PositionedStack> outputs = new ArrayList<PositionedStack>();

        private CachedSalvageRecipe(SalvageRecipe recipe) {
            input = NEILayout.stack(normalizeForDisplay(recipe.input), NEILayout.SALVAGE_INPUT);
            station = NEILayout.stack(new ItemStack(BlockListMF.salvage_basic), NEILayout.SALVAGE_STATION);
            for (Object output : recipe.outputs) {
                addOutput(output);
            }
        }

        private void addOutput(Object output) {
            ItemStack stack = toStack(output);
            if (!NEIHelper.isValidStack(stack) || outputs.size() >= NEILayout.SALVAGE_OUTPUTS.length) {
                return;
            }
            for (PositionedStack existing : outputs) {
                if (CustomToolHelper.areEqual(existing.items[0], stack)) {
                    existing.items[0].stackSize += stack.stackSize;
                    return;
                }
            }
            PositionedStack positionedStack = NEILayout.stack(stack, NEILayout.SALVAGE_OUTPUTS[outputs.size()]);
            if (positionedStack != null) {
                outputs.add(positionedStack);
            }
        }

        private ItemStack toStack(Object output) {
            if (output instanceof ItemStack) {
                return normalizeForDisplay((ItemStack) output);
            }
            if (output instanceof Item) {
                return new ItemStack((Item) output);
            }
            if (output instanceof Block) {
                return new ItemStack((Block) output);
            }
            return null;
        }

        private boolean hasOutput(ItemStack stack) {
            for (PositionedStack output : outputs) {
                for (ItemStack outputStack : output.items) {
                    if (CustomToolHelper.areEqual(outputStack, stack)) {
                        return true;
                    }
                }
            }
            return false;
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
        public List<PositionedStack> getOtherStacks() {
            ArrayList<PositionedStack> otherStacks = new ArrayList<PositionedStack>();
            if (station != null) {
                otherStacks.add(station);
            }
            for (int i = 1; i < outputs.size(); i++) {
                otherStacks.add(outputs.get(i));
            }
            return otherStacks;
        }

        @Override
        public PositionedStack getResult() {
            return outputs.isEmpty() ? null : outputs.get(0);
        }

        private ItemStack normalizeForDisplay(ItemStack stack) {
            ItemStack copy = NEIHelper.validCopy(stack);
            if (copy != null && copy.isItemStackDamageable()) {
                copy.setItemDamage(0);
            }
            return copy;
        }
    }
}
