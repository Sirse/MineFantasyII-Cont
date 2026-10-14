package minefantasy.mf2.entity;

import java.util.Random;

import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * The paths of a blast's fragments: straight lines from its centre, stopped by the first solid block. The server hurts
 * what they cross and the client draws them from the same seed, so both see the same spray without each fragment being
 * sent.
 */
public final class Fragments {

    private Fragments() {}

    /** Directions of unit length, spread evenly over a sphere, or over its upper half for a blast from the ground. */
    public static Vec3[] directions(long seed, int count, boolean upward) {
        Random random = new Random(seed);
        Vec3[] directions = new Vec3[count];
        for (int i = 0; i < count; i++) {
            double y = upward ? 0.1D + random.nextDouble() * 0.9D : random.nextDouble() * 2D - 1D;
            double angle = random.nextDouble() * Math.PI * 2D;
            double flat = Math.sqrt(1D - y * y);
            directions[i] = Vec3
                    .createVectorHelper(flat * MathHelper.cos((float) angle), y, flat * MathHelper.sin((float) angle));
        }
        return directions;
    }

    /** Where a fragment from the start along a direction stops: the first solid block, or the end of its range. */
    public static Vec3 end(World world, Vec3 start, Vec3 direction, double range) {
        Vec3 far = start.addVector(direction.xCoord * range, direction.yCoord * range, direction.zCoord * range);
        MovingObjectPosition hit = world.func_147447_a(
                Vec3.createVectorHelper(start.xCoord, start.yCoord, start.zCoord),
                far,
                false,
                true,
                false);
        return hit == null ? far : hit.hitVec;
    }
}
