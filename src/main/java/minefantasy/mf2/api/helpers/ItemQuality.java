package minefantasy.mf2.api.helpers;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * How well an item was made, kept in its NBT: a quality from {@link #MIN} to {@link #MAX}, {@link #ORDINARY} when
 * unset, and a grade, inferior or superior, that scales its stats on top. Only items that do not stack have either.
 */
public final class ItemQuality {

    /** NBT key of the quality; its value is kept for saved worlds. */
    public static final String QUALITY_KEY = "MFCraftQuality";
    /** NBT key of the grade: true for inferior, false for superior, absent for ordinary. */
    public static final String INFERIOR_KEY = "MF_Inferior";

    public static final float MIN = 0F;
    public static final float ORDINARY = 100F;
    public static final float MAX = 200F;

    /** The grade of an item, besides its quality. */
    public enum Grade {

        ORDINARY,
        INFERIOR,
        SUPERIOR;

        /** Scales a stat by the grade: divided by the factor when inferior, multiplied when superior. */
        public float scale(float value, float factor) {
            switch (this) {
                case INFERIOR:
                    return value / factor;
                case SUPERIOR:
                    return value * factor;
                default:
                    return value;
            }
        }
    }

    private ItemQuality() {}

    /** Whether the item can have a quality and grade: only one that does not stack. */
    public static boolean applies(ItemStack item) {
        return item != null && item.getMaxStackSize() == 1;
    }

    public static boolean hasQuality(ItemStack item) {
        return item != null && item.hasTagCompound() && item.getTagCompound().hasKey(QUALITY_KEY);
    }

    /** The quality; {@link #ORDINARY} when unset or for an item that stacks. */
    public static float get(ItemStack item) {
        return applies(item) && hasQuality(item) ? item.getTagCompound().getFloat(QUALITY_KEY) : ORDINARY;
    }

    /** Sets the quality; an item that stacks is left alone. */
    public static void set(ItemStack item, float quality) {
        if (applies(item)) {
            tag(item).setFloat(QUALITY_KEY, quality);
        }
    }

    public static Grade getGrade(ItemStack item) {
        if (item == null || !item.hasTagCompound() || !item.getTagCompound().hasKey(INFERIOR_KEY)) {
            return Grade.ORDINARY;
        }
        return item.getTagCompound().getBoolean(INFERIOR_KEY) ? Grade.INFERIOR : Grade.SUPERIOR;
    }

    /** Sets the grade; an ordinary one takes the mark off, so nothing is left scaling the stats. */
    public static void setGrade(ItemStack item, Grade grade) {
        if (!applies(item)) {
            return;
        }
        if (grade == Grade.ORDINARY) {
            if (item.hasTagCompound()) {
                item.getTagCompound().removeTag(INFERIOR_KEY);
            }
        } else {
            tag(item).setBoolean(INFERIOR_KEY, grade == Grade.INFERIOR);
        }
    }

    private static NBTTagCompound tag(ItemStack item) {
        if (!item.hasTagCompound()) {
            item.setTagCompound(new NBTTagCompound());
        }
        return item.getTagCompound();
    }
}
