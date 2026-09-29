package minefantasy.mf2.mechanics;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.weapon.*;
import minefantasy.mf2.item.weapon.*;
import minefantasy.mf2.util.XSTRandom;

/** Extra damage from silver, ornate and dragonforged weapons against the creatures they bane. */
public class WeaponBanes {

    /**
     * Damage done by silver to undead/witches
     */
    public static final float specialUnholyModifier = 2.0F;
    /**
     * Damage done by silver to werewolves
     */
    public static final float specialWerewolfModifier = 8.0F;
    /**
     * Damage done with dragonforged design to dragons
     */
    public static final float specialDragonModifier = 1.5F;
    /**
     * Damage done with ornate design to undead/witches
     */
    public static final float specialOrnateModifier = 1.5F;
    private static final XSTRandom random = new XSTRandom();

    public static void applyUndeadBane(EntityLivingBase living) {
        living.playSound("random.fizz", 0.5F, 0.5F);
        living.addPotionEffect(new PotionEffect(Potion.weakness.id, 1200, 2));
        living.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 1200, 2));
        if (random.nextInt(5) == 0) {
            living.setFire(3);
        }
    }

    public static float getSpecialModifier(CustomMaterial material, String design, Entity target, boolean addEffect) {
        if (target == null) return 1.0F;

        float modifier = 1.0F;

        if (design != null) {
            if (design.equalsIgnoreCase("dragonforged")) {
                if (TacticalManager.isDragon(target)) {
                    modifier *= specialDragonModifier;
                }
            }

            if (design.equalsIgnoreCase("ornate")) {
                if (TacticalManager.isUnholyCreature(target)) {
                    modifier *= specialOrnateModifier;
                }
            }
        }

        if (material != null) {
            if (isSilverishMaterial(material.name) && target instanceof EntityLivingBase) {
                if (target.getClass().getName().contains("Werewolf")) {
                    modifier *= specialWerewolfModifier;
                    applyUndeadBane((EntityLivingBase) target);
                } else if (TacticalManager.isUnholyCreature(target)) {
                    modifier *= specialUnholyModifier;
                    applyUndeadBane((EntityLivingBase) target);
                }
            }
        }

        return modifier;
    }

    public static boolean isSilverishMaterial(String material) {
        return material.equalsIgnoreCase("silver");
    }
}
