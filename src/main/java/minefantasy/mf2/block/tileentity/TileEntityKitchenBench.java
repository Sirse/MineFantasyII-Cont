package minefantasy.mf2.block.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.kitchen.CraftingManagerKitchen;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.Diagnosis;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RunningCraft;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.config.ConfigKitchen;
import minefantasy.mf2.container.ContainerKitchenBench;
import minefantasy.mf2.network.NetworkUtils;
import minefantasy.mf2.network.packet.KitchenBenchPacket;

public class TileEntityKitchenBench extends TileEntity implements IInventory, Diagnosis.Source {

    public final int width = 4;
    public final int height = 4;
    public float progressMax;
    public float progress;
    public float dirtyProgress;
    public float dirtyMax = ConfigKitchen.dirtyProgressMax;
    private float pendingDirtyAmount;
    private ItemStack[] inventory;
    private Random rand = new Random();
    private int ticksExisted;
    private ContainerKitchenBench syncBench;
    private InventoryCrafting craftMatrix;
    private String lastPlayerHit = "";
    private String toolTypeRequired = "hands";
    private String craftSound = "step.wood";
    private String researchRequired = "";
    private Skill skillUsed;
    private boolean resetRecipe = false;
    private ItemStack recipe;
    private GridRecipe activeRecipe;
    /** The project read from the save, checked on the first recipe update after loading. */
    /** The craft the grid holds, worked out in full; the HUD, the save and finishing all read it. */
    private CraftPlan project;
    /** The project the progress belongs to, kept across saves. */
    private final RunningCraft running = new RunningCraft();
    private static final RecipeId UNLISTED = RecipeId.of("minefantasy2", "kitchen/unlisted");

