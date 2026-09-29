package minefantasy.mf2.api.helpers;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Items thrown into the world. Nothing is thrown on the client, nor for an empty stack. */
public final class Drops {

    private Drops() {}

    /** Thrown with the usual random nudge; {@code delay} is the ticks before it can be picked up. */
    public static EntityItem spawn(World world, double x, double y, double z, ItemStack stack, int delay) {
        EntityItem entity = make(world, x, y, z, stack, delay);
        if (entity != null) {
            world.spawnEntityInWorld(entity);
        }
        return entity;
    }

    /** Thrown with the given motion, set before players are told of it. */
    public static EntityItem spawn(World world, double x, double y, double z, ItemStack stack, int delay,
            double motionX, double motionY, double motionZ) {
        EntityItem entity = make(world, x, y, z, stack, delay);
        if (entity != null) {
            entity.motionX = motionX;
            entity.motionY = motionY;
            entity.motionZ = motionZ;
            world.spawnEntityInWorld(entity);
        }
        return entity;
    }

    /** Laid down where it stands, without a nudge. */
    public static EntityItem still(World world, double x, double y, double z, ItemStack stack, int delay) {
        return spawn(world, x, y, z, stack, delay, 0, 0, 0);
    }

    private static EntityItem make(World world, double x, double y, double z, ItemStack stack, int delay) {
        if (world == null || world.isRemote || stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return null;
        }
        EntityItem entity = new EntityItem(world, x, y, z, stack);
        entity.delayBeforeCanPickup = delay;
        return entity;
    }

    /** Thrown from the middle of a block. */
    public static EntityItem fromBlock(World world, int x, int y, int z, ItemStack stack) {
        return spawn(world, x + 0.5D, y + 0.5D, z + 0.5D, stack, 10);
    }

    /** Thrown from a random spot within a block, as a broken block drops. */
    public static EntityItem scattered(World world, int x, int y, int z, ItemStack stack) {
        if (world == null) {
            return null;
        }
        float spread = 0.7F;
        double dx = world.rand.nextFloat() * spread + (1.0F - spread) * 0.5D;
        double dy = world.rand.nextFloat() * spread + (1.0F - spread) * 0.5D;
        double dz = world.rand.nextFloat() * spread + (1.0F - spread) * 0.5D;
        return spawn(world, x + dx, y + dy, z + dz, stack, 10);
    }

    /** Laid at the player's feet, to be picked up at once. */
    public static EntityItem toPlayer(EntityPlayer player, ItemStack stack) {
        return still(player.worldObj, player.posX, player.posY, player.posZ, stack, 0);
    }
}
