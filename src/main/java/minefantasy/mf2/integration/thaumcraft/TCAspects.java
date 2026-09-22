package minefantasy.mf2.integration.thaumcraft;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.ITieredComponent;
import minefantasy.mf2.util.MFLogUtil;
import thaumcraft.api.ThaumcraftApi;

/**
 * Hands the aspect tables to Thaumcraft. Values live in {@link TCAspectTables}; what is allowed through is here.
 */
public class TCAspects {

    private static boolean registered;

    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;

        List<TCAspectEntry> table = TCAspectTables.buildItemTable();
        int written = 0;
        int skipped = 0;
        for (TCAspectEntry entry : table) {
            if (entry.stack == null || entry.stack.getItem() == null) {
                continue;
            }
            if (isMaterialBound(entry.stack)) {
                MFLogUtil.logWarn(
                        "Skipping aspects for " + name(entry.stack)
                                + ": the item carries its material in NBT, so one entry cannot describe it.");
                continue;
            }
            // Someone else already described this item. Theirs wins: a pack or addon that tuned MF resources on
            // purpose must not be undone by simply loading this version.
            if (ThaumcraftApi.exists(entry.stack.getItem(), entry.stack.getItemDamage())) {
                MFLogUtil.logDebug("Aspects for " + name(entry.stack) + " already registered, leaving them alone");
                skipped++;
                continue;
            }
            ThaumcraftApi.registerObjectTag(entry.stack, entry.aspects);
            written++;
        }
        MFLogUtil.log(
                "Thaumcraft aspects registered for " + written + " MF resources, " + skipped + " already had them");

        TCEntityAspects.registerAll();
    }

    /**
     * True when the item stores a material in NBT, which makes one Item plus damage entry meaningless: the cheapest
     * variant would decide the aspects of the most expensive. Plain metadata variants are not that, and the tables
     * describe those by listing them.
     */
    private static boolean isMaterialBound(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof ITieredComponent && ((ITieredComponent) item).getMaterialType(stack) != null;
    }

    private static String name(ItemStack stack) {
        return Item.itemRegistry.getNameForObject(stack.getItem()) + "@" + stack.getItemDamage();
    }
}
