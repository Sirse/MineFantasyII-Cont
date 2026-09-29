package minefantasy.mf2.block.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import minefantasy.mf2.api.crafting.IHeatUser;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.Requirements;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.Tiles;
import minefantasy.mf2.api.recipe.CraftInventory;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.api.refine.AlloyRecipes;
import minefantasy.mf2.api.refine.SmokeMechanics;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.refining.BlockCrucible;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;

public class TileEntityCrucible extends TileEntityStation implements ISidedInventory, IHeatUser {

    // Constants
    private static final int GRID_SLOT_COUNT = 9;
    private static final int OUTPUT_SLOT = 9;
    private static final int INVENTORY_SIZE = 10;
    private static final float BASE_PROGRESS_MAX = 400F;
    private static final float ADVANCED_PROGRESS_MAX = 2000F;
    private static final float SMELT_TEMPERATURE_THRESHOLD = 600F;

    private final int[] gridSlots = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
    private final int[] outputSlots = new int[] { OUTPUT_SLOT };
    private final int[] allSlots = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

    private int ticksExisted;
    private int cachedTier = -1;
    private boolean cachedCoated;
    public float progress = 0;
    public float progressMax = BASE_PROGRESS_MAX;
    public float temperature;
    private ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];
    private final Random rand = new Random();
    private ItemStack cachedRecipeOutput;
    private RecipeId cachedRecipeId;
    private int[] cachedRequiredAmounts;
    private long cachedRecipeVersion = -1;
    /**
     * Set whenever the contents change before a world is available (NBT load); cleared on the next server tick
     */
    private boolean recipeCacheDirty = true;

    @Override
    public void updateEntity() {
        super.updateEntity();
        ++ticksExisted;

        if (!worldObj.isRemote && recipeCacheDirty) {
            updateCachedRecipe();
            recipeCacheDirty = false;
        }

        if (cachedTier < 0 || ticksExisted % 40 == 0) {
            refreshStructureCache();
        }

        if (worldObj.isRemote) {
            if (this.getTier() >= 2 && rand.nextInt(8) == 0) {
                spawnStructureParticles();
            }
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

        // The grid pays what the alloy owes per slot; containers go back to their slot or drop off the crucible
        CraftPlan.Builder plan = CraftPlan
                .builder(cachedRecipeId, MFRecipes.ALLOY.published().getGeneration(), OUTPUT_SLOT);
        GridProject.addGrid(plan, this, GRID_SLOT_COUNT, cachedRequiredAmounts, false);
        List<ItemStack> spill = new ArrayList<>();
        if (plan.output(this.cachedRecipeOutput).build().apply(CraftInventory.of(this), spill)) {
            GridProject.drop(this, spill);
        }
        onInventoryChanged(); // Update recipe after consuming ingredients
    }

    private void spawnStructureParticles() {
        spawnParticle(-3, 0, 0);
        spawnParticle(3, 0, 0);
        spawnParticle(0, 0, -3);
        spawnParticle(0, 0, 3);
    }

    private void spawnParticle(int x, int y, int z) {
        this.worldObj
                .spawnParticle("portal", xCoord + x + 0.5D, yCoord + y + 0.5D, zCoord + z + 0.5D, 0.0D, -0.5D, 0.0D);
    }

    private boolean canSmelt() {
        ensureRecipeIsCurrent();
        if (this.temperature <= 0 || this.cachedRecipeOutput == null) {
            return false;
        }
        for (int i = 0; i < GRID_SLOT_COUNT; i++) {
            if (inventory[i] != null && inventory[i].stackSize < getRequiredAmount(i)) {
                return false;
            }
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

        RecipeEntry<Alloy> entry = AlloyRecipes.find(inputs);
        Alloy alloy = entry == null ? null : entry.getRecipe();
        if (alloy != null && Requirements.CRUCIBLE.stationFits(getTier(), alloy.getLevel())) {
            this.cachedRecipeId = entry.getId();
            this.cachedRecipeOutput = alloy.getRecipeOutput();
            this.cachedRequiredAmounts = alloy.getRequiredAmounts(inputs);
        } else {
            this.cachedRecipeOutput = null;
            this.cachedRequiredAmounts = null;
        }
        this.cachedRecipeVersion = AlloyRecipes.getVersion();
    }

    /**
     * The cache only refreshes on inventory changes, so a script reload could leave a loaded crucible smelting a recipe
     * that no longer exists. Comparing a registry stamp costs one int per tick instead of a full rescan.
     */
    private void ensureRecipeIsCurrent() {
        if (cachedRecipeVersion != AlloyRecipes.getVersion()) {
            updateCachedRecipe();
        }
    }

    private int getRequiredAmount(int slot) {
        if (cachedRequiredAmounts != null && slot >= 0 && slot < cachedRequiredAmounts.length) {
            return Math.max(1, cachedRequiredAmounts[slot]);
        }
        return 1;
    }

    @Override
    public Block getBlockType() {
        if (worldObj == null) {
            return Blocks.air;
        }
        return super.getBlockType();
    }

    public int getTier() {
        if (cachedTier < 0) {
            refreshStructureCache();
        }
        return Math.max(0, cachedTier);
    }

    private void refreshStructureCache() {
        Block block = this.getBlockType();
        if (block == null || block == Blocks.air) {
            // Chunk not loaded/ready yet: retry next tick instead of caching a bogus tier
            cachedTier = -1;
            cachedCoated = false;
            return;
        }
        cachedTier = block instanceof BlockCrucible ? ((BlockCrucible) block).tier : 0;
        cachedCoated = computeCoated();
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
            return 5000F;
        }

        Block under = worldObj.getBlock(xCoord, yCoord - 1, zCoord);
        Material underMaterial = under.getMaterial();

        if (underMaterial == Material.fire) {
            return 250F;
        }
        if (underMaterial == Material.lava) {
            return 750F;
        }

        TileEntityForge forge = Tiles.get(worldObj, xCoord, yCoord - 1, zCoord, TileEntityForge.class);
        return forge != null ? Math.min(forge.getBlockTemperature(), 2500F) : 0F;
    }

    public boolean isCoated() {
        if (cachedTier < 0) {
            refreshStructureCache();
        }
        return cachedCoated;
    }

    private boolean computeCoated() {
        if (this.getTier() >= 2) {
            return isEnderAlter(-1, -1, -3) && isEnderAlter(-1, -1, 3)
                    && isEnderAlter(-3, -1, -1)
                    && isEnderAlter(3, -1, -1)
                    && isEnderAlter(1, -1, -3)
                    && isEnderAlter(1, -1, 3)
                    && isEnderAlter(-3, -1, 1)
                    && isEnderAlter(3, -1, 1);
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

        InventorySlots.write(nbt, "Items", inventory);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        progress = nbt.getFloat("progress");
        progressMax = nbt.getFloat("progressMax");

        inventory = InventorySlots.read(nbt, "Items", inventory.length);
        // The world is assigned after readFromNBT, so the recipe cannot be resolved yet: just mark the cache
        // stale and let the first server tick rebuild it.
        recipeCacheDirty = true;
    }

    public void onInventoryChanged() {
        if (worldObj == null) {
            // Called from readFromNBT before the tile is added to the world
            recipeCacheDirty = true;
            return;
        }
        if (!worldObj.isRemote) {
            updateCachedRecipe();
            recipeCacheDirty = false;
        }
    }

    @Override
    public String getInventoryName() {
        return "gui.crucible.name";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return slot != OUTPUT_SLOT;
    }

    private boolean isBlastOutput() {
        if (worldObj == null) {
            return false;
        }
        return Tiles.is(worldObj, xCoord, yCoord + 1, zCoord, TileEntityBlastFH.class);
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

    @Override
    protected ItemStack[] slots() {
        return inventory;
    }
}
