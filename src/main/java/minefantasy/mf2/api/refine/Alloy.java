package minefantasy.mf2.api.refine;

import java.util.*;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.RecipeChecks;

/**
 * A crucible alloy: ingredients in any arrangement melting into the output, in a crucible of at least {@link #level}.
 * Registered through {@link minefantasy.mf2.api.MineFantasyAPI#alloyRecipe}, which also adds its ratio copies.
 */
public class Alloy implements RecipeChecks.Validated {

    @Override
    public void validate() {
        RecipeChecks.output("output", recipeOutput);
        RecipeChecks.notNegative("crucible level", level);
        RecipeChecks.grid(0, 0, recipeItems.size(), 3, 3);
        for (int i = 0; i < recipeItems.size(); i++) {
            if (recipeItems.get(i) instanceof ItemStack) {
                RecipeChecks.ingredient("ingredient " + (i + 1), (ItemStack) recipeItems.get(i));
            }
        }
    }

    /** Stacks, copied; script alloys keep their ingredients, which do not change. */
    private final List<Object> recipeItems;
    private final ItemStack recipeOutput;
    public final int level;
    private Map props = new HashMap();

    public Alloy(ItemStack output, int requiredLevel, List items) {
        recipeItems = entries(items);
        recipeOutput = output == null ? null : output.copy();
        level = requiredLevel;
    }

    public ItemStack getRecipeOutput() {
        // A copy: the recipe is shared by every bench and must not change with a caller's stack
        return this.recipeOutput == null ? null : this.recipeOutput.copy();
    }

    public Alloy addProperty(String id, Object prop) {
        props.put(id, prop);
        return this;
    }

    public Object getProperty(String id) {
        return props.get(id);
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(ItemStack[] inventory) {
        ArrayList checkRecipe = new ArrayList(this.recipeItems);

        for (ItemStack itemstack : inventory) {
            if (itemstack != null) {
                boolean matches = false;
                Iterator iterator = checkRecipe.iterator();

                while (iterator.hasNext()) {
                    ItemStack checkItem = (ItemStack) iterator.next();

                    if (itemstack.isItemEqual(checkItem) && areMaterialsEqual(itemstack, checkItem)
                            && (checkItem.getItemDamage() == OreDictionary.WILDCARD_VALUE
                                    || itemstack.getItemDamage() == checkItem.getItemDamage())) {
                        matches = true;
                        checkRecipe.remove(checkItem);
                        break;
                    }
                    if (areBothCarbon(itemstack, checkItem)) {
                        matches = true;
                        checkRecipe.remove(checkItem);
                        break;
                    }
                }

                if (!matches) {
                    return false;
                }
            }
        }

        return checkRecipe.isEmpty();
    }

    /**
     * Items to take from each grid slot, or null when every filled slot gives up exactly one. Native alloys list one
     * entry per slot, so they always need one; script recipes can demand a count from a single slot.
     */
    public int[] getRequiredAmounts(ItemStack[] inventory) {
        return null;
    }

    private boolean areMaterialsEqual(ItemStack itemstack, ItemStack checkItem) {
        CustomMaterial material1 = CustomToolHelper.getCustomPrimaryMaterial(itemstack);
        CustomMaterial material2 = CustomToolHelper.getCustomPrimaryMaterial(checkItem);
        if (material1 == null && material2 == null) {
            return true;
        }
        if (material1 != null && material2 != null) {
            return material1 == material2;
        }
        return false;
    }

    public boolean areBothCarbon(ItemStack item1, ItemStack item2) {
        return MineFantasyFuels.isCarbon(item1) && MineFantasyFuels.isCarbon(item2);
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return this.recipeOutput.copy();
    }

    /**
     * Returns the size of the recipe area
     */
    public int getRecipeSize() {
        return this.recipeItems.size();
    }

    /**
     * Gets the level of the Alloy, requiring a more powerful smelter
     *
     * @return the minimal level required to make (crucible is 0, alloy forge = 1, etc)
     */
    public int getLevel() {
        return level;
    }

    /** Copies of the ingredients: stacks, or a script alloy's ingredients. */
    public List<Object> getIngredients() {
        return entries(recipeItems);
    }

    private static List<Object> entries(List<?> items) {
        List<Object> copy = new ArrayList<>();
        for (Object item : items) {
            copy.add(item instanceof ItemStack ? ((ItemStack) item).copy() : item);
        }
        return Collections.unmodifiableList(copy);
    }
}
