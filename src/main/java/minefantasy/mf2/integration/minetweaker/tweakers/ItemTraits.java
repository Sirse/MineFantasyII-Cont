package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.armour.CustomArmourEntry;
import minefantasy.mf2.api.armour.CustomDamageRatioEntry;
import minefantasy.mf2.api.crafting.CustomCrafterEntry;
import minefantasy.mf2.api.farming.CustomHoeEntry;
import minefantasy.mf2.api.helpers.ItemTraitRules;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * How MineFantasy treats other mods' items: armour weight and class, the damage types of a weapon or projectile, a
 * crafting tool's type, tier and efficiency, and a hoe's efficiency. Traits belong to the item, every damage value of
 * it; MineFantasy's own items keep theirs. A reload puts back what was there before the script, config entries
 * included.
 *
 * <pre>
 * mods.minefantasy.ItemTraits.armour(&lt;mod:helmet&gt;, "plate"); // weight, bulk and class of a design
 * mods.minefantasy.ItemTraits.armour(&lt;mod:helmet&gt;, "mail", 0.8, "light"); // lighter, and counted as light
 * mods.minefantasy.ItemTraits.wornArmour(&lt;mod:helmet&gt;, 4.5, 1.0, "medium"); // kg on the body, suit bulk
 * mods.minefantasy.ItemTraits.damage(&lt;mod:mace&gt;, 0, 0, 1); // cutting, piercing, blunt
 * mods.minefantasy.ItemTraits.projectileDamage("mod.Bolt", 0, 1, 0);
 * mods.minefantasy.ItemTraits.crafter(&lt;mod:hammer&gt;, "hammer", 3.0, 2);
 * mods.minefantasy.ItemTraits.hoe(&lt;mod:hoe&gt;, 8.0);
 * mods.minefantasy.ItemTraits.clear(&lt;mod:helmet&gt;); // takes all of them away
 * </pre>
 */
@ZenClass("mods.minefantasy.ItemTraits")
public class ItemTraits {

    private ItemTraits() {}

    @ZenMethod
    public static void armour(IItemStack stack, String design) {
        armour(stack, design, 1F, null);
    }

    @ZenMethod
    public static void armour(IItemStack stack, String design, float weight, @Optional String group) {
        String line = "ItemTraits.armour(" + stack + ", \"" + design + "\", ...)";
        Item item = item(line, stack);
        if (item != null) {
            build(line, CustomArmourEntry.entries, item, () -> ItemTraitRules.armour(item, design, weight, group));
        }
    }

    @ZenMethod
    public static void wornArmour(IItemStack stack, float kilograms, float suitBulk, String group) {
        String line = "ItemTraits.wornArmour(" + stack + ", ...)";
        Item item = item(line, stack);
        if (item != null) {
            build(
                    line,
                    CustomArmourEntry.entries,
                    item,
                    () -> ItemTraitRules.wornArmour(item, kilograms, suitBulk, group));
        }
    }

    @ZenMethod
    public static void damage(IItemStack stack, float cutting, float piercing, float blunt) {
        String line = "ItemTraits.damage(" + stack + ", " + cutting + ", " + piercing + ", " + blunt + ")";
        Item item = item(line, stack);
        if (item != null) {
            build(
                    line,
                    CustomDamageRatioEntry.entries,
                    item,
                    () -> ItemTraitRules.damage(item, cutting, piercing, blunt));
        }
    }

    @ZenMethod
    public static void projectileDamage(String entity, float cutting, float piercing, float blunt) {
        String line = "ItemTraits.projectileDamage(\"" + entity
                + "\", "
                + cutting
                + ", "
                + piercing
                + ", "
                + blunt
                + ")";
        if (entity != null && !EntityList.stringToClassMapping.containsKey(entity)) {
            MineTweakerAPI.logWarning(line + ": no entity of that name is registered now");
        }
        build(
                line,
                CustomDamageRatioEntry.entriesProj,
                entity,
                () -> ItemTraitRules.projectileDamage(entity, cutting, piercing, blunt));
    }

    @ZenMethod
    public static void crafter(IItemStack stack, String type, float efficiency, int tier) {
        String line = "ItemTraits.crafter(" + stack + ", \"" + type + "\", " + efficiency + ", " + tier + ")";
        Item item = item(line, stack);
        if (item != null) {
            build(line, CustomCrafterEntry.entries, item, () -> ItemTraitRules.crafter(item, type, efficiency, tier));
        }
    }

    @ZenMethod
    public static void hoe(IItemStack stack, float efficiency) {
        String line = "ItemTraits.hoe(" + stack + ", " + efficiency + ")";
        Item item = item(line, stack);
        if (item != null) {
            build(line, CustomHoeEntry.entries, item, () -> ItemTraitRules.hoe(item, efficiency));
        }
    }

    @ZenMethod
    public static void clear(IItemStack stack) {
        String line = "ItemTraits.clear(" + stack + ")";
        Item item = item(line, stack);
        if (item == null) {
            return;
        }
        put(line, CustomArmourEntry.entries, item, null);
        put(line, CustomDamageRatioEntry.entries, item, null);
        put(line, CustomCrafterEntry.entries, item, null);
        put(line, CustomHoeEntry.entries, item, null);
    }

    private static Item item(String line, IItemStack stack) {
        ItemStack held = stack == null ? null : MineTweakerMC.getItemStack(stack);
        if (held == null || held.getItem() == null) {
            MineTweakerAPI.logError(line + ": no item. " + ScriptRecipes.NOTHING_CHANGED);
            return null;
        }
        if (held.getItemDamage() != 0 || held.hasTagCompound()) {
            MineTweakerAPI.logWarning(line + ": traits belong to the item, every damage value and tag of it");
        }
        return held.getItem();
    }

    /** Applies the entry the rules build, or reports why they refuse it and changes nothing. */
    private static <K, V> void build(String line, Map<K, V> map, K key, Supplier<V> entry) {
        V value;
        try {
            value = entry.get();
        } catch (IllegalArgumentException e) {
            MineTweakerAPI.logError(line + ": " + e.getMessage() + ". " + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        put(line, map, key, value);
    }

    private static <K, V> void put(String line, Map<K, V> map, K key, V value) {
        MineTweakerAPI.apply(new Put<K, V>(line, map, key, value));
    }

    /** Sets a map entry, or takes it out for null, and on undo puts back what was there. */
    private static final class Put<K, V> implements IUndoableAction {

        private final String line;
        private final Map<K, V> map;
        private final K key;
        private final V value;
        private V before;

        Put(String line, Map<K, V> map, K key, V value) {
            this.line = line;
            this.map = map;
            this.key = key;
            this.value = value;
        }

        @Override
        public void apply() {
            before = map.get(key);
            set(value);
        }

        private void set(V v) {
            if (v == null) {
                map.remove(key);
            } else {
                map.put(key, v);
            }
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            set(before);
        }

        @Override
        public String describe() {
            return line;
        }

        @Override
        public String describeUndo() {
            return "Undoing " + line;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
