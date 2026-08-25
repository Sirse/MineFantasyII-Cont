package minefantasy.mf2.api.refine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.util.MFLogUtil;

public class AlloyRecipes {

    public static List<Alloy> alloys = new ArrayList<Alloy>();

    public static Alloy addAlloy(ItemStack out, int level, List in) {
        Alloy alloy = new Alloy(out, level, in);
        alloys.add(alloy);
        return alloy;
    }

    public static void addAlloy(ItemStack out, List in) {
        addAlloy(out, 0, in);
    }

    public static void addAlloy(Alloy alloy) {
        alloys.add(alloy);
    }

    public static Alloy getResult(ItemStack[] inv) {
        for (Alloy alloy : alloys) {
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
            alloys[a - 1] = addAlloy(out2, level, list2);
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
