package minefantasy.mf2.api.heating;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Forge fuel: how long and how hot each item burns, for the forge, the big furnace heater and the cogwork suit. Script
 * entries count ahead of the mods' own, the latest first, and a reload takes them back out.
 */
public class ForgeItemHandler {

    private static final List<ForgeFuel> forgeFuel = new ArrayList<>();
    private static final List<ForgeFuel> scriptFuel = new ArrayList<>();

    /** Adds a mod's own fuel; see {@link minefantasy.mf2.api.crafting.MineFantasyFuels#addForgeFuel}. */
    public static void add(ForgeFuel fuel) {
        if (fuel != null && fuel.fuel != null) forgeFuel.add(fuel);
    }

    /** Adds fuel for a script, ahead of every other; one burning no time takes the item out as fuel. */
    public static void addScript(ForgeFuel fuel) {
        scriptFuel.add(fuel);
    }

    public static void removeScript(ForgeFuel fuel) {
        scriptFuel.remove(fuel);
    }

    /** The fuel entry for the item, matching its damage unless the entry takes any; null when it is no fuel. */
    public static ForgeFuel getStats(ItemStack item) {
        if (item == null || item.getItem() == null) return null;
        for (int i = scriptFuel.size() - 1; i >= 0; i--) {
            ForgeFuel fuel = scriptFuel.get(i);
            if (OreDictionary.itemMatches(fuel.fuel, item, false)) return fuel.duration > 0 ? fuel : null;
        }
        for (ForgeFuel fuel : forgeFuel) {
            if (OreDictionary.itemMatches(fuel.fuel, item, false)) return fuel;
        }
        return null;
    }

    /** Every item that burns in a forge now, each once, for showing what fuel there is. */
    public static List<ForgeFuel> fuels() {
        List<ForgeFuel> all = new ArrayList<>();
        List<ForgeFuel> ordered = new ArrayList<>(scriptFuel);
        Collections.reverse(ordered);
        ordered.addAll(forgeFuel);
        for (ForgeFuel fuel : ordered) {
            if (getStats(fuel.fuel) == fuel) all.add(fuel);
        }
        return all;
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
