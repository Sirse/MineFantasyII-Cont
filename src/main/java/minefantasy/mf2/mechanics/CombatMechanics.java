package minefantasy.mf2.mechanics;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.World;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.api.armour.IElementalResistance;
import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.api.weapon.*;
import minefantasy.mf2.config.ConfigArmour;
import minefantasy.mf2.config.ConfigExperiment;
import minefantasy.mf2.config.ConfigStamina;
import minefantasy.mf2.config.ConfigWeapon;
import minefantasy.mf2.entity.Shockwave;
import minefantasy.mf2.item.weapon.*;
import minefantasy.mf2.knowledge.KnowledgeListMF;
import minefantasy.mf2.util.MFLogUtil;
import minefantasy.mf2.util.XSTRandom;

public class CombatMechanics {

    private static final float power_attack_base = 25F;
    private static XSTRandom random = new XSTRandom();

    /**
     * 0 = false 1 = true -1 = failure
     */
    private static int initPowerAttack(EntityLivingBase user, Entity target, boolean properHit) {
        if (!canExecutePower(user)) {
            return 0;
        }
        if (StaminaBar.isSystemActive && StaminaBar.doesAffectEntity(user)) {
            float points = power_attack_base * (StaminaBar.getBaseDecayModifier(user, true, true) * 0.5F + 0.5F);
            if (StaminaBar.isStaminaAvailable(user, points, properHit)) {
                if (properHit) {
                    ItemWeaponMF.applyFatigue(user, points);
                }
                return Parrying.getPostHitCooldown(user) > 0 ? -1 : 1;
            } else {
                return 0;
            }
        }
        return 1;
    }

    private static boolean canExecutePower(EntityLivingBase user) {
        if (user.isInWater()) {
            return false;
        }
        if (user instanceof EntityPlayer) {
            if (!isFightStance(user)) return false;
        }
        return user.fallDistance > 0 && !user.isOnLadder();
    }

    static boolean isFightStance(EntityLivingBase user) {
        return user.isSneaking();
    }

    /**
     * How much strength is added (directly adds to melee dmg)
     */
    public static float getStrengthEnhancement(EntityLivingBase user) {
        float mod = 0F;
        if (PowerArmour.isPowered(user)) {
            mod += 3F;
        }
        return Math.max(-0.5F, mod);
    }

    @SubscribeEvent
    public void initAttack(LivingAttackEvent event) {
        EntityLivingBase hitter = getHitter(event.source);
        int spd = EventManagerMF.getHitspeedTime(hitter);
        if (hitter != null && !hitter.worldObj.isRemote) {
            if (spd > 0
                    && !(event.entityLiving instanceof EntityPlayer || event.entityLiving instanceof EntityEnderman)) {
                event.setCanceled(true);
                return;
            }
        }
        DamageSource src = event.source;
        EntityLivingBase hit = event.entityLiving;
        World world = hit.worldObj;
        boolean powerArmour = PowerArmour.isFullyArmoured(hit);
        float damage = modifyDamage(src, world, hit, event.ammount, false);

        if (hitter instanceof EntityPlayer) {
            applyHeavyBalance((EntityPlayer) hitter);
        }

        if (event.source.isProjectile() && !event.source.isFireDamage()) {
            if (powerArmour || (damage < event.ammount && hit.getTotalArmorValue() > 0))// only if dam has been reduced
            {
                if (ConfigArmour.resistArrow && !event.isCanceled() && (damage <= 0.5F))// TacticalManager.resistArrow(event.entityLiving,
                // event.source, damage))
                {
                    if (event.source.getSourceOfDamage() != null
                            && !event.source.getSourceOfDamage().getEntityData().hasKey("arrowDeflectMF")
                            && !(event.source.getEntity() instanceof EntityEnderPearl)) {
                        event.source.getSourceOfDamage().getEntityData().setBoolean("arrowDeflectMF", true);
                        event.entityLiving.worldObj.playSoundAtEntity(event.entityLiving, "random.break", 1.0F, 0.5F);
                        event.setCanceled(true);
                    }
                }
            }
        }

        if (damage <= 0) {
            event.setCanceled(true);
        }
        if (hitter instanceof EntityLivingBase) {
            int hitTime = 5;
            if (hitter.getHeldItem() != null) {
                ItemStack weapon = hitter.getHeldItem();
                if (weapon.getItem() instanceof IWeaponSpeed) {
                    hitTime += ((IWeaponSpeed) weapon.getItem()).modifyHitTime(hitter, weapon);
                }
            }
            if (hitTime > 0) EventManagerMF.setHitTime(hitter, hitTime);
        }
    }

