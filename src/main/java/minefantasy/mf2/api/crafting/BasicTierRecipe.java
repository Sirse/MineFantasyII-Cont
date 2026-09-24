package minefantasy.mf2.api.crafting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import minefantasy.mf2.api.helpers.CustomToolHelper;

public class BasicTierRecipe extends ShapedRecipes {

    public BasicTierRecipe(int width, int height, ItemStack[] input, ItemStack output) {
        super(width, height, input, output);
    }

    public static BasicTierRecipe add(ItemStack result, Object... input) {
        RecipePattern pattern = RecipePattern.shaped(input);
        BasicTierRecipe recipe = new BasicTierRecipe(pattern.width, pattern.height, pattern.cells, result);
        GameRegistry.addRecipe(recipe);
        return recipe;
    }

    @Override
    public boolean matches(InventoryCrafting matrix, World world) {
        for (int i = 0; i <= 3 - this.recipeWidth; ++i) {
            for (int j = 0; j <= 3 - this.recipeHeight; ++j) {
                if (this.checkTierMatch(matrix, i, j, true)) {
                    return true;
                }

                if (this.checkTierMatch(matrix, i, j, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if the region of a crafting inventory is match for the recipe.
     */
    private boolean checkTierMatch(InventoryCrafting matrix, int x, int y, boolean mirror) {
        String wood = null;
        String metal = null;
        for (int k = 0; k < 3; ++k) {
            for (int l = 0; l < 3; ++l) {
                int i1 = k - x;
                int j1 = l - y;
                ItemStack itemstack = null;

                if (i1 >= 0 && j1 >= 0 && i1 < this.recipeWidth && j1 < this.recipeHeight) {
                    if (mirror) {
                        itemstack = this.recipeItems[this.recipeWidth - i1 - 1 + j1 * this.recipeWidth];
                    } else {
                        itemstack = this.recipeItems[i1 + j1 * this.recipeWidth];
                    }
                }

                ItemStack itemstack1 = matrix.getStackInRowAndColumn(k, l);

                if (itemstack1 != null || itemstack != null) {
                    // String recipe_wood = CustomToolHelper.getComponentMaterial(itemstack,
                    // "wood");
                    String recipe_metal = CustomToolHelper.getComponentMaterial(itemstack, "metal");

                    // String component_wood = CustomToolHelper.getComponentMaterial(itemstack1,
                    // "wood");
                    String component_metal = CustomToolHelper.getComponentMaterial(itemstack1, "metal");

                    if (recipe_metal != null) {
                        if (component_metal == null) return false;
                        if (!component_metal.equalsIgnoreCase(recipe_metal)) return false;
                    }

                    if (itemstack1 == null && itemstack != null || itemstack1 != null && itemstack == null) {
                        return false;
                    }

                    if (itemstack.getItem() != itemstack1.getItem()) {
                        return false;
                    }

                    if (itemstack.getItemDamage() != 32767 && itemstack.getItemDamage() != itemstack1.getItemDamage()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
