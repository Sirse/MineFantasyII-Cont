package minefantasy.mf2.api.crafting.carpenter;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.api.crafting.kitchen.IKitchenRecipe;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.rpg.Skill;

/**
 * @author AnonymousProductions
 */
public class ShapedCarpenterRecipes implements IKitchenRecipe, IStackedCarpenterRecipe {

    public final int recipeHammer;
    public final boolean outputHot;
    /**
     * The Block Tier needed to craft
     */
    public final int blockTier;
    public final int recipeTime;
    public final float recipeExperience;
    public final String toolType;
    public final String soundOfCraft;
    public final String research;
    public final Skill skillUsed;
    /**
     * How many horizontal slots this recipe is wide.
     */
    public int recipeWidth;
    /**
     * How many vertical slots this recipe uses.
     */
    public int recipeHeight;
    /**
     * Is a array of ItemStack that composes the recipe.
     */
    public ItemStack[] recipeItems;
    /**
     * Is the ItemStack that you get when craft the recipe.
     */
    public ItemStack recipeOutput;
    /**
     * Dirty progress added to a kitchen bench when this recipe is crafted
     */
    private float dirtyAmount;

    /**
     * True when any slot asks for more than one item. Computed once at registration and never mutated, so the lookup
     * stays free of per-bench state. Recipes without stacked ingredients skip the second offset scan entirely.
     */
    private final boolean hasStackedIngredients;

    public ShapedCarpenterRecipes(int wdth, int heit, ItemStack[] inputs, ItemStack output, String toolType, int time,
            int hammer, int anvi, float exp, boolean hot, String sound, String research, Skill skill) {
        this.research = research;
        this.outputHot = hot;
        this.recipeWidth = wdth;
        this.blockTier = anvi;
        this.recipeHeight = heit;
        this.recipeItems = inputs;
        this.recipeOutput = output;
        this.recipeTime = time;
        this.recipeHammer = hammer;
        this.recipeExperience = exp;
        this.toolType = toolType;
        this.soundOfCraft = sound;
        this.skillUsed = skill;
        boolean stacked = false;
        if (inputs != null) {
            for (ItemStack ingredient : inputs) {
                if (ingredient != null && ingredient.stackSize > 1) {
                    stacked = true;
                    break;
                }
            }
        }
        this.hasStackedIngredients = stacked;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.recipeOutput;
    }

    @Override
    public int getCraftTime() {
        return recipeTime;
    }

    @Override
    public float getExperiance() {
        return this.recipeExperience;
    }

