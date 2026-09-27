package minefantasy.mf2.api.refine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeSource;
import minefantasy.mf2.api.recipe.RecipeTransaction;
import minefantasy.mf2.util.MFLogUtil;

/**
 * Crucible alloys, kept in {@link MFRecipes#ALLOY}. Native alloys are named after their output; the ratio copies of one
 * alloy ({@link Builder#ratio}) share its id with {@code _x2}, {@code _x3}... and are registered together.
 */
public class AlloyRecipes {

    /** The published alloys in lookup order. */
    public static List<Alloy> alloys() {
        return MFRecipes.ALLOY.published().recipes();
    }

    /**
     * Changes whenever the published alloys do. The crucible caches a matched recipe and would otherwise keep smelting
     * one that a script reload has already taken away.
     */
    public static long getVersion() {
        return MFRecipes.ALLOY.published().getGeneration();
    }

    /** A native alloy making the output; see {@link Builder}. */
    public static Builder alloy(ItemStack output) {
        return new Builder(output);
    }

    /**
     * A native alloy: any crucible, and the ratio taken once, unless said otherwise. It is registered with its
     * ingredients, items, blocks or stacks.
     */
    public static final class Builder {

        private final ItemStack output;
        private int level;
        private int ratio = 1;

        private Builder(ItemStack output) {
            this.output = output;
        }

        /** The crucible tier it needs. */
        public Builder level(int level) {
            this.level = level;
            return this;
        }

        /**
         * Also made with every ingredient and the output multiplied, up to the given number of times as far as the nine
         * crucible slots allow: 2 copper and 1 tin make it, and so do 4 and 2, and 6 and 3.
         */
        public Builder ratio(int times) {
            this.ratio = times;
            return this;
        }

        /** Registers the alloy and its ratio copies; they are returned in order, the plain one first. */
        public Alloy[] of(Object... ingredients) {
            return addRatioRecipe(output, level, stacks(ingredients), ratio);
        }
    }

    private static List<ItemStack> stacks(Object[] ingredients) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Object ingredient : ingredients) {
            if (ingredient instanceof ItemStack) {
                stacks.add(((ItemStack) ingredient).copy());
            } else if (ingredient instanceof Item) {
                stacks.add(new ItemStack((Item) ingredient));
            } else if (ingredient instanceof Block) {
                stacks.add(new ItemStack((Block) ingredient));
            } else {
                throw new IllegalArgumentException(
                        "An alloy ingredient must be an item, block or stack: " + ingredient);
            }
        }
        return stacks;
    }

    public static void addAlloy(Alloy alloy) {
        NativeRecipes.addGrid(MFRecipes.ALLOY, alloy.getRecipeOutput(), alloy, 0);
    }

    /** The first alloy the crucible grid holds, with its id; null for none. */
    public static RecipeEntry<Alloy> find(ItemStack[] inv) {
        for (RecipeEntry<Alloy> entry : MFRecipes.ALLOY.published().all()) {
            if (entry.getRecipe().matches(inv)) {
                return entry;
            }
        }
        return null;
    }

    public static Alloy getResult(ItemStack[] inv) {
        for (Alloy alloy : alloys()) {
            if (alloy.matches(inv)) {
                return alloy;
            }
        }
        return null;
    }

    /**
     * Adds an alloy, and duplicates it so the ratio can be copied eg a recipe with 2 'x' ore and 1 'y' ore can be made
     * with 4 'x' ore and 2 'y' ore...
     *
     * @param the amount of times the ratio can be added
     */
    private static Alloy[] addRatioRecipe(ItemStack out, int level, List in, int levels) {
        Alloy[] alloys = ratioAlloys(out, level, in, levels);
        if (alloys.length == 0) {
            return alloys;
        }
        RecipeId id = NativeRecipes.nativeId(MFRecipes.ALLOY, out);
        try (RecipeTransaction tx = MFRecipes.REGISTRIES.begin(RecipeSource.NATIVE)) {
            stageRatio(tx, id, alloys, 0);
            tx.commit();
        }
        return alloys;
    }

    /** Stages the alloy and its ratio copies under {@code id}, {@code id_x2}, {@code id_x3}... */
    public static void stageRatio(RecipeTransaction tx, RecipeId id, Alloy[] alloys, int priority) {
        for (int a = 0; a < alloys.length; a++) {
            tx.add(MFRecipes.ALLOY, a == 0 ? id : id.withSuffix("_x" + (a + 1)), alloys[a], priority);
        }
    }

    /**
     * The alloy and copies of it with every ingredient and the output multiplied, as far as the nine crucible slots
     * allow: a recipe with 2 'x' ore and 1 'y' ore can be made with 4 'x' ore and 2 'y' ore...
     *
     * @param levels the amount of times the ratio can be added
     */
    public static Alloy[] ratioAlloys(ItemStack out, int level, List in, int levels) {
        int maxLevels = (int) (9F / Math.max(1, in.size()));
        if (maxLevels <= 0) {
            MFLogUtil.logWarn(
                    "Skipping alloy ratio recipe for " + out.getDisplayName()
                            + ": "
                            + in.size()
                            + " ingredients do not fit the 9 crucible slots");
            return new Alloy[0];
        }
        levels = Math.min(Math.max(levels, 1), maxLevels);
        Alloy[] alloys = new Alloy[levels];
        for (int a = 1; a <= levels; a++) {
            List list2 = createDupeList(in, a);
            ItemStack out2 = out.copy();
            int ss = Math.min(out2.getMaxStackSize(), out2.stackSize * a);
            out2.stackSize = ss;
            alloys[a - 1] = new Alloy(out2, level, list2);
        }
        return alloys;
    }

    public static List createDupeList(List list) {
        return createDupeList(list, 2);
    }

    public static List createDupeList(List list, int dupe) {
        if (dupe == 0) {
            dupe = 1;
        }
        List list2 = new ArrayList();
        for (int a = 0; a < dupe; a++) {
            for (int b = 0; b < list.size(); b++) {
                list2.add(list.get(b));
            }
        }
        return list2;
    }
}
