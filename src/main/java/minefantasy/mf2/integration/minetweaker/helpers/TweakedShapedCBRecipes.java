package minefantasy.mf2.integration.minetweaker.helpers;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.MineFantasyAPI;
import minefantasy.mf2.api.crafting.carpenter.CarpenterCraftMatrix;
import minefantasy.mf2.api.crafting.carpenter.IStackedCarpenterRecipe;
import minefantasy.mf2.api.crafting.carpenter.ShapelessCarpenterRecipes;
import minefantasy.mf2.api.crafting.kitchen.IKitchenRecipe;
import minefantasy.mf2.api.rpg.Skill;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;

public class TweakedShapedCBRecipes implements IKitchenRecipe, IStackedCarpenterRecipe {

    private int hammer, anvil, /* craft, */
            time, width, height;
    private float exp;
    private float dirtyAmount;
    private IItemStack result;
    private IIngredient[][] ingreds;
    private Skill s;
    private String research, tool, sound;

    public TweakedShapedCBRecipes(IIngredient[][] input, IItemStack output, String tool, int time, int hammer,
            int anvil, float exp, String sound, String research, Skill s) {
        this.height = 4;
        this.width = 6;
        this.ingreds = input;
        this.result = output;
        this.tool = tool;
        this.hammer = hammer;
        this.anvil = anvil;
        this.exp = exp;
        this.sound = sound;
        this.research = research;
        this.s = s;
        this.time = time;
        this.dirtyAmount = MineFantasyAPI.kitchenDirtyFor(time);
    }

    @Override
    public int getAnvil() {
        return anvil;
    }

    @Override
    public int getCraftTime() {
        return time;
    }

    @Override
    public ItemStack getCraftingResult(CarpenterCraftMatrix arg0) {
        return this.getRecipeOutput().copy();
    }

    @Override
    public float getExperiance() {
        return exp;
    }

    @Override
    public int getRecipeHammer() {
        return hammer;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return MineTweakerMC.getItemStack(result).copy();
    }

    @Override
    public int getRecipeSize() {
        return this.width * this.height;
    }

    @Override
    public String getResearch() {
        return research;
    }

    @Override
    public Skill getSkill() {
        return this.s;
    }

    @Override
    public String getToolType() {
        return tool;
    }

    @Override
    public boolean matches(CarpenterCraftMatrix inv) {
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                ItemStack stack = inv.getStackInRowAndColumn(x, y);
                // Scripts may declare a pattern smaller than the grid, so cells outside it count as empty
                IIngredient ingredient = TweakedIngredients.gridCell(this.ingreds, y, x);

                if (stack == null && ingredient == null) {
                    continue;
                }
                if (stack == null || ingredient == null) {
                    return false;
                }
                if (!TweakedIngredients.matches(ingredient, stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String getSound() {
        return sound;
    }

    @Override
    public boolean outputHot() {
        return false;
    }

    @Override
    public int[] getRequiredAmounts(CarpenterCraftMatrix matrix) {
        int gridW = ShapelessCarpenterRecipes.globalWidth;
        int gridH = ShapelessCarpenterRecipes.globalHeight;
        int[] amounts = new int[gridW * gridH];
        java.util.Arrays.fill(amounts, 1);
        for (int x = 0; x < gridW; x++) {
            for (int y = 0; y < gridH; y++) {
                IIngredient ingredient = TweakedIngredients.gridCell(this.ingreds, y, x);
                if (ingredient != null) {
                    amounts[x + y * gridW] = Math.max(1, ingredient.getAmount());
                }
            }
        }
        return amounts;
    }

    public IIngredient[][] getIngredients() {
        return ingreds;
    }

    @Override
    public float getDirtyAmount() {
        return dirtyAmount;
    }

    public TweakedShapedCBRecipes setDirtyAmount(float amount) {
        this.dirtyAmount = amount;
        return this;
    }
}
