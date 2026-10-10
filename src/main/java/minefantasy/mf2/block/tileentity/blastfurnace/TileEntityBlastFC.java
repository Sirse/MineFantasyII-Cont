package minefantasy.mf2.block.tileentity.blastfurnace;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.refine.ISmokeCarrier;
import minefantasy.mf2.api.refine.SmokeMechanics;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.InventorySlots;
import minefantasy.mf2.block.tileentity.TileEntityStation;
import minefantasy.mf2.mechanics.ActionOwner;
import minefantasy.mf2.util.MFLogUtil;

public class TileEntityBlastFC extends TileEntityStation implements ISidedInventory, ISmokeCarrier {

    public int ticksExisted;
    public boolean isBuilt = false;
    public int fireTime;
    private final ActionOwner owner = new ActionOwner();
    public int tempUses;
    protected ItemStack[] items = new ItemStack[2];
    protected int smokeStorage;
    protected Random rand = new Random();

    public static boolean isCarbon(ItemStack item) {
        return MineFantasyFuels.isCarbon(item);
    }

    public static boolean isFlux(ItemStack item) {
        return true;
    }

    public static boolean isInput(ItemStack item) {
        return MFRecipes.accepts(MFRecipes.BLAST_FURNACE, item);
    }

    protected static ItemStack getResult(ItemStack input) {
        RecipeEntry<ProcessRecipe> entry = MFRecipes.find(MFRecipes.BLAST_FURNACE, input);
        return entry == null ? null : entry.getRecipe().getOutput();
    }

    /** Reads the shaft's next product without spending carbon charges or checking its surrounding blocks. */
    public CheckResult inspectWork() {
        if (getSizeInventory() < 2) return CheckResult.failure(CheckResult.Reason.MISSING_INPUT);
        if (!isBuilt) return CheckResult.failure(CheckResult.Reason.of("not_built"));
        ItemStack input = getStackInSlot(1);
        if (input == null) return CheckResult.failure(CheckResult.Reason.MISSING_INPUT);
        if (tempUses <= 0 && !isCarbon(getStackInSlot(0))) {
            return CheckResult.failure(CheckResult.Reason.of("carbon", 0, 1));
        }
        RecipeEntry<ProcessRecipe> entry = MFRecipes.find(MFRecipes.BLAST_FURNACE, input);
        if (entry == null) return CheckResult.failure(CheckResult.Reason.NO_RECIPE);
        return CheckResult.success(
                CraftPlan.builder(entry.getId(), MFRecipes.BLAST_FURNACE.published().getGeneration())
                        .use(1, entry.getRecipe().getInput(), input).product(entry.getRecipe().getOutput()).build());
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        ++ticksExisted;
        int dropFrequency = 5;

        if (!worldObj.isRemote && ticksExisted % dropFrequency == 0) {
            TileEntity neighbour = worldObj.getTileEntity(xCoord, yCoord - 1, zCoord);
            if (neighbour instanceof TileEntityBlastFC && !(neighbour instanceof TileEntityBlastFH)) {
                interact((TileEntityBlastFC) neighbour);
            }
        }
        if (ticksExisted % 200 == 0) {
            updateBuild();
        }
        if (smokeStorage > 0) {
            SmokeMechanics.emitSmokeFromCarrier(worldObj, xCoord, yCoord, zCoord, this, 5);
        }
        if (!worldObj.isRemote && smokeStorage > getMaxSmokeStorage() && rand.nextInt(1000) == 0) {
            explodeFromSmoke();
        }
    }

    /**
     * The blast of a furnace choked with smoke, attributed to its owner so protection weighs the owner's rights. With
     * no owner known no one's rights can be asked, so it neither breaks nor sets alight anything: vanilla sets fire
     * apart from breaking, so both are turned off.
     */
    public void explodeFromSmoke() {
        EntityPlayer cause = owner();
        boolean changesWorld = cause != null;
        worldObj.newExplosion(cause, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, 5F, changesWorld, changesWorld);
    }

    /**
     * Records the builder, so the smoke-overload blast is attributed to the owner rather than to whoever happens to
     * stand nearby and protection plugins evaluate the right permissions. Called from the block on placement.
     */
    public void setOwner(EntityPlayer player) {
        owner.set(player);
        saveLater();
    }