    public TileEntityKitchenBench() {
        inventory = new ItemStack[width * height + 5];
        setContainer(new ContainerKitchenBench(this));
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        running.read(nbt);

        inventory = InventorySlots.read(nbt, "Items", inventory.length);
        progress = nbt.getFloat("Progress");
        progressMax = nbt.getFloat("ProgressMax");
        dirtyProgress = nbt.getFloat("DirtyProgress");
        toolTypeRequired = nbt.getString("toolTypeRequired");
        craftSound = nbt.getString("craftSound");
        researchRequired = nbt.getString("researchRequired");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        running.write(nbt);

        InventorySlots.write(nbt, "Items", inventory);

        nbt.setFloat("Progress", progress);
        nbt.setFloat("ProgressMax", progressMax);
        nbt.setFloat("DirtyProgress", dirtyProgress);
        nbt.setString("toolTypeRequired", toolTypeRequired);
        nbt.setString("craftSound", craftSound);
        nbt.setString("researchRequired", researchRequired);
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
        onInventoryChanged();
        return InventorySlots.take(inventory, slot, num);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return InventorySlots.takeAll(inventory, slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack item) {
        onInventoryChanged();
        inventory[slot] = item;
    }

    @Override
    public String getInventoryName() {
        return "gui.kitchenbench.name";
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
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return true;
    }

    @Override
    public void updateEntity() {
        ++ticksExisted;
        if (!worldObj.isRemote) {
            dirtyMax = ConfigKitchen.dirtyProgressMax;
        }
        // onInventoryChanged already refreshes the recipe; this is only a fallback poll, so skip it while the
        // grid is empty and nothing is cached. Idle benches otherwise rescanned the whole recipe list forever.
        if (!worldObj.isRemote && ticksExisted % 20 == 0
                && (hasInputs() || recipe != null || activeRecipe != null || progress > 0)) {
            updateCraftingData();
        }
        resetRecipe = false;
    }

    public void onInventoryChanged() {
        if (!resetRecipe) {
            updateCraftingData();
            resetRecipe = true;
        }
    }

    /**
     * Right-click interaction: crafting hits with the proper tool, washing with a water container.
     */
    public boolean interact(EntityPlayer user) {
        ItemStack held = user.getHeldItem();

        if (!worldObj.isRemote && isWaterContainer(held)) {
            if (dirtyProgress > 0 || user.capabilities.isCreativeMode) {
                washBench(user);
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "random.splash", 0.75F, 1.0F);
            }
            return true;
        }

        String toolType = ToolHelper.getCrafterTool(held);
        int toolTier = ToolHelper.getCrafterTier(held);
        float efficiency = ToolHelper.getCrafterEfficiency(held);
        if (!toolType.equalsIgnoreCase("hands") || recipeRequiresHands()) {
            if (worldObj.isRemote) {
                return true;
            }
            boolean dirty = isDirty();
            if (!dirty && held != null && !recipeRequiresHands()) {
                held.damageItem(1, user);
                if (held.getItemDamage() >= held.getMaxDamage()) {
                    user.destroyCurrentEquippedItem();
                }
            }

            if (dirty) {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "step.stone", 1.25F, 1.5F);
            } else if (doesPlayerKnowCraft(user) && canCraft() && isToolSufficient(toolType, toolTier)) {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, getCraftingSound(), 1.0F, 1.0F);

                if (user.swingProgress > 0 && user.swingProgress <= 1.0) {
                    efficiency *= (0.5F - user.swingProgress);
                }
                progress += Math.max(0.2F, efficiency);
                if (progress >= progressMax) {
                    craftItem(user);
                }
            } else {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "step.stone", 1.25F, 1.5F);
            }
            lastPlayerHit = user.getCommandSenderName();
            updateCraftingData();
            return true;
        }
        updateCraftingData();
        return false;
    }

    private boolean recipeRequiresHands() {
        return toolTypeRequired.equalsIgnoreCase("hands");
    }

    private boolean isToolSufficient(String toolType, int toolTier) {
        if (toolTier < getToolTierNeeded()) {
            return false;
        }
        if (recipeRequiresHands()) {
            return true;// hands recipes may also be hit with any tool
        }
        return toolType.equalsIgnoreCase(toolTypeRequired);
    }

    private boolean isWaterContainer(ItemStack held) {
        if (held == null) {
            return false;
        }
        FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(held);
        return fluid != null && fluid.getFluid() == FluidRegistry.WATER;
    }

    private void washBench(EntityPlayer user) {
        float strength = ConfigKitchen.dirtyProgressMax * ConfigKitchen.washStrengthFraction;
        dirtyProgress = Math.max(0F, dirtyProgress - strength);

        ItemStack held = user.getHeldItem();
        ItemStack empty = FluidContainerRegistry.drainFluidContainer(held);
        user.inventory.decrStackSize(user.inventory.currentItem, 1);
        if (empty != null && !user.inventory.addItemStackToInventory(empty)) {
            user.entityDropItem(empty, 0.0F);
        }
        updateCraftingData();
    }

    public boolean isDirty() {
        return dirtyProgress >= dirtyMax;
    }

    public float getDirtyMax() {
        return dirtyMax;
    }

    public void setDirtyMax(float max) {
        dirtyMax = max;
    }

    private void craftItem(EntityPlayer user) {
        // Re-read the grid before paying out: inventory changes after the first one do not refresh the recipe, so the
        // project may no longer match what is actually on the bench
        CraftPlan crafting = project;
        updateCraftingData();
        if (crafting == null || !crafting.sameAs(project)) {
            progress = 0;
            return;
        }
        if (this.canCraft() && !isDirty()) {
            addXP(user);
            addDirtyProgress(user);
            ItemStack result = project.getProduct();
            // The grid pays first: nothing is produced if it no longer holds what the project takes
            if (!payGrid()) {
                onInventoryChanged();
                progress = 0;
                return;
            }
            int output = getOutputSlotNum();

            if (this.inventory[output] == null) {
                if (result.getMaxStackSize() == 1 && !lastPlayerHit.isEmpty()) {
                    getNBT(result).setString("MF_CraftedByName", lastPlayerHit);
                }
                this.inventory[output] = result;
            } else if (this.inventory[output].isItemEqual(result)
                    && ItemStack.areItemStackTagsEqual(this.inventory[output], result)) {
                        ItemStack outputStack = this.inventory[output];
                        int max = outputStack.getMaxStackSize();
                        int toAdd = Math.min(result.stackSize, max - outputStack.stackSize);
                        outputStack.stackSize += toAdd;
                        if (result.stackSize > toAdd) {
                            ItemStack overflow = result.copy();
                            overflow.stackSize = result.stackSize - toAdd;
                            dropItem(overflow);
                        }
                    } else {
                        dropItem(result);
                    }
        }
        onInventoryChanged();
        progress = 0;
    }

    /**
     * More provisioning skill means less mess.
     */
    private void addDirtyProgress(EntityPlayer user) {
        if (pendingDirtyAmount <= 0) {
            return;
        }
        float amount = pendingDirtyAmount;
        if (skillUsed == null) {
            dirtyProgress += amount;
            return;
        }
        float maxLevel = skillUsed.getMaxLevel();
        float level = RPGElements.getLevel(user, skillUsed);
        float levelMod = maxLevel > 0 ? (level / maxLevel) : 0F;
        float reduction = amount / ConfigKitchen.dirtyProgressSkillModifier * levelMod;
        dirtyProgress += Math.max(0F, amount - reduction);
    }

    private int getOutputSlotNum() {
        return getSizeInventory() - 5;
    }

    private boolean hasInputs() {
        for (int a = 0; a < getOutputSlotNum(); a++) {
            if (inventory[a] != null) {
                return true;
            }
        }
        return false;
    }

    private NBTTagCompound getNBT(ItemStack item) {
        if (!item.hasTagCompound()) {
            item.setTagCompound(new NBTTagCompound());
        }
        return item.getTagCompound();
    }

    private void dropItem(ItemStack itemstack) {
        while (itemstack.stackSize > 0) {
            int j1 = Math.min(itemstack.stackSize, itemstack.getMaxStackSize());
            itemstack.stackSize -= j1;
            EntityItem entityitem = new EntityItem(
                    worldObj,
                    xCoord + 0.5D,
                    yCoord + 0.75D,
                    zCoord + 0.5D,
                    new ItemStack(itemstack.getItem(), j1, itemstack.getItemDamage()));
            if (itemstack.hasTagCompound()) {
                entityitem.getEntityItem().setTagCompound((NBTTagCompound) itemstack.getTagCompound().copy());
            }
            entityitem.motionX = (float) rand.nextGaussian() * 0.05F;
            entityitem.motionY = 0.2F;
            entityitem.motionZ = (float) rand.nextGaussian() * 0.05F;
            worldObj.spawnEntityInWorld(entityitem);
        }
    }

    /** Result as the server last sent it; clients never run the recipe lookup themselves */
    private ItemStack clientResult;

    /** The recipe result, or on the client the copy the server synced for display */
    public ItemStack getShownResult() {
        return worldObj != null && worldObj.isRemote ? clientResult : recipe;
    }

    public void setClientResult(ItemStack result) {
        clientResult = result;
    }

    /** True while the grid holds a recipe; getResultName always returns text, even with nothing to make */
    public boolean hasProject() {
        ItemStack result = getShownResult();
        return result != null && result.getItem() != null;
    }

    public String getResultName() {
        ItemStack result = getShownResult();
        if (hasProject() && result.getDisplayName() != null) {
            return result.getDisplayName();
        }
        return StatCollector.translateToLocal("gui.noproject");
    }

    public String getToolNeeded() {
        return toolTypeRequired;
    }

    public String getCraftingSound() {
        return craftSound;
    }

    public int getToolTierNeeded() {
        return 0;
    }

    public int getBenchTierNeeded() {
        return -1;
    }

    /** Takes what the project owes from the grid; containers go back to their slot, the return slots, the floor. */
    private boolean payGrid() {
        if (project == null) {
            return false;
        }
        List<ItemStack> spill = new ArrayList<>();
        resetRecipe = true;
        boolean paid = GridProject.pay(project, this, spill);
        resetRecipe = false;
        for (ItemStack stack : spill) {
            dropItem(stack);
        }
        onInventoryChanged();
        return paid;
    }

    private boolean canFitResult(ItemStack result) {
        ItemStack resSlot = inventory[getOutputSlotNum()];
        if (resSlot != null && result != null) {
            if (!resSlot.isItemEqual(result) || !ItemStack.areItemStackTagsEqual(resSlot, result)) {
                return false;
            }
            if (resSlot.stackSize + result.stackSize > resSlot.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    // CRAFTING CODE
    public ItemStack getResult() {
        if (syncBench == null || craftMatrix == null) {
            return null;
        }
        if (ticksExisted <= 1) return null;

        for (int a = 0; a < getOutputSlotNum(); a++) {
            craftMatrix.setInventorySlotContents(a, inventory[a]);
        }
        GridRecipe.Match match = CraftingManagerKitchen.getInstance().match(craftMatrix);
        return match == null ? null : match.getResult();
    }

    /** Works out the project for the grid as it is now; null when it makes nothing. */
    private CraftPlan buildProject(GridRecipe.Match match) {
        if (recipe == null) {
            return null;
        }
        RecipeId id = MFRecipes.KITCHEN.published().idOf(activeRecipe);
        int returns = getSizeInventory() - 4;
        CraftPlan.Builder plan = CraftPlan.builder(
                id == null ? UNLISTED : id,
                MFRecipes.KITCHEN.published().getGeneration(),
                returns,
                returns + 1,
                returns + 2,
                returns + 3);
        GridProject.addGrid(plan, this, getOutputSlotNum(), match == null ? null : match.getAmounts(), false);
        if (match == null) {
            GridProject.requireRepair(plan, "hands");
        } else {
            GridProject.require(plan, match);
        }
        return plan.product(recipe).build();
    }

    /** Shows the project on the station: the HUD and the work checks read these. */
    private void show(CraftPlan plan) {
        if (plan == null) {
            return;
        }
        progressMax = plan.require(MFRecipeKeys.TIME, 0F);
        toolTypeRequired = plan.require(MFRecipeKeys.TOOL, "hands");
        researchRequired = plan.require(MFRecipeKeys.RESEARCH, "");
    }

    /**
     * Every recipe matching the grid, in lookup order: the first is crafted unless the player or the bench stops it,
     * the rest are shadowed by it.
     */
    @Override
    public Diagnosis diagnose(EntityPlayer player) {
        updateCraftingData();
        List<RecipeEntry<GridRecipe>> matched = new ArrayList<>();
        if (craftMatrix != null) {
            for (RecipeEntry<GridRecipe> entry : MFRecipes.KITCHEN.published().all()) {
                if (entry.getRecipe().matches(craftMatrix)) {
                    matched.add(entry);
                }
            }
        }
        List<Diagnosis.Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < matched.size(); i++) {
            candidates.add(
                    Diagnosis.candidate(
                            matched.get(i),
                            i == 0 ? requirementProblem(player) : CheckResult.Reason.of("shadowed")));
        }
        if (candidates.isEmpty()) {
            return Diagnosis.problem("kitchen", CheckResult.Reason.of("no_match"));
        }
        return Diagnosis.of("kitchen", candidates);
    }

    /** What stops the player crafting the project, or null; a soft reason means it only works harder. */
    private CheckResult.Reason requirementProblem(EntityPlayer player) {
        if (project == null) {
            return CheckResult.Reason.NO_RECIPE;
        }
        ItemStack held = player.getHeldItem();
        String tool = ToolHelper.getCrafterTool(held);
        int toolTier = ToolHelper.getCrafterTier(held);
        String needTool = project.require(MFRecipeKeys.TOOL, "");
        int needToolTier = project.require(MFRecipeKeys.TOOL_TIER, 0);
        int needStation = project.require(MFRecipeKeys.TIER, 0);
        String research = project.require(MFRecipeKeys.RESEARCH, "");
        if (isDirty()) {
            return CheckResult.Reason.DIRTY;
        }
        if (!recipeRequiresHands() && !needTool.equalsIgnoreCase(tool)) {
            return CheckResult.Reason.of("tool", needTool, tool);
        }
        if (!research.isEmpty() && !ResearchLogic.hasInfoUnlocked(player, research)) {
            return CheckResult.Reason.of("research", research);
        }
        if (!canCraft()) {
            return CheckResult.Reason.OUTPUT_FULL;
        }
        return null;
    }

    public void updateCraftingData() {
        if (!worldObj.isRemote) {
            if (craftMatrix != null) {
                for (int a = 0; a < getOutputSlotNum(); a++) {
                    craftMatrix.setInventorySlotContents(a, inventory[a]);
                }
            }
            GridRecipe.Match match = craftMatrix == null ? null
                    : CraftingManagerKitchen.getInstance().match(craftMatrix);
            activeRecipe = match == null ? null : match.getRecipe();
            recipe = match == null ? null : match.getResult();
            skillUsed = activeRecipe == null ? null : activeRecipe.getSkill();
            craftSound = activeRecipe == null ? craftSound : activeRecipe.getSound();
            pendingDirtyAmount = activeRecipe == null ? 0F : activeRecipe.getDirtyAmount();

            project = buildProject(match);
            show(project);

            // Progress belongs to one project: another recipe, material, requirement or input starts over
            if (progress > 0 && (!canCraft() || isDirty() || !running.holds(project))) {
                progress = 0;
            }
            running.start(project);
            if (progress > progressMax) progress = progressMax - 1;
            syncData();
        }
    }

    /** Sends what the GUI and the in-world HUD show */
    public void syncData() {
        if (worldObj.isRemote) return;
        NetworkUtils.sendToWatchers(new KitchenBenchPacket(this).generatePacket(), worldObj, this.xCoord, this.zCoord);
    }

    /**
     * Sent by the game when a player starts watching this chunk. syncData only fires when the recipe is recomputed,
     * which an idle bench never does, so without this a dirty empty bench looked clean after the chunk reloaded.
     */
    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setFloat("Progress", progress);
        nbt.setFloat("ProgressMax", progressMax);
        nbt.setFloat("DirtyProgress", dirtyProgress);
        nbt.setFloat("DirtyMax", dirtyMax);
        nbt.setString("ToolNeeded", toolTypeRequired == null ? "" : toolTypeRequired);
        nbt.setString("Research", researchRequired == null ? "" : researchRequired);
        if (recipe != null) {
            nbt.setTag("Result", recipe.writeToNBT(new NBTTagCompound()));
        }
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        NBTTagCompound nbt = packet.func_148857_g();
        progress = nbt.getFloat("Progress");
        progressMax = nbt.getFloat("ProgressMax");
        dirtyProgress = nbt.getFloat("DirtyProgress");
        dirtyMax = nbt.getFloat("DirtyMax");
        toolTypeRequired = nbt.getString("ToolNeeded");
        researchRequired = nbt.getString("Research");
        clientResult = nbt.hasKey("Result") ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("Result")) : null;
    }

    public boolean canCraft() {
        if (isDirty()) {
            return false;
        }
        return progressMax > 0 && recipe != null && canFitResult(recipe);
    }

    public boolean shouldRenderCraftMetre() {
        return recipe != null;
    }

    public int getProgressBar(int i) {
        if (progressMax <= 0.0F) {
            return 0;
        }
        return (int) Math.ceil((i * progress) / progressMax);
    }

    public int getDirtyBar(int i) {
        float max = dirtyMax;
        if (max <= 0) {
            return 0;
        }
        return (int) Math.ceil((i * dirtyProgress) / max);
    }

    public void setCraftingSound(String sound) {
        this.craftSound = sound;
    }

    public String getResearchNeeded() {
        return researchRequired;
    }

    public boolean doesPlayerKnowCraft(EntityPlayer user) {
        if (getResearchNeeded().isEmpty()) {
            return true;
        }
        return ResearchLogic.hasInfoUnlocked(user, getResearchNeeded());
    }

    private void addXP(EntityPlayer smith) {
        if (skillUsed != null) {
            float baseXP = project.require(MFRecipeKeys.TIME, 0F) / 10F;
            skillUsed.addXP(smith, (int) baseXP + 1);
        }
    }

    public void setContainer(ContainerKitchenBench container) {
        syncBench = container;
        craftMatrix = new InventoryCrafting(syncBench, width, height);
    }

    // region client sync: the server shows the project; its packets and container fill these on the client

    public void setToolType(String toolType) {
        toolTypeRequired = toolType;
    }

    public void setResearch(String research) {
        researchRequired = research;
    }

    // endregion
}
