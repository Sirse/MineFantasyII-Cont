package minefantasy.mf2.api.helpers;

import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;

import minefantasy.mf2.api.armour.ArmourDesign;
import minefantasy.mf2.api.armour.CustomArmourEntry;
import minefantasy.mf2.api.armour.CustomDamageRatioEntry;
import minefantasy.mf2.api.armour.IArmourMF;
import minefantasy.mf2.api.crafting.CustomCrafterEntry;
import minefantasy.mf2.api.farming.CustomHoeEntry;
import minefantasy.mf2.api.tool.IToolMF;
import minefantasy.mf2.api.weapon.IDamageType;

/**
 * Checks and builds the traits other mods' items are given, by the config and by scripts alike: each method returns an
 * entry not yet registered, or throws an {@link IllegalArgumentException} saying what is wrong.
 */
public final class ItemTraitRules {

    private ItemTraitRules() {}

    /** Armour by a design: its suit weight times the modifier, and its bulk shared out to the slot. */
    public static CustomArmourEntry armour(Item piece, String design, float weightModifier, String group) {
        notOwn(piece instanceof IArmourMF);
        ArmourDesign template = design == null ? null : ArmourDesign.designs.get(design);
        if (template == null) {
            throw new IllegalArgumentException(
                    "no armour design '" + design + "'; there are " + ArmourDesign.designs.keySet());
        }
        if (!finite(weightModifier) || weightModifier <= 0F || weightModifier > 100F) {
            throw new IllegalArgumentException("the weight modifier is above 0 to 100");
        }
        String kind = group(group == null ? template.getGroup() : group);
        float share = CustomArmourEntry.slotShare(piece);
        return CustomArmourEntry.of(piece, template.getWeight() * weightModifier, template.getBulk() * share, kind);
    }

    /** Armour by the weight it has on the body and the bulk of a full suit. */
    public static CustomArmourEntry wornArmour(Item piece, float kilograms, float suitBulk, String group) {
        notOwn(piece instanceof IArmourMF);
        if (!(piece instanceof ItemArmor)) {
            throw new IllegalArgumentException("only armour is worn; give other items a design");
        }
        if (!finite(kilograms) || kilograms < 0F
                || kilograms > 1000F
                || !finite(suitBulk)
                || suitBulk < 0F
                || suitBulk > 100F) {
            throw new IllegalArgumentException("the weight is 0 to 1000 kg and the bulk 0 to 100");
        }
        String kind = group(group);
        int slot = ((ItemArmor) piece).armorType;
        if (slot < 0 || slot >= ArmourCalculator.sizes.length) {
            throw new IllegalArgumentException(
                    "the item sits in armour slot " + slot + ", not one of the four worn ones");
        }
        float share = ArmourCalculator.sizes[slot];
        return CustomArmourEntry.of(piece, kilograms / share, suitBulk * share, kind);
    }

    public static CustomDamageRatioEntry damage(Item weapon, float cutting, float piercing, float blunt) {
        notOwn(weapon instanceof IDamageType);
        return ratio(cutting, piercing, blunt);
    }

    public static CustomDamageRatioEntry projectileDamage(String entity, float cutting, float piercing, float blunt) {
        if (entity == null || entity.trim().isEmpty()) {
            throw new IllegalArgumentException("no entity named");
        }
        return ratio(cutting, piercing, blunt);
    }

    public static CustomCrafterEntry crafter(Item tool, String type, float efficiency, int tier) {
        notOwn(tool instanceof IToolMF);
        if (type == null || type.isEmpty()
                || !type.equals(type.trim())
                || "nothing".equals(type)
                || "hands".equals(type)) {
            throw new IllegalArgumentException("the tool type is a name such as hammer or knife");
        }
        if (!finite(efficiency) || efficiency <= 0F || efficiency > 1000F || tier < -1 || tier > 100) {
            throw new IllegalArgumentException("the efficiency is above 0 to 1000 and the tier -1 to 100");
        }
        return CustomCrafterEntry.of(tool, type, efficiency, tier);
    }

    public static CustomHoeEntry hoe(Item hoe, float efficiency) {
        if (!finite(efficiency) || efficiency <= 0F || efficiency > 1000F) {
            throw new IllegalArgumentException("the efficiency is above 0 to 1000");
        }
        return CustomHoeEntry.of(hoe, efficiency);
    }

    /** Cutting, piercing, blunt, kept as the entries keep them: cutting, blunt, piercing. */
    private static CustomDamageRatioEntry ratio(float cutting, float piercing, float blunt) {
        if (!finite(cutting) || !finite(piercing)
                || !finite(blunt)
                || cutting < 0F
                || piercing < 0F
                || blunt < 0F
                || cutting + piercing + blunt <= 0F
                || cutting + piercing + blunt > 1000F) {
            throw new IllegalArgumentException("the ratio parts are 0 or more, and not all 0");
        }
        return CustomDamageRatioEntry.of(new float[] { cutting, blunt, piercing });
    }

    private static String group(String group) {
        if (ArmourDesign.light.equals(group) || "medium".equals(group) || ArmourDesign.heavy.equals(group)) {
            return group;
        }
        throw new IllegalArgumentException("the armour class is light, medium or heavy");
    }

    private static void notOwn(boolean own) {
        if (own) {
            throw new IllegalArgumentException("the item sets these traits itself, so they would never be read");
        }
    }

    private static boolean finite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