    private void applyHeavyBalance(EntityPlayer hitter) {
        if (!ConfigWeapon.useBalance) return;
        if (hitter.getHeldItem() != null && hitter.getHeldItem().getItem() instanceof ItemWeaponMF) {
            ItemWeaponMF hitterWeapon = ((ItemWeaponMF) hitter.getHeldItem().getItem());
            if (hitterWeapon.isHeavyWeapon()) {
                TacticalManager.throwPlayerOffBalance(hitter, hitterWeapon.getBalance(), true);
            }
        }
    }

    /**
     * gets the melee hitter
     */
    private EntityLivingBase getHitter(DamageSource source) {
        if (source != null && source.getEntity() != null
                && source.getEntity() == source.getSourceOfDamage()
                && source.getEntity() instanceof EntityLivingBase) {
            return (EntityLivingBase) source.getEntity();
        }
        return null;
    }

    @SubscribeEvent
    public void onHit(LivingHurtEvent event) {
        DamageSource src = event.source;
        EntityLivingBase hit = event.entityLiving;

        if (src != null && src == DamageSource.fall) {
            onFall(hit, event.ammount);
        }
        World world = hit.worldObj;
        float damage = modifyDamage(src, world, hit, event.ammount, true);

        if (damage > 0 && hit.isSprinting()) {
            hit.setSprinting(false);
        }
        // Zombie armour
        if (event.entityLiving.getEntityData().hasKey(MonsterUpgrader.zombieArmourNBT)
                && event.entityLiving instanceof EntityZombie) {
            ItemStack[] armours = new ItemStack[4];
            for (int a = 1; a < 5; a++) {
                armours[a - 1] = event.entityLiving.getEquipmentInSlot(a);
            }
            damage = ISpecialArmor.ArmorProperties.ApplyArmor(event.entityLiving, armours, event.source, damage);
        }
        // Stuck arrows (experimental)
        if (ConfigExperiment.stickArrows && event.source.getSourceOfDamage() != null
                && event.source.getSourceOfDamage() instanceof EntityArrow) {
            if (!event.entity.worldObj.isRemote) {
                ArrowEffectsMF.stickArrowIn(
                        event.entity,
                        ArrowEffectsMF.getDroppedArrow(event.source.getSourceOfDamage()),
                        event.source.getSourceOfDamage());
            }
        }
        if (damage > 0) {
            onOfficialHit(src, hit, damage);

            if (event.source instanceof EntityDamageSource && !(event.source instanceof EntityDamageSourceIndirect)
                    && !event.source.damageType.equals("battlegearExtra")) {
                Entity entityHitter = event.source.getEntity();

                if (entityHitter instanceof EntityLivingBase) {
                    EntityLivingBase attacker = (EntityLivingBase) entityHitter;
                    StaminaMechanics.onAttack(attacker, hit);
                    ItemStack weapon = attacker.getHeldItem();
                    HitSoundGenerator.makeHitSound(weapon, event.entityLiving);
                }
            }
        }
        event.ammount = damage;
    }

    private void onFall(EntityLivingBase fallen, float height) {
        float weight = ArmourCalculator.getTotalWeightOfWorn(fallen, false);
        if (weight > 100) {
            weight -= 100F;
            float power = (height / 4F) * (weight / 100F);
            newShockwave(fallen, fallen.posX, fallen.posY, fallen.posZ, power, false, true);
        }
    }

    public Shockwave newShockwave(Entity source, double x, double y, double z, float power, boolean fire,
            boolean smoke) {
        Shockwave explosion = new Shockwave("humanstomp", source.worldObj, source, x, y, z, power);
        explosion.isFlaming = fire;
        explosion.isSmoking = smoke;
        // Player-caused shockwaves hurt and push entities but never damage blocks
        explosion.isGriefing = false;
        explosion.initiate();
        explosion.decorateWave(true);
        return explosion;
    }

    private float modifyDamage(DamageSource src, World world, EntityLivingBase hit, float dam, boolean properHit) {
        Entity source = src.getSourceOfDamage();
        Entity hitter = src.getEntity();

        if (PowerArmour.allowDamageToBlock(src)) {
            dam = PowerArmour.modifyDamage(hit, dam, src);
        }

        if (properHit && hit instanceof EntityPlayer) {
            dam = modifyPlayerDamage((EntityPlayer) hit, dam);
        }

        if (source != null && hitter instanceof EntityLivingBase) {
            dam = modifyUserHitDamage(dam, (EntityLivingBase) hitter, source, hitter == source, hit, properHit);
        }
        if (src.isExplosion() && isSkeleton(hit)) {
            dam *= 5F;
        }

        // Elemental resistance
        dam *= TacticalManager.getResistance(hit, src);
        if (src.isFireDamage()) {
            if (dam <= 0.0F) {
                hit.extinguish();
            }
        }

        return onUserHit(hit, hitter, src, dam, properHit);
    }

