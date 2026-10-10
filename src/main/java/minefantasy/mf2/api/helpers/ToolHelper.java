package minefantasy.mf2.api.helpers;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import minefantasy.mf2.api.crafting.CustomCrafterEntry;
import minefantasy.mf2.api.tier.IToolMaterial;
import minefantasy.mf2.api.tool.IToolMF;
import minefantasy.mf2.api.weapon.ISharpenable;

public class ToolHelper {
    // CRAFTER TOOLS//

    public static final String sharpnessLevelNBT = "MF_Sharpness_Level";
    private static String specialItem = "MF_SpecialItemType";

    public static boolean shouldShowTooltip(ItemStack tool) {
        String type = getCrafterTool(tool);

        return type != null && !type.equalsIgnoreCase("nothing") && !type.equalsIgnoreCase("hands");
    }

    public static float getCrafterEfficiency(ItemStack tool) {
        if (tool == null) {
            return 1F;
        }
        if (tool.getItem() instanceof IToolMF) {
            return ((IToolMF) tool.getItem()).getEfficiency(tool);
        }
        return CustomCrafterEntry.getEntryEfficiency(tool);
    }

    // MATERIALS//

    public static int getCrafterTier(ItemStack tool) {
        if (tool == null) {
            return 0;
        }
        if (tool.getItem() instanceof IToolMF) {
            return ((IToolMF) tool.getItem()).getTier(tool);
        }
        return CustomCrafterEntry.getEntryTier(tool);
    }

    // QUALITY//

    /**
     * Wears the held crafting tool by one use, breaking it when it is spent. Only an item that has durability wears:
     * food, armour, a bucket or anything else held without any is never touched, let alone destroyed.
     */
    public static void wearCrafterTool(EntityPlayer user) {
        ItemStack held = user.getHeldItem();
        if (held == null || !held.isItemStackDamageable()) {
            return;
        }
        held.damageItem(1, user);
        if (held.stackSize <= 0 || held.getItemDamage() >= held.getMaxDamage()) {
            if (user.worldObj.isRemote) {
                user.renderBrokenItemStack(held);
            }
            user.destroyCurrentEquippedItem();
        }
    }

    public static String getCrafterTool(ItemStack tool) {
        if (tool == null) {
            return "hands";
        }
        if (tool.getItem() instanceof IToolMF) {
            return ((IToolMF) tool.getItem()).getToolType(tool);
        }
        return CustomCrafterEntry.getEntryType(tool);
    }

    /**
     * Compares an itemstack to see if the item is made from the said material ONLY APPLIES TO IToolMaterial in api.
     */
    public static boolean isItemMaterial(ItemStack stack, ToolMaterial material) {
        if (stack != null) {
            // You need the item to implement this so it can see
            if (stack.getItem() instanceof IToolMaterial) {
                IToolMaterial mat = (IToolMaterial) stack.getItem();

                return mat.getMaterial() != null && mat.getMaterial() == material;
            }
        }
        return false;
    }

    public static int setDuraOnQuality(ItemStack item, int dura) {
        float quality = ItemQuality.get(item);
        dura = (int) ItemQuality.getGrade(item).scale(dura, 2F);

        if (quality > 100) {
            // This means 100+ adds to 2x durability at level 200. Cast spelled out: the compound assignment was
            // already truncating the result, so the value is unchanged.
            dura = (int) (dura + dura / 100F * (quality - 100));
        }
        if (quality < 100) {
            // This means 100- takes upto 75% from it's max durability, lvl0 is 25%dura
            dura = (int) (dura - (dura * 0.75) / 100 * (100F - quality));
        }
        return dura;
    }

    public static float modifyDigOnQuality(ItemStack item, float digspeed) {
        digspeed = ItemQuality.getGrade(item).scale(digspeed, 1.25F);
        float quality = ItemQuality.get(item);

        if (quality > 100) {
            digspeed += ((digspeed * 0.5F) / 100F * (quality - 100));// This means 100+ adds 50% speed at level 200
        }
        if (quality < 100) {
            digspeed -= ((digspeed * 0.5) / 100 * (100F - quality));// This means 100- takes upto 50% from it's max
            // speed lvl0 is 50% speed
        }
        return digspeed;
    }

    // NBT//

    public static float modifyDamOnQuality(ItemStack item, float damage) {
        float quality = ItemQuality.get(item);
        damage = ItemQuality.getGrade(item).scale(damage, 1.25F);
        if (quality > 100) {
            damage += ((damage * 0.25F) / 100F * (quality - 100));// This means 100+ adds 25% damage at level 200
        }
        if (quality < 100) {
            damage -= ((damage * 0.25) / 100 * (100F - quality));// This means 100- takes upto 25% from it's max damage
            // lvl0 is 75% damage
        }
        return damage;
    }