    /**
     * The player the furnace acts for, whose rights protection weighs for what it does to the world: the owner when
     * online, otherwise a stand-in with the owner's profile, as machines act for absent owners. None when no owner is
     * known, and then the furnace changes nothing around it that needs rights.
     */
    public EntityPlayer owner() {
        return owner.resolve(worldObj);
    }

    protected void interact(TileEntityBlastFC tile) {
        if (!tile.isBuilt) return;

        for (int a = 0; a < getSizeInventory(); a++) {
            ItemStack mySlot = getStackInSlot(a);

            if (mySlot != null && canShare(mySlot, a)) {
                ItemStack theirSlot = tile.getStackInSlot(a);
                if (theirSlot == null) {
                    ItemStack copy = mySlot.copy();
                    copy.stackSize = 1;
                    tile.setInventorySlotContents(a, copy);
                    this.decrStackSize(a, 1);
                } else if (CustomToolHelper.areEqual(theirSlot, mySlot)) {
                    if ((theirSlot.stackSize) < getMaxStackSizeForDistribute()) {
                        theirSlot.stackSize++;
                        this.decrStackSize(a, 1);
                        tile.setInventorySlotContents(a, theirSlot);
                    }
                }
            }
        }
    }

    private boolean canShare(ItemStack mySlot, int a) {
        if (a == 1) return isInput(mySlot);
        return isCarbon(mySlot);
    }

    public void updateBuild() {
        isBuilt = getIsBuilt();
    }

    protected boolean getIsBuilt() {
        return (isFirebrick(-1, 0, 0) && isFirebrick(1, 0, 0) && isFirebrick(0, 0, -1) && isFirebrick(0, 0, 1));
    }

    protected boolean isFirebrick(int x, int y, int z) {
        Block block = worldObj.getBlock(xCoord + x, yCoord + y, zCoord + z);
        if (block != null) {
            return block == BlockListMF.firebricks;
        }
        return false;
    }

    protected boolean isAir(int x, int y, int z) {
        return !worldObj.isBlockNormalCubeDefault(xCoord + x, yCoord + y, zCoord + z, false);
    }

    private int getMaxStackSizeForDistribute() {
        return 1;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        owner.writeToNBT(nbt);
        nbt.setInteger("fireTime", fireTime);
        nbt.setInteger("CarbonUses", tempUses);
        nbt.setBoolean("isBuilt", isBuilt);
        nbt.setInteger("ticksExisted", ticksExisted);
        nbt.setInteger("StoredSmoke", smokeStorage);
        InventorySlots.write(nbt, "Items", items);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        owner.readFromNBT(nbt);
        fireTime = nbt.getInteger("fireTime");
        tempUses = nbt.getInteger("CarbonUses");
        isBuilt = nbt.getBoolean("isBuilt");
        ticksExisted = nbt.getInteger("ticksExisted");
        smokeStorage = nbt.getInteger("StoredSmoke");
        items = InventorySlots.read(nbt, "Items", items.length);
    }

    @Override
    public String getInventoryName() {
        return "gui.blastfurnace.name";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        if (slot == 0) {
            return isCarbon(item);
        }
        return isInput(item);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == 1 ? new int[] { 0, 1 } : new int[] {};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack item, int side) {
        return isItemValidForSlot(slot, item);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack item, int side) {
        return false;
    }

    @Override
    public int getSmokeValue() {
        return smokeStorage;
    }

    @Override
    public void setSmokeValue(int smoke) {
        smokeStorage = smoke;
    }

    @Override
    public int getMaxSmokeStorage() {
        return 10;
    }

    @Override
    public boolean canAbsorbIndirect() {
        return false;
    }

    public boolean shouldRemoveCarbon() {
        if (tempUses > 0) {
            --tempUses;
            MFLogUtil.logDebug("Decr Carbon Uses: " + tempUses);
            return false;
        } else {
            int carb = MineFantasyFuels.getCarbon(getStackInSlot(0)) - 1;
            if (carb > 0) {
                tempUses = carb;
                MFLogUtil.logDebug("Set Carbon Uses: " + tempUses);
            }
            return true;
        }
    }

    @Override
    protected ItemStack[] slots() {
        return items;
    }
}