    private boolean isSkeleton(Entity target) {
        return target instanceof EntitySkeleton;
    }

    private float modifyUserHitDamage(float dam, EntityLivingBase user, Entity source, boolean melee, Entity target,
            boolean properHit) {
        dam = modifyMobDamage(user, dam);
        String special = "standard";
        // Power Attack
        if (melee) {
            int powerAttack = initPowerAttack(user, target, properHit);
            if (powerAttack == 1) {
                dam *= (2F / 1.5F);
                onPowerAttack(dam, user, target, properHit);

                if (isSkeleton(target)) {
                    dam *= 1.5F;
                    if (properHit) {
                        if (random.nextInt(2) == 0) {
                            target.entityDropItem(new ItemStack(Items.bone), 0.5F);
                        }
                    }
                }
            }
            if (powerAttack == -1) {
                dam /= 2F;
            }
        } else {
            String arrowDesign = source.getEntityData().getString("Design");
            if (arrowDesign != null && arrowDesign.length() > 0) {
                special = arrowDesign;
            }
        }
        if (user instanceof EntityLivingBase) {
            EntityLivingBase player = user;

            // Stamina
            if (StaminaBar.isSystemActive) {
                if (StaminaBar.getStaminaValue(player) <= 0) {
                    dam *= ConfigStamina.weaponDrain;
                }
            }
        }
        if (user.hurtResistantTime > 12) {
            dam *= 0.5F;
        }

        ItemStack weapon = user.getHeldItem();
        if (weapon != null) {
            if (weapon.getItem() instanceof IDamageModifier) {
                // Weapons that modify their own damage
                dam = ((IDamageModifier) weapon.getItem()).modifyDamage(weapon, user, target, dam, properHit);
            }
            CustomMaterial material = CustomToolHelper.getCustomPrimaryMaterial(weapon);
            String weaponType = CustomToolHelper.getCustomStyle(weapon);
            dam *= WeaponBanes.getSpecialModifier(material, weaponType, target, true);
        }
        return dam;
    }

