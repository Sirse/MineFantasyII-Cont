package minefantasy.mf2.api.heating;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import minefantasy.mf2.api.helpers.Tiles;

public class TongsHelper {

    /**
     * Determines if an item is held
     */
    public static boolean hasHeldItem(ItemStack tongs) {
        NBTTagCompound nbt = getNBT(tongs);

        return nbt.hasKey("Held") && nbt.getBoolean("Held");
    }

    /**
     * Empties the item
     *
     * @return
     */
    public static ItemStack clearHeldItem(ItemStack tongs, EntityLivingBase user) {
        if (!user.worldObj.isRemote) {
            NBTTagCompound nbt = getNBT(tongs);
            nbt.setBoolean("Held", false);
        }
        tongs.damageItem(1, user);

        return tongs;
    }

    /**
     * Picks up an item
     */
    public static boolean trySetHeldItem(ItemStack tongs, ItemStack item) {
        if (item == null || item.getItem() == null || !isHotItem(item) || item.getItem() instanceof ItemBlock) {
            return false;
        }
        NBTTagCompound nbt = getNBT(tongs);
        nbt.setBoolean("Held", true);
        NBTTagCompound save = new NBTTagCompound();
        item.writeToNBT(save);
        nbt.setTag("Saved", save);

        return true;
    }

    /**
     * Used to determine if an item burns you when held, and if tongs can pick it up
     */
    public static boolean isHotItem(ItemStack item) {
        if (item.getItem() instanceof IHotItem) {
            return ((IHotItem) item.getItem()).isHot(item);
        }
        return false;
    }

    /**
     * Determines if it can be cooled in a water source
     */
    public static boolean isCoolableItem(ItemStack item) {
        if (item.getItem() instanceof IHotItem) {
            return ((IHotItem) item.getItem()).isCoolable(item);
        }
        return false;
    }

    /**
     * Gets the item picked up
     */
    public static ItemStack getHeldItem(ItemStack tongs) {
        NBTTagCompound nbt = getNBT(tongs);
        if (nbt.hasKey("Held")) {
            if (nbt.getBoolean("Held")) {
                if (nbt.hasKey("Saved")) {
                    NBTTagCompound save = nbt.getCompoundTag("Saved");
                    return ItemStack.loadItemStackFromNBT(save);
                }
            }
        }
        return null;
    }

    /**
     * Gets the item held from tongs
     *
     * @param tongs the itemstack used
     */
    public static ItemStack getHeldItemTongs(ItemStack tongs) {
        NBTTagCompound nbt = getNBT(tongs);
        if (nbt.hasKey("Held")) {
            if (nbt.getBoolean("Held")) {
                if (nbt.hasKey("Saved")) {
                    NBTTagCompound save = nbt.getCompoundTag("Saved");
                    return ItemStack.loadItemStackFromNBT(save);
                }
            }
        }
        return null;
    }

    /**
     * Used for getting the NBT for itemstacks, if none exists; it creates one
     */
    public static NBTTagCompound getNBT(ItemStack item) {
        if (!item.hasTagCompound()) {
            item.setTagCompound(new NBTTagCompound());
        }
        return item.getTagCompound();
    }

    /** The fluid a quench takes from a tank: a trough unit, 62.5 mB, rounded up. */
    public static final int QUENCH_FLUID = 63;
    /** How much riskier water out of a proper bath, a cauldron or open water, quenches. */
    public static final float RAW_WATER_RISK = 1.5F;

    /**
     * Where a piece can be quenched here, taking what the quench uses: a quench block, any tank of a quenching fluid
     * (which loses {@link #QUENCH_FLUID}), a water block (which boils away) or a cauldron (which loses a level). Null
     * when there is nothing to quench in. Only the server takes from a tank or cauldron.
     */
    public static Quench.Source findQuench(World world, int i, int j, int k) {
        IQuenchBlock block = Tiles.get(world, i, j, k, IQuenchBlock.class);
        if (block != null) {
            // Asked before quenching: the last of it may go. A fluid that no longer quenches is not taken
            QuenchMedium medium = QuenchMedium.of(block.quenchFluid());
            if (medium == null && block.quenchFluid() != null) {
                return null;
            }
            if (block.quench() >= 0) {
                return new Quench.Source(medium == null ? QuenchMedium.WATER : medium, 1F);
            }
        }
        QuenchMedium tank = takeFromTank(world, i, j, k);
        if (tank != null) {
            return new Quench.Source(tank, 1F);
        }
        if (world.getBlock(i, j, k).getMaterial() == Material.water) {
            world.setBlockToAir(i, j, k);
            return new Quench.Source(QuenchMedium.WATER, RAW_WATER_RISK);
        }
        if (isCauldron(world, i, j, k)) {
            lowerCauldron(world, i, j, k);
            return new Quench.Source(QuenchMedium.WATER, RAW_WATER_RISK);
        }
        return null;
    }

    /** Takes a quench's worth of a quenching fluid from a tank here; the client only asks. */
    private static QuenchMedium takeFromTank(World world, int x, int y, int z) {
        IFluidHandler tank = Tiles.get(world, x, y, z, IFluidHandler.class);
        if (tank == null) {
            return null;
        }
        FluidStack offered = tank.drain(ForgeDirection.UNKNOWN, QUENCH_FLUID, false);
        QuenchMedium medium = offered == null ? null : QuenchMedium.of(offered.getFluid());
        if (medium == null || offered.amount < QUENCH_FLUID) {
            return null;
        }
        if (!world.isRemote) {
            tank.drain(ForgeDirection.UNKNOWN, new FluidStack(offered.getFluid(), QUENCH_FLUID), true);
        }
        return medium;
    }

    /**
     * Draws {@code amount} millibuckets of water here for a container: from any tank (troughs included), a cauldron
     * (which loses a level) or a water block, which is left in place, as a bottle leaves it. Returns whether there was
     * water; only the server takes it.
     */
    public static boolean drawWater(World world, int x, int y, int z, int amount) {
        if (takeTankWater(world, x, y, z, amount)) {
            return true;
        }
        if (isCauldron(world, x, y, z)) {
            lowerCauldron(world, x, y, z);
            return true;
        }
        return world.getBlock(x, y, z).getMaterial() == Material.water;
    }

    /** Takes exactly {@code amount} of water from a tank here, if it has that much; the client only asks. */
    private static boolean takeTankWater(World world, int x, int y, int z, int amount) {
        IFluidHandler tank = Tiles.get(world, x, y, z, IFluidHandler.class);
        if (tank == null) {
            return false;
        }
        FluidStack wanted = new FluidStack(FluidRegistry.WATER, amount);
        FluidStack offered = tank.drain(ForgeDirection.UNKNOWN, wanted, false);
        if (offered == null || offered.getFluid() != FluidRegistry.WATER || offered.amount < amount) {
            return false;
        }
        if (!world.isRemote) {
            tank.drain(ForgeDirection.UNKNOWN, wanted, true);
        }
        return true;
    }

    private static void lowerCauldron(World world, int x, int y, int z) {
        if (!world.isRemote) {
            Blocks.cauldron.func_150024_a(world, x, y, z, world.getBlockMetadata(x, y, z) - 1);
        }
    }

    public static boolean isCauldron(World world, int x, int y, int z) {
        return world.getBlock(x, y, z) == Blocks.cauldron && world.getBlockMetadata(x, y, z) > 0;
    }

}
