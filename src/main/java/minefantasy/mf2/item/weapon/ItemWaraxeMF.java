package minefantasy.mf2.item.weapon;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

import minefantasy.mf2.api.helpers.Cooldowns;
import minefantasy.mf2.api.weapon.WeaponClass;
import minefantasy.mf2.mechanics.EventManagerMF;

/**
 * @author Anonymous Productions
 */
public class ItemWaraxeMF extends ItemWeaponMF {

    private float injuryChance = 0.05F;

    /**
     * The waraxe is for brutal players. It does more damage and a higher bonus when sprinting or jumping. Waraxes are
     * pure damage
     * <p>
     * These are for the brutal player
     */
    public ItemWaraxeMF(String name, ToolMaterial material, int rarity, float weight) {
        super(material, name, rarity, weight);
    }

    public static void brutalise(EntityLivingBase entityHitting, EntityLivingBase entityHit, float power) {
        AxeCombat.brutalise(entityHitting, entityHit, power);
    }

    @Override
    public float modifyDamage(ItemStack item, EntityLivingBase wielder, Entity hit, float initialDam,
            boolean properHit) {
        return AxeCombat.modifyDamage(
                this,
                wielder,
                hit,
                super.modifyDamage(item, wielder, hit, initialDam, properHit),
                properHit);
    }

    @Override
    public void onProperHit(EntityLivingBase user, ItemStack weapon, Entity hit, float dam) {
        if (rand.nextFloat() < injuryChance && dam > 0) {
            Cooldowns.set(hit, EventManagerMF.injuredNBT, 200 + rand.nextInt(600));
        }
        super.onProperHit(user, weapon, hit, dam);
    }

    @Override
    public float getDigSpeed(ItemStack itemstack, Block block, int metadata) {
        if (Items.iron_axe.getDigSpeed(itemstack, block, metadata) > 2.0F) {
            return material.getEfficiencyOnProperMaterial() * 0.75F;
        }
        return super.getDigSpeed(itemstack, block, metadata);
    }

    @Override
    public boolean playCustomParrySound(EntityLivingBase blocker, Entity attacker, ItemStack weapon) {
        blocker.worldObj.playSoundAtEntity(blocker, "minefantasy2:weapon.wood_parry", 1.0F, 1.0F);
        return true;
    }

    @Override
    public int modifyHitTime(EntityLivingBase user, ItemStack item) {
        return super.modifyHitTime(user, item) + speedModAxe;
    }

    @Override
    public float getDamageModifier() {
        return damageModAxe;
    }

    @Override
    protected float[] getWeaponRatio(ItemStack implement) {
        return hackingDamage;
    }

    @Override
    public float getPenetrationLevel(Object implement) {
        return axeAPModifier;
    }

    /**
     * gets the time after being hit your guard will be let down
     */
    @Override
    public int getParryCooldown(DamageSource source, float dam, ItemStack weapon) {
        return axeParryTime;
    }

    @Override
    protected float getStaminaMod() {
        return axeStaminaCost;
    }

    @Override
    public WeaponClass getWeaponClass() {
        return WeaponClass.AXE;
    }

    @Override
    public boolean canCounter() {
        return true;
    }

    @Override
    public float[] getCounterRatio() {
        return piercingDamage;
    }

    @Override
    public float getCounterDamage() {
        return 1.0F;
    }
}
