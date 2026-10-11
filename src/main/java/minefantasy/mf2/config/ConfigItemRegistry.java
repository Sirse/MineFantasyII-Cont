package minefantasy.mf2.config;

import java.util.Arrays;
import java.util.regex.Pattern;

import net.minecraft.item.Item;

import minefantasy.mf2.api.armour.CustomArmourEntry;
import minefantasy.mf2.api.armour.CustomDamageRatioEntry;
import minefantasy.mf2.api.crafting.CustomCrafterEntry;
import minefantasy.mf2.api.farming.CustomHoeEntry;
import minefantasy.mf2.api.helpers.ItemTraitRules;
import minefantasy.mf2.util.MFLogUtil;

public class ConfigItemRegistry extends ConfigurationBaseMF {

    public static final String CATEGORY_ARMOUR = "Armour List";
    public static final String CATEGORY_FARM = "Farming";
    public static final String CATEGORY_TOOL = "Tools";
    public static final String CATEGORY_WEPS = "Weapon Register";
    public static String[] armourListAC = new String[0];
    public static String[] hoeList = new String[0];
    public static String[] crafterList = new String[0];
    public static String[] customDamagerList = new String[0];
    public static String[] customDamagerEntityList = new String[0];

    /** Reads every list, line by line: a bad line is named in the log and skipped, the rest still count. */
    public static void readCustoms() {
        MFLogUtil.logDebug("Loading Custom Item Entries from config...");
        for (String line : armourListAC) {
            read("Auto-Armour Registry", line, 4, ConfigItemRegistry::armour);
        }
        for (String line : hoeList) {
            read("Hoe Registry", line, 2, ConfigItemRegistry::hoe);
        }
        for (String line : crafterList) {
            read("Crafter Registry", line, 4, ConfigItemRegistry::crafter);
        }
        for (String line : customDamagerList) {
            read("Custom Damage Ratios", line, 4, ConfigItemRegistry::damage);
        }
        for (String line : customDamagerEntityList) {
            read("Custom Entity Damage Ratios", line, 4, ConfigItemRegistry::entityDamage);
        }
    }

    private interface Entry {

        /** Registers the parts of a line, or says what is wrong with it. */
        void register(String[] parts);
    }

    private static void read(String list, String line, int count, Entry entry) {
        String[] parts = line.split(Pattern.quote("|"), -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        try {
            if (parts.length != count) {
                throw new IllegalArgumentException("needs " + count + " parts separated by |");
            }
            entry.register(parts);
        } catch (IllegalArgumentException e) {
            MFLogUtil.logWarn("Skipped '" + line + "' in " + list + ": " + e.getMessage());
        }
    }

    private static void armour(String[] parts) {
        Item piece = item(parts[0]);
        CustomArmourEntry.entries.put(piece, ItemTraitRules.armour(piece, parts[1], number(parts[3]), parts[2]));
    }

    private static void hoe(String[] parts) {
        Item hoe = item(parts[0]);
        CustomHoeEntry.entries.put(hoe, ItemTraitRules.hoe(hoe, number(parts[1])));
    }

    private static void crafter(String[] parts) {
        Item tool = item(parts[0]);
        float tier = number(parts[2]);
        if (tier != (int) tier) {
            throw new IllegalArgumentException("the tier is a whole number");
        }
        CustomCrafterEntry.entries.put(tool, ItemTraitRules.crafter(tool, parts[3], number(parts[1]), (int) tier));
    }

    private static void damage(String[] parts) {
        Item weapon = item(parts[0]);
        CustomDamageRatioEntry.entries
                .put(weapon, ItemTraitRules.damage(weapon, number(parts[1]), number(parts[2]), number(parts[3])));
    }

    private static void entityDamage(String[] parts) {
        CustomDamageRatioEntry.entriesProj.put(
                parts[0],
                ItemTraitRules.projectileDamage(parts[0], number(parts[1]), number(parts[2]), number(parts[3])));
    }

    private static Item item(String id) {
        Object object = Item.itemRegistry.getObject(id);
        if (!(object instanceof Item)) {
            throw new IllegalArgumentException("no item '" + id + "'");
        }
        return (Item) object;
    }

    private static float number(String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a number");
        }
    }

