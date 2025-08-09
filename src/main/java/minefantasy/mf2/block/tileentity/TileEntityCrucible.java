package minefantasy.mf2.block.tileentity;

import minefantasy.mf2.api.crafting.IHeatUser;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.api.refine.AlloyRecipes;
import minefantasy.mf2.api.refine.SmokeMechanics;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.refining.BlockCrucible;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import java.util.Random;

public class TileEntityCrucible extends TileEntity implements IInventory, ISidedInventory, IHeatUser {
    // Constants
    private static final int GRID_SLOT_COUNT = 9;
    private static final int OUTPUT_SLOT = 9;
    private static final int INVENTORY_SIZE = 10;
    private static final float BASE_PROGRESS_MAX = 400F;
    private static final float ADVANCED_PROGRESS_MAX = 2000F;
    private static final float SMELT_TEMPERATURE_THRESHOLD = 600F;

    private final int[] gridSlots = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    private final int[] outputSlots = new int[]{OUTPUT_SLOT};
    private final int[] allSlots = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public float progress = 0;
    public float progressMax = BASE_PROGRESS_MAX;
    public float temperature;
    private ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];
    private final Random rand = new Random();
    private ItemStack cachedRecipeOutput;

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (worldObj.isRemote) {
            return;
        }

        boolean wasHot = getIsHot();
        this.temperature = getTemperature();
        this.progressMax = (getTier() >= 2) ? ADVANCED_PROGRESS_MAX : BASE_PROGRESS_MAX;

        if (wasHot && canSmelt()) {
            updateSmelting();
        } else {
            progress = 0;
        }

        if (progress > 0 && rand.nextInt(4) == 0 && !isOutside() && this.getTier() < 2) {
            SmokeMechanics.emitSmokeIndirect(worldObj, xCoord, yCoord, zCoord, 1);
        }

        if (wasHot != getIsHot()) {
            BlockCrucible.updateFurnaceBlockState(this.temperature > 0, worldObj, xCoord, yCoord, zCoord);
        }
    }

    private void updateSmelting() {
        progress += (temperature / SMELT_TEMPERATURE_THRESHOLD);
        if (progress >= progressMax) {
            progress = 0;
            smeltItem();
            if (isAuto()) {
                onAutoSmelt();
            }
        }
    }

    private boolean getIsHot() {
        if (this.getTier() >= 2) {
            return this.isCoated();
        }
        return this.temperature > 0;
    }

    private void onAutoSmelt() {
        worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "random.fizz", 1.0F, 1.0F);
        worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "random.piston.out", 1.0F, 1.0F);
    }

    private boolean isOutside() {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (!worldObj.canBlockSeeTheSky(xCoord + x, yCoord + 1, zCoord + y)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void smeltItem() {
        if (!canSmelt()) {
            return;
        }

        ItemStack result = this.cachedRecipeOutput.copy();
        ItemStack outputSlot = inventory[OUTPUT_SLOT];

        if (outputSlot == null) {
            inventory[OUTPUT_SLOT] = result;
        } else if (CustomToolHelper.areEqual(outputSlot, result)) {
            outputSlot.stackSize += result.stackSize;
        }

        for (int i = 0; i < GRID_SLOT_COUNT; i++) {
            if (inventory[i] != null) {
                inventory[i].stackSize--;
                if (inventory[i].stackSize <= 0) {
                    inventory[i] = null;
                }
            }
        }
        onInventoryChanged(); // Update recipe after consuming ingredients

        if (worldObj.isRemote && getTier() >= 2) {
            spawnStructureParticles();
        }
    }

    private void spawnStructureParticles() {
        spawnParticle(-3, 0, 0);
        spawnParticle(3, 0, 0);
        spawnParticle(0, 0, -3);
        spawnParticle(0, 0, 3);
    }

    private void spawnParticle(int x, int y, int z) {
        this.worldObj.playAuxSFX(2003, xCoord + x, yCoord + y, zCoord + z, 0);
    }

    private boolean canSmelt() {
        if (this.temperature <= 0 || this.cachedRecipeOutput == null) {
            return false;
        }

        ItemStack result = this.cachedRecipeOutput;
        ItemStack outputSlot = inventory[OUTPUT_SLOT];

        if (outputSlot == null) {
            return true;
        }
        if (CustomToolHelper.areEqual(outputSlot, result)) {
            return (outputSlot.stackSize + result.stackSize) <= outputSlot.getMaxStackSize();
        }
        return false;
    }

    private void updateCachedRecipe() {
        ItemStack[] inputs = new ItemStack[GRID_SLOT_COUNT];
        for (int i = 0; i < GRID_SLOT_COUNT; i++) {
            inputs[i] = inventory[i];
        }

        Alloy alloy = AlloyRecipes.getResult(inputs);
        if (alloy != null && alloy.getLevel() <= getTier()) {
            this.cachedRecipeOutput = alloy.getRecipeOutput();
        } else {
            this.cachedRecipeOutput = null;
        }
    }

    @Override
    public Block getBlockType() {
        if (worldObj == null) {
            return Blocks.air;
        }
        return super.getBlockType();
    }

    public int getTier() {
        Block block = this.getBlockType();
        if (block instanceof BlockCrucible) {
            return ((BlockCrucible) block).tier;
        }
        return 0;
    }

    public boolean isAuto() {
        Block block = this.getBlockType();
        if (block instanceof BlockCrucible) {
            return ((BlockCrucible) block).isAuto;
        }
        return false;
    }

    public float getTemperature() {
        if (this.getTier() >= 1 && !isCoated()) {
            return 0F;
        }
        if (getTier() >= 2) {
            return 500F;
        }

        Block under = worldObj.getBlock(xCoord, yCoord - 1, zCoord);
        Material underMaterial = under.getMaterial();

        if (underMaterial == Material.fire) {
            return 10F;
        }
        if (underMaterial == Material.lava) {
            return 50F;
        }

        TileEntity tile = worldObj.getTileEntity(xCoord, yCoord - 1, zCoord);
        if (tile instanceof TileEntityForge) {
            return ((TileEntityForge) tile).getBlockTemperature();
        }
        return 0F;
    }

    public boolean isCoated() {
        if (this.getTier() >= 2) {
            return isEnderAlter(-1, -1, -3) && isEnderAlter(-1, -1, 3) && isEnderAlter(-3, -1, -1)
                    && isEnderAlter(3, -1, -1) && isEnderAlter(1, -1, -3) && isEnderAlter(1, -1, 3)
                    && isEnderAlter(-3, -1, 1) && isEnderAlter(3, -1, 1);
        }
        return isFirebrick(0, 0, -1) && isFirebrick(0, 0, 1) && isFirebrick(-1, 0, 0) && isFirebrick(1, 0, 0);
    }

    private boolean isFirebrick(int x, int y, int z) {
        return worldObj.getBlock(xCoord + x, yCoord + y, zCoord + z) == BlockListMF.firebricks;
    }

    private boolean isEnderAlter(int x, int y, int z) {
        Block block = worldObj.getBlock(xCoord + x, yCoord + y, zCoord + z);
        int meta = worldObj.getBlockMetadata(xCoord + x, yCoord + y, zCoord + z);
        return block == Blocks.end_portal_frame && BlockEndPortalFrame.isEnderEyeInserted(meta);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setFloat("progress", progress);
        nbt.setFloat("progressMax", progressMax);

        NBTTagList savedItems = new NBTTagList();
        for (int i = 0; i < this.inventory.length; ++i) {
            if (this.inventory[i] != null) {
                NBTTagCompound savedSlot = new NBTTagCompound();
                savedSlot.setByte("Slot", (byte) i);
                this.inventory[i].writeToNBT(savedSlot);
                savedItems.appendTag(savedSlot);
            }
        }
        nbt.setTag("Items", savedItems);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        progress = nbt.getFloat("progress");
        progressMax = nbt.getFloat("progressMax");

        NBTTagList savedItems = nbt.getTagList("Items", 10);
        this.inventory = new ItemStack[this.getSizeInventory()];
        for (int i = 0; i < savedItems.tagCount(); ++i) {
            NBTTagCompound savedSlot = savedItems.getCompoundTagAt(i);
            byte slotNum = savedSlot.getByte("Slot");
            if (slotNum >= 0 && slotNum < this.inventory.length) {
                this.inventory[slotNum] = ItemStack.loadItemStackFromNBT(savedSlot);
            }
        }
        onInventoryChanged(); // Update cache on load
    }

    @Override
    public int getSizeInventory() {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int num) {
        if (this.inventory[slot] == null) {
            return null;
        }
        ItemStack itemstack;
        if (this.inventory[slot].stackSize <= num) {
            itemstack = this.inventory[slot];
            this.inventory[slot] = null;
        } else {
            itemstack = this.inventory[slot].splitStack(num);
            if (this.inventory[slot].stackSize == 0) {
                this.inventory[slot] = null;
            }
        }
        onInventoryChanged();
        return itemstack;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return inventory[slot];
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack item) {
        inventory[slot] = item;
        if (item != null && item.stackSize > this.getInventoryStackLimit()) {
            item.stackSize = this.getInventoryStackLimit();
        }
        onInventoryChanged();
    }

    public void onInventoryChanged() {
        if (!worldObj.isRemote) {
            updateCachedRecipe();
        }
    }

    @Override
    public String getInventoryName() {
        return "gui.crucible.name";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer user) {
        return user.getDistance(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) < 8D;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return slot != OUTPUT_SLOT;
    }

    private boolean isBlastOutput() {
        if (worldObj == null) {
            return false;
        }
        TileEntity tile = worldObj.getTileEntity(xCoord, yCoord + 1, zCoord);
        return tile instanceof TileEntityBlastFH;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (isBlastOutput()) {
            return allSlots;
        }
        return side == 0 ? outputSlots : gridSlots;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack item, int side) {
        return !isBlastOutput() && isItemValidForSlot(slot, item);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack item, int side) {
        return isAuto() && slot == OUTPUT_SLOT;
    }

    @Override
    public boolean canAccept(TileEntity tile) {
        return tile instanceof TileEntityForge;
    }
}
