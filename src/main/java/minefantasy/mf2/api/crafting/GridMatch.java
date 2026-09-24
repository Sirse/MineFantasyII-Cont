package minefantasy.mf2.api.crafting;

import java.util.Arrays;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.IHotItem;
import minefantasy.mf2.api.helpers.CustomToolHelper;

/**
 * Grid matching shared by the anvil, carpenter and kitchen recipes, native and scripted alike. A recipe describes its
 * cells; this class places them on the station grid and, on a match, tells how many items each grid slot owes. Matching
 * and consuming then always agree on which cell landed where.
 */
public final class GridMatch {

    /** One recipe cell. */
    public interface Cell {

        boolean accepts(ItemStack stack);

        /** Items the cell takes from its slot, at least 1. */
        int amount();
    }

    private GridMatch() {}

    /**
     * Places a shaped pattern of {@code width * height} cells (null for an empty cell) on the grid. Every grid slot
     * outside the pattern must be empty.
     *
     * @param shift  whether the pattern may sit anywhere on the grid rather than only in its top left corner
     * @param mirror whether the pattern may also match mirrored left to right
     * @return items owed per grid slot ({@code col + row * gridWidth}, 1 for slots the pattern leaves empty), or null
     *         when it does not match
     */
    public static int[] shaped(InventoryCrafting grid, int gridWidth, int gridHeight, Cell[] pattern, int width,
            int height, boolean shift, boolean mirror) {
        if (width > gridWidth || height > gridHeight) {
            return null;
        }
        int maxX = shift ? gridWidth - width : 0;
        int maxY = shift ? gridHeight - height : 0;
        for (int x = 0; x <= maxX; x++) {
            for (int y = 0; y <= maxY; y++) {
                if (mirror) {
                    int[] amounts = place(grid, gridWidth, gridHeight, pattern, width, height, x, y, true);
                    if (amounts != null) {
                        return amounts;
                    }
                }
                int[] amounts = place(grid, gridWidth, gridHeight, pattern, width, height, x, y, false);
                if (amounts != null) {
                    return amounts;
                }
            }
        }
        return null;
    }

    private static int[] place(InventoryCrafting grid, int gridWidth, int gridHeight, Cell[] pattern, int width,
            int height, int offX, int offY, boolean mirrored) {
        int[] amounts = ones(gridWidth * gridHeight);
        for (int col = 0; col < gridWidth; col++) {
            for (int row = 0; row < gridHeight; row++) {
                int x = col - offX;
                int y = row - offY;
                Cell cell = null;
                if (x >= 0 && y >= 0 && x < width && y < height) {
                    cell = pattern[(mirrored ? width - x - 1 : x) + y * width];
                }
                ItemStack stack = grid.getStackInRowAndColumn(col, row);
                if (cell == null && stack == null) {
                    continue;
                }
                if (!fits(cell, stack)) {
                    return null;
                }
                amounts[col + row * gridWidth] = Math.max(1, cell.amount());
            }
        }
        return amounts;
    }

    /**
     * Pairs every cell with its own filled grid slot, trying other pairings when the first fit leaves a cell out, so
     * the order items were laid out in never decides the match. Every filled slot must be used.
     *
     * @return items owed per grid slot as for {@link #shaped}, or null when it does not match
     */
    public static int[] shapeless(InventoryCrafting grid, int gridWidth, int gridHeight, Cell[] cells) {
        ItemStack[] slots = new ItemStack[gridWidth * gridHeight];
        int filled = 0;
        for (int col = 0; col < gridWidth; col++) {
            for (int row = 0; row < gridHeight; row++) {
                ItemStack stack = grid.getStackInRowAndColumn(col, row);
                slots[col + row * gridWidth] = stack;
                if (stack != null) {
                    filled++;
                }
            }
        }
        if (filled != cells.length) {
            return null;
        }
        boolean[][] fitting = new boolean[cells.length][slots.length];
        for (int i = 0; i < cells.length; i++) {
            for (int s = 0; s < slots.length; s++) {
                fitting[i][s] = slots[s] != null && fits(cells[i], slots[s]);
            }
        }
        int[] owner = pairAll(fitting, slots.length);
        if (owner == null) {
            return null;
        }
        int[] amounts = ones(slots.length);
        for (int s = 0; s < owner.length; s++) {
            if (owner[s] >= 0) {
                amounts[s] = Math.max(1, cells[owner[s]].amount());
            }
        }
        return amounts;
    }

    /**
     * Pairs every row of {@code fits} with a distinct column it fits.
     *
     * @return the row index per column (-1 for an unused column), or null when some row is left without one
     */
    public static int[] pairAll(boolean[][] fits, int columns) {
        int[] owner = new int[columns];
        Arrays.fill(owner, -1);
        for (int i = 0; i < fits.length; i++) {
            if (!claim(i, fits, owner, new boolean[columns])) {
                return null;
            }
        }
        return owner;
    }

    /** Augmenting path step: gives the row a free column, moving earlier claims aside where they can go */
    private static boolean claim(int row, boolean[][] fits, int[] owner, boolean[] visited) {
        for (int c = 0; c < owner.length; c++) {
            if (!fits[row][c] || visited[c]) continue;
            visited[c] = true;
            if (owner[c] < 0 || claim(owner[c], fits, owner, visited)) {
                owner[c] = row;
                return true;
            }
        }
        return false;
    }

    private static boolean fits(Cell cell, ItemStack stack) {
        return cell != null && stack != null && cell.accepts(stack) && stack.stackSize >= Math.max(1, cell.amount());
    }

    private static int[] ones(int size) {
        int[] amounts = new int[size];
        Arrays.fill(amounts, 1);
        return amounts;
    }

    /**
     * A native recipe stack: same item, same meta unless the recipe uses the wildcard, same materials.
     *
     * @param amount items the cell takes; native shapeless recipes take one whatever the stack size
     */
    public static Cell stack(ItemStack recipe, int amount) {
        if (recipe == null) {
            return null;
        }
        return new Cell() {

            @Override
            public boolean accepts(ItemStack stack) {
                return recipe.getItem() == stack.getItem()
                        && (recipe.getItemDamage() == OreDictionary.WILDCARD_VALUE
                                || recipe.getItemDamage() == stack.getItemDamage())
                        && CustomToolHelper.doesMatchForRecipe(recipe, stack);
            }

            @Override
            public int amount() {
                return amount;
            }
        };
    }

    /**
     * An anvil cell around another: the stack must be at working heat, or not heatable at all when heating is required,
     * and a hot stack is judged by the item it carries. The slot still pays with the hot stack's own size.
     */
    public static Cell anvil(Cell inner) {
        if (inner == null) {
            return null;
        }
        return new Cell() {

            @Override
            public boolean accepts(ItemStack stack) {
                if (Heatable.requiresHeating && Heatable.canHeatItem(stack)) {
                    return false;
                }
                if (!Heatable.isWorkable(stack)) {
                    return false;
                }
                if (stack.getItem() instanceof IHotItem) {
                    ItemStack held = Heatable.getItem(stack);
                    if (held != null) {
                        held.stackSize = stack.stackSize;
                        return inner.accepts(held);
                    }
                }
                return inner.accepts(stack);
            }

            @Override
            public int amount() {
                return inner.amount();
            }
        };
    }

}
