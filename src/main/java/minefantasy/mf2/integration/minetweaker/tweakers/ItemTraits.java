package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.Map;

import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.armour.ArmourDesign;
import minefantasy.mf2.api.armour.CustomArmourEntry;
import minefantasy.mf2.api.armour.CustomDamageRatioEntry;
import minefantasy.mf2.api.armour.IArmourMF;
import minefantasy.mf2.api.crafting.CustomCrafterEntry;
import minefantasy.mf2.api.farming.CustomHoeEntry;
import minefantasy.mf2.api.helpers.ArmourCalculator;
import minefantasy.mf2.api.tool.IToolMF;
import minefantasy.mf2.api.weapon.IDamageType;
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
    public static void armour(IItemStack stack, String design, @Optional float weight, @Optional String group) {
        String line = "ItemTraits.armour(" + stack + ", \"" + design + "\", ...)";
        Item item = item(line, stack);
        if (item == null || !notOwn(line, item, item instanceof IArmourMF)) {
            return;
        }
        ArmourDesign template = design == null ? null : ArmourDesign.designs.get(design);
        if (template == null) {
            MineTweakerAPI.logError(
                    line + ": no armour design of that name; there are "
                            + ArmourDesign.designs.keySet()
                            + ". "
                            + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        float scale = weight == 0F ? 1F : weight;
        if (!finite(scale) || scale <= 0F || scale > 100F) {
            MineTweakerAPI.logError(
                    line + ": the weight is a share of the design's, above 0 to 100. " + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        String kind = group == null ? template.getGroup() : group;
        if (!group(line, kind)) {
            return;
        }
        float share = item instanceof ItemArmor ? ArmourCalculator.sizes[((ItemArmor) item).armorType] : 0.25F;
        // As CustomArmourEntry.registerItem(piece, template, ...) works it out
        put(
                line,
                CustomArmourEntry.entries,
                item,
                CustomArmourEntry.of(item, template.getWeight() * scale, template.getBulk() * share, kind));
    }

    @ZenMethod
    public static void wornArmour(IItemStack stack, float kilograms, float suitBulk, String group) {
        String line = "ItemTraits.wornArmour(" + stack + ", ...)";
        Item item = item(line, stack);
        if (item == null || !notOwn(line, item, item instanceof IArmourMF) || !group(line, group)) {
            return;
        }
        if (!(item instanceof ItemArmor)) {
            MineTweakerAPI.logError(
                    line + ": only armour is worn; use armour with a design. " + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        if (!finite(kilograms) || kilograms < 0F
                || kilograms > 1000F
                || !finite(suitBulk)
                || suitBulk < 0F
                || suitBulk > 100F) {
            MineTweakerAPI.logError(
                    line + ": the weight is 0 to 1000 kg and the bulk 0 to 100. " + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        float share = ArmourCalculator.sizes[((ItemArmor) item).armorType];
        put(
                line,
                CustomArmourEntry.entries,
                item,
                CustomArmourEntry.of(item, kilograms / share, suitBulk * share, group));
    }

    @ZenMethod
    public static void damage(IItemStack stack, float cutting, float piercing, float blunt) {
        String line = "ItemTraits.damage(" + stack + ", " + cutting + ", " + piercing + ", " + blunt + ")";
        Item item = item(line, stack);
        if (item == null || !notOwn(line, item, item instanceof IDamageType)
                || !ratio(line, cutting, piercing, blunt)) {
            return;
        }
        put(
                line,
                CustomDamageRatioEntry.entries,
                item,
                CustomDamageRatioEntry.of(new float[] { cutting, blunt, piercing }));
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
        if (entity == null || entity.isEmpty()) {
            MineTweakerAPI.logError(line + ": no entity named. " + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        if (!ratio(line, cutting, piercing, blunt)) {
            return;
        }
        if (!EntityList.stringToClassMapping.containsKey(entity)) {
            MineTweakerAPI.logWarning(line + ": no entity of that name is registered now");
        }
        put(
                line,
                CustomDamageRatioEntry.entriesProj,
                entity,
                CustomDamageRatioEntry.of(new float[] { cutting, blunt, piercing }));
    }

    @ZenMethod
    public static void crafter(IItemStack stack, String type, float efficiency, int tier) {
        String line = "ItemTraits.crafter(" + stack + ", \"" + type + "\", " + efficiency + ", " + tier + ")";
        Item item = item(line, stack);
        if (item == null || !notOwn(line, item, item instanceof IToolMF)) {
            return;
        }
        if (type == null || type.isEmpty()
                || !type.equals(type.trim())
                || "nothing".equals(type)
                || "hands".equals(type)) {
            MineTweakerAPI.logError(
                    line + ": the tool type is a name such as hammer or knife. " + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        if (!finite(efficiency) || efficiency <= 0F || efficiency > 1000F || tier < -1 || tier > 100) {
            MineTweakerAPI.logError(
                    line + ": the efficiency is above 0 to 1000 and the tier -1 to 100. "
                            + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        put(line, CustomCrafterEntry.entries, item, CustomCrafterEntry.of(item, type, efficiency, tier));
    }

    @ZenMethod
    public static void hoe(IItemStack stack, float efficiency) {
        String line = "ItemTraits.hoe(" + stack + ", " + efficiency + ")";
        Item item = item(line, stack);
        if (item == null) {
            return;
        }
        if (!finite(efficiency) || efficiency <= 0F || efficiency > 1000F) {
            MineTweakerAPI.logError(line + ": the efficiency is above 0 to 1000. " + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        put(line, CustomHoeEntry.entries, item, CustomHoeEntry.of(item, efficiency));
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

    private static boolean notOwn(String line, Item item, boolean own) {
        if (own) {
            MineTweakerAPI.logError(
                    line + ": the item sets these traits itself, so they would never be read. "
                            + ScriptRecipes.NOTHING_CHANGED);
        }
        return !own;
    }

    private static boolean group(String line, String group) {
        if (ArmourDesign.light.equals(group) || "medium".equals(group) || ArmourDesign.heavy.equals(group)) {
            return true;
        }
        MineTweakerAPI
                .logError(line + ": the armour class is light, medium or heavy. " + ScriptRecipes.NOTHING_CHANGED);
        return false;
    }

    /** The entries keep cutting, blunt, piercing. */
    private static boolean ratio(String line, float cutting, float piercing, float blunt) {
        if (finite(cutting) && finite(piercing)
                && finite(blunt)
                && cutting >= 0F
                && piercing >= 0F
                && blunt >= 0F
                && cutting + piercing + blunt > 0F
                && cutting + piercing + blunt <= 1000F) {
            return true;
        }
        MineTweakerAPI.logError(line + ": the parts are 0 or more, and not all 0. " + ScriptRecipes.NOTHING_CHANGED);
        return false;
    }

    private static boolean finite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
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
