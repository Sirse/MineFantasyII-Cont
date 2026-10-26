package minefantasy.mf2.entity.mob;

import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.armour.IArmourPenetrationMob;
import minefantasy.mf2.api.armour.IArmouredEntity;
import minefantasy.mf2.api.helpers.ArmourCalculator;
import minefantasy.mf2.api.helpers.PlayerModeHelper;
import minefantasy.mf2.api.helpers.PowerArmour;
import minefantasy.mf2.api.helpers.TacticalManager;
import minefantasy.mf2.config.ConfigMobs;
import minefantasy.mf2.entity.Shockwave;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.ToolListMF;

public class EntityDragon extends EntityFlyingMF
        implements IMob, IBossDisplayData, IArmouredEntity, IArmourPenetrationMob {

    private static final int dataID = 12;
    /** Ticks a dragon takes to die and lie still before it is gone; vanilla gives every creature twenty. */
    public static final int DEATH_TICKS = 60;
    public static int interestTimeSeconds = 90;
    public static float heartChance = 1.0F;
    public int courseChangeCooldown;
    public double waypointX;
    public double waypointY;
    public double waypointZ;
    public int fireBreathCooldown;
    private Entity targetedEntity;
    private int attackCounter;
    private int targetScanCooldown;
    private Entity lastEnemy;
    private int interestTime;
    /** How far off the dragon still fights its target; past it, it only flies. */
    private static final double HUNT_RANGE = 200.0D;

    /**
     * In flight a dragon circles its prey above and out of reach, then dives on it, as its breath's way of hunting
     * says; landed, it fights on foot a while before taking off again.
     */
    enum Pass {
        CIRCLE,
        DIVE
    }

    static final int GROUND_TICKS = 160;
    private Pass pass = Pass.CIRCLE;
    private int runTicks;
    /** Ticks the target has been out of sight, and how long a player out of sight is searched for. */
    private int unseenTicks;
    static final int SEARCH_TICKS = 40;
    /** How far a dragon rises to go over what stands in its way, and the ticks its hunt waits meanwhile. */
    static final double CLIMB = 8.0D;
    static final int DETOUR_TICKS = 20;
    private int detourTicks;
    /** The share of its health a blow must take to break off a dive or a fight on foot. */
    static final float HEAVY_BLOW = 0.1F;
    private final DragonThreats threats = new DragonThreats();
    private final DragonBreathAttack breath = new DragonBreathAttack(this);
    /** Degrees the body turns a tick at most, for the youngest; bigger dragons swing round wider. */
    static final float BODY_TURN = 10.0F;
    /** How far the head turns from the body to follow the prey. */
    static final float HEAD_ARC = 75.0F;
    /** Ticks after a heavy blow broke off its attack during which another does not. */
    static final int BREAK_OFF_TICKS = 100;
    private int brokeOffAt = -BREAK_OFF_TICKS;
    /** How far above open water or lava a wandering dragon keeps. */
    static final double WATER_CLEARANCE = 6.0D;
    /** Where the target was last seen, searched for while it is out of sight. */
    private double seenX, seenY, seenZ;
    /** Which way round the dragon circles, 1 or -1. */
    private int circling = 1;
    /** The pose on the client; the server never advances it. */
    private final DragonAnimation animation = new DragonAnimation();

    /** Ticks into its death at which vanilla settles a creature's reward, which a dragon keeps. */
    private static final int REWARD_TICK = 20;
    /** The experience settled at {@link #REWARD_TICK}, paid when the body is gone. */
    private int deathExperience;

    /**
     * Vanilla's death, given longer: a dragon this size needs time to fall. Whether a kill earns experience, and how
     * much, is still settled at twenty ticks as vanilla does, so the longer fall gives a player's hit no less time to
     * count; the experience is dropped when the body is removed.
     */
    @Override
    protected void onDeathUpdate() {
        ++deathTime;
        if (deathTime == REWARD_TICK && !worldObj.isRemote
                && (recentlyHit > 0 || isPlayer())
                && func_146066_aG()
                && worldObj.getGameRules().getGameRuleBooleanValue("doMobLoot")) {
            deathExperience = getExperiencePoints(attackingPlayer);
        }
        if (deathTime < DEATH_TICKS) return;
        while (deathExperience > 0) {
            int orb = EntityXPOrb.getXPSplit(deathExperience);
            deathExperience -= orb;
            worldObj.spawnEntityInWorld(new EntityXPOrb(worldObj, posX, posY, posZ, orb));
        }
        setDead();
        for (int i = 0; i < 20; ++i) {
            worldObj.spawnParticle(
                    "explode",
                    posX + (rand.nextFloat() * 2.0F - 1.0F) * width,
                    posY + rand.nextFloat() * height,
                    posZ + (rand.nextFloat() * 2.0F - 1.0F) * width,
                    rand.nextGaussian() * 0.02D,
                    rand.nextGaussian() * 0.02D,
                    rand.nextGaussian() * 0.02D);
        }
    }

    public EntityDragon(World world) {
        super(world);
        this.setSize(4.0F, 3.0F);
        this.isImmuneToFire = true;
        this.experienceValue = 50;
        stepHeight = 1.25F;
    }

    @Override
    protected void fall(float distance) {
        if (isTerrestrial()) {
            float power = Math.min(distance / 2F, 3F) * getScale();
            createShockwave(posX, posY, posZ, power / 2F, ConfigMobs.dragonGriefGeneral);
        }
    }

    public void setDragon(int tier) {
        setTier(tier);
        setBreed(DragonBreed.getRandomDragon(this, tier));
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(getType().health);
        setHealth(getMaxHealth());
        this.setSize(3.0F * getScale(), 2.0F * getScale());
        stepHeight = 1.25F + (tier * 0.25F);
        this.experienceValue = 50 * (tier + 1);
    }

    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
        setDragon(getRandomTier());
        return super.onSpawnWithEgg(data);
    }

    private int getRandomTier() {
        float f = rand.nextFloat() * 100F;

        if (f < 25F) {
            return 0;// 0-25 (25%)
        }
        if (f < 50F) {
            return 2;// 25-50 (25%)
        }
        if (f < 59F) {
            return 3;// 50-59 (9%)
        }
        if (f < 60F)// 59-60 (1%)
        {
            return 4;// 55-60
        }
        return 1;// 60-100 40%
    }

    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(dataID, Byte.valueOf((byte) 0));
        this.dataWatcher.addObject(dataID + 1, Float.valueOf(0F));
        this.dataWatcher.addObject(dataID + 2, Float.valueOf(0F));
        this.dataWatcher.addObject(dataID + 3, Integer.valueOf(0));
        this.dataWatcher.addObject(dataID + 4, Integer.valueOf(0));
        this.dataWatcher.addObject(dataID + 5, Integer.valueOf(rand.nextInt(5)));
        // Where it is in a breath, for the client to open its jaw and lift its tail through it
        this.dataWatcher.addObject(dataID + 6, Byte.valueOf((byte) 0));
    }

    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(ConfigMobs.dragonHP);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(1.5D);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (fireBreathCooldown > 0) --fireBreathCooldown;

        if (!isTerrestrial()) {
            this.fallDistance = 0;
        }
        if (!worldObj.isRemote && getHealth() <= 0.0F) {
            // A dead dragon neither flies nor thinks: it drops where it died and lies there
            setTerrestrial(true);
        } else if (!worldObj.isRemote) {

            int disengageTime = getDisengageTime();
            if (disengageTime > 0) {
                targetedEntity = null;
                setDisengageTime(--disengageTime);
                // Its break over, it comes back for the player it broke off from, if it can see them
                if (disengageTime == 0) {
                    if (lastEnemy != null && this.canAttackEntity(lastEnemy)) setTarget(lastEnemy);
                    lastEnemy = null;
                }
            }

            if (ticksExisted % 20 == 0) threats.fade();
            chooseFlight();

            breath.tick(targetedEntity);
            byte stage = (byte) (breath.drawing() ? BREATH_DRAWN : breath.exhaling() ? BREATH_LET_OUT : 0);
            if (dataWatcher.getWatchableObjectByte(dataID + 6) != stage) dataWatcher.updateObject(dataID + 6, stage);

            if (targetedEntity == null && (rand.nextInt(100) == 0) && disengageTime <= 0) {
                wanderTo(
                        this.posX + (this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F,
                        this.posY + (this.rand.nextFloat() * 1.5F - 1.0F) * 12.0F,
                        this.posZ + (this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F);
            }
            if (targetedEntity == null && (rand.nextInt(100) == 0) && disengageTime > 0) {
                wanderTo(
                        this.posX + (this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F,
                        this.posY - 4D,
                        this.posZ + (this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F);
            }
            if (getHealth() < getMaxHealth() && this.ticksExisted % 100 == 0 && getType().regenRate > 0) {
                heal(getType().regenRate);
                if (getHealth() > getMaxHealth()) {
                    setHealth(getMaxHealth());
                }
            }
            int time = getInterestTime();
            int maxTime = ((getHealth() > (getMaxHealth() * 0.75F)) ? time : time * 2) * 20;
            if (dimension != -1) {
                ++interestTime;
                if (interestTime > maxTime && worldObj.canBlockSeeTheSky((int) posX, (int) posY, (int) posZ)
                        && posY > 128F) {
                    if (ConfigMobs.dragonMSG) {
                        List list = worldObj.playerEntities;
                        Iterator players = list.iterator();
                        while (players.hasNext()) {
                            Object instance = players.next();
                            if (instance instanceof EntityPlayer) {
                                if (((EntityPlayer) instance).getDistanceToEntity(this) < 128D) {
                                    ((EntityPlayer) instance).addChatMessage(
                                            new ChatComponentText(
                                                    StatCollector.translateToLocal("event.dragonaway.name")));
                                }
                            }
                        }
                    }
                    this.setDead();
                }
                if (interestTime > maxTime - 60 && worldObj.canBlockSeeTheSky((int) posX, (int) posY, (int) posZ)) {
                    this.waypointY = this.posY + 4D;
                }
            }
        } else {
            float jawMove = getJawMove();
            if (jawMove > 0) {
                --jawMove;
            }
            setJawMove(jawMove);
            float neckAngle = getNeckAngle();
            if (neckAngle > 0) {
                --neckAngle;
            }
            setNeckAngle(neckAngle);

            if (animation.tick(this)) {
                worldObj.playSoundAtEntity(this, "mob.enderdragon.flap", 0.5F, 1.5F);
            }
        }
    }

    private int getInterestTime() {
        return interestTimeSeconds;
    }

    /** Stages of a breath as the client is told them. */
    private static final byte BREATH_DRAWN = 1, BREATH_LET_OUT = 2;

    /** Whether it is drawing a breath, as the client is told. */
    public boolean showsDrawingBreath() {
        return dataWatcher.getWatchableObjectByte(dataID + 6) == BREATH_DRAWN;
    }

    /** Whether it is drawing or letting out a breath, as the client is told: its jaw stays open all the while. */
    public boolean showsBreath() {
        return dataWatcher.getWatchableObjectByte(dataID + 6) != 0;
    }

    /**
     * Turns the body toward a heading, no faster than its size allows: a big dragon swings round wide, and its body
     * follows its flight rather than the prey.
     */
    private void turnBody(float heading) {
        float most = BODY_TURN / Math.max(1.0F, getScale());
        rotationYaw += MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(heading - rotationYaw), -most, most);
        renderYawOffset = rotationYaw;
    }

    /** The heading of a course, in the game's degrees. */
    static float heading(double dx, double dz) {
        return -(float) Math.atan2(dx, dz) * 180.0F / (float) Math.PI;
    }

    /** The head follows the prey as far round as the neck allows; the body keeps its own heading. */
    void lookAt(Entity target) {
        double dx = target.posX - posX, dz = target.posZ - posZ;
        double dy = target.posY + target.getEyeHeight() - (posY + getEyeHeight());
        float yaw = MathHelper.wrapAngleTo180_float(heading(dx, dz) - renderYawOffset);
        rotationYawHead = renderYawOffset + MathHelper.clamp_float(yaw, -HEAD_ARC, HEAD_ARC);
        rotationPitch = -(float) Math.toDegrees(Math.atan2(dy, MathHelper.sqrt_double(dx * dx + dz * dz)));
    }

    /**
     * Sets a waypoint to wander to, never down into water or lava: over open liquid the dragon keeps a little above it,
     * as it would sink and drown in it. Over land it may come down where it likes.
     */
    private void wanderTo(double x, double y, double z) {
        waypointX = x;
        waypointY = y;
        waypointZ = z;
        int bx = MathHelper.floor_double(x), bz = MathHelper.floor_double(z);
        if (!worldObj.blockExists(bx, 0, bz) || worldObj.provider.hasNoSky) return;
        int top = worldObj.getHeightValue(bx, bz);
        if (top > 0 && worldObj.getBlock(bx, top - 1, bz).getMaterial().isLiquid()) {
            waypointY = Math.max(y, top + WATER_CLEARANCE);
        }
    }

    /**
     * Whether the dragon walks or flies, decided in one place: it lands on touching the ground or on dropping onto its
     * prey at the end of a dive, and takes off now and then when idle or when its prey stands above it. Fighting on
     * foot and taking off again is part of {@link #attackRun}.
     */
    private void chooseFlight() {
        Entity target = targetedEntity;
        if (isInWater() || handleLavaMovement()) {
            // A dragon does not swim: in water it beats its way up out of it, whatever it was doing
            setTerrestrial(false);
            motionY = Math.max(motionY, 0.3D);
            waypointY = Math.max(waypointY, posY + CLIMB);
            return;
        }
        if (onGround) {
            setTerrestrial(true);
        }
        // Circling, it passes over its prey without landing; a dive ends on it
        if (target != null && (pass == Pass.DIVE || isTerrestrial())
                && target.posY < posY - 5.0D
                && getDistance(target.posX, posY, target.posZ) < 2.0D) {
            setTerrestrial(true);
        }
        if (target == null ? rand.nextInt(100) == 0 : target.posY > posY + 2.0D) {
            setTerrestrial(false);
            jump();
        }
    }

    /**
     * A pass at the prey: in flight the dragon circles above it out of reach, breathing on it as the cooldown allows,
     * then dives to bite and climbs to circle again; landed, it fights on foot a while before taking off.
     */
    private void attackRun(Entity target) {
        ++runTicks;
        // Going over something in its way comes first; the pass picks up again once it is past
        if (detourTicks > 0 && !isTerrestrial()) return;
        // Out of sight, it searches where it last saw its prey, not where the prey has gone since
        if (unseenTicks > 0) {
            waypointX = seenX;
            waypointY = seenY + (isTerrestrial() ? 0.0D : getType().rangedAttack.circleHeight / 2.0D);
            waypointZ = seenZ;
            return;
        }
        if (isTerrestrial()) {
            waypointX = target.posX;
            waypointY = target.posY;
            waypointZ = target.posZ;
            if (runTicks > GROUND_TICKS) {
                startCircling();
                setTerrestrial(false);
                jump();
            }
            return;
        }
        DragonBreath hunt = getType().rangedAttack;
        if (pass == Pass.DIVE) {
            waypointX = target.posX;
            waypointY = target.posY + target.height / 2.0F;
            waypointZ = target.posZ;
            if (runTicks > hunt.diveTicks) startCircling();
            return;
        }
        // A point a little further round the circle than the dragon, so it keeps flying round
        double angle = Math.atan2(posZ - target.posZ, posX - target.posX) + circling * 0.5D;
        waypointX = target.posX + Math.cos(angle) * hunt.circleRadius;
        waypointY = target.posY + hunt.circleHeight;
        waypointZ = target.posZ + Math.sin(angle) * hunt.circleRadius;
        if (runTicks > hunt.circleTicks) {
            pass = Pass.DIVE;
            runTicks = 0;
        }
    }

    private void startCircling() {
        pass = Pass.CIRCLE;
        runTicks = 0;
        circling = rand.nextBoolean() ? 1 : -1;
    }

    @Override
    protected void updateEntityActionState() {
        if (!this.worldObj.isRemote && this.worldObj.difficultySetting == EnumDifficulty.PEACEFUL) {
            this.setDead();
        }
        this.despawnEntity();
        steer();
        findTarget();
        Entity target = targetedEntity;
        if (target != null && getDisengageTime() <= 0 && target.getDistanceSqToEntity(this) < HUNT_RANGE * HUNT_RANGE) {
            fight(target);
        } else {
            if (motionX * motionX + motionZ * motionZ >= 0.01D) turnBody(heading(motionX, motionZ));
            rotationYawHead = renderYawOffset;
        }
        if (this.attackCounter > 0) {
            --this.attackCounter;
        }
    }

    /**
     * Flies toward the waypoint, a new one picked when it is reached or lost far off. Something in the way sends it up
     * to go over it rather than hanging where it is, and the hunt waits meanwhile.
     */
    private void steer() {
        double dx = this.waypointX - this.posX;
        double dy = this.waypointY - this.posY;
        double dz = this.waypointZ - this.posZ;
        double distance = dx * dx + dy * dy + dz * dz;
        if (distance < 1.0D || distance > 3600.0D) {
            wanderTo(
                    this.posX + (this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F,
                    this.posY + (this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F,
                    this.posZ + (this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F);
        }
        if (this.detourTicks > 0) --this.detourTicks;
        if (this.courseChangeCooldown-- > 0) return;
        this.courseChangeCooldown += this.rand.nextInt(5) + 2;
        distance = MathHelper.sqrt_double(distance);
        if (this.isCourseTraversable(this.waypointX, this.waypointY, this.waypointZ, distance)) {
            double push = 0.5D * getFlightSpeed() / distance;
            this.motionX += dx * push;
            this.motionY += dy * push;
            this.motionZ += dz * push;
        } else {
            this.waypointY = this.posY + CLIMB;
            this.detourTicks = DETOUR_TICKS;
        }
    }

    /** Lets a dead target go, and with none looks for the nearest player it can hunt, then anything, now and then. */
    private void findTarget() {
        if (targetedEntity != null && targetedEntity.isDead) {
            targetedEntity = null;
        }
        if (targetedEntity == null) {
            EntityPlayer closest = this.worldObj.getClosestVulnerablePlayerToEntity(this, 100.0D);
            if (closest != null && canAttackEntity(closest)) {
                targetedEntity = closest;
            }
        }
        if (targetedEntity == null && --this.targetScanCooldown <= 0) {
            this.targetScanCooldown = 20;
            this.setEntityToAttack(EntityPlayer.class);
            if (ConfigMobs.dragonKillNPC && this.targetedEntity == null) {
                this.setEntityToAttack(EntityLiving.class);
            }
        }
    }

    /**
     * A tick of the fight: a player out of sight is searched for a while, a creature given up at once; the dragon
     * turns, then breathes or bites, one attack at a time, and makes its pass.
     */
    private void fight(Entity target) {
        boolean seen = this.canEntityBeSeen(target);
        unseenTicks = seen ? 0 : unseenTicks + 1;
        if (!worldObj.isRemote && unseenTicks > (target instanceof EntityPlayer ? SEARCH_TICKS : 0)) {
            unseenTicks = 0;
            disengage(200);
            return;
        }
        // On foot, or drawing or letting out a breath, it squares up to its prey; else its body follows its course
        if (isTerrestrial() || breath.facingPrey() || motionX * motionX + motionZ * motionZ < 0.01D) {
            turnBody(heading(target.posX - posX, target.posZ - posZ));
        } else {
            turnBody(heading(motionX, motionZ));
        }
        lookAt(target);
        if (seen) {
            seenX = target.posX;
            seenY = target.posY;
            seenZ = target.posZ;
        }

        float range = getAttackRange(), distance = target.getDistanceToEntity(this);
        boolean busy = breath.busy();
        if (!busy && seen && distance > range && distance < range * 6 && fireBreathCooldown <= 0 && breath.hasPace()) {
            breath.draw();
            fireBreathCooldown = 600 + getBreathCooldown() + rand.nextInt(getBreathCooldown());
            // One attack at a time: an older dragon's reach and breath overlap, and it does not bite as it draws
            busy = true;
        }
        if (seen && onGround && distance > range * 2 && distance < range * 8 && rand.nextInt(100) == 0) {
            TacticalManager.lungeEntity(this, target, 3.0F, 1.5F);
        }
        if (!busy && seen
                && fallDistance <= 0
                && distance < range * getScale()
                && ((target.isSneaking() && !target.onGround) || this.attackCounter <= 0)) {
            bite(target);
        }
        // Broken off after its bite, wounded: nothing left to hunt this tick
        if (targetedEntity == null) return;
        attackRun(target);
    }

    /** Bites, knocking a rider it comes down on out of the saddle; a wounded dragon then breaks off. */
    private void bite(Entity target) {
        if (target.isRiding() && posY > target.posY) {
            target.mountEntity(null);
            target.motionY = 1.0F;
            target.motionX = this.motionX;
            target.motionZ = this.motionZ;
            if (target instanceof EntityLivingBase) {
                EntityLivingBase rider = (EntityLivingBase) target;
                rider.addPotionEffect(new PotionEffect(Potion.confusion.id, 200, 5));
                rider.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 200, 5));
                rider.addPotionEffect(new PotionEffect(Potion.weakness.id, 200, 2));
            }
        }
        attackEntity(target, this.getAttackStrength(target));
        this.attackCounter = getAttackTime();
        setJawMove(40);
        // The dive is done once it has bitten: it climbs away to circle again
        if (pass == Pass.DIVE) startCircling();
        if (getHealth() <= this.getLowHPThreshold()) {
            disengage(50);
        }
        worldObj.playSoundAtEntity(this, "minefantasy2:mob.dragon.bite", 1, 1);
    }

    private float getAttackRange() {
        return 6.0F;
    }

    private float getFlightSpeed() {
        return 0.8F;
    }

    @Override
    public boolean canEntityBeSeen(Entity entity) {
        return entity != null && super.canEntityBeSeen(entity);
    }

    private int getBreathCooldown() {
        return getType().coolTimer;
    }

    private int getAttackTime() {
        return getType().meleeSpeed;
    }

    /**
     * True if the body can fly straight to the point {@code distance} away, past no block and no liquid. The body is
     * looked at in steps no longer than it is wide or tall, so nothing slips between two looks, rather than at every
     * block of the way; a chunk not loaded is neither looked into nor loaded.
     */
    boolean isCourseTraversable(double x, double y, double z, double distance) {
        double dx = (x - this.posX) / distance;
        double dy = (y - this.posY) / distance;
        double dz = (z - this.posZ) / distance;
        double step = Math.max(1.0D, Math.min(this.width, this.height));
        for (double along = step; along < distance + step; along += step) {
            double at = Math.min(along, distance);
            AxisAlignedBB box = this.boundingBox.getOffsetBoundingBox(dx * at, dy * at, dz * at);
            if (!this.worldObj.getCollidingBoundingBoxes(this, box).isEmpty()) {
                return false;
            }
            if (worldObj.checkChunksExist(
                    MathHelper.floor_double(box.minX),
                    MathHelper.floor_double(box.minY),
                    MathHelper.floor_double(box.minZ),
                    MathHelper.floor_double(box.maxX),
                    MathHelper.floor_double(box.maxY),
                    MathHelper.floor_double(box.maxZ)) && worldObj.isAnyLiquid(box)) {
                return false;
            }
        }
        return true;
    }

    protected void attackEntity(Entity entity, float f) {
        float damage = this.getAttackStrength(entity);

        if (rand.nextInt(3) == 0) {
            entity.motionY = 2;
            jump();
            setNeckAngle(10);
            damage = 2.0F;
        }
        worldObj.playSoundAtEntity(this, "minefantasy2:mob.dragon.bite", 1, 1);
        entity.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
    }

    @Override
    public void playLivingSound() {
        super.playLivingSound();
        setJawMove(20);
    }

    private float getAttackStrength(Entity entity) {
        return getType().meleeDamage;
    }

    public void setEntityToAttack(Class enClass) {
        List list = worldObj.getEntitiesWithinAABB(
                enClass,
                AxisAlignedBB.getBoundingBox(posX, posY, posZ, posX + 1.0D, posY + 1.0D, posZ + 1.0D)
                        .expand(getAggro(), getAggro(), getAggro()));
        // Walk the list instead of draining it: getEntitiesWithinAABB hands back an ArrayList, so removing the
        // head per rejected candidate shifted the backing array once for every element still behind it
        double r = getAggro();
        for (int i = 0; i < list.size(); i++) {
            Entity target = (Entity) list.get(i);
            if (canAttackEntity(target) && this.getDistanceToEntity(target) <= r) {
                setTarget(target);
                return;
            }
        }
    }

    public void setTarget(Entity target) {
        this.targetedEntity = target;
    }

    private boolean canAttackEntity(Entity target) {
        // Cheap rejections first: canEntityBeSeen is a ray trace, and it used to run twice for a valid non player
        if (getDisengageTime() > 0 || !isPrey(target)) {
            return false;
        }
        if (this.getType().isBlind() && !this.canHearEntity(target)) {
            return false;
        }
        return this.canEntityBeSeen(target);
    }

    /** Whether a dragon would ever hunt this: not another dragon, its rider, a creative player or a spectator. */
    private boolean isPrey(Entity target) {
        if (target == this || target instanceof EntityDragon || target == this.riddenByEntity) {
            return false;
        }
        if (target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.isCreativeMode) {
            return false;
        }
        return !PlayerModeHelper.isSpectator(target);
    }

    Entity getTarget() {
        return targetedEntity;
    }

    private double getAggro() {
        return 256D;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float damage) {
        if (source.getEntity() != null) {
            this.interestTime = 0;
        }
        if (getDisengageTime() <= 0 && this.getHealth() < getLowHPThreshold()) {
            disengage(100);
        }
        if (source.isFireDamage()) return false;

        if (source == DamageSource.inWall) return false;

        float before = getHealth();
        boolean hurt = super.attackEntityFrom(source, damage);
        // Turns on whoever does it clearly the most harm, never on a player out of play
        Entity attacker = source.getEntity();
        if (hurt && attacker != null
                && isPrey(attacker)
                && threats.note(attacker, before - getHealth(), source.isProjectile(), targetedEntity)
                && getDisengageTime() <= 0) {
            setTarget(attacker);
        }
        // A heavy blow breaks off a dive or a fight on foot: the dragon climbs away to come round again
        // Not again straight after: blow after blow from several hands must not keep it from ever coming down
        if (hurt && getHealth() > 0.0F
                && before - getHealth() >= getMaxHealth() * HEAVY_BLOW
                && (pass == Pass.DIVE || isTerrestrial())
                && ticksExisted - brokeOffAt >= BREAK_OFF_TICKS) {
            brokeOffAt = ticksExisted;
            startCircling();
            setTerrestrial(false);
            jump();
        }
        return hurt;
    }

    /**
     * @return The health threshold where dragons constantly retreat
     */
    private float getLowHPThreshold() {
        return getType().health / 4;
    }

    @Override
    protected String getLivingSound() {
        return "minefantasy2:mob.dragon.say";
    }

    @Override
    protected void func_145780_a(int x, int y, int z, Block floor) {
        this.playSound("minefantasy2:mob.dragon.step", 1.0F, 1.0F);
    }

    /**
     * Returns the sound this mob makes when it is hurt.
     */
    @Override
    protected String getHurtSound() {
        return "minefantasy2:mob.dragon.hurt";
    }

    /**
     * Returns the volume for the sounds this mob makes.
     */
    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    protected float getSoundPitch() {
        return 1.0F;
    }

    protected Item getDropItem() {
        return ComponentListMF.dragon_heart;
    }

    /**
     * Drop 0-2 items of this living's type. @param par1 - Whether this entity has recently been hit by a player. @param
     * par2 - Level of Looting used to kill this mob.
     */
    @Override
    protected void dropFewItems(boolean playerKill, int looting) {
        int count = getLootCount(getTier()) + rand.nextInt(looting + 1);
        for (int a = 0; a < count; a++) {
            Item drop = getLoot(getTier());
            this.dropItem(drop, 1);
        }
        if (getTier() == 4)// ANCIENT
        {
            this.dropItem(Items.nether_star, 1);
        }
        if (didDropHeart(this.getTier())) {
            this.dropItem(ComponentListMF.dragon_heart, 1);
        }
    }

    private Item getLoot(int tier) {
        if (tier == 4)// Ancient
        {
            return ToolListMF.loot_sack_rare;
        }
        return ToolListMF.loot_sack_uc;// Any
    }

    private boolean didDropHeart(int tier) {
        if (tier == 4)// Ancient
        {
            return true;
        }
        if (tier == 3)// Elder
        {
            return true;
        }
        if (tier == 2)// Mature
        {
            return rand.nextFloat() * heartChance > 0.25F;// 75% chance
        }
        if (tier == 1)// Adult
        {
            return rand.nextFloat() * heartChance > 0.85F;// 15% chance
        }
        return false;// Young
    }

    private int getLootCount(int tier) {
        if (tier == 4)// Ancient
        {
            return 1;// 1 Exquisite
        }
        if (tier == 3)// Elder
        {
            return 2 + rand.nextInt(4);// 2-5 Valuable
        }
        if (tier == 2)// Mature
        {
            return 2 + rand.nextInt(1);// 2-3 Valuable
        }
        if (tier == 1)// Adult
        {
            return 1 + rand.nextInt(1);// 1-2 Valuable
        }
        return 1;// Young, 1 Valuable
    }

    /**
     * Checks if the entity's current position is a valid location to spawn this entity.
     */
    @Override
    public boolean getCanSpawnHere() {
        return super.getCanSpawnHere() && this.worldObj.difficultySetting != EnumDifficulty.PEACEFUL;
    }

    /**
     * Will return how many at most can spawn in a chunk at once.
     */
    public int getMaxSpawnedInChunk() {
        return 1;
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);

        nbt.setInteger("FireBreath", breath.exhale);
        nbt.setInteger("FireBreathCooldown", fireBreathCooldown);

        nbt.setInteger("Breed", getBreed());
        nbt.setInteger("Tier", getTier());
        nbt.setInteger("interestTime", interestTime);
        // Settled at twenty ticks into death, paid at its end: a dragon saved between keeps it
        if (deathExperience > 0) nbt.setInteger("DeathExperience", deathExperience);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);

        breath.exhale = nbt.getInteger("FireBreath");
        fireBreathCooldown = nbt.getInteger("FireBreathCooldown");

        setBreed(nbt.getInteger("Breed"));
        setTier(nbt.getInteger("Tier"));
        interestTime = nbt.getInteger("interestTime");
        deathExperience = Math.max(0, nbt.getInteger("DeathExperience"));

    }

    @SideOnly(Side.CLIENT)
    public String getTexture() {
        return getType().tex;
    }

    @SideOnly(Side.CLIENT)
    public DragonAnimation animation() {
        return animation;
    }

    @SideOnly(Side.CLIENT)
    public float getJawMove() {
        return dataWatcher.getWatchableObjectFloat(dataID + 1);
    }

    public void setJawMove(float i) {
        dataWatcher.updateObject(dataID + 1, i);
    }

    @SideOnly(Side.CLIENT)
    public float getNeckAngle() {
        return dataWatcher.getWatchableObjectFloat(dataID + 2);
    }

    public void setNeckAngle(float i) {
        dataWatcher.updateObject(dataID + 2, i);
    }

    public int getDisengageTime() {
        return dataWatcher.getWatchableObjectInt(dataID + 3);
    }

    public void setDisengageTime(int i) {
        dataWatcher.updateObject(dataID + 3, i);
    }

    @Override
    public boolean isTerrestrial() {
        return dataWatcher.getWatchableObjectByte(dataID) == 1;
    }

    public void setTerrestrial(boolean flag) {
        dataWatcher.updateObject(dataID, (byte) (flag ? 1 : 0));
    }

    public double wingAngle() {
        return isTerrestrial() ? 0D : 45D;
    }

    /**
     * Called over a player: it circles off a while before coming down, and then that player is the first it hunts, if
     * it can see them, rather than whoever happens to be nearest.
     */
    public void callTo(EntityPlayer player) {
        setTarget(player);
        disengage(100);
    }

    public void disengage(int time) {
        if (targetedEntity instanceof EntityPlayer) {
            lastEnemy = targetedEntity;
        }
        setDisengageTime(time);
        setTerrestrial(false);
        wanderTo(
                this.posX + (this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F,
                this.posY + (this.rand.nextFloat()) * 16.0F,
                this.posZ + (this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F);
        targetedEntity = null;
    }

    public DragonBreed getType() {
        return DragonBreed.getBreed(getTier(), getBreed());
    }

    public int getBreed() {
        return dataWatcher.getWatchableObjectInt(dataID + 4);
    }

    public void setBreed(int i) {
        dataWatcher.updateObject(dataID + 4, i);
    }

    public int getTier() {
        return dataWatcher.getWatchableObjectInt(dataID + 5);
    }

    public void setTier(int i) {
        dataWatcher.updateObject(dataID + 5, i);
    }

    public String getCommandSenderName() {
        String tierName = StatCollector.translateToLocal("entity." + getType().name + ".name");
        String breedName = StatCollector.translateToLocal("entity.dragonbreed." + getType().breedName + ".name");
        return StatCollector.translateToLocalFormatted(tierName, breedName);
    }

    public float getScale() {
        return 0.6F + (getTier() * 0.2F);
    }

    public Shockwave createShockwave(double x, double y, double z, float power, boolean grief) {
        return newShockwave(x, y, z, power, false, grief);
    }

    /**
     * returns a new explosion. Does initiation (at time of writing Explosion is not finished)
     */
    public Shockwave newShockwave(double x, double y, double z, float power, boolean fire, boolean smoke) {
        Shockwave explosion = new Shockwave("dragonstomp", worldObj, this, x, y, z, power);
        explosion.isFlaming = fire;
        // mobGriefing is weighed for each block the wave would break
        explosion.isGriefing = true;
        explosion.isSmoking = smoke;
        explosion.initiate();
        explosion.decorateWave(true);
        return explosion;
    }

    public float getFirePyro() {
        if (!ConfigMobs.dragonGriefFire) {
            return 0F;
        }
        return getType().pyro;
    }

    @Override
    public float getThreshold(DamageSource src) {
        if (!src.isUnblockable()) {
            return getType().DT;
        }
        return 0;
    }

    @Override
    public float[] getHitTraits() {
        return new float[] { 0, 1, 9 };// 90% Piercing, 10% BLunt
    }

    private boolean canHearEntity(Entity target) {
        if (target instanceof EntityLivingBase && PowerArmour.isWearingCogwork((EntityLivingBase) target)) return true;

        double distance = getDistanceToEntity(target);

        if (distance < width / 2) {
            return true;// touching
        }
        if (distance < 16 && this.getAttackTarget() != null && getAttackTarget() == target) {
            return true;// Smell
        }
        float tarSpeed = (float) Math.hypot(target.motionX, target.motionZ);

        if (tarSpeed < 0.1F) {
            return false;
        }
        if (target instanceof EntityLivingBase) {
            return getSound((EntityLivingBase) target, tarSpeed) >= distance;
        }
        return true;
    }

    private double getSound(EntityLivingBase target, float speed) {
        float value = 10F * (60F + ArmourCalculator.getTotalWeightOfWorn(target, false)) * speed;
        return value;
    }
}
