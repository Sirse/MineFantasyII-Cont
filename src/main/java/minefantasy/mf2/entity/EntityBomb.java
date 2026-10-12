package minefantasy.mf2.entity;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.item.gadget.EnumCasingType;
import minefantasy.mf2.item.gadget.EnumExplosiveType;
import minefantasy.mf2.item.gadget.EnumFuseType;
import minefantasy.mf2.item.gadget.EnumPowderType;
import minefantasy.mf2.mechanics.CombatMechanics;
import minefantasy.mf2.mechanics.MobTactics;

public class EntityBomb extends Entity {

    private final int typeId = 2;
    /**
     * How long the fuse is
     */
    public int fuse;
    private EntityLivingBase thrower;

    public EntityBomb(World world) {
        super(world);
        this.fuse = getFuseTime();
        this.preventEntitySpawning = true;
        this.setSize(0.5F, 0.5F);
        this.yOffset = this.height / 2.0F;
    }

    public EntityBomb(World world, EntityLivingBase thrower) {
        this(world);
        this.thrower = thrower;

        this.setLocationAndAngles(
                thrower.posX,
                thrower.posY + thrower.getEyeHeight(),
                thrower.posZ,
                thrower.rotationYaw,
                thrower.rotationPitch);
        this.posX -= MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI) * 0.16F;
        this.posY -= 0.10000000149011612D;
        this.posZ -= MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI) * 0.16F;
        this.setPosition(this.posX, this.posY, this.posZ);

        float force = this.getThrownForce() + (CombatMechanics.getStrengthEnhancement(thrower) / 4);

        float f = 0.4F;
        this.motionX = -MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI)
                * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI)
                * f;
        this.motionZ = MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI)
                * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI)
                * f;
        this.motionY = -MathHelper.sin((this.rotationPitch + this.getThrownOffset()) / 180.0F * (float) Math.PI) * f;
        this.setThrowableHeading(this.motionX, this.motionY, this.motionZ, force, 1.0F);

        this.prevPosX = posX;
        this.prevPosY = posY;
        this.prevPosZ = posZ;
    }

    public static DamageSource causeBombDamage(Entity bomb, Entity user) {
        return (new EntityDamageSourceBomb(bomb, user)).setProjectile();
    }

    private int getFuseTime() {
        return (this.fuse = getFuseType().time) + (isSticky() ? 20 : 0);
    }

    public void setThrowableHeading(double x, double y, double z, float offset, float force) {
        float f2 = MathHelper.sqrt_double(x * x + y * y + z * z);
        x /= f2;
        y /= f2;
        z /= f2;
        x += this.rand.nextGaussian() * 0.007499999832361937D * force;
        y += this.rand.nextGaussian() * 0.007499999832361937D * force;
        z += this.rand.nextGaussian() * 0.007499999832361937D * force;
        x *= offset;
        y *= offset;
        z *= offset;
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
        float f3 = MathHelper.sqrt_double(x * x + z * z);
        this.prevRotationYaw = this.rotationYaw = (float) (Math.atan2(x, z) * 180.0D / Math.PI);
        this.prevRotationPitch = this.rotationPitch = (float) (Math.atan2(y, f3) * 180.0D / Math.PI);
    }

    protected float getThrownOffset() {
        return 0.0F;
    }

    protected float getThrownForce() {
        return 1.5F;
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(typeId, (byte) 0);
        dataWatcher.addObject(typeId + 1, (byte) 0);
        dataWatcher.addObject(typeId + 2, (byte) 0);
        dataWatcher.addObject(typeId + 3, (byte) 0);
    }

    /**
     * returns if this entity triggers Block.onEntityWalking on the blocks they walk on. used for spiders and wolves to
     * prevent them from trampling crops
     */
    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    /**
     * Returns true if other Entities should be prevented from moving through this Entity.
     */
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.motionY -= (0.03999999910593033D * getGravityModifier());
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
            if (!collide.isEmpty() && !(thrower != null && collide.contains(thrower))) {
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
        if (isStuckInBlock()) {
            motionX = motionY = motionZ = 0;
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

    private boolean isStuckInBlock() {
        return false;// isSticky() && isAABBInSolid(boundingBox.expand(0.5D, 0.5D, 0.5D));
    }

    private boolean isSticky() {
        return getEntityData().hasKey("stickyBomb");
    }

    private double getGravityModifier() {
        return isStuckInBlock() ? 0F : getCase().weightModifier;
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setInteger("Fuse", this.fuse);
        // The four components live in the DataWatcher, which the base Entity does not serialise
        nbt.setByte("Filling", getFilling());
        nbt.setByte("Casing", getCasing());
        nbt.setByte("FuseType", getFuse());
        nbt.setByte("Powder", getPowder());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        // Restore the components straight into the DataWatcher: setType would reset the timer to a full fuse
        dataWatcher.updateObject(typeId, nbt.getByte("Filling"));
        dataWatcher.updateObject(typeId + 1, nbt.getByte("Casing"));
        dataWatcher.updateObject(typeId + 2, nbt.getByte("FuseType"));
        dataWatcher.updateObject(typeId + 3, nbt.getByte("Powder"));
        this.fuse = nbt.getInteger("Fuse");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public float getShadowSize() {
        return 0.25F;
    }

    /**
     * returns null or the entityliving it was placed or ignited by
     */
    public EntityLivingBase getTntPlacedBy() {
        return this.thrower;
    }

    @Override
    public void applyEntityCollision(Entity entity) {
        if (!(thrower != null && thrower == entity)) {
            this.motionX = 0;
            this.motionZ = 0;
        }
    }

    public void explode() {
        Blast blast = new Blast(
                this,
                getRangeOfBlast(),
                getDamage(),
                causeBombDamage(this, thrower != null ? thrower : this)).filling(getFilling(), 5).ridden(ridingEntity);
        if (getCasing() == 2) blast.noFalloff();
        blast.explode();
    }

    private double getRangeOfBlast() {
        return getBlast().range * getCase().rangeModifier * getPowderType().rangeModifier;
    }

    private int getDamage() {
        return (int) (getBlast().damage * getCase().damageModifier * getPowderType().damageModifier);
    }

    public byte getFilling() {
        return dataWatcher.getWatchableObjectByte(typeId);
    }

    public byte getCasing() {
        return dataWatcher.getWatchableObjectByte(typeId + 1);
    }

    public byte getFuse() {
        return dataWatcher.getWatchableObjectByte(typeId + 2);
    }

    public byte getPowder() {
        return dataWatcher.getWatchableObjectByte(typeId + 3);
    }

    public EntityBomb setType(byte fill, byte casing, byte fuse, byte powder) {
        dataWatcher.updateObject(typeId, fill);
        dataWatcher.updateObject(typeId + 1, casing);
        dataWatcher.updateObject(typeId + 2, fuse);
        dataWatcher.updateObject(typeId + 3, powder);

        this.fuse = getFuseType().time;

        return this;
    }

    private EnumExplosiveType getBlast() {
        return EnumExplosiveType.getType(getFilling());
    }

    private EnumCasingType getCase() {
        return EnumCasingType.getType(getCasing());
    }

    private EnumFuseType getFuseType() {
        return EnumFuseType.getType(getFuse());
    }

    private EnumPowderType getPowderType() {
        return EnumPowderType.getType(getPowder());
    }

    public boolean isAABBInSolid(AxisAlignedBB field) {
        int i = MathHelper.floor_double(field.minX);
        int j = MathHelper.floor_double(field.maxX + 1.0D);
        int k = MathHelper.floor_double(field.minY);
        int l = MathHelper.floor_double(field.maxY + 1.0D);
        int i1 = MathHelper.floor_double(field.minZ);
        int j1 = MathHelper.floor_double(field.maxZ + 1.0D);

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    Block block = worldObj.getBlock(k1, l1, i2);

                    if (block.getMaterial().isSolid()) {
                        int j2 = worldObj.getBlockMetadata(k1, l1, i2);
                        double d0 = l1 + 1;

                        if (j2 < 8) {
                            d0 = l1 + 1 - j2 / 8.0D;
                        }

                        if (d0 >= field.minY) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
