package minefantasy.mf2.entity;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.item.gadget.EnumExplosiveType;
import minefantasy.mf2.network.packet.BlastPacket;

/**
 * The blast of a bomb, a mine or an explosive arrow: no block is broken, living things within the radius are hurt, past
 * half of it less with distance and cover, and a filling throws fragments that strike as projectiles, burning for fire.
 */
final class Blast {

    /** Horizontal shove at the centre, a plain hit's knockback, falling to nothing at the edge. */
    static final double PUSH = 0.4D;
    /** How much farther than the blast its fragments fly. */
    static final double FRAGMENT_REACH = 1.5D;
    /**
     * Ticks a blast stuns a player for: at the edge of its radius, and more right at it. Vanilla blindness under a
     * second fades the whole time instead of holding full dark.
     */
    static final int STUN_EDGE = 4;
    static final int STUN_CENTRE = 10;
    /** How far away players see the blast. */
    private static final double SEEN_FROM = 64D;

    private final Entity bomb;
    private final double radius;
    private final float damage;
    private final DamageSource source;
    private boolean falloff = true;
    private int filling;
    private int burnSeconds;
    private Entity ridden;
    private boolean upward;

    /** A blast of this radius and damage, dealt as this source; fire is added for a fire filling. */
    Blast(Entity bomb, double radius, float damage, DamageSource source) {
        this.bomb = bomb;
        this.radius = radius;
        this.damage = damage;
        this.source = source.setExplosion();
    }

    /** Full damage to the edge of the radius instead of less past half of it. */
    Blast noFalloff() {
        falloff = false;
        return this;
    }

    /** The filling: 1 throws fragments, 2 burning fragments and fire damage, setting those hurt alight this long. */
    Blast filling(int filling, int burnSeconds) {
        this.filling = filling;
        this.burnSeconds = burnSeconds;
        if (filling == 2) source.setFireDamage();
        return this;
    }

    /** What the bomb is stuck to, hurt half again as much. */
    Blast ridden(Entity ridden) {
        this.ridden = ridden;
        return this;
    }

    /** Fragments thrown up and out, as from the ground, instead of every way. */
    Blast upward() {
        upward = true;
        return this;
    }

    void explode() {
        World world = bomb.worldObj;
        world.playSoundAtEntity(bomb, "random.explode", 0.3F, 5F);
        world.createExplosion(bomb, bomb.posX, bomb.posY, bomb.posZ, 0, false);
        if (world.isRemote) return;
        long seed = world.rand.nextLong();
        Vec3 centre = centre();
        double reach = Math.max(radius, fragmentRange());
        AxisAlignedBB box = bomb.boundingBox.expand(reach, reach, reach);
        List<?> near = world.getEntitiesWithinAABB(EntityLivingBase.class, box);
        Map<EntityLivingBase, Float> struck = fragments(world, centre, near, seed);
        shake(world, centre);
        for (Object value : near) {
            EntityLivingBase hit = (EntityLivingBase) value;
            hurt(world, centre, hit);
            Float pieces = struck.get(hit);
            if (pieces != null) hitWithFragments(hit, pieces);
        }
        MineFantasyII.packetHandler.sendPacketAround(
                bomb,
                SEEN_FROM,
                new BlastPacket(
                        bomb.posX,
                        centre.yCoord,
                        bomb.posZ,
                        (float) radius,
                        filling,
                        upward,
                        fragmentCount(),
                        (float) fragmentRange(),
                        seed).generatePacket());
        bomb.setDead();
    }

    /** Explosives within the radius and not behind cover feel the blast; each decides whether it holds. */
    private void shake(World world, Vec3 centre) {
        AxisAlignedBB box = bomb.boundingBox.expand(radius, radius, radius);
        for (Object value : world.getEntitiesWithinAABB(EntityExplosive.class, box)) {
            EntityExplosive other = (EntityExplosive) value;
            if (other == bomb || other.isDead || bomb.getDistanceToEntity(other) >= radius) continue;
            if (world.getBlockDensity(centre, other.boundingBox) <= 0F) continue;
            other.attackEntityFrom(source, damage);
        }
    }

    private Vec3 centre() {
        return Vec3.createVectorHelper(bomb.posX, bomb.posY + 0.5D, bomb.posZ);
    }