    public static float modifyArmourRating(ItemStack item, float rating) {
        float quality = ItemQuality.get(item);
        rating = ItemQuality.getGrade(item).scale(rating, 1.25F);

        if (quality > 100) {
            rating += ((rating * 0.5F) / 100F * (quality - 100));// This means 100+ adds 50% armour at level 200
        }
        if (quality < 100) {
            rating -= ((rating * 0.5) / 100 * (100F - quality));// This means 100- takes upto 50% from it's max armour
            // lvl0 is 50% armour
        }
        return rating;
    }

    private static NBTTagCompound getOrCreateNBT(ItemStack item) {
        if (!item.hasTagCompound()) {
            item.setTagCompound(new NBTTagCompound());
        }

        return item.getTagCompound();
    }

    public static void setSpecial(ItemStack item, String type) {
        if (!item.hasTagCompound()) {
            item.setTagCompound(new NBTTagCompound());
        }
        item.getTagCompound().setString(specialItem, type);
    }

    public static boolean isSpecial(ItemStack item, String type) {
        return getSpecial(item) != null && getSpecial(item).equals(type);
    }

    public static String getSpecial(ItemStack item) {
        if (item.hasTagCompound() && item.getTagCompound().hasKey(specialItem)) {
            return item.getTagCompound().getString(specialItem);
        }
        return null;
    }

    public static void setToolSharpness(ItemStack item, float level) {
        NBTTagCompound nbt = getOrCreateNBT(item);
        float currentLevel = getSharpnessLevel(item);
        float maxLevel = getMaxSharpness(item);
        nbt.setFloat(sharpnessLevelNBT, Math.min(maxLevel, currentLevel + level));
    }

    public static float getSharpnessLevel(ItemStack item) {
        NBTTagCompound nbt = item.getTagCompound();
        if (nbt != null && nbt.hasKey(sharpnessLevelNBT)) {
            return nbt.getFloat(sharpnessLevelNBT);
        }
        return 0F;
    }

    public static boolean canBeSharpened(ItemStack itemstack, float level) {
        if (itemstack == null) return false;
        return false;
    }

    /**
     * The max amount of uses of sharpness
     */
    public static float getMaxSharpness(ItemStack item) {
        return getSharpnessTraits(item)[0];
    }

    /**
     * The max percent that it can increase damage
     */
    public static float getMaxSharpnessPercent(ItemStack item) {
        return getSharpnessTraits(item)[1];
    }

    /**
     * The modifier for how much percent is added each use
     */
    public static float getSharpUsesModifier(ItemStack item) {
        return getSharpnessTraits(item)[2];
    }

    // maxSharpness, maxBuff, sharpnessModifier
    public static float[] getSharpnessTraits(ItemStack item) {
        float[] list = new float[] { 100, 20F, 3F };

        if (item != null && item.getItem() instanceof ISharpenable) {
            ISharpenable instance = (ISharpenable) item.getItem();
            list[0] = instance.getMaxSharpness(item);
            list[1] = instance.getDamagePercentMax(item);
            list[2] = instance.getSharpUsesModifier(item);
        }
        return list;
    }

    public static String[] breakdownLineForResearchArray(String string) {
        String temp = "";
        ArrayList<String> entries = new ArrayList<String>();

        for (int a = 0; a < string.length(); a++) {
            if (a == string.length() - 1) {
                temp = temp + string.charAt(a);
            }
            if (string.charAt(a) == " ".charAt(0) || a == string.length() - 1) {
                entries.add(temp);
            } else {
                if (string.charAt(a) != " ".charAt(0)) {
                    temp = temp + string.charAt(a);
                }
            }
        }
        int size = entries.size();
        String[] stringList = new String[size];
        for (int i = 0; i < size; i++) {
            stringList[i] = entries.get(i);
        }
        return stringList;
    }

    public static boolean isToolSufficient(ItemStack heldItem, String toolNeeded, int toolTierNeeded) {
        String toolType = getCrafterTool(heldItem);
        int tier = getCrafterTier(heldItem);

        return toolType.equalsIgnoreCase(toolNeeded) && tier >= toolTierNeeded;
    }

    public static void setUnbreakable(ItemStack tool, boolean isUnbreakable) {
        if (!tool.hasTagCompound()) {
            tool.setTagCompound(new NBTTagCompound());
        }
        tool.getTagCompound().setBoolean("Unbreakable", isUnbreakable);
    }
}
