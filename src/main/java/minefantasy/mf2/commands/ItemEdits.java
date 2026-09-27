package minefantasy.mf2.commands;

import java.util.Locale;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.ItemQuality;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;

/**
 * Changes to a held item for {@code /mf edit}, without the chat: each checks the change applies, makes it and says how
 * it went, as a translation key with its arguments.
 */
final class ItemEdits {

    /** Quality at or below this makes the item inferior; at or above {@link #SUPERIOR}, superior. */
    static final int INFERIOR = 50;
    static final int SUPERIOR = 150;

    /** Where an item keeps a material: the head, or a wooden part on its own, and the haft. */
    enum Slot {

        MAIN(CustomToolHelper.slot_main),
        HAFT(CustomToolHelper.slot_haft);

        final String key;

        Slot(String key) {
            this.key = key;
        }

        String getName() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Slot byName(String name) {
            for (Slot slot : values()) {
                if (slot.getName().equals(name.toLowerCase(Locale.ROOT))) {
                    return slot;
                }
            }
            return null;
        }
    }

    /** How an edit went. */
    static final class Result {

        final boolean success;
        final String key;
        final Object[] args;

        private Result(boolean success, String key, Object... args) {
            this.success = success;
            this.key = key;
            this.args = args;
        }

        static Result done(String key, Object... args) {
            return new Result(true, key, args);
        }

        static Result refused(String key, Object... args) {
            return new Result(false, key, args);
        }
    }

    private ItemEdits() {}

    /**
     * Gives the item the material in a slot: the one named, else the slot already holding a material of the same type,
     * so a wooden part changes its wood and a tool its head or haft as the material fits.
     *
     * @param slot the slot, or null to find it
     */
    static Result material(ItemStack item, String name, Slot slot) {
        if (!CustomToolHelper.hasAnyMaterial(item)) {
            return Result.refused("command.mf.edit.no_materials");
        }
        CustomMaterial material = CustomMaterial.getMaterial(name);
        if (material == null) {
            return Result.refused("command.mf.edit.unknown_material", name);
        }
        if (slot == null) {
            slot = slotFor(item, material);
            if (slot == null) {
                return Result.refused("command.mf.edit.which_slot", material.getName(), material.type);
            }
        } else if (CustomMaterial.getMaterialFor(item, slot.key) == null) {
            return Result.refused("command.mf.edit.no_slot", slot.getName());
        }
        CustomMaterial.addMaterial(item, slot.key, material.getName());
        return Result.done("command.mf.edit.material", slot.getName(), material.getName());
    }

    /** The slot holding a material of the same type as the given one; null when none or both do. */
    static Slot slotFor(ItemStack item, CustomMaterial material) {
        Slot found = null;
        for (Slot slot : Slot.values()) {
            CustomMaterial held = CustomMaterial.getMaterialFor(item, slot.key);
            if (held != null && held.type.equalsIgnoreCase(material.type)) {
                if (found != null) {
                    return null;
                }
                found = slot;
            }
        }
        return found;
    }

    /**
     * Sets how well the item was made, 0 to 200 with 100 ordinary: low quality makes it inferior and high superior, and
     * in between it is neither, whatever it was before.
     */
    static Result quality(ItemStack item, int quality) {
        if (!ItemQuality.applies(item)) {
            return Result.refused("command.mf.edit.stackable");
        }
        ItemQuality.set(item, quality);
        ItemQuality.setGrade(
                item,
                quality <= INFERIOR ? ItemQuality.Grade.INFERIOR
                        : quality >= SUPERIOR ? ItemQuality.Grade.SUPERIOR : ItemQuality.Grade.ORDINARY);
        return Result.done("command.mf.edit.quality", quality);
    }

    static Result unbreakable(ItemStack item, boolean unbreakable) {
        ToolHelper.setUnbreakable(item, unbreakable);
        return Result.done(unbreakable ? "command.mf.edit.unbreakable" : "command.mf.edit.breakable");
    }
}
