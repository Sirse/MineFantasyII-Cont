package minefantasy.mf2.client;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.entity.Fragments;

/**
 * What a blast looks like: a cloud of smoke, the trails of its fragments along the paths the server hurt along, and a
 * shake for whoever is near. Whoever it hurts the server stuns with blindness.
 */
@SideOnly(Side.CLIENT)
public final class BlastEffects {

    private BlastEffects() {}

    public static void show(World world, double x, double y, double z, float radius, int filling, boolean upward,
            int fragments, float fragmentRange, long seed) {
        Random random = new Random(seed ^ 0x5DEECE66DL);
        Vec3 centre = Vec3.createVectorHelper(x, y, z);
        boolean fire = filling == 2;
        cloud(world, random, x, y, z, radius, fire);
        for (Vec3 direction : Fragments.directions(seed, fragments, upward)) {
            trail(world, random, centre, direction, Fragments.end(world, centre, direction, fragmentRange), fire);
        }
        shake(world, centre, radius);
    }

    /** The ground shakes behind walls too, so only distance matters. */
    private static void shake(World world, Vec3 centre, float radius) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || player.worldObj != world) return;
        double distance = centre
                .distanceTo(Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ));
        CameraShake.blast(radius, distance, world.getTotalWorldTime());
    }

    private static void cloud(World world, Random random, double x, double y, double z, float radius, boolean fire) {
        int puffs = 8 + (int) (radius * 4);
        double spread = radius * 0.4D;
        for (int i = 0; i < puffs; i++) {
            double px = x + (random.nextDouble() - 0.5D) * spread;
            double py = y + random.nextDouble() * spread * 0.5D;
            double pz = z + (random.nextDouble() - 0.5D) * spread;
            world.spawnParticle("largesmoke", px, py, pz, 0D, 0.05D + random.nextDouble() * 0.05D, 0D);
            if (fire) world.spawnParticle("flame", px, py, pz, 0D, 0.02D, 0D);
        }
    }

    private static void trail(World world, Random random, Vec3 start, Vec3 direction, Vec3 end, boolean fire) {
        double length = start.distanceTo(end);
        for (double along = 0.5D; along < length; along += 0.6D) {
            double jitter = random.nextDouble() * 0.3D;
            world.spawnParticle(
                    fire ? "flame" : "crit",
                    start.xCoord + direction.xCoord * (along + jitter),
                    start.yCoord + direction.yCoord * (along + jitter),
                    start.zCoord + direction.zCoord * (along + jitter),
                    direction.xCoord * 0.2D,
                    direction.yCoord * 0.2D,
                    direction.zCoord * 0.2D);
        }
        world.spawnParticle("smoke", end.xCoord, end.yCoord, end.zCoord, 0D, 0.02D, 0D);
    }
}