    @Override
    protected void loadConfig() {
        // Weight

        String AAdesc = "This will register items under a certain 'Design' calculating the variables itself.\n Each entry has its own line:\n"
                + "Order itemid|Design|WeightGroup|WeightModifier \n"
                + "The WeightModifier alters the weight for heavier or lighter materials keep it at 1.0 unless you have a special material (like mithril and adamantium)\n"
                + "Designs can be any that are registered: MineFantasy designs are for example 'clothing', 'leather', 'mail', 'default' (basic metal armour) and 'plate'; a bad name is reported in the log\n"
                + "WeightGroup refers to whether it is light medium or heavy armour \n"
                + "EXAMPLE (This is what vanilla gold is registered under) \n"
                + "minecraft:golden_helmet|default|medium|2.0 \n"
                + "minecraft:golden_chestplate|default|medium|2.0 \n"
                + "minecraft:golden_leggings|default|medium|2.0 \n"
                + "minecraft:golden_boots|default|medium|2.0 \n"
                + "The 2.0 means it is 2x heavier than other vanilla armours \n"
                + "This does not override existing MF armours \n";

        armourListAC = config.get(CATEGORY_ARMOUR, "Auto-Armour Registry", new String[0], AAdesc).getStringList();
        Arrays.sort(armourListAC);

        // Hoes

        String hoeDesc = "This Registers Hoe items to an efficiency level: (It uses the same variable as efficiency, you may need to find that out first, by default: it should be able to guess it:\n"
                + "Order itemid|efficiency \n"
                + "Efficiency is a variable that goes into play with the failure chance, higher efficiency has easier tiling\n";

        hoeList = config.get(CATEGORY_FARM, "Hoe Registry", new String[0], hoeDesc).getStringList();
        Arrays.sort(hoeList);

        // Crafters

        String craftDesc = "This Registers items to a tool type and efficiency (such as hammer, hvyHammer, knife, saw, etc):\n"
                + "Order itemid|efficiency|tier|tooltype \n"
                + "tooltype can be hammer, hvyHammer, knife, shears, needle, spoon, mallet, saw, spanner \n"
                + "efficiency is the measure of how fast it works (similar to dig speed)";

        crafterList = config.get(CATEGORY_TOOL, "Crafter Registry", new String[0], craftDesc).getStringList();
        Arrays.sort(crafterList);

        // Weapons

        String damageDesc = "[Experimental] This registers weapons for the damage variable mechanics implemented by option. \n"
                + "Though mod-added armours have absolutely no support, and never can without being specifically coded to \n"
                + "MineFantasy armours will take these variables and function differently on the values. But weapon items can \n"
                + "be added to the list: Put each entry on its own line set out like this: \n"
                + "id|cutting|pierce|blunt \n"
                + "id is the item id as a string (you need to find it out yourself), cutting, pierce and blunt are the ratio. \n"
                + "EXAMPLE (for example... making a stick to piercing damage) \n"
                + "minecraft:stick|0|1.0|0 \n"
                + "Only the ratio matters: 1|0|0 is all cutting, 3|0|1 is 3 cutting to 1 blunt (75%, 25%).";

        customDamagerList = config.get(CATEGORY_WEPS, "Custom Damage Ratios", new String[0], damageDesc)
                .getStringList();
        Arrays.sort(customDamagerList);

        String damageEDesc = "Similar method to 'Custom Damage Ratios' only with entities, This is for registering things like arrows, same format only with the entity registry name (as /summon takes it, usually modid.Name)";

        customDamagerEntityList = config.get(CATEGORY_WEPS, "Custom Entity Damage Ratios", new String[0], damageEDesc)
                .getStringList();
        Arrays.sort(customDamagerEntityList);
    }
}
