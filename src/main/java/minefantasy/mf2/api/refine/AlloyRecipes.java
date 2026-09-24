package minefantasy.mf2.api.refine;

import java.util.ArrayList;
import java.util.List;

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
 * alloy ({@link #addRatioRecipe}) share its id with {@code _x2}, {@code _x3}... and are registered together.
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

    public static Alloy addAlloy(ItemStack out, int level, List in) {
        Alloy alloy = new Alloy(out, level, in);
        addAlloy(alloy);
        return alloy;
    }

    public static void addAlloy(ItemStack out, List in) {
        addAlloy(out, 0, in);
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
    public static Alloy[] addRatioRecipe(ItemStack out, int level, List in, int levels) {
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
