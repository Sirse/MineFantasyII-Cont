package minefantasy.mf2.integration.thaumcraft;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.util.MFLogUtil;
import thaumcraft.api.ItemApi;

/**
 * Puts the Thaumcraft planks under the ore names the MF wood materials look for. The material names stay as they are:
 * they live in the NBT of items already made.
 */
public class TCWood {

    private static boolean registered;

    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;

        add("planksGreatwoodWood", 6);
        add("planksSilverwoodWood", 7);
    }

    /** Metadata of blockWoodenDevice: 6 is greatwood planks, 7 silverwood. */
    private static void add(String oreName, int meta) {
        ItemStack planks = ItemApi.getBlock("blockWoodenDevice", meta);
        if (planks == null || planks.getItem() == null) {
            MFLogUtil.logWarn("Thaumcraft did not supply " + oreName + "; that wood gets no MF components");
            return;
        }
        if (alreadyListed(oreName, planks)) {
            MFLogUtil.logDebug(oreName + " is already registered, leaving it alone");
            return;
        }
        OreDictionary.registerOre(oreName, planks);
        MFLogUtil.log("Thaumcraft planks registered as " + oreName);
    }

    private static boolean alreadyListed(String oreName, ItemStack planks) {
        ArrayList<ItemStack> listed = OreDictionary.getOres(oreName);
        if (listed == null) {
            return false;
        }
        for (ItemStack entry : listed) {
            if (entry != null && OreDictionary.itemMatches(entry, planks, false)) {
                return true;
            }
        }
        return false;
    }
}
