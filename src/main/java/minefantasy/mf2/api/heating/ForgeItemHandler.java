package minefantasy.mf2.api.heating;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class ForgeItemHandler {

    public static List<ForgeFuel> forgeFuel = new ArrayList<>();
    public static int forgeMaxTemp = 0;

    /** The fuel entry for the item, matching its damage unless the entry takes any; null when it is no fuel. */
    public static ForgeFuel getStats(ItemStack item) {
        if (item == null) return null;

        for (ForgeFuel fuel : forgeFuel) {
            if (fuel != null && fuel.fuel.getItem() == item.getItem()
                    && (fuel.fuel.getItemDamage() == OreDictionary.WILDCARD_VALUE
                            || fuel.fuel.getItemDamage() == item.getItemDamage())) {
                return fuel;
            }
        }
        return null;
    }

    /** How long the item burns in a forge, 0 when it is no fuel. */
    public static float getForgeFuel(ItemStack item) {
        ForgeFuel fuel = getStats(item);
        return fuel == null ? 0 : fuel.duration;
    }

    /** The temperature the item burns at, 0 when it is no fuel. */
    public static int getForgeHeat(ItemStack item) {
        ForgeFuel fuel = getStats(item);
        return fuel == null ? 0 : fuel.baseHeat;
    }
}
