package minefantasy.mf2.recipe;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import minefantasy.mf2.item.list.ToolListMF;

public class RecipeSyringe implements IRecipe {

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @Override
    public boolean matches(InventoryCrafting matrix, World world) {
        ItemStack syringe = null;
        ItemStack filler = null;
        int itemCount = 0;

        for (int i = 0; i < matrix.getSizeInventory(); ++i) {
            ItemStack itemstack1 = matrix.getStackInSlot(i);

            if (itemstack1 != null) {
                itemCount++;
                if (itemstack1.getItem() == ToolListMF.syringe_empty) {
                    if (syringe != null) {
                        return false;
                    }
                    syringe = itemstack1;
                } else if (itemstack1.getItem() instanceof ItemPotion) {
                    ItemPotion potion = (ItemPotion) itemstack1.getItem();

                    if (potion.isSplash(itemstack1.getItemDamage())) {
                        return false;
                    }
                    if (filler != null) {
                        return false;
                    }
                    filler = itemstack1;
                } else {
                    return false;
                }
            }
        }

        return itemCount == 2 && syringe != null && filler != null;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    @Override
    public ItemStack getCraftingResult(InventoryCrafting matrix) {
        ItemStack syringe = null;
        ItemStack filler = null;
        int itemCount = 0;

        for (int i = 0; i < matrix.getSizeInventory(); ++i) {
            ItemStack itemstack1 = matrix.getStackInSlot(i);

            if (itemstack1 != null) {
                itemCount++;
                if (itemstack1.getItem() == ToolListMF.syringe_empty) {
                    if (syringe != null) {
                        return null;
                    }
                    syringe = itemstack1;
                } else if (itemstack1.getItem() instanceof ItemPotion) {
                    ItemPotion potion = (ItemPotion) itemstack1.getItem();

                    if (potion.isSplash(itemstack1.getItemDamage())) {
                        return null;
                    }

                    if (filler != null) {
                        return null;
                    }
                    filler = itemstack1;
                } else {
                    return null;
                }
            }
        }
        if (itemCount == 2 && syringe != null && filler != null) {
            return new ItemStack(ToolListMF.syringe, 1, filler.getItemDamage());
        }
        return null;
    }

    /**
     * Returns the size of the recipe area
     */
    @Override
    public int getRecipeSize() {
        return 10;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return null;
    }
}
