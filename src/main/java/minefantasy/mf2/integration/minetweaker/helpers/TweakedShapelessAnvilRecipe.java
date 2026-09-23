package minefantasy.mf2.integration.minetweaker.helpers;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.anvil.AnvilCraftMatrix;
import minefantasy.mf2.api.crafting.anvil.IAnvilRecipe;
import minefantasy.mf2.api.crafting.anvil.IStackedAnvilRecipe;
import minefantasy.mf2.api.crafting.anvil.ShapelessAnvilRecipes;
import minefantasy.mf2.api.rpg.Skill;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;

public class TweakedShapelessAnvilRecipe implements IAnvilRecipe, IStackedAnvilRecipe {

    private int hammer, anvil, /* craft, */
            time, width, height;
    private IItemStack result;
    private IIngredient[] ingreds;
    private Skill s;
    private String research, tool;
    private boolean hot;

    public TweakedShapelessAnvilRecipe(IIngredient[] input, IItemStack output, String tool, int time, int hammer,
            int anvil, boolean hot, String research, Skill s) {
        this.height = 4;
        this.width = 4;
        this.ingreds = input;
        this.result = output;
        this.tool = tool;
        this.hammer = hammer;
        this.anvil = anvil;
        this.hot = hot;
        this.research = research;
        this.s = s;
        this.time = time;
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
    public ItemStack getCraftingResult(AnvilCraftMatrix arg0) {
        return this.getRecipeOutput().copy();
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
        return s;
    }

    @Override
    public String getToolType() {
        return tool;
    }

    @Override
    public boolean matches(AnvilCraftMatrix inv) {
        return assign(inv, null);
    }

    @Override
    public int[] getRequiredAmounts(AnvilCraftMatrix matrix) {
        int[] amounts = new int[ShapelessAnvilRecipes.globalWidth * ShapelessAnvilRecipes.globalHeight];
        java.util.Arrays.fill(amounts, 1);
        assign(matrix, amounts);
        return amounts;
    }

    /**
     * Runs the greedy ingredient to cell assignment once. Passing an amounts array records how many items each cell
     * owes, so matching and consuming always agree on which ingredient landed where.
     */
    private boolean assign(AnvilCraftMatrix inv, int[] amounts) {
        boolean matches[] = new boolean[this.ingreds.length];
        boolean items[][] = new boolean[6][4];
        for (int a = 0; a < this.ingreds.length; a++) {
            IIngredient i = this.ingreds[a];
            boolean found = false;
            for (int x = 0; x < 6 && !found; x++) {
                for (int y = 0; y < 4 && !found; y++) {
                    ItemStack stack = inv.getStackInRowAndColumn(x, y);
                    if (stack == null || items[x][y]) continue;
                    // Ask the ingredient itself so NBT conditions and wildcard damage are honoured
                    if (TweakedIngredients.matchesAnvil(i, stack)) {
                        matches[a] = true;
                        items[x][y] = true;
                        found = true;
                        if (amounts != null) {
                            amounts[x + y * ShapelessAnvilRecipes.globalWidth] = Math.max(1, i.getAmount());
                        }
                    }
                }
            }
        }
        boolean isMatch = true;
        for (boolean b : matches) if (!b) isMatch = false;
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 4; y++) {
                if (!items[x][y] && inv.getStackInRowAndColumn(x, y) != null) isMatch = false;
            }
        }
        return isMatch;
    }

    @Override
    public boolean outputHot() {
        return hot;
    }

    @Override
    public boolean useCustomTiers() {
        // TODO Auto-generated method stub
        return false;
    }

    public IIngredient[] getIngredients() {
        return ingreds;
    }

}
