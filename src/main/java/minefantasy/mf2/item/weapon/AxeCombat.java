package minefantasy.mf2.item.weapon;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import minefantasy.mf2.api.helpers.TacticalManager;

/** Shared axe attacks; callers supply their own base damage and ability costs. */
final class AxeCombat {

    private AxeCombat() {}

    static void brutalise(EntityLivingBase entityHitting, EntityLivingBase entityHit, float power) {
        if (!entityHit.worldObj.isRemote) {
            if (entityHitting instanceof EntityPlayer) {
                ((EntityPlayer) entityHitting).onCriticalHit(entityHit);
            }
        }
    }

    static float modifyDamage(ItemWeaponMF weapon, EntityLivingBase wielder, Entity hit, float damage,
            boolean properHit) {
        if (!(hit instanceof EntityLivingBase)) {
            return damage;
        }
        EntityLivingBase target = (EntityLivingBase) hit;

        if (wielder.isSprinting()) {
            if (wielder.fallDistance > 0
                    && ItemWeaponMF.tryPerformAbility(wielder, weapon.lunge_cost, !properHit, true, true, true)) {
                return chargeAt(weapon, wielder, target, true, damage, properHit);
            } else if (wielder.onGround && getHeightGap(target, wielder) <= 0.5F
                    && ItemWeaponMF.tryPerformAbility(wielder, weapon.charge_cost)) {
                        return chargeAt(weapon, wielder, target, false, damage, properHit);
                    }
        } else if (wielder.fallDistance > 0 && !wielder.isOnLadder()
                && ItemWeaponMF.tryPerformAbility(wielder, weapon.jump_cost)) {
                    if (properHit) brutalise(wielder, target, 1.0F);
                    return damage * 1.25F;
                }
        return damage;
    }

    private static float chargeAt(ItemWeaponMF weapon, EntityLivingBase entityHitting, EntityLivingBase entityHit,
            boolean lunge, float dam, boolean properHit) {
        if (properHit) brutalise(entityHitting, entityHit, 1.0F);

        if (lunge) {
            float fallBonus = (Math.min(entityHitting.fallDistance, weapon.getMeleeDamage(entityHitting.getHeldItem())))
                    / 2F;
            if (entityHit.onGround && !properHit) {
                entityHitting.hurtResistantTime = 20;
                TacticalManager.knockbackEntity(entityHit, entityHitting, 4F, 0.5F);
                TacticalManager.lungeEntity(entityHitting, entityHit, 2F, 0.1F);
            }
            if (properHit) {
                if (!entityHit.isPotionActive(Potion.moveSlowdown.id)) {
                    entityHit.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 400, 3));
                    entityHit.addPotionEffect(new PotionEffect(Potion.confusion.id, 200, 1));
                }

                entityHitting.fallDistance -= fallBonus;
            }
            if (fallBonus > 0) {
                dam += fallBonus;
            }
        } else if (properHit && !entityHit.isPotionActive(Potion.moveSlowdown.id)) {
            // The old grounded !properHit branch was nested inside properHit and could never run.
            entityHit.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 200, 3));
            entityHit.addPotionEffect(new PotionEffect(Potion.confusion.id, 100, 0));
        }
        return dam * 1.25F;
    }

    private static double getHeightGap(EntityLivingBase target, EntityLivingBase wielder) {
        return Math.abs(wielder.posY - target.posY);
    }
}
