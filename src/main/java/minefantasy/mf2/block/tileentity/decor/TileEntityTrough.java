package minefantasy.mf2.block.tileentity.decor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import minefantasy.mf2.api.heating.IQuenchBlock;
import minefantasy.mf2.api.helpers.FluidContainers;
import minefantasy.mf2.fluid.FluidsMF;
import minefantasy.mf2.item.food.FoodListMF;

public class TileEntityTrough extends TileEntityWoodDecor implements IQuenchBlock, IFluidHandler {

    public static int capacityScale = 8;
    public int fill;
    private int ticksExisted;

    public TileEntityTrough() {
        super("trough_wood");
    }

    @Override
    public float quench() {
        if (fill > 0) {
            if (!worldObj.isRemote) {
                store(stored() - PER_UNIT);
            } else {
                --fill;
            }
            return 0F;
        }
        return -1F;
    }

    /**
     * Water is counted exactly in sixteenths of a millibucket: a unit of fill, which quenching and rain count in, is
     * 1000 of them (62.5 mB), so a bucket is 16 units to the drop, and what is short of a unit is kept here.
     */
    private static final int PER_UNIT = 1000;
    private static final int PER_MB = 16;
    private int partial;

    /**
     * The fluid held by its registry name, null for water, as old troughs held only that. A name no mod registers now
     * is kept, not lost: the trough then neither takes nor gives until it is registered again.
     */
    private String fluidName;

    /** The water one salt turns to salt water: a bucket. */
    public static final int SALT_WATER_PER_SALT = 1000;

    /** Whether a trough holds this fluid: water, seed oil and salt water. */
    public static boolean holds(Fluid fluid) {
        return fluid != null
                && (fluid == FluidRegistry.WATER || fluid == FluidsMF.seedOil || fluid == FluidsMF.saltWater);
    }

    /** The fluid held, water when empty; null for one no longer registered. */
    public Fluid getFluid() {
        return fluidName == null ? FluidRegistry.WATER : FluidRegistry.getFluid(fluidName);
    }

    /** The registry name of the fluid held, null for water. */
    public String getFluidName() {
        return fluidName;
    }

    /** Fills the trough with whole units of a fluid by its name, null for water, as a placed trough item does. */
    public void setContents(String fluid, int units) {
        fill = Math.max(0, Math.min(units, getCapacity()));
        partial = 0;
        fluidName = fill == 0 || fluid == null || fluid.isEmpty() || fluid.equals(FluidRegistry.WATER.getName()) ? null
                : fluid;
    }

    /**
     * Pours from the held container or fills it from the trough, through the same tank pipes use: any container Forge
     * knows of a fluid the trough holds, a bucket, bottle or jug emptied whole if it fits, a cell or tank by the
     * millibucket. Salt turns the water to salt water, one for each bucket of it.
     */
    public boolean interact(EntityPlayer user, ItemStack held) {
        if (held == null) {
            return false;
        }
        if (held.getItem() == FoodListMF.salt) {
            return salt(user, held);
        }
        if (FluidContainers.exchange(user, this, ForgeDirection.UNKNOWN) <= 0) {
            return false;
        }
        if (!worldObj.isRemote) {
            syncData();
        }
        return true;
    }

    /** Dissolves salt in the water, one for each bucket of it, all at once or not at all; the pots are handed back. */
    private boolean salt(EntityPlayer user, ItemStack held) {
        if (stored() <= 0 || fluidName != null || FluidsMF.saltWater == null) {
            return false;
        }
        long perSalt = (long) SALT_WATER_PER_SALT * PER_MB;
        int salts = (int) ((stored() + perSalt - 1) / perSalt);
        boolean creative = user.capabilities.isCreativeMode;
        if (!creative && held.stackSize < salts) {
            return false;
        }
        if (worldObj.isRemote) {
            return true;
        }
        if (!creative) {
            ItemStack pot = held.getItem().hasContainerItem(held) ? held.getItem().getContainerItem(held) : null;
            held.stackSize -= salts;
            if (held.stackSize <= 0) {
                user.inventory.setInventorySlotContents(user.inventory.currentItem, null);
            }
            for (int i = 0; pot != null && i < salts; i++) {
                ItemStack given = pot.copy();
                if (!user.inventory.addItemStackToInventory(given)) {
                    user.dropPlayerItemWithRandomChoice(given, false);
                }
            }
            user.inventoryContainer.detectAndSendChanges();
        }
        fluidName = FluidsMF.saltWater.getName();
        changed = true;
        markDirty();
        syncData();
        return true;
    }

