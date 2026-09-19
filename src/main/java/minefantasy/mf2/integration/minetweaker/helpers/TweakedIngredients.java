package minefantasy.mf2.integration.minetweaker.helpers;

import net.minecraft.item.ItemStack;

import minetweaker.api.item.IIngredient;
import minetweaker.api.minecraft.MineTweakerMC;

/**
 * Shared ingredient matching for the CraftTweaker recipe adapters.
 * <p>
 * The adapters used to walk {@link IIngredient#getItems()} and compare item and damage by hand, which ignored every NBT
 * condition the script declared and broke wildcard damage. Asking the ingredient itself keeps material, quality and
 * other NBT requirements meaningful.
 */
public final class TweakedIngredients {

    private TweakedIngredients() {}

    /**
     * True when the ingredient accepts this stack, honouring NBT conditions, wildcard damage and required amounts.
     */
    public static boolean matches(IIngredient ingredient, ItemStack stack) {
        if (ingredient == null || stack == null || stack.getItem() == null) {
            return false;
        }
        return ingredient.matches(MineTweakerMC.getIItemStack(stack));
    }

    /**
     * Reads a cell of a possibly ragged ingredient grid, treating anything outside it as empty. Scripts may register
     * patterns smaller than the bench grid, so the row and column both have to be range checked.
     */
    public static IIngredient gridCell(IIngredient[][] grid, int row, int column) {
        if (grid == null || row < 0 || row >= grid.length) {
            return null;
        }
        IIngredient[] line = grid[row];
        if (line == null || column < 0 || column >= line.length) {
            return null;
        }
        return line[column];
    }
}
