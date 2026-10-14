package minefantasy.mf2.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.weapon.IDamageType;
import minefantasy.mf2.item.gadget.EnumCasingType;
import minefantasy.mf2.item.gadget.EnumExplosiveType;
import minefantasy.mf2.item.gadget.EnumFuseType;
import minefantasy.mf2.item.gadget.EnumPowderType;

/**
 * A bomb or a mine: built from a filling, casing, fuse and powder, owned by whoever set it, and set off early by a
 * player's blow or arrow or by fire. Another blast sets off only a crude casing.
 */
public abstract class EntityExplosive extends Entity implements IDamageType {

    private static final int TYPE_ID = 2;
    /**
     * How long the fuse is
     */
    public int fuse;
    private EntityLivingBase thrower;
    /**
     * Name of whoever set it, so attribution survives a chunk reload
     */
    private String placerName = "";

    protected EntityExplosive(World world, float width, float height) {
        super(world);
        this.preventEntitySpawning = true;
        this.setSize(width, height);
        this.yOffset = this.height / 2.0F;
    }

    /** Thrown from the eyes of whoever sets it, this hard. */
    protected EntityExplosive(World world, float width, float height, EntityLivingBase thrower, float force) {
        this(world, width, height);
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

        float f = 0.4F;
        this.motionX = -MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI)
                * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI)
                * f;
        this.motionZ = MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI)
                * MathHelper.cos(this.rotationPitch / 180.0F * (float) Math.PI)
                * f;
        this.motionY = -MathHelper.sin(this.rotationPitch / 180.0F * (float) Math.PI) * f;
        this.setThrowableHeading(this.motionX, this.motionY, this.motionZ, force, 1.0F);

        this.prevPosX = posX;
        this.prevPosY = posY;
        this.prevPosZ = posZ;
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

    @Override
    protected void entityInit() {
        dataWatcher.addObject(TYPE_ID, (byte) 0);
        dataWatcher.addObject(TYPE_ID + 1, (byte) 0);
        dataWatcher.addObject(TYPE_ID + 2, (byte) 0);
        dataWatcher.addObject(TYPE_ID + 3, (byte) 0);
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
        return !this.isDead;
    }

    // region setting off

    /**
     * A player's blow or arrow sets it off, as whoever struck it. A creature's does not, so a skeleton shooting at a
     * player cannot spring the player's own mine. Another blast sets off only a crude casing, too poorly made to hold.
     */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isDead || isEntityInvulnerable()) return false;
        if (source.isExplosion()) {
            if (!getCase().setOffByBlasts) return false;
            if (!worldObj.isRemote) setOff(null);
            return true;
        }
        if (source.isFireDamage()) {
            if (!worldObj.isRemote) setOff(null);
            return true;
        }
        if (!(source.getEntity() instanceof EntityPlayer)) return false;
        if (!worldObj.isRemote) setOff((EntityPlayer) source.getEntity());
        setBeenAttacked();
        return true;
    }

    /** Catching fire, from a burning arrow, lava or a fire, sets it off. */
    @Override
    public void setFire(int seconds) {
        if (!isDead && !worldObj.isRemote) setOff(null);
    }

    /**
     * Sets it off early. Whoever did is then behind the blast, so protection weighs them; with nobody, the owner still
     * is.
     */
    public final void setOff(EntityPlayer by) {
        if (by != null) {
            thrower = by;
            placerName = by.getCommandSenderName();
        }
        onSetOff();
    }

    /** Shortens the fuse for a blast soon after. */
    protected abstract void onSetOff();

    // endregion

    // region blast

    public void explode() {
        Blast blast = new Blast(this, getRangeOfBlast(), getDamage(), damageSource());
        if (!getCase().falloff) blast.noFalloff();
        shape(blast);
        blast.explode();
    }

    /** The source of the blast's damage: dealt by whoever set it, so protection weighs that player. */
    protected abstract DamageSource damageSource();

    /** What only this kind of blast does: its filling, shrapnel and what it is stuck to. */
    protected abstract void shape(Blast blast);

    protected static DamageSource damageSource(String type, Entity explosive, Entity user) {
        return new EntityDamageSourceIndirect(type, explosive, user);
    }

    protected double getRangeOfBlast() {
        return getBlast().range * getCase().rangeModifier * getPowderType().rangeModifier;
    }

    protected float getDamage() {
        return getBlast().damage * getCase().damageModifier * getPowderType().damageModifier;
    }

    /**
     * What its fragments strike as: blunt and piercing, or only blunt when burning. The blast itself is blunt, as
     * armour takes every explosion.
     */
    @Override
    public float[] getDamageRatio(Object... implement) {
        return getFilling() == 2 ? new float[] { 0, 1, 0 } : new float[] { 0, 1, 1 };
    }

    @Override
    public float getPenetrationLevel(Object implement) {
        return 0;
    }

    // endregion

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
        nbt.setString("Placer", thrower == null ? placerName : thrower.getCommandSenderName());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        // Restore the components straight into the DataWatcher: setType would reset the timer to a full fuse
        dataWatcher.updateObject(TYPE_ID, nbt.getByte("Filling"));
        dataWatcher.updateObject(TYPE_ID + 1, nbt.getByte("Casing"));
        dataWatcher.updateObject(TYPE_ID + 2, nbt.getByte("FuseType"));
        dataWatcher.updateObject(TYPE_ID + 3, nbt.getByte("Powder"));
        placerName = nbt.getString("Placer");
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
        return getPlacer();
    }

    /**
     * Resolves the placer lazily: after a reload only the name survives, and the player may be offline.
     */
    protected EntityLivingBase getPlacer() {
        if (thrower == null && worldObj != null && placerName != null && !placerName.isEmpty()) {
            thrower = worldObj.getPlayerEntityByName(placerName);
        }
        return thrower;
    }

    /** Whether this is whoever set it, also after a reload while they are away. */
    protected boolean isPlacer(Entity entity) {
        if (entity == null) return false;
        if (entity == thrower) return true;
        return entity instanceof EntityPlayer && !placerName.isEmpty()
                && placerName.equals(entity.getCommandSenderName());
    }

    public byte getFilling() {
        return dataWatcher.getWatchableObjectByte(TYPE_ID);
    }

    public byte getCasing() {
        return dataWatcher.getWatchableObjectByte(TYPE_ID + 1);
    }

    public byte getFuse() {
        return dataWatcher.getWatchableObjectByte(TYPE_ID + 2);
    }

    public byte getPowder() {
        return dataWatcher.getWatchableObjectByte(TYPE_ID + 3);
    }

    protected void setComponents(byte fill, byte casing, byte fuse, byte powder) {
        dataWatcher.updateObject(TYPE_ID, fill);
        dataWatcher.updateObject(TYPE_ID + 1, casing);
        dataWatcher.updateObject(TYPE_ID + 2, fuse);
        dataWatcher.updateObject(TYPE_ID + 3, powder);
        this.fuse = getFuseType().time;
    }

    protected EnumExplosiveType getBlast() {
        return EnumExplosiveType.getType(getFilling());
    }

    protected EnumCasingType getCase() {
        return EnumCasingType.getType(getCasing());
    }

    protected EnumFuseType getFuseType() {
        return EnumFuseType.getType(getFuse());
    }

    protected EnumPowderType getPowderType() {
        return EnumPowderType.getType(getPowder());
    }
}
