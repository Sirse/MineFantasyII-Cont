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
import minefantasy.mf2.api.material.CustomMaterial;

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

    /**
     * Shared with the handler info in NEIConfig. GTNH NEI reads that one (still capped by what fits on screen) and only
     * falls back to recipiesPerPage when a handler has no info, so both carry the same value.
     */
    public static final int RECIPES_PER_PAGE = 5;

    @Override
    public int recipiesPerPage() {
        return RECIPES_PER_PAGE;
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        drawSlotFrame(NEILayout.SALVAGE_INPUT);
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
        GL11.glVertex2f(30, 20);
        GL11.glVertex2f(46, 20);
        GL11.glVertex2f(42, 16);
        GL11.glVertex2f(46, 20);
        GL11.glVertex2f(42, 24);
        GL11.glVertex2f(46, 20);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        CustomMaterial material = CustomToolHelper.getCustomPrimaryMaterial(result);
        for (SalvageRecipe recipe : Salvage.displayList) {
            CachedSalvageRecipe cachedRecipe = createRecipe(recipe, null);
            if (cachedRecipe != null && cachedRecipe.hasOutput(result)) {
                arecipes.add(cachedRecipe);
                continue;
            }
            if (material == null || recipe == null || !NEIHelper.isValidStack(recipe.input)) {
                continue;
            }
            // Salvaged parts take the material of the item they came from, so try the input made of the wanted
            // material, as its head and then as its haft
            for (String slot : new String[] { CustomToolHelper.slot_main, CustomToolHelper.slot_haft }) {
                ItemStack input = recipe.input.copy();
                CustomMaterial.addMaterial(input, slot, material.name);
                cachedRecipe = createRecipe(recipe, input);
                if (cachedRecipe != null && cachedRecipe.hasOutput(result)) {
                    arecipes.add(cachedRecipe);
                    break;
                }
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
                // A looked-up item with materials shows the parts it would actually give back
                ItemStack input = null;
                if (CustomToolHelper.hasAnyMaterial(ingredient)) {
                    input = ingredient.copy();
                    input.stackSize = recipe.input.stackSize;
                }
                CachedSalvageRecipe cachedRecipe = createRecipe(recipe, input);
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

    /**
     * @param input the item being salvaged, when it carries materials the parts should inherit; null shows the
     *              registered recipe as is
     */
    private CachedSalvageRecipe createRecipe(SalvageRecipe recipe, ItemStack input) {
        if (recipe == null || !NEIHelper.isValidStack(recipe.input) || recipe.outputs == null) {
            return null;
        }
        CachedSalvageRecipe cachedRecipe = new CachedSalvageRecipe(recipe, input);
        return cachedRecipe.outputs.isEmpty() ? null : cachedRecipe;
    }

    private class CachedSalvageRecipe extends CachedRecipe {

        private final PositionedStack input;
        private final ArrayList<PositionedStack> outputs = new ArrayList<PositionedStack>();

        private CachedSalvageRecipe(SalvageRecipe recipe, ItemStack source) {
            ItemStack shown = source != null ? source : recipe.input;
            input = NEILayout.stack(normalizeForDisplay(shown), NEILayout.SALVAGE_INPUT);
            for (Object output : recipe.outputs) {
                addOutput(output, source);
            }
        }

        private void addOutput(Object output, ItemStack source) {
            ItemStack stack = toStack(output);
            if (stack != null && source != null) {
                // Same hand-off as Salvage.dropItemStack, which also overrides a material the part already has
                stack = CustomToolHelper.tryDeconstruct(stack.copy(), source);
            }
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
                    // A material-less output stands for "whatever the input was made of"; it should not answer a
                    // lookup for one specific material, or every custom tool would list under every metal
                    if (CustomToolHelper.hasAnyMaterial(stack) && !CustomToolHelper.hasAnyMaterial(outputStack)) {
                        continue;
                    }
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
