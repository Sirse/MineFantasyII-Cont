package minefantasy.mf2.integration;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class CustomStone {

    private static int stoneOreId = -1;
    private static int stoneSmoothOreId = -1;
    private static boolean initialized;

    public static void init() {
        initialized = true;
        stoneOreId = OreDictionary.getOreID("stone");
        stoneSmoothOreId = OreDictionary.getOreID("stoneSmooth");
    }

    public static boolean isStone(Block block) {
        return isStone(block, 0);
    }

    public static boolean isStone(Block block, int metadata) {
        if (block == null) return false;
        if (block == Blocks.stone) return true;
        return isStone(new ItemStack(block, 1, metadata));
    }

    public static boolean isStone(ItemStack stack) {
        ensureInitialized();
        if (stack == null || stack.getItem() == null) return false;
        int[] oreIds = OreDictionary.getOreIDs(stack);
        for (int oreId : oreIds) {
            if (oreId == stoneOreId || oreId == stoneSmoothOreId) {
                return true;
            }
        }
        return false;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }
}
