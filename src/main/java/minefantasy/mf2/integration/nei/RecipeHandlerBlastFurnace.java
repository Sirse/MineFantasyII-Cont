package minefantasy.mf2.integration.nei;

import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.refine.BlastFurnaceRecipes;
import minefantasy.mf2.knowledge.KnowledgeListMF;

public class RecipeHandlerBlastFurnace extends MFNEIRecipeHandler {

    public RecipeHandlerBlastFurnace() {
        super("minefantasy2.blast_furnace");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.blastfurnace");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/blast_chamber.png";
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 0, 133, 112);
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        if (!NEIHelper.isValidStack(result)) {
            return;
        }
        if (NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, KnowledgeListMF.blastfurn)) {
            for (Entry<ItemStack, ItemStack> entry : BlastFurnaceRecipes.smelting().getSmeltingList().entrySet()) {
                if (entry != null && NEIHelper.isValidStack(entry.getKey())
                        && NEIHelper.isValidStack(entry.getValue())
                        && CustomToolHelper.areEqual(entry.getValue(), result)) {
                    CachedBlastFurnaceRecipe recipe = new CachedBlastFurnaceRecipe(entry.getKey(), result);
                    arecipes.add(recipe);
                }
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!NEIHelper.isValidStack(ingredient)) {
            return;
        }
        if (NEIHelper.canViewResearch(Minecraft.getMinecraft().thePlayer, KnowledgeListMF.blastfurn)) {
            ItemStack result = BlastFurnaceRecipes.smelting().getSmeltingResult(ingredient);
            if (NEIHelper.isValidStack(result)) {
                CachedBlastFurnaceRecipe recipe = new CachedBlastFurnaceRecipe(ingredient, result);
                arecipes.add(recipe);
            }
        }
    }

    private class CachedBlastFurnaceRecipe extends CachedRecipe {

        private PositionedStack input;
        private PositionedStack output;

        private CachedBlastFurnaceRecipe(ItemStack inputStack, ItemStack outputStack) {
            input = NEIHelper.positionedStack(inputStack, 75, 30);
            output = NEIHelper.positionedStack(outputStack, 75, 68);
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
