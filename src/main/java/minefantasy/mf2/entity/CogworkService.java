package minefantasy.mf2.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.PowerArmour;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.config.ConfigArmour;
import minefantasy.mf2.item.list.ComponentListMF;

/** Seeing to a cogwork suit by hand: fuelling it, and in a repair frame bolting plating on or spannering it off. */
final class CogworkService {

    private CogworkService() {}

    /** Fuels the suit from the held item, if it burns; whether it was fuel. */
    static boolean refuel(EntityCogwork cog, EntityPlayer user, ItemStack item) {
        float fuelItem = PowerArmour.getFuelValue(item);
        if (fuelItem <= 0) {
            return false;
        }
        float fuel = cog.getFuel();
        float max = cog.getMaxFuel();
        if (fuel < max) {
            cog.setFuel(Math.min(max, fuel + Math.max(0F, fuelItem * ConfigArmour.cogworkFuelUnits)));

            if (!user.capabilities.isCreativeMode) {
                --item.stackSize;

                ItemStack container = item.getItem().getContainerItem(item);
                if (container != null && item.stackSize >= 1) {
                    if (!user.inventory.addItemStackToInventory(container)) {
                        user.entityDropItem(container, 0F);
                    }
                }
                if (item.stackSize <= 0) {
                    user.setCurrentItemOrArmor(0, container);
                }
            }
        }
        return true;
    }

    /**
     * Fits cogwork plating to a bare suit: first each bolt it takes, one a click, then the plating itself. Whether the
     * held item was plating.
     */
    static boolean plate(EntityCogwork cog, EntityPlayer user, ItemStack item) {
        if (cog.getPlating() != null || item.getItem() != ComponentListMF.cogwork_armour) {
            return false;
        }
        CustomMaterial material = CustomToolHelper.getCustomPrimaryMaterial(item);
        if (material == null) {
            return false;
        }
        cog.playSound("mob.horse.armor", 1.0F, 1.0F);
        int boltCount = cog.getBolts();
        if (boltCount < EntityCogwork.maxBolts) {
            if (!user.isSwingInProgress && user.capabilities.isCreativeMode
                    || user.inventory.consumeInventoryItem(ComponentListMF.bolt)) {
                cog.setBolts(boltCount + 1);
            }
            user.swingItem();
            return true;
        }
        cog.setCustomMaterial(material.name);
        float damagePercent = 1F - ((float) item.getItemDamage() / (float) item.getMaxDamage());
        cog.setHealth(cog.getMaxHealth() * damagePercent);
        if (!user.capabilities.isCreativeMode) {
            --item.stackSize;
            if (item.stackSize <= 0) {
                user.setCurrentItemOrArmor(0, null);
            }
        }
        user.swingItem();
        return true;
    }

    /** Takes the plating and its bolts off with a spanner, handing them to the player. Whether it did. */
    static boolean unplate(EntityCogwork cog, EntityPlayer user, ItemStack item) {
        if (cog.getPlating() == null || !ToolHelper.getCrafterTool(item).equalsIgnoreCase("spanner")) {
            return false;
        }
        cog.playSound("mob.horse.armor", 1.2F, 1.0F);
        user.swingItem();
        int boltCount = cog.getBolts();
        if (boltCount > 0) {
            give(cog, user, new ItemStack(ComponentListMF.bolt, boltCount));
            cog.setBolts(0);
        }
        float damagePercent = 1F - (cog.getHealth() / cog.getMaxHealth());
        if (!cog.worldObj.isRemote) {
            give(cog, user, ComponentListMF.cogwork_armour.createComm(cog.getPlating().name, 1, damagePercent));
        }
        cog.playSound("mob.irongolem.hit", 1.0F, 1.0F);
        cog.setCustomMaterial("");
        return true;
    }

    /** Hands a part back to a survival player, dropping it by the suit if their inventory is full. */
    private static void give(EntityCogwork cog, EntityPlayer user, ItemStack part) {
        if (!cog.worldObj.isRemote && !user.capabilities.isCreativeMode
                && !user.inventory.addItemStackToInventory(part)) {
            cog.entityDropItem(part, 0.0F);
        }
    }
}