    private long stored() {
        return (long) fill * PER_UNIT + partial;
    }

    /** Keeps the water within the trough, saves it, and lets watchers learn of it on the next throttled sync. */
    private void store(long amount) {
        long kept = Math.max(0, Math.min(amount, (long) getCapacity() * PER_UNIT));
        fill = (int) (kept / PER_UNIT);
        partial = (int) (kept % PER_UNIT);
        if (kept == 0) {
            fluidName = null;
        }
        changed = true;
        markDirty();
    }

    // region IFluidHandler: one fluid at a time, from every side, in whole millibuckets

    /** Whether the level changed since watchers last learned it. */
    private boolean changed;
    private int lastSync;
    /** Pipes may fill every tick; watchers learn of it at most this often. */
    private static final int SYNC_TICKS = 10;

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0 || !canFill(from, resource.getFluid())) {
            return 0;
        }
        long room = ((long) getCapacity() * PER_UNIT - stored()) / PER_MB;
        int accepted = (int) Math.min(resource.amount, room);
        if (accepted > 0 && doFill && !worldObj.isRemote) {
            if (stored() == 0) {
                fluidName = resource.getFluid() == FluidRegistry.WATER ? null : resource.getFluid().getName();
            }
            store(stored() + (long) accepted * PER_MB);
        }
        return Math.max(0, accepted);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        Fluid held = getFluid();
        if (resource == null || held == null || !resource.isFluidEqual(new FluidStack(held, 1))) {
            return null;
        }
        return drain(from, resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        Fluid held = getFluid();
        int given = (int) Math.min(Math.max(0, maxDrain), stored() / PER_MB);
        if (given <= 0 || held == null) {
            return null;
        }
        if (doDrain && !worldObj.isRemote) {
            store(stored() - (long) given * PER_MB);
        }
        return new FluidStack(held, given);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return holds(fluid) && (stored() == 0 || fluid == getFluid());
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return fluid == null || fluid == getFluid();
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        int amount = (int) (stored() / PER_MB);
        Fluid held = getFluid();
        return new FluidTankInfo[] { new FluidTankInfo(
                amount > 0 && held != null ? new FluidStack(held, amount) : null,
                (int) ((long) getCapacity() * PER_UNIT / PER_MB)) };
    }

    // endregion

    /** The comparator signal: 0 when empty, 1 to 15 as the trough fills. */
    public int comparatorSignal() {
        int capacity = getCapacity();
        return fill <= 0 || capacity <= 0 ? 0 : 1 + (int) ((long) fill * 14 / capacity);
    }

    @Override
    public void updateEntity() {
        ++ticksExisted;
        if (ticksExisted == 20 || ticksExisted % 100 == 0 || changed && ticksExisted - lastSync >= SYNC_TICKS) {
            syncData();
        }
        // Rain fills an empty trough or tops up water, never another fluid
        if (ticksExisted % 100 == 0 && fluidName == null && worldObj.canLightningStrikeAt(xCoord, yCoord + 1, zCoord)) {
            addCapacity(1);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("fill", fill);
        nbt.setInteger("fillPartial", partial);
        if (fluidName != null) {
            nbt.setString("fillFluid", fluidName);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        fill = nbt.getInteger("fill");
        partial = Math.max(0, Math.min(PER_UNIT - 1, nbt.getInteger("fillPartial")));
        fluidName = nbt.hasKey("fillFluid") ? nbt.getString("fillFluid") : null;
    }

    private void addCapacity(int units) {
        store(stored() + (long) units * PER_UNIT);
    }

    @Override
    public int getCapacity() {
        return super.getCapacity() * capacityScale;
    }

    public void syncData() {
        changed = false;
        lastSync = ticksExisted;
        sendState(false);
    }

    @Override
    protected NBTTagCompound describe() {
        NBTTagCompound state = super.describe();
        state.setInteger("Fill", fill);
        if (fluidName != null) {
            state.setString("Fluid", fluidName);
        }
        return state;
    }

    @Override
    public void show(NBTTagCompound state) {
        super.show(state);
        fill = Math.max(0, Math.min(state.getInteger("Fill"), getCapacity()));
        fluidName = state.hasKey("Fluid") ? state.getString("Fluid") : null;
    }
}
