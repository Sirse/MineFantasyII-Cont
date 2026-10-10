package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.GridMatch;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.IHotItem;
import minefantasy.mf2.api.recipe.Input;
import minetweaker.api.data.IData;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.oredict.IOreDictEntry;

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
     * Whether a script stack names this one, to pick recipes to remove. MineTweaker's matches compares the item, damage
     * and size but never NBT, so {@code <minefantasy2:custom_bar>.withTag({...: "Copper"})} would name every bar: a tag
     * the script gives must be contained in the stack's own.
     */
    public static boolean names(IIngredient ingredient, IItemStack stack) {
        if (ingredient == null || stack == null || !ingredient.matches(stack)) {
            return false;
        }
        if (!(ingredient instanceof IItemStack)) {
            return true;
        }
        IData tag = ((IItemStack) ingredient).getTag();
        if (tag == null || tag.asMap() == null || tag.asMap().isEmpty()) {
            return true;
        }
        IData own = stack.getTag();
        return own != null && own.contains(tag);
    }

    /**
     * MineFantasy recipes check ingredients with matches(), which never runs CraftTweaker transformers (reuse,
     * transformDamage and the like): an ingredient with some is refused rather than having them silently dropped.
     */
    public static void requireNoTransformers(IIngredient ingredient) {
        if (ingredient != null && ingredient.hasTransformers()) {
            throw new IllegalArgumentException(
                    ingredient + " has transformers (such as reuse or transformDamage), which MineFantasy recipes do"
                            + " not run");
        }
    }

    /** The items an ingredient lists; none rather than null, which CraftTweaker allows (for {@code <*>}). */
    public static List<IItemStack> items(IIngredient ingredient) {
        List<IItemStack> items = ingredient == null ? null : ingredient.getItems();
        return items == null ? Collections.<IItemStack>emptyList() : items;
    }

    /**
     * The items of an ingredient for an API that keeps items only (salvage, special forging), refusing what it would
     * have to drop: conditions such as {@code only} or {@code onlyWithTag}, and materials where they are not kept.
     * Plain stacks and ore names pass; alternatives are written one line per item.
     */
    public static List<ItemStack> plainItems(String api, IIngredient ingredient, boolean keepsMaterials) {
        boolean kept = ingredient instanceof IItemStack || ingredient instanceof IOreDictEntry
                || keepsMaterials && ingredient instanceof MaterialInput
                        && ((MaterialInput) ingredient).getConditions().isEmpty()
                        && ((MaterialInput) ingredient).keptByItem();
        if (!kept) {
            throw new IllegalArgumentException(
                    api + " keeps items only and cannot keep the rule of "
                            + ingredient
                            + ": give "
                            + (keepsMaterials ? "a plain item, an ore name or MF.input" : "a plain item or an ore name")
                            + ", one line per item");
        }
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (IItemStack item : items(ingredient)) {
            ItemStack stack = MineTweakerMC.getItemStack(item);
            if (stack != null && stack.getItem() != null) {
                out.add(stack);
            }
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException(api + ": " + ingredient + " lists no valid items");
        }
        return out;
    }

    public static boolean names(IIngredient ingredient, ItemStack stack) {
        return stack != null && stack.getItem() != null && names(ingredient, MineTweakerMC.getIItemStack(stack));
    }

    /** Whether the input would take the script's stack, given as many as it asks for. */
    public static boolean takes(Input input, IItemStack item) {
        ItemStack stack = item == null ? null : MineTweakerMC.getItemStack(item);
        if (input == null || stack == null || stack.getItem() == null) {
            return false;
        }
        ItemStack probe = stack.copy();
        probe.stackSize = Math.max(stack.stackSize, input.getAmount());
        return input.matches(probe);
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
        ScriptWarnings.inputTag(ingredient);
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
     * A script's shaped recipe, empty cells null: anchored, the pattern sits in the grid's top left corner and is never
     * mirrored, as scripts have always behaved; otherwise it may shift and mirror as MineFantasy's own recipes.
     */
    public static GridRecipe.Builder shaped(GridRecipe.Grid grid, IIngredient[][] pattern, IItemStack output,
            boolean anchored) {
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
        GridRecipe.Builder builder = GridRecipe
                .shaped(grid, width, height, entries, cells, MineTweakerMC.getItemStack(output));
        return anchored ? builder.anchored() : builder;
    }

    /** A script's shapeless recipe. */
    public static GridRecipe.Builder shapeless(GridRecipe.Grid grid, IIngredient[] ingredients, IItemStack output) {
        GridMatch.Cell[] cells = new GridMatch.Cell[ingredients.length];
        for (int i = 0; i < ingredients.length; i++) {
            cells[i] = cell(ingredients[i], grid.heat);
        }
        return GridRecipe.shapeless(grid, ingredients, cells, MineTweakerMC.getItemStack(output));
    }

    /** The widest row of a possibly ragged pattern. */
    public static int gridWidth(IIngredient[][] grid) {
        int width = 0;
        for (IIngredient[] row : grid) {
            if (row != null) width = Math.max(width, row.length);
        }
        return width;
    }
}
