package minefantasy.mf2.block.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import minefantasy.mf2.api.crafting.exotic.SpecialForging;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.IHotItem;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.ItemQuality;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.entity.EntityItemUnbreakable;
import minefantasy.mf2.item.armour.ItemArmourMF;
import minefantasy.mf2.item.heatable.ItemHeated;
import minefantasy.mf2.mechanics.PlayerTickHandlerMF;

/**
 * What an anvil makes of the product once the work is done: dragonforged or special designs, the colour and wear of
 * armour laid in the grid, the smith's name, the grade the hits earned, and the heat the metal kept.
 */
final class AnvilFinish {

    private AnvilFinish() {}

    /** The finished item for the product, as the grid and the hits on the anvil leave it. */
    static ItemStack finish(TileEntityAnvilMF anvil, ItemStack product, String smith, boolean hotOutput) {
        ItemStack result = specials(anvil, product, smith);
        if (result.getItem() instanceof ItemArmourMF) {
            result = armour(anvil, result);
        }
        if (result.getMaxStackSize() == 1 && !smith.isEmpty()) {
            tag(result).setString("MF_CraftedByName", smith);
        }
        int temp = averageTemp(anvil);
        if (hotOutput && temp > 0) {
            result = ItemHeated.createHotItem(result, temp);
        }
        return result;
    }

    /** Armour takes the dye and the wear of the armour laid in the grid. */
    private static ItemStack armour(TileEntityAnvilMF anvil, ItemStack result) {
        ItemArmourMF item = (ItemArmourMF) result.getItem();
        boolean canColour = item.canColour();
        int colour = -1;
        for (int a = 0; a < anvil.getSizeInventory() - 1; a++) {
            ItemStack slot = anvil.getStackInSlot(a);
            if (slot != null && slot.getItem() instanceof ItemArmor) {
                ItemArmor slotitem = (ItemArmor) slot.getItem();
                if (canColour && slotitem.hasColor(slot)) {
                    colour = slotitem.getColor(slot);
                }
                if (result.isItemStackDamageable()) {
                    result.setItemDamage(slot.getItemDamage());
                }
            }
        }
        if (colour != -1 && canColour) {
            item.func_82813_b(result, colour);
        }
        return result;
    }

    private static ItemStack specials(TileEntityAnvilMF anvil, ItemStack result, String smith) {
        boolean isTool = result.getMaxStackSize() == 1 && result.isItemStackDamageable();
        EntityPlayer player = anvil.getWorldObj().getPlayerEntityByName(smith);

        Item dragon = SpecialForging.getDragonCraft(result);
        TileEntityForge forge = dragon != null ? anvil.heartedForge(player) : null;
        if (forge != null) {
            // DRAGONFORGE: one heart per craft, however many forges nearby hold one
            forge.dragonHeartPower = 0;
            anvil.getWorldObj().createExplosion(player, forge.xCoord, forge.yCoord, forge.zCoord, 1F, false);
            PlayerTickHandlerMF.spawnDragon(player);
            PlayerTickHandlerMF.addDragonEnemyPts(player, 2);
            result = retype(result, dragon);
        } else {
            ItemStack design = anvil.getStackInSlot(anvil.getSizeInventory() - 1);
            Item special = SpecialForging.getSpecialCraft(SpecialForging.getItemDesign(design), result);
            if (special != null) {
                result = retype(result, special);
            }
        }

        if (isPerfect(anvil)) {
            grade(result, ItemQuality.Grade.SUPERIOR);
            if (CustomToolHelper.isMythic(result)) {
                ToolHelper.setUnbreakable(result, true);
                result.getTagCompound().setBoolean(EntityItemUnbreakable.persistNBT, true);
            } else {
                ItemQuality.set(result, ItemQuality.MAX);
            }
            return result;
        }
        if (isTool) {
            result = qualityComponents(anvil, result);
        }
        return damage(anvil, result);
    }

    /** The same stack as another item, keeping its size, damage and tag. */
    private static ItemStack retype(ItemStack stack, Item item) {
        ItemStack result = new ItemStack(item, stack.stackSize, stack.getItemDamage());
        if (stack.hasTagCompound()) {
            result.setTagCompound((NBTTagCompound) stack.getTagCompound().copy());
        }
        return result;
    }

    /** Superior parts in the grid lift the quality of a tool; inferior ones only count against the average. */
    private static ItemStack qualityComponents(TileEntityAnvilMF anvil, ItemStack result) {
        float totalPts = 0F;
        int totalItems = 0;
        for (int a = 0; a < anvil.getSizeInventory(); a++) {
            ItemQuality.Grade grade = ItemQuality.getGrade(anvil.getStackInSlot(a));
            if (grade != ItemQuality.Grade.ORDINARY) {
                ++totalItems;
                totalPts += grade == ItemQuality.Grade.INFERIOR ? -50F : 100F;
            }
        }
        if (totalItems > 0 && totalPts > 0) {
            totalPts /= totalItems;
            ItemQuality.set(result, ItemQuality.get(result) + totalPts);
            if (totalPts <= -85F) {
                grade(result, ItemQuality.Grade.INFERIOR);
            }
            if (totalPts >= 80) {
                grade(result, ItemQuality.Grade.SUPERIOR);
            }
        }
        return result;
    }

    private static int averageTemp(TileEntityAnvilMF anvil) {
        float totalTemp = 0;
        int itemCount = 0;
        for (int a = 0; a < anvil.getSizeInventory() - 1; a++) {
            ItemStack item = anvil.getStackInSlot(a);
            if (item != null && item.getItem() instanceof IHotItem) {
                ++itemCount;
                totalTemp += Heatable.getTemp(item);
            }
        }
        if (totalTemp > 0 && itemCount > 0) {
            return (int) (totalTemp / itemCount);
        }
        return 0;
    }

    private static NBTTagCompound tag(ItemStack item) {
        if (!item.hasTagCompound()) {
            item.setTagCompound(new NBTTagCompound());
        }
        return item.getTagCompound();
    }

    /** How far past the hit window the balance ended, from 0 inside it to 1 at the edge of ruin. */
    private static float wear(TileEntityAnvilMF anvil) {
        int threshold = (int) (100F * anvil.getThresholdPosition() / 2F);
        int total = (int) (100F * anvil.getAbsoluteBalance() - threshold);
        if (total > threshold) {
            return ((float) total - (float) threshold) / (100F - threshold);
        }
        return 0F;
    }

    private static boolean isPerfect(TileEntityAnvilMF anvil) {
        int threshold = (int) (100F * anvil.getSuperThresholdPosition() / 2F);
        int total = (int) (100F * anvil.getAbsoluteBalance() - threshold);
        return total <= threshold;
    }

    /** Hits off the window leave the item worn, and far off it inferior. */
    private static ItemStack damage(TileEntityAnvilMF anvil, ItemStack item) {
        float wear = wear(anvil);
        if (wear > 0.5F) {
            grade(item, ItemQuality.Grade.INFERIOR);
            float q = 100F * (0.75F - (wear - 0.5F));
            ItemQuality.set(item, Math.max(10F, q));
        }
        float damage = wear * item.getMaxDamage();
        if (item.isItemStackDamageable() && damage > 0) {
            item.setItemDamage((int) (damage));
        }
        return item;
    }

    /** Grades a forged item; only one that wears down has a grade. */
    private static void grade(ItemStack item, ItemQuality.Grade grade) {
        if (item != null && item.isItemStackDamageable()) {
            ItemQuality.setGrade(item, grade);
        }
    }
}
