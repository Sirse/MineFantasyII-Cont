package minefantasy.mf2.api.weapon;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public interface IKnockbackWeapon {
    float getAddedKnockback(EntityLivingBase user, ItemStack item);
}
