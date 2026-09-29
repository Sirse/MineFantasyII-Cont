package minefantasy.mf2.mechanics;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.api.weapon.*;
import minefantasy.mf2.config.ConfigExperiment;
import minefantasy.mf2.item.weapon.*;
import minefantasy.mf2.network.packet.ParryPacket;
import minefantasy.mf2.util.MFLogUtil;
import minefantasy.mf2.util.XSTRandom;

/** Parrying hits with the held weapon, evading past the attacker, and the cooldowns in between. */
public class Parrying {

    public static final String parryCooldownNBT = "MF_Parry_Cooldown";
    public static final String posthitCooldownNBT = "MF_PostHit";
    private static final float parryFatigue = 5F;
    private static final float jumpEvade_cost = 30;
    private static final float evade_cost = 10;
    private static final XSTRandom random = new XSTRandom();

    @SubscribeEvent
    public void onUpdate(LivingUpdateEvent event) {
        tickParryCooldown(event.entityLiving);
        tickPostHitCooldown(event.entityLiving);
    }

    public static void setParryCooldown(EntityLivingBase user, int ticks) {
        Cooldowns.set(user, parryCooldownNBT, ticks);

        if (!user.worldObj.isRemote && user instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) user;
            MineFantasyII.packetHandler.sendPacketToPlayer(new ParryPacket(ticks, player).generatePacket(), player);
        }
    }

    public static int getParryCooldown(EntityLivingBase user) {
        return Cooldowns.left(user, parryCooldownNBT);
    }

    public static void tickParryCooldown(EntityLivingBase user) {
        Cooldowns.tick(user, parryCooldownNBT);
    }

    public static boolean isParryAvailable(EntityLivingBase user) {
        return getParryCooldown(user) <= 0;
    }

    public static void setPostHitCooldown(EntityLivingBase user, int ticks) {
        Cooldowns.set(user, posthitCooldownNBT, ticks);
    }

    public static int getPostHitCooldown(EntityLivingBase user) {
        return Cooldowns.left(user, posthitCooldownNBT);
    }

    public static void tickPostHitCooldown(EntityLivingBase user) {
        Cooldowns.tick(user, posthitCooldownNBT);
    }

    /** Blocks what the held weapon can of a hit, returning the damage that gets through. */
    static float parry(EntityLivingBase user, Entity entityHitting, DamageSource source, float dam, boolean properHit) {
        ItemStack weapon = user.getHeldItem();
        if ((properHit || source.isProjectile()) && weapon != null
                && !source.isUnblockable()
                && !source.isExplosion()) {
            float threshold = 10;// DEFAULT PARRY THRESHOLD
            float weaponFatigue = 2.0F;// DEFAULT FATIGUE
            int ticks = 18;// DEFAULT TICKS
            IParryable parry = null;

            if (weapon.getItem() instanceof IParryable) {
                parry = (IParryable) weapon.getItem();

                ticks = parry.getParryCooldown(source, dam, weapon);
                threshold = parry.getMaxDamageParry(user, weapon);
                weaponFatigue = parry.getParryStaminaDecay(source, weapon);
            }
            if (StaminaBar.isSystemActive && !StaminaBar.isAnyStamina(user, false)) {
                threshold /= 2;
            }
            threshold *= TacticalManager.getHighgroundModifier(user, entityHitting, 1.15F);

            if (ArmourCalculator.advancedDamageTypes && !user.worldObj.isRemote) {
                threshold = ArmourCalculator.adjustACForDamage(source, threshold, 1.0F, 0.75F, 0.5F);
            }

            if (ConfigExperiment.debugParry && !user.worldObj.isRemote) {
                MFLogUtil.logDebug("Init Parry: Damage = " + dam + " Threshold = " + threshold);
            }

            // USED FOR PARRYING its harder to block arrows
            if (TacticalManager.canParry(source, user, entityHitting, weapon)) {
                float previousDam = dam;
                dam = Math.max(0F, dam - threshold);

                if (properHit || dam <= 0) {
                    user.hurtResistantTime = user.maxHurtResistantTime;
                    user.hurtTime = 0;

                    int result = onParry(source, user, entityHitting, dam, previousDam, parry);

                    if (result == 1) {
                        dam = 0;
                    }
                    ticks = ArmourCalculator.modifyParryCooldown(user, ticks);

                    if (StaminaBar.isSystemActive && StaminaBar.doesAffectEntity(user)
                            && !StaminaBar.isAnyStamina(user, false)) {
                        ticks *= 3;
                    }
                    if (ticks > getParryCooldown(user)) {
                        setParryCooldown(user, ticks);
                    }

                    ItemWeaponMF.applyFatigue(
                            user,
                            TacticalManager.getHighgroundModifier(user, entityHitting, 2.0F) * (dam + 1F)
                                    * parryFatigue
                                    * weaponFatigue);
                    if (parry == null) {
                        user.worldObj.playSoundAtEntity(
                                user,
                                getDefaultParrySound(weapon),
                                1.0F,
                                1.25F + (random.nextFloat() * 0.5F));
                    } else if (!parry.playCustomParrySound(user, entityHitting, weapon)) {
                        user.worldObj
                                .playSoundAtEntity(user, "mob.zombie.metal", 1.0F, 1.25F + (random.nextFloat() * 0.5F));
                    }
                    if (user instanceof EntityPlayer) {
                        ((EntityPlayer) user).stopUsingItem();
                        ItemWeaponMF.setParry(weapon, 20);
                    }

                    if (entityHitting instanceof EntityLivingBase) {
                        EntityLivingBase hitter = (EntityLivingBase) entityHitting;
                        int hitTime = 5;
                        if (hitter.getHeldItem() != null) {
                            ItemStack attackingWep = hitter.getHeldItem();
                            if (attackingWep.getItem() instanceof IWeaponSpeed) {
                                hitTime += ((IWeaponSpeed) attackingWep.getItem()).modifyHitTime(hitter, attackingWep);
                            }
                        }
                        if (hitTime > 0) {
                            MFLogUtil.logDebug(
                                    "Recoil hitter: " + hitter.getCommandSenderName()
                                            + " for "
                                            + hitTime * 3
                                            + " ticks.");
                            EventManagerMF.setHitTime(hitter, hitTime * 3);
                        }
                    }
                }
            }
        }
        return dam;
    }

    private static String getDefaultParrySound(ItemStack weapon) {
        if (weapon.getUnlocalizedName().contains("wood") || weapon.getUnlocalizedName().contains("Wood")
                || weapon.getUnlocalizedName().contains("stone")
                || weapon.getUnlocalizedName().contains("Stone")) {
            return "minefantasy2:weapon.wood_parry";
        }
        return "mob.zombie.metal";
    }

    /**
     * @return 0 for normal parry and 1 for evade
     */
    private static int onParry(DamageSource source, EntityLivingBase user, Entity attacker, float dam, float prevDam,
            IParryable parry) {
        /*
         * if(RPGElements.isSystemActive && user instanceof EntityPlayer) { SkillList.block.addXP((EntityPlayer)user, 10
         * + (int)prevDam*2); }
         */
        if (RPGElements.isSystemActive && user instanceof EntityPlayer) {
            SkillList.combat.addXP((EntityPlayer) user, (int) (prevDam / 3F));
        }
        if (parry != null) {
            parry.onParry(source, user, attacker, dam);
        }
        if (user instanceof ISpecialCombatMob) {
            ((ISpecialCombatMob) user).onParry(source, attacker, dam);
        }

        boolean groundBlock = user.onGround;
        ItemStack weapon = user.getHeldItem();

        // Redirect
        if (!user.worldObj.isRemote && !TacticalManager.isRanged(source)) {
            if (canEvade(user)) {
                float powerMod = attacker.isSprinting() ? 4.0F : 2.5F;

                attacker.setSprinting(false);
                TacticalManager.lungeEntity(attacker, user, powerMod, 0.0F);
                TacticalManager.lungeEntity(user, attacker, 3F, 0.0F);
                return 1;
            }
        }
        return 0;
    }

    /**
     * Determines if an evade can be made (jump or normal)
     */
    private static boolean canEvade(EntityLivingBase user) {
        float stamModifier = 1.0F;
        if (user instanceof EntityPlayer) {
            if (!ResearchLogic.hasInfoUnlocked((EntityPlayer) user, "parrypro")) {
                return false;
            }

            if (!CombatMechanics.isFightStance(user)) {
                return false;
            }
        } else {
            if (random.nextInt(10) != 0)// Mobs can evade
            {
                return false;
            }
        }

        if (!user.onGround && !tryJumpEvade(user, stamModifier)) {
            return false;
        }
        return tryGroundEvade(user, stamModifier);
    }

    /**
     * If the player can slip past enemies Should be any armour but heavy
     */
    private static boolean tryGroundEvade(EntityLivingBase user, float cost) {
        return ItemWeaponMF.tryPerformAbility(user, evade_cost * cost, true, false);
    }

    /**
     * If the player can jump over enemies in evading Only ment for unarmoured/Lightarmour
     */
    private static boolean tryJumpEvade(EntityLivingBase user, float cost) {
        return ItemWeaponMF.tryPerformAbility(user, jumpEvade_cost * cost, true, false);
    }
}
