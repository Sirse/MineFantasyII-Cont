package minefantasy.mf2.api.armour;

import java.util.HashMap;

import net.minecraft.item.Item;

public class CustomDamageRatioEntry {

    /**
     * Keyed by the item itself, not by its id: Forge renumbers modded ids when a world is loaded, and an entry filed
     * under the old number would never be found again.
     */
    public static HashMap<Item, CustomDamageRatioEntry> entries = new HashMap<Item, CustomDamageRatioEntry>();
    public static HashMap<String, CustomDamageRatioEntry> entriesProj = new HashMap<String, CustomDamageRatioEntry>();

    public float[] vars;

    private CustomDamageRatioEntry(float[] vars) {
        this.vars = vars;
    }

    /**
     * Register a weapon to give variables
     *
     * @param item the item
     * @param vars the damage type ratio cutting:blunt
     */
    public static void registerItem(Item item, float[] vars) {
        entries.put(item, new CustomDamageRatioEntry(vars));
    }

    /**
     * Register a weapon to give variables
     *
     * @param id   the item id, resolved to the item it stands for right away
     * @param vars the damage type ratio cutting:blunt
     */
    public static void registerItem(int id, float[] vars) {
        Item item = Item.getItemById(id);
        if (item != null) {
            registerItem(item, vars);
        }
    }

    /**
     * Register an entity (like arrows) to give variables
     *
     * @param id   the entity id
     * @param vars the damage type ratio cutting:blunt
     */
    public static void registerEntity(String id, float[] vars) {
        entriesProj.put(id, new CustomDamageRatioEntry(vars));
    }

    /**
     * Gets the ratio for an item, null if it's not found
     */
    public static float[] getTraits(Item item) {
        CustomDamageRatioEntry entry = item == null ? null : entries.get(item);
        return entry != null ? entry.vars : null;
    }

    /**
     * Gets the ratio for an item, null if it's not found
     */
    public static float[] getTraits(int id) {
        return getTraits(Item.getItemById(id));
    }

    /**
     * Gets the ratio for an entity, null if it's not found
     */
    public static float[] getTraits(String id) {
        return entriesProj.get(id) != null ? entriesProj.get(id).vars : new float[] { 1, 1, 1 };
    }
}
