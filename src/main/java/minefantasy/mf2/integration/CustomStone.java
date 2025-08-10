package minefantasy.mf2.integration;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.oredict.OreDictionary;

public class CustomStone {

    private static final ArrayList<ItemStack> customStones = new ArrayList<>();

    public static void init() {
        String stoneEntry = ForgeVersion.getBuildVersion() < 934 ? "stoneSmooth" : "stone";
        if (!OreDictionary.doesOreNameExist(stoneEntry)) {
            customStones.clear();
            return;
        }

        customStones.clear();
        customStones.addAll(OreDictionary.getOres(stoneEntry));
    }

    public static boolean isStone(Block block) {
        if (block == null) return false;
        return block == Blocks.stone || isStone(new ItemStack(block));
    }

    public static boolean isStone(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        for (ItemStack stoneStack : customStones) {
            if (stoneStack != null && OreDictionary.itemMatches(stoneStack, stack, false)) {
                return true;
            }
        }
        return false;
    }
}
