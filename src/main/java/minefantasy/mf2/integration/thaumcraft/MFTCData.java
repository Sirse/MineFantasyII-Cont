package minefantasy.mf2.integration.thaumcraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * NBT MF writes onto an infused item. No Thaumcraft code here: the tag must stay readable when the mod is gone.
 */
public class MFTCData {

    public static final String TAG = "MF_TC";
    public static final String SCHEMA = "Schema";
    public static final String EFFECT = "Effect";

    /** Bumped only when the meaning of the existing keys changes, never for a new effect. */
    public static final int SCHEMA_VERSION = 1;

    public static final String EFFECT_REVEALING = "revealing";

    public static boolean hasEffect(ItemStack stack, String effect) {
        NBTTagCompound tag = getTag(stack);
        return tag != null && tag.getInteger(SCHEMA) == SCHEMA_VERSION && effect.equals(tag.getString(EFFECT));
    }

    /** True for any MF effect, so a second infusion can be refused without listing every effect. */
    public static boolean hasAnyEffect(ItemStack stack) {
        NBTTagCompound tag = getTag(stack);
        return tag != null && !tag.getString(EFFECT).isEmpty();
    }

    /** Returns a new stack of size one. The input is never modified: the matrix asks for the output more than once. */
    public static ItemStack withEffect(ItemStack input, String effect) {
        ItemStack result = input.copy();
        result.stackSize = 1;
        if (!result.hasTagCompound()) {
            result.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(SCHEMA, SCHEMA_VERSION);
        tag.setString(EFFECT, effect);
        result.getTagCompound().setTag(TAG, tag);
        return result;
    }

    /** The effect on a stack, or null when it carries none. */
    static String getEffect(ItemStack stack) {
        NBTTagCompound tag = getTag(stack);
        if (tag == null) {
            return null;
        }
        String effect = tag.getString(EFFECT);
        return effect.isEmpty() ? null : effect;
    }

    static NBTTagCompound getTag(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound root = stack.getTagCompound();
        return root.hasKey(TAG) ? root.getCompoundTag(TAG) : null;
    }
}
