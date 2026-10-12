package minefantasy.mf2.api.crafting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.api.heating.ForgeFuel;
import minefantasy.mf2.api.heating.ForgeItemHandler;

/**
 * Forge fuels, with how long and how hot they burn; fuel for the blast furnace heater and the firepit; and carbon for
 * the bloomery and the blast furnace, kept as the ore name {@code Carbon-<uses>}.
 */
public class MineFantasyFuels {

    public static void addForgeFuel(Object input, float time, int temperature) {
        addForgeFuel(input, time, temperature, false, false);
    }

    public static void addForgeFuel(Object input, float time, int temperature, boolean willLight) {
        addForgeFuel(input, time, temperature, willLight, false);
    }

    public static void addForgeFuel(Object input, float time, int temperature, boolean willLight, boolean refined) {
        ItemStack item = convert(input);
        if (item != null) {
            ForgeItemHandler.forgeFuel.add(new ForgeFuel(item, time, temperature, willLight, refined));
            if ((int) (temperature * 1.25) > ForgeItemHandler.forgeMaxTemp) {
                ForgeItemHandler.forgeMaxTemp = (int) (temperature * 1.25);
            }
        }
    }

    /**
     * Adds a carbon item for smelting. A stack names its own metadata only, every one with
     * {@link OreDictionary#WILDCARD_VALUE} ({@code <item:*>}); an Item or a Block names metadata 0.
     *
     * @param input Item Block or ItemStack
     * @param uses
     */
    public static void addCarbon(Object input, int uses) {
        ItemStack itemstack = convert(input);

        if (itemstack != null) {
            OreDictionary.registerOre("Carbon-" + uses, itemstack);
        }
    }

    /**
     * Carbon a script adds, kept apart from the ore names so a script reload can take it back out: Forge 1.7.10 cannot
     * remove an ore dictionary entry. It counts ahead of the ore names, the latest first.
     */
    private static final List<ScriptCarbon> scriptCarbon = new ArrayList<ScriptCarbon>();

    /** Carbon one script line added: the handle a reload takes it back out with. */
    public static final class ScriptCarbon {

        private final ItemStack item;
        private final int uses;

        private ScriptCarbon(ItemStack item, int uses) {
            this.item = item;
            this.uses = uses;
        }
    }

    /** As {@link #addCarbon}: the stack's metadata only, every one for a wildcard. */
    public static ScriptCarbon addScriptCarbon(ItemStack item, int uses) {
        ScriptCarbon carbon = new ScriptCarbon(item.copy(), uses);
        scriptCarbon.add(carbon);
        return carbon;
    }

    public static void removeScriptCarbon(ScriptCarbon carbon) {
        scriptCarbon.remove(carbon);
    }

    /**
     * How many smelts (blast furn or bloomery) this can give as carbon
     */
    public static int getCarbon(ItemStack item) {
        if (item == null) return 0;
        for (int i = scriptCarbon.size() - 1; i >= 0; i--) {
            ScriptCarbon carbon = scriptCarbon.get(i);
            if (OreDictionary.itemMatches(carbon.item, item, false)) {
                return carbon.uses;
            }
        }

        for (String name : minefantasy.mf2.api.recipe.OreNames.get().namesOf(item)) {
            if (name != null && name.startsWith("Carbon-")) {
                String s = name.substring(7);
                int uses = Integer.parseInt(s);
                return uses;
            }
        }

        return 0;
    }

    /** Every item known as carbon, script carbon first, for showing what a carbon input takes. */
    public static List<ItemStack> carbonItems() {
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (int i = scriptCarbon.size() - 1; i >= 0; i--) {
            items.add(scriptCarbon.get(i).item.copy());
        }
        for (String name : OreDictionary.getOreNames()) {
            if (name != null && name.startsWith("Carbon-")) {
                for (ItemStack item : OreDictionary.getOres(name)) {
                    items.add(item.copy());
                }
            }
        }
        return items;
    }

    /**
     * Determines if the item can be used for carbon in smelting.
     */
    public static boolean isCarbon(ItemStack item) {
        return getCarbon(item) > 0;
    }

    /**
     * Fuel one kind of fire burns: items, every metadata for a wildcard, and how many ticks each burns. Script entries
     * count ahead of the mods' own, the latest first, and a reload takes them back out.
     */
    public static final class FuelTable {

        private final List<Fuel> fuel = new ArrayList<Fuel>();
        private final List<Fuel> scriptFuel = new ArrayList<Fuel>();

        private FuelTable() {}

        /**
         * Adds fuel. A stack names its own metadata only, every one with {@link OreDictionary#WILDCARD_VALUE}; an Item
         * or a Block names metadata 0.
         *
         * @param input Item Block or ItemStack
         * @param ticks how long one item burns
         */
        public void add(Object input, int ticks) {
            ItemStack item = convert(input);
            if (item != null && ticks > 0) fuel.add(new Fuel(item.copy(), ticks));
        }

        /** As {@link #add}, for a script; no ticks takes the item out as fuel. */
        public Fuel addScript(ItemStack item, int ticks) {
            Fuel added = new Fuel(item.copy(), Math.max(0, ticks));
            scriptFuel.add(added);
            return added;
        }

        public void removeScript(Fuel added) {
            scriptFuel.remove(added);
        }

        /** How many ticks this burns; 0 when it is no fuel here. */
        public int get(ItemStack item) {
            if (item == null || item.getItem() == null) return 0;
            for (int i = scriptFuel.size() - 1; i >= 0; i--) {
                Fuel entry = scriptFuel.get(i);
                if (OreDictionary.itemMatches(entry.item, item, false)) return entry.ticks;
            }
            for (Fuel entry : fuel) {
                if (OreDictionary.itemMatches(entry.item, item, false)) return entry.ticks;
            }
            return 0;
        }
    }

    /** One fuel entry: the handle a script takes it back out with. */
    public static final class Fuel {

        private final ItemStack item;
        private final int ticks;

        private Fuel(ItemStack item, int ticks) {
            this.item = item;
            this.ticks = ticks;
        }
    }

    /** Fuel for the blast furnace heater. */
    public static final FuelTable BLAST = new FuelTable();
    /** Fuel for the firepit; material planks burn longer or shorter by their wood. */
    public static final FuelTable FIREPIT = new FuelTable();

    public static ItemStack convert(Object input) {
        if (input == null) return null;

        ItemStack itemstack = null;
        if (input instanceof ItemStack) {
            return (ItemStack) input;
        } else if (input instanceof Block) {
            return new ItemStack((Block) input);
        } else if (input instanceof Item) {
            return new ItemStack((Item) input);
        }

        return null;
    }
}
