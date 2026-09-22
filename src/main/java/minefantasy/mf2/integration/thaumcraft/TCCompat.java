package minefantasy.mf2.integration.thaumcraft;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInterModComms;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.config.ConfigIntegration;
import minefantasy.mf2.util.MFLogUtil;

/**
 * Entry points for the optional Thaumcraft integration. Nothing here may load a Thaumcraft class outside a guarded
 * branch.
 */
public class TCCompat {

    public static final String MODID = "Thaumcraft";

    private static boolean scanned;
    private static boolean thaumiumIngot;
    private static boolean voidIngot;

    public static boolean isActive() {
        return ConfigIntegration.tcIntegration && Loader.isModLoaded(MODID);
    }

    /**
     * Sent in init: Thaumcraft reads these messages in its own postInit, and MF loads after it. Metadata 0 is the ripe
     * bush, picked by right clicking, which is what a clickable crop means to a golem.
     */
    public static void registerGolemCrops() {
        if (!isActive()) {
            return;
        }
        FMLInterModComms.sendRuntimeMessage(
                MineFantasyII.MODID,
                MODID,
                "harvestClickableCrop",
                new ItemStack(BlockListMF.berryBush, 1, 0));
        MFLogUtil.log("Thaumcraft golems told how to pick ripe berry bushes");
    }

    /**
     * Reports what Thaumcraft registered. Standard Thaumcraft provides both ingots, so MF adds no aliases and guesses
     * no metadata.
     */
    public static void beforeRecipes() {
        if (!isActive()) {
            return;
        }
        scanIngots();

        MFLogUtil.log(
                "Thaumcraft detected: ingotThaumium " + (thaumiumIngot ? "found" : "missing")
                        + ", ingotVoid "
                        + (voidIngot ? "found" : "missing"));
        if (!thaumiumIngot || !voidIngot) {
            MFLogUtil.logWarn(
                    "Thaumcraft is loaded but does not register the expected ingots; the matching MF materials will have no forging cycle.");
        }
    }

    /** Runs after the MF recipes, so every item the tables name already exists. */
    public static void afterRecipes() {
        if (!isActive()) {
            return;
        }
        if (ConfigIntegration.tcAspects) {
            TCAspects.registerAll();
        }
        if (ConfigIntegration.tcInfusion) {
            TCRecipes.init();
            TCResearch.init();
        }
    }

    public static boolean hasThaumiumIngot() {
        scanIngots();
        return thaumiumIngot;
    }

    public static boolean hasVoidIngot() {
        scanIngots();
        return voidIngot;
    }

    private static void scanIngots() {
        if (scanned) {
            return;
        }
        scanned = true;
        thaumiumIngot = isRegistered("ingotThaumium");
        voidIngot = isRegistered("ingotVoid");
    }

    private static boolean isRegistered(String oreName) {
        for (ItemStack stack : OreDictionary.getOres(oreName)) {
            if (stack != null && stack.getItem() != null) {
                return true;
            }
        }
        return false;
    }
}
