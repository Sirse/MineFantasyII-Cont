package minefantasy.mf2.integration.minetweaker.helpers;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.GridMatch;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.IHotItem;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
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
     * Anvil matching: a hot stack is judged by the item it carries, as the native anvil recipes do, so script NBT
     * conditions such as materials apply to the heated piece. A script naming the hot item itself still matches, and a
     * piece that has cooled past working heat does not.
     */
    public static boolean matchesAnvil(IIngredient ingredient, ItemStack stack) {
        if (stack != null && stack.getItem() instanceof IHotItem) {
            if (!Heatable.isWorkable(stack)) {
                return false;
            }
            ItemStack held = Heatable.getItem(stack);
            if (held != null) {
                // The carried count is frozen at heating time; the real amount is the hot stack's own size
                held.stackSize = stack.stackSize;
                if (matches(ingredient, held)) {
                    return true;
                }
            }
        }
        return matches(ingredient, stack);
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

    /** A grid cell for a script ingredient; anvil cells judge a hot stack by the item it carries. */
    public static GridMatch.Cell cell(IIngredient ingredient, boolean anvil) {
        if (ingredient == null) {
            return null;
        }
        return new GridMatch.Cell() {

            @Override
            public boolean accepts(ItemStack stack) {
                return anvil ? matchesAnvil(ingredient, stack) : matches(ingredient, stack);
            }

            @Override
            public int amount() {
                return ingredient.getAmount();
            }
        };
    }

    /**
     * A script's shaped recipe: the pattern sits in the grid's top left corner and is never mirrored, as scripts have
     * always behaved. Empty cells are null.
     */
    public static GridRecipe.Builder shaped(GridRecipe.Grid grid, IIngredient[][] pattern, IItemStack output) {
        int width = gridWidth(pattern);
        int height = pattern.length;
        Object[] entries = new Object[width * height];
        GridMatch.Cell[] cells = new GridMatch.Cell[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                IIngredient ingredient = gridCell(pattern, y, x);
                entries[x + y * width] = ingredient;
                cells[x + y * width] = cell(ingredient, grid.heat);
            }
        }
        return GridRecipe.shaped(grid, width, height, entries, cells, MineTweakerMC.getItemStack(output)).anchored();
    }

    /** A script's shapeless recipe. */
    public static GridRecipe.Builder shapeless(GridRecipe.Grid grid, IIngredient[] ingredients, IItemStack output) {
        GridMatch.Cell[] cells = new GridMatch.Cell[ingredients.length];
        for (int i = 0; i < ingredients.length; i++) {
            cells[i] = cell(ingredients[i], grid.heat);
        }
        return GridRecipe.shapeless(grid, ingredients, cells, MineTweakerMC.getItemStack(output));
    }

    /**
     * Whether the recipe could use the given ingredient: one of its stacks matches it, or one of its script ingredients
     * shares an item with it. For {@code removeByOutput} filters.
     */
    public static boolean usesIngredient(GridRecipe recipe, IIngredient input) {
        for (Object entry : recipe.getEntries()) {
            if (entry instanceof ItemStack) {
                if (input.matches(MineTweakerMC.getIItemStack((ItemStack) entry))) {
                    return true;
                }
            } else if (entry instanceof IIngredient) {
                for (IItemStack item : ((IIngredient) entry).getItems()) {
                    if (input.matches(item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** The widest row of a possibly ragged pattern. */
    public static int gridWidth(IIngredient[][] grid) {
        int width = 0;
        for (IIngredient[] row : grid) {
            if (row != null) width = Math.max(width, row.length);
        }
        return width;
    }

    /**
     * False, with a script error, when a shaped pattern reaches past the station grid: the cells beyond it would never
     * be checked, so the recipe would craft without them.
     */
    public static boolean fitsGrid(IIngredient[][] grid, int width, int height, String station) {
        boolean fits = grid != null && grid.length <= height;
        if (fits) {
            for (IIngredient[] row : grid) {
                if (row != null && row.length > width) fits = false;
            }
        }
        if (!fits) {
            MineTweakerAPI.logError(
                    "Skipping " + station + " recipe: the pattern must fit a " + width + "x" + height + " grid");
        }
        return fits;
    }
}
