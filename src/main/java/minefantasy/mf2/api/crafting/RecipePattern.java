package minefantasy.mf2.api.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * The vanilla style recipe arguments the native registration methods take: pattern rows (as strings or one string
 * array) followed by character and item pairs, or a plain list of items for a shapeless recipe.
 */
public final class RecipePattern {

    public final int width;
    public final int height;
    /** Row by row; null for an empty cell. */
    public final ItemStack[] cells;

    private RecipePattern(int width, int height, ItemStack[] cells) {
        this.width = width;
        this.height = height;
        this.cells = cells;
    }

    /**
     * Reads a shaped pattern. Rows may differ in length (short rows end in empty cells) and characters without a key
     * are empty; a key for anything but an item, block or stack is refused. An item or block key takes any metadata.
     */
    public static RecipePattern shaped(Object... input) {
        List<String> rows = new ArrayList<>();
        int next = 0;
        if (input.length > 0 && input[0] instanceof String[]) {
            for (String row : (String[]) input[0]) {
                rows.add(row);
            }
            next = 1;
        } else {
            while (next < input.length && input[next] instanceof String) {
                rows.add((String) input[next++]);
            }
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("A shaped recipe needs pattern rows");
        }
        Map<Character, ItemStack> keys = new HashMap<>();
        for (; next < input.length; next += 2) {
            if (!(input[next] instanceof Character) || next + 1 >= input.length) {
                throw new IllegalArgumentException("Pattern keys must be character and item pairs, got " + input[next]);
            }
            keys.put((Character) input[next], stack(input[next + 1], OreDictionary.WILDCARD_VALUE));
        }
        int width = 0;
        for (String row : rows) {
            width = Math.max(width, row.length());
        }
        int height = rows.size();
        ItemStack[] cells = new ItemStack[width * height];
        for (int y = 0; y < height; y++) {
            String row = rows.get(y);
            for (int x = 0; x < row.length(); x++) {
                ItemStack key = keys.get(row.charAt(x));
                cells[x + y * width] = key == null ? null : key.copy();
            }
        }
        return new RecipePattern(width, height, cells);
    }

    /** Reads a shapeless ingredient list; an item or block takes metadata 0. */
    public static ItemStack[] shapeless(Object... input) {
        ItemStack[] stacks = new ItemStack[input.length];
        for (int i = 0; i < input.length; i++) {
            stacks[i] = stack(input[i], 0);
        }
        return stacks;
    }

    private static ItemStack stack(Object entry, int itemMeta) {
        if (entry instanceof ItemStack) {
            return ((ItemStack) entry).copy();
        }
        if (entry instanceof Item) {
            return new ItemStack((Item) entry, 1, itemMeta);
        }
        if (entry instanceof Block) {
            return new ItemStack((Block) entry, 1, itemMeta);
        }
        throw new IllegalArgumentException("Not a recipe ingredient: " + entry);
    }
}
