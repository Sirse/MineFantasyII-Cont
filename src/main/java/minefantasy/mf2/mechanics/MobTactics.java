package minefantasy.mf2.mechanics;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.api.weapon.*;
import minefantasy.mf2.entity.EntityCogwork;
import minefantasy.mf2.entity.mob.EntityMinotaur;
import minefantasy.mf2.item.weapon.*;
import minefantasy.mf2.util.XSTRandom;

/** How armed mobs press a fight: leaping in, sprinting, and panicking while they burn. */
public class MobTactics {

    private static final XSTRandom random = new XSTRandom();

    @SubscribeEvent
    public void onUpdate(LivingUpdateEvent event) {
        if (event.entityLiving instanceof EntityLiving) {
            EntityLiving living = (EntityLiving) event.entityLiving;
            EntityLiving mob = living;
            ItemStack held = mob.getHeldItem();

            {
                EntityLivingBase tar = mob.getAttackTarget();

                if (tar instanceof EntityPlayer && ((EntityPlayer) tar).isBlocking()) {
                    double dist = mob.getDistanceSqToEntity(tar);

                    if (mob instanceof EntityZombie && mob.onGround
                            && mob.getRNG().nextInt(10) == 0
                            && dist > 1D
                            && dist < 4D) {
                        mob.motionY = 0.5F;
                    }
                }
            }
            if (isAxe(held)) {
                EntityLivingBase tar = mob.getAttackTarget();

                if (tar != null) {
                    double dist = mob.getDistanceSqToEntity(tar);

                    if (mob.onGround && mob.getRNG().nextInt(5) == 0 && dist > 1D && dist < 4.0D) {
                        mob.motionY = 0.5F;
                    }
                }
                if (mob.getRNG().nextInt(100) == 0 && !mob.isSprinting() && !mob.isChild()) {
                    mob.setSprinting(true);
                }
            }
            if (isFastblade(held)) {
                EntityLivingBase tar = mob.getAttackTarget();

                if (tar != null) {
                    double dist = mob.getDistanceSqToEntity(tar);

                    if (mob.onGround && mob.getRNG().nextInt(20) == 0 && dist > 1D && dist < 4.0D) {
                        mob.motionY = 0.5F;
                    }
                }
                if (mob.getRNG().nextInt(20) == 0 && !mob.isSprinting() && !mob.isChild()) {
                    mob.setSprinting(true);
                }
            }
            if (living.isBurning() && !living.isImmuneToFire()) {
                panic(living, 0.25F, 5);
            }
        }
    }

    private boolean isAxe(ItemStack held) {
        return held != null && held.getItem() instanceof ItemWaraxeMF
                || held != null && held.getItem() instanceof ItemBattleaxeMF;
    }

    private boolean isFastblade(ItemStack held) {
        return held != null && held.getItem() instanceof ItemDagger
                || held != null && held.getItem() instanceof ItemKatanaMF;
    }

    /*
     * Causes the victim to 'Spaz out' which never stops being funny (Apply every tick)
     */
    public static void panic(EntityLivingBase victim, float speed, int directionTimer) {
        if (!shouldPanic(victim)) {
            return;
        }
        double moveX = victim.getEntityData().getDouble("MF2_PanicX");
        double moveZ = victim.getEntityData().getDouble("MF2_PanicZ");
        victim.setJumping(true);

        if ((moveX == 0 && moveZ == 0) || random.nextInt(directionTimer) == 0) {
            moveX = (random.nextDouble() - 0.5D) * 0.85D * speed;
            moveZ = (random.nextDouble() - 0.5D) * 0.85D * speed;

            victim.getEntityData().setDouble("MF2_PanicX", moveX);
            victim.getEntityData().setDouble("MF2_PanicZ", moveZ);
            if (victim.onGround) victim.motionY = 0.25F;
            victim.rotationYaw = (float) (Math.atan2(moveX, moveZ));
        }
        victim.swingItem();
        victim.limbSwing = 1.0F;
        victim.moveEntity(moveX, 0D, moveZ);
    }

    private static boolean shouldPanic(EntityLivingBase victim) {
        if (victim instanceof EntityMinotaur) {
            return ((EntityMinotaur) victim).getRageLevel() < 80;
        }
        return !(victim instanceof EntityPlayer || victim instanceof EntityCogwork);
    }
}
