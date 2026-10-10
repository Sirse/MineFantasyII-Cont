package minefantasy.mf2.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.IHotItem;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.RecipeId;

/**
 * ZenScript for what a pack maker sees in game: an item as a result or as an ingredient, and the line removing a recipe
 * by id. Only what a recipe means is written: heat, wear and other tags an item picks up in use never become
 * conditions, and the tags left out are listed.
 */
public final class ItemScript {

    private static final Map<String, String> CLASSES = new LinkedHashMap<String, String>();

    static {
        CLASSES.put("anvil", "Anvil");
        CLASSES.put("carpenter", "CarpenterBench");
        CLASSES.put("kitchen", "KitchenBench");
        CLASSES.put("quern", "Quern");
        CLASSES.put("bloomery", "Bloomery");
        CLASSES.put("tanning", "TanningRack");
        CLASSES.put("big_furnace", "BigFurnace");
        CLASSES.put("blast_furnace", "BlastFurnace");
        CLASSES.put("paint_oil", "PaintOil");
        CLASSES.put("cooking", "Cooking");
        CLASSES.put("forge_heat", "Forge");
        CLASSES.put("alloy", "Crucible");
    }

    private ItemScript() {}

    /** The item in angle brackets: {@code <mod:name>}, with the damage when it is not 0, {@code *} for any. */
    public static String bracket(ItemStack stack) {
        String name = Item.itemRegistry.getNameForObject(stack.getItem());
        int damage = stack.getItemDamage();
        if (damage == OreDictionary.WILDCARD_VALUE) {
            return "<" + name + ":*>";
        }
        return damage == 0 || stack.getItem().isDamageable() ? "<" + name + ">" : "<" + name + ":" + damage + ">";
    }

    /** The item as a result: its materials through MF.stack, its count; heat and wear left out. */
    public static String output(ItemStack stack) {
        ItemStack item = cold(stack);
        String[] materials = materials(item);
        String text = materials == null ? bracket(item)
                : "mods.minefantasy.MF.stack(" + bracket(item) + materialArgs(materials) + ")";
        return item.stackSize > 1 ? text + " * " + item.stackSize : text;
    }

    /** The item as an ingredient: its materials through MF.input, its count; any wear taken. */
    public static String input(ItemStack stack) {
        ItemStack item = cold(stack);
        String[] materials = materials(item);
        String text;
        if (materials != null) {
            text = "mods.minefantasy.MF.input(" + bracket(item) + materialArgs(materials) + ")";
        } else if (item.getItem().isDamageable()) {
            text = bracket(item) + ".anyDamage()";
        } else {
            text = bracket(item);
        }
        return item.stackSize > 1 ? text + " * " + item.stackSize : text;
    }

    /** The tags a script would not keep: everything but the materials, once heat is taken off. */
    public static List<String> droppedTags(ItemStack stack) {
        List<String> dropped = new ArrayList<String>();
        NBTTagCompound tag = cold(stack).getTagCompound();
        if (tag != null) {
            for (Object key : tag.func_150296_c()) {
                if (!CustomMaterial.NBTBase.equals(key)) {
                    dropped.add((String) key);
                }
            }
        }
        return dropped;
    }

    /** The removal line for a recipe, or null for a station scripts remove otherwise. */
    public static String removeLine(RecipeId id) {
        String zen = zenClass(id);
        return zen == null ? null : "mods.minefantasy." + zen + ".remove(\"" + id + "\");";
    }

    private static String station(RecipeId id) {
        int slash = id.getPath().indexOf('/');
        return slash < 0 ? id.getPath() : id.getPath().substring(0, slash);
    }

    private static String zenClass(RecipeId id) {
        return CLASSES.get(station(id));
    }

    /** The item a hot stack carries, as the script would name it. */
    private static ItemStack cold(ItemStack stack) {
        if (stack.getItem() instanceof IHotItem) {
            ItemStack held = Heatable.getItem(stack);
            if (held != null) {
                held.stackSize = stack.stackSize;
                return held;
            }
        }
        return stack;
    }

    /** Main and haft material names, or null when the item has none. */
    private static String[] materials(ItemStack stack) {
        NBTTagCompound tag = CustomMaterial.getNBT(stack, false);
        if (tag == null || !tag.hasKey(CustomToolHelper.slot_main)) {
            return null;
        }
        // "any" is how MineFantasy names no material; it is never one a script can give
        String main = tag.getString(CustomToolHelper.slot_main);
        if (main.isEmpty() || main.equalsIgnoreCase("any")) {
            return null;
        }
        String haft = tag.hasKey(CustomToolHelper.slot_haft) ? tag.getString(CustomToolHelper.slot_haft) : null;
        if (haft != null && (haft.isEmpty() || haft.equalsIgnoreCase("any"))) {
            haft = null;
        }
        return new String[] { main.toLowerCase(), haft == null ? null : haft.toLowerCase() };
    }

    private static String materialArgs(String[] materials) {
        return ", \"" + materials[0] + "\"" + (materials[1] == null ? "" : ", \"" + materials[1] + "\"");
    }
}