    @Override
    public int getRecipeHammer() {
        return recipeHammer;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @Override
    public boolean matches(CarpenterCraftMatrix matrix) {
        for (int var2 = 0; var2 <= ShapelessCarpenterRecipes.globalWidth - this.recipeWidth; ++var2) {
            for (int var3 = 0; var3 <= ShapelessCarpenterRecipes.globalHeight - this.recipeHeight; ++var3) {
                if (this.checkMatch(matrix, var2, var3, true)) {
                    return true;
                }

                if (this.checkMatch(matrix, var2, var3, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if the region of a crafting inventory is match for the recipe.
     */
    private boolean checkMatch(InventoryCrafting matrix, int x, int y, boolean b) {
        for (int var5 = 0; var5 < ShapelessCarpenterRecipes.globalWidth; ++var5) {
            for (int var6 = 0; var6 < ShapelessCarpenterRecipes.globalHeight; ++var6) {
                int var7 = var5 - x;
                int var8 = var6 - y;
                ItemStack recipeItem = null;

                if (var7 >= 0 && var8 >= 0 && var7 < this.recipeWidth && var8 < this.recipeHeight) {
                    if (b) {
                        recipeItem = this.recipeItems[this.recipeWidth - var7 - 1 + var8 * this.recipeWidth];
                    } else {
                        recipeItem = this.recipeItems[var7 + var8 * this.recipeWidth];
                    }
                }

                ItemStack inputItem = matrix.getStackInRowAndColumn(var5, var6);

                if (inputItem != null || recipeItem != null) {
                    if (inputItem == null && recipeItem != null || inputItem != null && recipeItem == null) {
                        return false;
                    }

                    if (inputItem == null) {
                        return false;
                    }

                    if (recipeItem.getItem() != inputItem.getItem()) {
                        return false;
                    }
                    if (!CustomToolHelper.doesMatchForRecipe(recipeItem, inputItem)) {
                        return false;
                    }
                    if (recipeItem.getItemDamage() != OreDictionary.WILDCARD_VALUE
                            && recipeItem.getItemDamage() != inputItem.getItemDamage()) {
                        return false;
                    }
                    if (recipeItem.stackSize > 1 && inputItem.stackSize < recipeItem.stackSize) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Per-slot item amounts this recipe consumes, indexed like the crafting matrix (col + row * globalWidth). The
     * matched layout offset and mirroring are honoured, so each matrix slot maps to the correct recipe cell. Slots
     * outside the matched region require 1.
     */
    @Override
    public int[] getRequiredAmounts(CarpenterCraftMatrix matrix) {
        if (!hasStackedIngredients) {
            // Every slot needs exactly one item: the caller treats null as "all ones" and skips the scan
            return null;
        }
        int gridW = ShapelessCarpenterRecipes.globalWidth;
        int gridH = ShapelessCarpenterRecipes.globalHeight;
        int[] amounts = new int[gridW * gridH];
        java.util.Arrays.fill(amounts, 1);

        for (int offX = 0; offX <= gridW - this.recipeWidth; ++offX) {
            for (int offY = 0; offY <= gridH - this.recipeHeight; ++offY) {
                if (this.checkMatch(matrix, offX, offY, true)) {
                    fillAmounts(amounts, gridW, gridH, offX, offY, true);
                    return amounts;
                }
                if (this.checkMatch(matrix, offX, offY, false)) {
                    fillAmounts(amounts, gridW, gridH, offX, offY, false);
                    return amounts;
                }
            }
        }
        return amounts;
    }

    private void fillAmounts(int[] amounts, int gridW, int gridH, int offX, int offY, boolean mirrored) {
        for (int col = 0; col < gridW; ++col) {
            for (int row = 0; row < gridH; ++row) {
                int recipeX = col - offX;
                int recipeY = row - offY;
                if (recipeX < 0 || recipeY < 0 || recipeX >= this.recipeWidth || recipeY >= this.recipeHeight) {
                    continue;
                }
                ItemStack recipeItem = mirrored
                        ? this.recipeItems[this.recipeWidth - recipeX - 1 + recipeY * this.recipeWidth]
                        : this.recipeItems[recipeX + recipeY * this.recipeWidth];
                if (recipeItem != null) {
                    amounts[col + row * gridW] = Math.max(1, recipeItem.stackSize);
                }
            }
        }
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    @Override
    public ItemStack getCraftingResult(CarpenterCraftMatrix matrix) {
        return recipeOutput.copy();
    }

    /**
     * Returns the size of the recipe area
     */
    @Override
    public int getRecipeSize() {
        return this.recipeWidth * this.recipeHeight;
    }

    @Override
    public int getAnvil() {
        return blockTier;
    }

    @Override
    public boolean outputHot() {
        return this.outputHot;
    }

    @Override
    public String getToolType() {
        return toolType;
    }

    @Override
    public String getSound() {
        return soundOfCraft;
    }

    @Override
    public String getResearch() {
        return research;
    }

    @Override
    public Skill getSkill() {
        return skillUsed;
    }

    @Override
    public float getDirtyAmount() {
        return dirtyAmount;
    }

    public ShapedCarpenterRecipes setDirtyAmount(float amount) {
        this.dirtyAmount = amount;
        return this;
    }
}
