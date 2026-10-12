package minefantasy.mf2.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * The blast of a bomb, a mine or an explosive arrow: no block is broken, living things within the radius are hurt, past
 * half of it less with distance and cover, and a filling throws shrapnel, burning for fire.
 */
final class Blast {

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

    /** The filling: 1 throws shrapnel, 2 burning shrapnel and fire damage, setting those hurt alight this long. */
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

    /** Shrapnel thrown up and out, as from the ground, instead of every way. */
    Blast upward() {
        upward = true;
        return this;
    }

    void explode() {
        World world = bomb.worldObj;
        world.playSoundAtEntity(bomb, "random.explode", 0.3F, 5F);
        world.createExplosion(bomb, bomb.posX, bomb.posY, bomb.posZ, 0, false);
        if (world.isRemote) return;
        hurt(world);
        throwShrapnel(world);
        bomb.setDead();
    }

    private void hurt(World world) {
        Vec3 centre = Vec3.createVectorHelper(bomb.posX, bomb.posY + 0.5D, bomb.posZ);
        double area = radius * 2D;
        AxisAlignedBB box = bomb.boundingBox.expand(area, area / 2, area);
        List<?> near = world.getEntitiesWithinAABB(EntityLivingBase.class, box);
        double half = radius / 2;
        for (Object value : near) {
            Entity hit = (Entity) value;
            double distance = bomb.getDistanceToEntity(hit);
            if (distance >= radius) continue;
            float dealt = damage;
            if (falloff && distance > half) {
                dealt *= 1F - (float) (Math.min(distance - half, half) / half);
            }
            dealt *= world.getBlockDensity(centre, hit.boundingBox);
            if (dealt <= 0F) continue;
            if (hit == ridden) dealt *= 1.5F;
            if (hit.attackEntityFrom(source, dealt) && filling == 2 && burnSeconds > 0) {
                hit.setFire(burnSeconds);
            }
        }
    }

    private void throwShrapnel(World world) {
        if (filling <= 0) return;
        float range = 0.6F;
        for (int a = 0; a < 16; a++) {
            double x = (bomb.worldObj.rand.nextDouble() - 0.5) * (upward ? range / 2 : range);
            double y = upward ? bomb.worldObj.rand.nextDouble() * range
                    : (bomb.worldObj.rand.nextDouble() - 0.5) * range;
            double z = (bomb.worldObj.rand.nextDouble() - 0.5) * (upward ? range / 2 : range);
            EntityShrapnel shrapnel = new EntityShrapnel(world, bomb.posX, bomb.posY + 0.5D, bomb.posZ, x, y, z);
            if (filling == 2) shrapnel.setFire(10);
            world.spawnEntityInWorld(shrapnel);
        }
    }
}