    private void onPowerAttack(float dam, EntityLivingBase user, Entity target, boolean properHit) {
        ItemStack weapon = user.getHeldItem();
        int ticks = 20;
        if (weapon != null && weapon.getItem() instanceof IPowerAttack) {
            ((IPowerAttack) weapon.getItem()).onPowerAttack(dam, user, target, properHit);
            ticks = ((IPowerAttack) weapon.getItem()).getParryModifier(weapon, user, target);
        }
        if (target instanceof EntityLivingBase) {
            if (ticks > Parrying.getParryCooldown((EntityLivingBase) target)) {
                Parrying.setParryCooldown((EntityLivingBase) target, ticks);
            }
        }
        if (!user.worldObj.isRemote) {
            user.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, 20, 5));
            user.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 20, 10));
        }
        if (user instanceof EntityPlayer) {
            TacticalManager.lungeEntity(user, target, 0.5F, 0F);
            TacticalManager.throwPlayerOffBalance((EntityPlayer) user, 0.5F, true);
        }

        target.worldObj.playSoundAtEntity(target, "minefantasy2:weapon.critical", 1.0F, 1.0F);
    }

    private float modifyMobDamage(EntityLivingBase user, float dam) {
        if (user instanceof EntityZombie && user.isChild()) {
            dam *= 0.65F;
        }
        return dam + getStrengthEnhancement(user);
    }

    private void onOfficialHit(DamageSource src, EntityLivingBase target, float damage) {
        Entity source = src.getSourceOfDamage();
        Entity hitter = src.getEntity();

        if (source != null && hitter instanceof EntityLivingBase) {
            if (source == hitter) {
                EntityLivingBase user = (EntityLivingBase) hitter;
                ItemStack weapon = user.getHeldItem();
                if (weapon != null) {
                    onWeaponHit(user, weapon, target, damage);
                }
                if (RPGElements.isSystemActive && user instanceof EntityPlayer && !user.worldObj.isRemote) {
                    SkillList.combat.addXP((EntityPlayer) user, (int) (damage / 5F));
                }
            }
        }
        Parrying.setPostHitCooldown(target, 10);
    }

    private void onWeaponHit(EntityLivingBase user, ItemStack weapon, Entity target, float dam) {
        if (target instanceof EntityLivingBase && weapon.getItem() instanceof ISpecialEffect) {
            ((ISpecialEffect) weapon.getItem()).onProperHit(user, weapon, target, dam);
        }

        if (weapon.getItem() instanceof IWeaponSpeed) {
            target.hurtResistantTime += ((IWeaponSpeed) weapon.getItem()).modifyHitTime(user, weapon);
        }
        if (weapon.getItem() instanceof IKnockbackWeapon) {
            float kb = ((IKnockbackWeapon) weapon.getItem()).getAddedKnockback(user, weapon);

            if (kb > 0) {
                TacticalManager.knockbackEntity(target, user, kb, 0F);
            }
        }
        if (ConfigWeapon.useBalance && user instanceof EntityPlayer) {
            applyBalance((EntityPlayer) user);
        }
    }

    private float onUserHit(EntityLivingBase user, Entity entityHitting, DamageSource source, float dam,
            boolean properHit) {
        dam = Parrying.parry(user, entityHitting, source, dam, properHit);
        if (StaminaBar.isSystemActive && StaminaBar.doesAffectEntity(user) && !StaminaBar.isAnyStamina(user, false)) {
            dam *= Math.max(1.0F, ConfigStamina.exhaustDamage);
        }

        // Fire dura degrade
        if (properHit && source.isFireDamage() && dam > 0) {
            for (int a = 0; a < 4; a++) {
                ItemStack armour = user.getEquipmentInSlot(a + 1);
                if (armour != null && armour.isItemStackDamageable()) {
                    int dura = (int) (dam) + 1;
                    if (!user.worldObj.isRemote && !isArmourFireImmune(armour, source)) {
                        MFLogUtil.logDebug("Armour Flame Damage: " + dura);
                        if (armour.getItemDamage() + dura < armour.getMaxDamage()) {
                            armour.damageItem(dura, user);
                        } else {
                            armour.setItemDamage(armour.getMaxDamage());
                        }
                    }
                    if (!user.worldObj.isRemote && armour.getItemDamage() >= armour.getMaxDamage()) {
                        user.setCurrentItemOrArmor(a + 1, null);
                        user.worldObj.playSoundEffect(
                                user.posX,
                                user.posY + user.getEyeHeight() - (0.4F * a),
                                user.posZ,
                                "random.break",
                                1.0F,
                                1.0F);
                    }
                }
            }
        }

        if ((dam > 0 && user instanceof EntityPlayer) || (entityHitting instanceof EntityPlayer)) {
            if (!user.worldObj.isRemote) {
                String type = "Mixed";
                float[] f = ArmourCalculator.getRatioForSource(source);
                if (f == null) {
                    type = "Basic";
                } else {
                    if (f[0] > f[1] && f[0] > f[2]) type = "Cutting";
                    if (f[2] > f[1] && f[2] > f[0]) type = "Piercing";
                    if (f[1] > f[0] && f[1] > f[2]) type = "Blunt";
                }

                MFLogUtil.logDebug(
                        dam + "x "
                                + type
                                + " Damage inflicted to: "
                                + user.getCommandSenderName()
                                + " ("
                                + user.getEntityId()
                                + ")");
            }
        }
        return dam;
    }

    private boolean isArmourFireImmune(ItemStack armour, DamageSource src) {
        if (armour != null && armour.getItem() instanceof IElementalResistance) {
            return ((IElementalResistance) armour.getItem()).getFireResistance(armour, src) >= 100F;
        }
        return false;
    }

    private void applyBalance(EntityPlayer entityPlayer) {
        MFLogUtil.logDebug("Weapon Balance Init");
        ItemStack weapon = entityPlayer.getHeldItem();
        float balance = 0.0F;

        if (weapon != null && weapon.getItem() instanceof IWeightedWeapon) {
            balance = ((IWeightedWeapon) weapon.getItem()).getBalance(entityPlayer);
        }

        if (ConfigWeapon.useBalance && balance > 0 && entityPlayer != null) {
            TacticalManager.throwPlayerOffBalance(entityPlayer, balance, true);
        }
    }

    @SubscribeEvent
    public void jump(LivingJumpEvent event) {
        if (event.entityLiving instanceof EntityPlayer && StaminaBar.isSystemActive
                && StaminaBar.doesAffectEntity(event.entityLiving)) {
            StaminaMechanics.onJump(event.entityLiving);
        }
    }

    private float modifyPlayerDamage(EntityPlayer hit, float dam) {
        if (ResearchLogic.hasInfoUnlocked(hit, KnowledgeListMF.toughness)) {
            dam *= 0.9F;// 10% Resist
        }
        return dam;
    }
}
