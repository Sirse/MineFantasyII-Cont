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
 * Forge fuels, with how long and how hot they burn, and carbon for the bloomery and the blast furnace, kept as the ore
 * name {@code Carbon-<uses>}.
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
     * Adds a carbon item for smelting. A stack names its own metadata only; its wildcard ({@code <item:*>}, an Item or
     * a Block) names every one.
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
