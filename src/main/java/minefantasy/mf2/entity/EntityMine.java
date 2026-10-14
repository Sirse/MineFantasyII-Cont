package minefantasy.mf2.entity;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import minefantasy.mf2.config.ConfigWeapon;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.item.tool.crafting.ItemEAnvilTools;

/**
 * A mine: armed once its fuse runs out, it is sprung by a living thing in reach that it can see, never by whoever set
 * it, and from closer by one sneaking.
 */
public class EntityMine extends EntityExplosive {

    /** Ticks from being sprung to the blast. */
    private static final int SPRUNG = 10;
    /** DataWatcher slot after the four components: whether it lies still on the ground. */
    private static final int LANDED_ID = 6;
    /** Whether the owner was in reach at the last look, so a forgetful owner is rolled for once per approach. */
    private boolean ownerWasNear;

    public EntityMine(World world) {
        super(world, 0.5F, 0.25F);
    }

    public EntityMine(World world, EntityLivingBase thrower) {
        super(world, 0.5F, 0.25F, thrower, 0.5F);
        this.fuse = 40;
    }

    /**
     * Disarms the mine for one sneaking with engineering tools in hand, wearing them by a use. A sprung mine is past
     * disarming.
     */
    @Override
    public boolean interactFirst(EntityPlayer user) {
        ItemStack tools = user.getHeldItem();
        if (isDead || fuse < 0
                || !user.isSneaking()
                || tools == null
                || !(tools.getItem() instanceof ItemEAnvilTools)) {
            return false;
        }
        setDead();
        user.swingItem();
        if (!worldObj.isRemote) {
            tools.damageItem(1, user);
            if (tools.stackSize <= 0) user.destroyCurrentEquippedItem();
            ItemStack item = ToolListMF.mine_custom.createMine(getCasing(), getFilling(), getFuse(), getPowder(), 1);
            if (!user.inventory.addItemStackToInventory(item)) {
                this.entityDropItem(item, 0.0F);
            }
        }
        return true;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(LANDED_ID, (byte) 0);
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (isLanded() && (!worldObj.isRemote || onGround)) {
            // Lying still: neither side moves it, so the client cannot drift off the server's position and hop back.
            // The client still falls the rest of the way, landing where the server did, or it would hang in the air.
            if (!worldObj.isRemote && ticksExisted % 10 == 0 && !hasGround()) setLanded(false);
        } else {
            fall();
        }

        if (fuse > 0) {
            --fuse;
        }

        if (!worldObj.isRemote && fuse == 0 && ticksExisted % 5 == 0 && isSomeoneInReach()) {
            spring();
        }
        if (fuse < 0) {
            fuse--;

            if (fuse < -SPRUNG) {
                this.setDead();

                if (!this.worldObj.isRemote) {
                    this.explode();
                }
            }
        }
    }

    private void fall() {
        this.motionY -= (0.03999999910593033D * getCase().weightModifier);
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.9800000190734863D;
        this.motionY *= 0.9800000190734863D;
        this.motionZ *= 0.9800000190734863D;

        if (this.onGround) {
            this.motionX = this.motionY = this.motionZ = 0;
            if (!worldObj.isRemote) land();
        }
    }

    /**
     * Lies still where it landed, and tells watching clients exactly where: the tracker leaves out moves under an
     * eighth of a block, so a client that fell differently (onto grass, or from a throw at the feet) would otherwise
     * stay put above the ground.
     */
    private void land() {
        setLanded(true);
        ((WorldServer) worldObj).getEntityTracker().func_151247_a(this, new S18PacketEntityTeleport(this));
    }

    private boolean hasGround() {
        return !worldObj.getCollidingBoundingBoxes(this, boundingBox.getOffsetBoundingBox(0, -0.05D, 0)).isEmpty();
    }

    boolean isLanded() {
        return dataWatcher.getWatchableObjectByte(LANDED_ID) != 0;
    }

    private void setLanded(boolean landed) {
        dataWatcher.updateObject(LANDED_ID, (byte) (landed ? 1 : 0));
    }

    /** Whether a living thing other than its owner is in reach and in sight; one sneaking only from half as far. */
    boolean isSomeoneInReach() {
        double radius = getTriggerRadius();
        List<?> list = worldObj
                .getEntitiesWithinAABB(EntityLivingBase.class, boundingBox.expand(radius * 2, radius, radius * 2));
        boolean ownerNear = false;
        boolean sprung = false;
        for (Object value : list) {
            EntityLivingBase e = (EntityLivingBase) value;
            double reach = e.isSneaking() ? radius / 2 : radius;
            if (e.getDistanceToEntity(this) >= reach || !canSee(e)) continue;
            if (isPlacer(e)) {
                ownerNear = true;
                if (springsForOwner()) sprung = true;
            } else {
                sprung = true;
            }
        }
        ownerWasNear = ownerNear;
        return sprung;
    }

    /**
     * Whether the mine springs for its owner now in reach: never, always, or for a forgetful owner by chance, rolled
     * once each time they come within reach rather than every look.
     */
    private boolean springsForOwner() {
        switch (ConfigWeapon.mineOwnerTrigger) {
            case ALWAYS:
                return true;
            case FORGETFUL:
                return !ownerWasNear && rand.nextFloat() < ConfigWeapon.mineOwnerChance;
            default:
                return false;
        }
    }

    private double getTriggerRadius() {
        return Math.max(1.0D, getRangeOfBlast() - 1.5D);
    }

    private boolean canSee(EntityLivingBase entity) {
        Vec3 from = Vec3.createVectorHelper(posX, posY + height, posZ);
        Vec3 to = Vec3.createVectorHelper(entity.posX, entity.posY + entity.getEyeHeight() / 2, entity.posZ);
        return worldObj.func_147447_a(from, to, false, true, false) == null;
    }

    /** Sprung: it clicks and goes off shortly after. Springing it again changes nothing. */
    private void spring() {
        if (fuse < 0) return;
        worldObj.playSoundEffect(posX, posY, posZ, "game.tnt.primed", 1.0F, 0.75F);
        fuse = -1;
    }

    @Override
    protected void onSetOff() {
        spring();
    }

    @Override
    protected DamageSource damageSource() {
        return damageSource("mine", this, getPlacer());
    }

    @Override
    protected void shape(Blast blast) {
        blast.filling(getFilling(), 8).upward();
    }

    public EntityMine setType(byte fill, byte casing, byte fuse, byte powder) {
        setComponents(fill, casing, fuse, powder);
        return this;
    }
}
