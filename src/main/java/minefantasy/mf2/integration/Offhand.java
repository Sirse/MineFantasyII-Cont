package minefantasy.mf2.integration;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.Loader;
import mods.battlegear2.api.shield.IShield;
import xonin.backhand.api.core.BackhandUtils;

/** The offhand item, which only Backhand gives players; without it there is none. */
public final class Offhand {

    private Offhand() {}

    public static ItemStack item(EntityLivingBase user) {
        if (!(user instanceof EntityPlayer) || !Loader.isModLoaded("backhand")) {
            return null;
        }
        return BackhandUtils.getOffhandItem((EntityPlayer) user);
    }

    /** Whether the item is a shield: only Battlegear has them. */
    public static boolean isShield(ItemStack item) {
        return item != null && Loader.isModLoaded("battlegear2") && isBattlegearShield(item);
    }

    /** Kept apart so the Battlegear interface is only loaded when the mod is there. */
    private static boolean isBattlegearShield(ItemStack item) {
        return item.getItem() instanceof IShield;
    }
}
