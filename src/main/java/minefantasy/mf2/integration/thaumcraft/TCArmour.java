package minefantasy.mf2.integration.thaumcraft;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.armour.ArmourDesign;
import minefantasy.mf2.api.armour.CustomArmourEntry;
import minefantasy.mf2.util.MFLogUtil;
import thaumcraft.api.ItemApi;

/**
 * Gives the Thaumcraft armour a weight and an armour class so it stops counting as generic medium armour. Protection,
 * enchanting, repair and the Thaumcraft effects are untouched: this registry only holds weight, bulk and class.
 */
public class TCArmour {

    private static boolean registered;

    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;

        int written = 0;

        // Cloth and goggles, so they no longer read as someone else's medium armour.
        written += add("itemGoggles", ArmourDesign.light, ArmourDesign.CLOTH, 1F);
        written += add("itemChestRobe", ArmourDesign.light, ArmourDesign.CLOTH, 2F);
        written += add("itemLegsRobe", ArmourDesign.light, ArmourDesign.CLOTH, 2F);
        written += add("itemBootsRobe", ArmourDesign.light, ArmourDesign.CLOTH, 1F);
        written += add("itemBootsTraveller", ArmourDesign.light, ArmourDesign.CLOTH, 2F);

        // Thaumium: a full suit reaches the 30kg slowdown threshold without crossing it.
        written += add("itemHelmetThaumium", ArmourDesign.medium, ArmourDesign.SOLID, 6F);
        written += add("itemChestThaumium", ArmourDesign.medium, ArmourDesign.SOLID, 9F);
        written += add("itemLegsThaumium", ArmourDesign.medium, ArmourDesign.SOLID, 9F);
        written += add("itemBootsThaumium", ArmourDesign.medium, ArmourDesign.SOLID, 6F);

        // Void is lighter but stays medium: the better material buys mass, not a free light class.
        written += add("itemHelmetVoid", ArmourDesign.medium, ArmourDesign.SOLID, 4F);
        written += add("itemChestVoid", ArmourDesign.medium, ArmourDesign.SOLID, 6F);
        written += add("itemLegsVoid", ArmourDesign.medium, ArmourDesign.SOLID, 6F);
        written += add("itemBootsVoid", ArmourDesign.medium, ArmourDesign.SOLID, 4F);

        // Fortress pays for its protection: heavy, and over the threshold with no boots to its name.
        written += add("itemHelmetFortress", ArmourDesign.heavy, ArmourDesign.SOLID, 8F);
        written += add("itemChestFortress", ArmourDesign.heavy, ArmourDesign.SOLID, 12F);
        written += add("itemLegsFortress", ArmourDesign.heavy, ArmourDesign.SOLID, 12F);

        // Void Thaumaturge stays light on the scales but heavy for parrying.
        written += add("itemHelmetVoidRobe", ArmourDesign.heavy, ArmourDesign.CLOTH, 4F);
        written += add("itemChestVoidRobe", ArmourDesign.heavy, ArmourDesign.CLOTH, 6F);
        written += add("itemLegsVoidRobe", ArmourDesign.heavy, ArmourDesign.CLOTH, 6F);

        MFLogUtil.log("Thaumcraft armour weighted: " + written + " pieces");
    }

    private static int add(String field, String armourClass, ArmourDesign design, float wornWeight) {
        ItemStack stack = ItemApi.getItem(field, 0);
        if (stack == null || stack.getItem() == null) {
            MFLogUtil.logDebug("Thaumcraft did not supply " + field + "; no weight registered for it");
            return 0;
        }
        Item item = stack.getItem();
        // A player config is read before this runs, so an entry already there is theirs and stays.
        if (CustomArmourEntry.getEntry(item) != null) {
            MFLogUtil.logDebug("Armour entry for " + field + " already set, leaving it alone");
            return 0;
        }
        if (!CustomArmourEntry.registerWornPiece(item, wornWeight, design.getBulk(), armourClass)) {
            MFLogUtil.logWarn("Thaumcraft item " + field + " is not armour; no weight registered for it");
            return 0;
        }
        return 1;
    }
}
