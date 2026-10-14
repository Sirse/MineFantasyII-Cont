package minefantasy.mf2.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import minefantasy.mf2.mechanics.CombatMechanics;
import minefantasy.mf2.mechanics.MobTactics;

/** A thrown bomb: it bursts when its fuse burns down, and sticks to what it hits if made sticky. */
public class EntityBomb extends EntityExplosive {

    public EntityBomb(World world) {
        super(world, 0.5F, 0.5F);
        this.fuse = getFuseTime();
    }

    public EntityBomb(World world, EntityLivingBase thrower) {
        super(world, 0.5F, 0.5F, thrower, 1.5F + CombatMechanics.getStrengthEnhancement(thrower) / 4);
        this.fuse = getFuseTime();
    }

    public static DamageSource causeBombDamage(Entity bomb, Entity user) {
        return damageSource("bomb", bomb, user).setProjectile();
    }

    private int getFuseTime() {
        return (this.fuse = getFuseType().time) + (isSticky() ? 20 : 0);
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.motionY -= (0.03999999910593033D * getCase().weightModifier);
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.9800000190734863D;
        this.motionY *= 0.9800000190734863D;
        this.motionZ *= 0.9800000190734863D;

        if (this.onGround) {
            double d = 0.75D;
            this.motionX *= d;
            this.motionZ *= d;
            this.motionY *= -0.99D;
        }
        if (ticksExisted % 3 == 0) {
            List collide = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox);
            if (!collide.isEmpty() && !(getPlacer() != null && collide.contains(getPlacer()))) {
                if (isSticky()) {
                    Object object = collide.get(0);
                    if (object instanceof Entity && object != this) {
                        if (this.ridingEntity == null && ((Entity) object).riddenByEntity == null
                                && canStick((Entity) object)) {
                            this.mountEntity((Entity) object);
                            this.fuse = getFuseTime();
                        }
                    }
                }
                this.motionX = motionZ = 0;
            }
        }

        if (this.fuse-- <= 0) {
            this.setDead();

            if (!this.worldObj.isRemote) {
                this.explode();
            }
        } else {
            this.worldObj.spawnParticle("smoke", this.posX, this.posY + 0.125D, this.posZ, 0.0D, 0.0D, 0.0D);
            this.worldObj.spawnParticle("flame", this.posX, this.posY + 0.125D, this.posZ, 0.0D, 0.0D, 0.0D);
        }

        if (!worldObj.isRemote && this.ridingEntity instanceof EntityLivingBase) {
            if (!(ridingEntity instanceof EntityChicken)) {
                MobTactics.panic((EntityLivingBase) ridingEntity, 1.0F, 30);
            }
        }
    }

    private boolean canStick(Entity entity) {
        return !(entity instanceof EntityCogwork);
    }

    @Override
    public void mountEntity(Entity object) {
        super.mountEntity(object);

        if (object instanceof EntityChicken) {
            EntityChicken chook = (EntityChicken) object;
            Entity target = worldObj
                    .findNearestEntityWithinAABB(IMob.class, chook.boundingBox.expand(64, 8, 64), chook);
            if (target != null) {
                if (target instanceof EntityLivingBase) {
                    chook.setCustomNameTag("Suicide Chook!");
                    fuse = getFuseTime() * 2;
                    chook.getNavigator().clearPathEntity();
                    chook.setAttackTarget((EntityLivingBase) target);
                    chook.targetTasks.addTask(1, new EntityAIAttackOnCollide(chook, IMob.class, 1.25D, true));
                    chook.setAIMoveSpeed(0.5F);
                }
            }
        }
    }

    private boolean isSticky() {
        return getEntityData().hasKey("stickyBomb");
    }

    @Override
    public void applyEntityCollision(Entity entity) {
        if (!isPlacer(entity)) {
            this.motionX = 0;
            this.motionZ = 0;
        }
    }

    /** Set off, it bursts within a few ticks. */
    @Override
    protected void onSetOff() {
        fuse = Math.min(fuse, 3);
    }

    @Override
    protected DamageSource damageSource() {
        return causeBombDamage(this, getPlacer());
    }

    @Override
    protected void shape(Blast blast) {
        blast.filling(getFilling(), 5).ridden(ridingEntity);
    }

    public EntityBomb setType(byte fill, byte casing, byte fuse, byte powder) {
        setComponents(fill, casing, fuse, powder);
        return this;
    }
}
