package minefantasy.mf2.api.knowledge.client;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import cpw.mods.fml.relauncher.ReflectionHelper;

/** A vanilla crafting recipe, on the workbench's three by three grid. */
public class EntryPageRecipeBase extends EntryPageRecipe {

    private static final int CELL = 29;
    private final IRecipe[] recipes;

    public EntryPageRecipeBase(List<IRecipe> recipes) {
        this(recipes.toArray(new IRecipe[0]));
    }

    public EntryPageRecipeBase(IRecipe... recipes) {
        super("craftGrid");
        this.recipes = recipes;
    }

    @Override
    protected int variantCount() {
        return recipes.length;
    }

    /** Still in the crafting manager's list: a script that removes a recipe takes it out of there. */
    @Override
    protected boolean isPresent(int variant) {
        return CraftingManager.getInstance().getRecipeList().contains(recipes[variant]);
    }

    @Override
    protected String station() {
        return "workbench";
    }

    @Override
    protected int stationY() {
        return 175;
    }

    @Override
    protected void drawRecipe(GuiScreen parent, int posX, int posY, int mx, int my) {
        IRecipe recipe = recipes.length == 0 ? null : recipes[variant()];
        if (recipe == null) {
            return;
        }
        Object[] inputs;
        int width;
        if (recipe instanceof ShapedRecipes) {
            inputs = ((ShapedRecipes) recipe).recipeItems;
            width = ((ShapedRecipes) recipe).recipeWidth;
        } else if (recipe instanceof ShapedOreRecipe) {
            inputs = ((ShapedOreRecipe) recipe).getInput();
            width = (Integer) ReflectionHelper.getPrivateValue(ShapedOreRecipe.class, (ShapedOreRecipe) recipe, 4);
        } else if (recipe instanceof ShapelessRecipes) {
            inputs = ((ShapelessRecipes) recipe).recipeItems.toArray();
            width = 3;
        } else if (recipe instanceof ShapelessOreRecipe) {
            inputs = ((ShapelessOreRecipe) recipe).getInput().toArray();
            width = 3;
        } else {
            inputs = new Object[0];
            width = 3;
        }
        for (int i = 0; i < Math.min(9, inputs.length); i++) {
            drawItem(stackOf(inputs[i]), posX + i % width * CELL + 51, posY + i / width * CELL + 86, mx, my);
        }
        drawItem(recipe.getRecipeOutput(), posX + 80, posY + 42, mx, my);
    }

    /** An ingredient as one stack: itself, or the first of an ore dictionary name's stacks. */
    private static ItemStack stackOf(Object input) {
        if (input instanceof ItemStack) {
            return (ItemStack) input;
        }
        if (input instanceof List && !((List<?>) input).isEmpty()) {
            Object first = ((List<?>) input).get(0);
            return first instanceof ItemStack ? (ItemStack) first : null;
        }
        return null;
    }
}
