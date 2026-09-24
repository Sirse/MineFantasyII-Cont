package minefantasy.mf2.api.knowledge.client;

import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.GridRecipe;

/** Where the knowledge book draws a grid recipe's entries. */
final class GridPages {

    interface EntryAt {

        void draw(int x, int y, ItemStack stack);
    }

    private GridPages() {}

    /**
     * Visits each stack at its cell: a shaped pattern as laid out, shapeless ingredients row by row across the station
     * grid. Script ingredients have no single stack to show and are skipped.
     */
    static void forEachEntry(GridRecipe recipe, EntryAt entryAt) {
        List<Object> entries = recipe.getEntries();
        int rowWidth = recipe.isShaped() ? recipe.getWidth() : recipe.getGrid().width;
        for (int i = 0; i < entries.size(); i++) {
            Object entry = entries.get(i);
            if (entry instanceof ItemStack) {
                entryAt.draw(i % rowWidth, i / rowWidth, (ItemStack) entry);
            }
        }
    }
}