    private void hurt(World world, Vec3 centre, EntityLivingBase hit) {
        double distance = bomb.getDistanceToEntity(hit);
        if (distance >= radius) return;
        float dealt = damage;
        double half = radius / 2;
        if (falloff && distance > half) {
            dealt *= 1F - (float) (Math.min(distance - half, half) / half);
        }
        dealt *= world.getBlockDensity(centre, hit.boundingBox);
        if (dealt <= 0F) return;
        if (hit == ridden) dealt *= 1.5F;
        double motionX = hit.motionX, motionY = hit.motionY, motionZ = hit.motionZ;
        if (!hit.attackEntityFrom(source, dealt)) return;
        push(hit, distance, motionX, motionY, motionZ);
        stun(hit, distance);
        if (filling == 2 && burnSeconds > 0) hit.setFire(burnSeconds);
    }

    /**
     * A player the blast hurt is stunned a moment, the closer the longer: vanilla blindness, which fades out on its
     * own. A blast farther off or behind a wall hurts nothing and stuns nobody.
     */
    private void stun(EntityLivingBase hit, double distance) {
        if (!(hit instanceof EntityPlayer)) return;
        int ticks = STUN_EDGE + (int) ((STUN_CENTRE - STUN_EDGE) * (1D - distance / radius));
        hit.addPotionEffect(new PotionEffect(Potion.blindness.id, ticks, 0, true));
    }

    // region fragments

    private EnumExplosiveType type() {
        return EnumExplosiveType.getType((byte) filling);
    }

    private int fragmentCount() {
        return filling <= 0 ? 0 : type().fragments;
    }

    private double fragmentRange() {
        return radius * FRAGMENT_REACH;
    }

    /** What each fragment does is scaled as the blast's damage is from its filling's. */
    private float fragmentDamage() {
        EnumExplosiveType type = type();
        return type.damage <= 0F ? 0F : type.fragmentDamage * damage / type.damage;
    }

    /** The fragment damage each living thing takes: every fragment hurts the first one in its path. */
    private Map<EntityLivingBase, Float> fragments(World world, Vec3 centre, List<?> near, long seed) {
        Map<EntityLivingBase, Float> struck = new IdentityHashMap<>();
        float each = fragmentDamage();
        if (each <= 0F) return struck;
        for (Vec3 direction : Fragments.directions(seed, fragmentCount(), upward)) {
            Vec3 end = Fragments.end(world, centre, direction, fragmentRange());
            EntityLivingBase first = null;
            double nearest = Double.MAX_VALUE;
            for (Object value : near) {
                EntityLivingBase candidate = (EntityLivingBase) value;
                // A body still falling, killed a moment ago, shields nothing
                if (!candidate.isEntityAlive()) continue;
                MovingObjectPosition crossed = candidate.boundingBox.calculateIntercept(centre, end);
                if (crossed == null) continue;
                double distance = centre.distanceTo(crossed.hitVec);
                if (distance < nearest) {
                    nearest = distance;
                    first = candidate;
                }
            }
            if (first != null) struck.merge(first, each, Float::sum);
        }
        return struck;
    }

    /**
     * Fragments strike as a projectile, of the bomb's damage type. They land together with the blast, so the blast's
     * invulnerability does not swallow them.
     */
    private void hitWithFragments(EntityLivingBase hit, float pieces) {
        DamageSource fragments = new EntityDamageSourceIndirect(
                source.getDamageType(),
                source.getSourceOfDamage(),
                source.getEntity()).setProjectile();
        hit.hurtResistantTime = 0;
        double motionX = hit.motionX, motionY = hit.motionY, motionZ = hit.motionZ;
        if (!hit.attackEntityFrom(fragments, pieces)) return;
        // The blast has shoved already: fragments do not add vanilla's knockback from whoever set the bomb
        hit.motionX = motionX;
        hit.motionY = motionY;
        hit.motionZ = motionZ;
        if (filling == 2) hit.setFire(Math.max(burnSeconds, 1));
    }

    // endregion

    /**
     * A light shove away from the blast, weaker with distance and knockback resistance. It replaces the knockback the
     * hurt dealt, which pushes away from whoever set the bomb rather than from the bomb.
     */
    private void push(EntityLivingBase hit, double distance, double motionX, double motionY, double motionZ) {
        double strength = PUSH * (1D - distance / radius)
                * (1D - hit.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getAttributeValue());
        double dx = hit.posX - bomb.posX, dz = hit.posZ - bomb.posZ;
        double flat = Math.sqrt(dx * dx + dz * dz);
        hit.motionX = motionX;
        hit.motionY = motionY;
        hit.motionZ = motionZ;
        if (strength <= 0D) return;
        if (flat > 1.0E-4D) {
            hit.motionX += dx / flat * strength;
            hit.motionZ += dz / flat * strength;
        }
        hit.motionY = Math.max(hit.motionY, 0D) + strength * 0.5D;
        hit.velocityChanged = true;
    }
}
