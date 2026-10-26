package minefantasy.mf2.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

import minefantasy.mf2.entity.EntityDragonBreath;

/**
 * A dragon's breath, on the server. It is drawn before it is let out, so it can be seen coming: the jaw opens and the
 * roar sounds, and only then does it come, along a line set as it starts, aimed where the prey will be, that turns
 * after the prey no faster than the dragon can turn its head and never out past where the head can turn. Spent, the
 * dragon catches its breath a while before it attacks again.
 */
final class DragonBreathAttack {

    static final int WINDUP = 16, RECOVERY = 30;
    /** Ticks the prey has to be watched before the dragon has its pace and breathes. */
    private static final int WATCH_TICKS = 5;
    /** Breaths let out together each tick of a breath. */
    private static final int SPREAD = 4;

    private final EntityDragon dragon;
    private int windup, recovery;
    /** Ticks of breath left to let out; saved with the dragon. */
    int exhale;
    private double aimX, aimY, aimZ;
    /** The prey's pace as the dragon has watched it, in blocks per tick. */
    private Entity watched;
    private double watchedX, watchedY, watchedZ, paceX, paceY, paceZ;
    private int watchedTicks;

    DragonBreathAttack(EntityDragon dragon) {
        this.dragon = dragon;
    }

    /** Drawing, letting out or catching its breath: the dragon starts no other attack meanwhile. */
    boolean busy() {
        return windup > 0 || exhale > 0 || recovery > 0;
    }

    /** Drawing or letting out a breath, the dragon squares up to its prey. */
    boolean facingPrey() {
        return windup > 0 || exhale > 0;
    }

    boolean drawing() {
        return windup > 0;
    }

    boolean exhaling() {
        return exhale > 0;
    }

    /** Whether it has watched its prey long enough to breathe at it. */
    boolean hasPace() {
        return watchedTicks >= WATCH_TICKS;
    }

    /** Begins drawing a breath, to be let out once drawn. */
    void draw() {
        windup = WINDUP;
        dragon.worldObj.playSoundAtEntity(dragon, "minefantasy2:mob.dragon.fire", 1, 1);
    }

    /**
     * A tick of the breath: drawn, let out along its line, spent. A target lost or gone ends it; one saved mid-breath
     * picks up its line again from where the target is.
     */
    void tick(Entity target) {
        watch(target);
        if (recovery > 0) --recovery;
        if (target == null) {
            // A breath cut short is spent all the same; one only drawn is not
            if (exhale > 0) recovery = RECOVERY;
            windup = exhale = 0;
            return;
        }
        if (windup > 0) {
            dragon.lookAt(target);
            // It holds the breath until it has turned far enough that its head bears on the prey
            float bearing = MathHelper.wrapAngleTo180_float(
                    EntityDragon.heading(target.posX - dragon.posX, target.posZ - dragon.posZ)
                            - dragon.renderYawOffset);
            if (windup == 1 && Math.abs(bearing) > EntityDragon.HEAD_ARC) return;
            if (--windup > 0) return;
            aimX = aimY = aimZ = 0.0D;
            exhale = dragon.getType().fireTimer;
        }
        if (exhale <= 0) return;
        dragon.lookAt(target);
        breathe(target);
        if (--exhale == 0) recovery = RECOVERY;
    }

    /** Follows the prey's pace from tick to tick; players move on the server without a motion of their own. */
    private void watch(Entity target) {
        if (target == null || target != watched) {
            watched = target;
            watchedTicks = 0;
            paceX = paceY = paceZ = 0.0D;
        } else {
            ++watchedTicks;
            // Smoothed, so one stumble does not throw the aim
            paceX = (paceX + target.posX - watchedX) * 0.5D;
            paceY = (paceY + target.posY - watchedY) * 0.5D;
            paceZ = (paceZ + target.posZ - watchedZ) * 0.5D;
        }
        if (target != null) {
            watchedX = target.posX;
            watchedY = target.posY;
            watchedZ = target.posZ;
        }
    }

    private void breathe(Entity target) {
        double x = target.posX - dragon.posX;
        double y = target.boundingBox.minY + target.height / 2.0F - (dragon.posY + dragon.height / 2.0F);
        double z = target.posZ - dragon.posZ;
        // Aimed where the target will be when the breath gets there
        double ticks = flightTicks(MathHelper.sqrt_double(x * x + y * y + z * z));
        x += paceX * ticks;
        y += paceY * ticks;
        z += paceZ * ticks;
        double length = MathHelper.sqrt_double(x * x + y * y + z * z);
        if (length < 1.0E-4D) return;
        x /= length;
        y /= length;
        z /= length;
        if (aimX == 0.0D && aimY == 0.0D && aimZ == 0.0D) {
            // The line is set where the breath starts
            aimX = x;
            aimY = y;
            aimZ = z;
        } else {
            // and then turns after the prey only so fast: moving off across it is how to get out of it
            double angle = Math.acos(Math.max(-1.0D, Math.min(1.0D, aimX * x + aimY * y + aimZ * z)));
            double share = angle <= 1.0E-6D ? 1.0D : Math.min(1.0D, turn(dragon.getTier()) / angle);
            aimX += (x - aimX) * share;
            aimY += (y - aimY) * share;
            aimZ += (z - aimZ) * share;
            double aim = MathHelper.sqrt_double(aimX * aimX + aimY * aimY + aimZ * aimZ);
            aimX /= aim;
            aimY /= aim;
            aimZ /= aim;
        }
        // Never out past where its head can turn: a dragon passing over its prey breathes ahead, not back over itself
        float off = MathHelper.wrapAngleTo180_float(EntityDragon.heading(aimX, aimZ) - dragon.renderYawOffset);
        if (Math.abs(off) > EntityDragon.HEAD_ARC) {
            double level = MathHelper.sqrt_double(aimX * aimX + aimZ * aimZ);
            double turned = Math.toRadians(
                    dragon.renderYawOffset
                            + MathHelper.clamp_float(off, -EntityDragon.HEAD_ARC, EntityDragon.HEAD_ARC));
            aimX = -Math.sin(turned) * level;
            aimZ = Math.cos(turned) * level;
        }
        // Given at the length of the shot: the breath's spread is added to it, and would scatter a bare direction
        for (int a = 0; a < SPREAD; a++) {
            EntityDragonBreath breath = new EntityDragonBreath(
                    dragon.worldObj,
                    dragon,
                    aimX * length,
                    aimY * length,
                    aimZ * length,
                    1.0F).setDamage(dragon.getType().fireDamage).setType(dragon.getType().rangedAttack.id);
            breath.posX = dragon.posX + aimX;
            breath.posY = dragon.posY + dragon.height / 2.0F + 0.5D;
            breath.posZ = dragon.posZ + aimZ;
            dragon.worldObj.spawnEntityInWorld(breath);
        }
    }

    /** Radians a tick the line of a breath turns after the prey: an older dragon follows it quicker. */
    private static double turn(int tier) {
        return Math.toRadians(2.0D + tier);
    }

    /**
     * Ticks a breath takes to cover {@code distance}: a fireball starts still and gains 0.1 a tick, keeping 0.95 of its
     * speed, as {@link net.minecraft.entity.projectile.EntityFireball} moves it.
     */
    private static double flightTicks(double distance) {
        double covered = 0.0D, speed = 0.0D;
        int ticks = 0;
        while (covered < distance && ticks < 100) {
            covered += speed;
            speed = (speed + 0.1D) * 0.95D;
            ticks++;
        }
        return ticks;
    }
}
