package minefantasy.mf2.block.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.carpenter.CraftingManagerCarpenter;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.Diagnosis;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RunningCraft;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.container.ContainerCarpenterMF;
import minefantasy.mf2.item.armour.ItemArmourMF;
import minefantasy.mf2.network.NetworkUtils;
import minefantasy.mf2.network.packet.CarpenterPacket;
import minefantasy.mf2.util.MFLogUtil;

public class TileEntityCarpenterMF extends TileEntity implements IInventory, Diagnosis.Source {

    public final int width = 4;
    public final int height = 4;
    public float progressMax;
    public float progress;
    private int tier;
    private ItemStack[] inventory;
    private Random rand = new Random();
    private int ticksExisted;
    private ContainerCarpenterMF syncCarpenter;
    private InventoryCrafting craftMatrix;
    private String lastPlayerHit = "";
    private String toolTypeRequired = "hands";
    private String craftSound = "step.wood";
    private String researchRequired = "";
    private Skill skillUsed;
    private boolean resetRecipe = false;
    /** Saved progress outlives the derived recipe cache, so rebuild it before the first canCraft check. */
    private boolean needsRecipeRestore;
    private ItemStack recipe;
    private GridRecipe activeRecipe;
    /** The project read from the save, checked on the first recipe update after loading. */
    /** The craft the grid holds, worked out in full; the HUD, the save and finishing all read it. */
    private CraftPlan project;
    /** The project the progress belongs to, kept across saves. */
    private final RunningCraft running = new RunningCraft();
    private static final RecipeId REPAIR = RecipeId.of("minefantasy2", "carpenter/repair");
    private int hammerTierRequired;
    private int CarpenterTierRequired;

    public TileEntityCarpenterMF() {
        this(0);
    }

    public TileEntityCarpenterMF(int tier) {
        inventory = new ItemStack[width * height + 5];
        this.tier = tier;
        setContainer(new ContainerCarpenterMF(this));
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        running.read(nbt);
        tier = nbt.getInteger("tier");

        inventory = InventorySlots.read(nbt, "Items", inventory.length);
        progress = nbt.getFloat("Progress");
        progressMax = nbt.getFloat("ProgressMax");
        needsRecipeRestore = progressMax > 0;
        toolTypeRequired = nbt.getString("toolTypeRequired");
        craftSound = nbt.getString("craftSound");
        researchRequired = nbt.getString("researchRequired");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        running.write(nbt);
        nbt.setInteger("tier", tier);

        InventorySlots.write(nbt, "Items", inventory);

        nbt.setFloat("Progress", progress);
        nbt.setFloat("ProgressMax", progressMax);
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
        return "gui.carpentermf.name";
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
        super.updateEntity();
        if (!worldObj.isRemote) {
            if (needsRecipeRestore) {
                needsRecipeRestore = false;
                updateCraftingData();
            }
            // onInventoryChanged already refreshes the recipe; this is only a fallback poll, so skip it while the
            // grid is empty and nothing is cached. Idle benches otherwise rescanned the whole recipe list forever.
            if (ticksExisted % 20 == 0 && (hasInputs() || recipe != null || activeRecipe != null || progress > 0)) {
                updateCraftingData();
            }
            if (!canCraft() && ticksExisted > 1) {
                progress = progressMax = 0;
                this.recipe = null;
            }
        }
        resetRecipe = false;
    }

    public void onInventoryChanged() {
        if (!resetRecipe) {
            updateCraftingData();
            MFLogUtil.logDebug("Carpenter: Optimised Inv Tick");
            resetRecipe = true;
        }
    }

    public boolean tryCraft(EntityPlayer user) {
        if (user == null) return false;

        String toolType = ToolHelper.getCrafterTool(user.getHeldItem());
        int hammerTier = ToolHelper.getCrafterTier(user.getHeldItem());
        if (!toolType.equalsIgnoreCase("nothing")) {
            if (user.getHeldItem() != null) {
                user.getHeldItem().damageItem(1, user);
                if (user.getHeldItem().getItemDamage() >= user.getHeldItem().getMaxDamage()) {
                    if (worldObj.isRemote) user.renderBrokenItemStack(user.getHeldItem());

                    user.destroyCurrentEquippedItem();
                }
            }
            if (worldObj.isRemote) return true;

            if (doesPlayerKnowCraft(user) && canCraft()
                    && toolType.equalsIgnoreCase(toolTypeRequired)
                    && tier >= CarpenterTierRequired
                    && hammerTier >= hammerTierRequired) {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, getUseSound(), 1.0F, 1.0F);
                float efficiency = ToolHelper.getCrafterEfficiency(user.getHeldItem());

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

    private String getUseSound() {
        if (craftSound.equalsIgnoreCase("engineering")) {
            if (rand.nextInt(5) == 0) {
                return "random.click";
            }
            if (rand.nextInt(20) == 0) {
                return "random.door_open";
            }
            return "step.wood";
        }
        return craftSound;
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
        if (this.canCraft()) {
            addXP(user);
            ItemStack result = project.getProduct();
            if (result != null && result.getItem() instanceof ItemArmourMF) {
                result = modifyArmour(result);
            }
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
            } else if (this.inventory[output].getItem() == result.getItem()
                    && ItemStack.areItemStackTagsEqual(this.inventory[output], result)) {
                        ItemStack outputStack = this.inventory[output];
                        int max = outputStack.getMaxStackSize();
                        int toAdd = Math.min(result.stackSize, max - outputStack.stackSize);
                        if (toAdd > 0) {
                            outputStack.stackSize += toAdd;
                        }
                        if (result.stackSize > toAdd) {
                            ItemStack overflow = result.copy();
                            overflow.stackSize = result.stackSize - toAdd;
                            this.dropItem(overflow);
                        }
                    } else {
                        this.dropItem(result);
                    }
        }
        onInventoryChanged();
        progress = 0;
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

    private ItemStack modifyArmour(ItemStack result) {
        ItemArmourMF item = (ItemArmourMF) result.getItem();
        boolean canColour = item.canColour();
        int colour = -1;
        for (int a = 0; a < getOutputSlotNum(); a++) {
            ItemStack slot = getStackInSlot(a);
            if (slot != null && slot.getItem() instanceof ItemArmor) {
                ItemArmor slotitem = (ItemArmor) slot.getItem();
                if (canColour && slotitem.hasColor(slot)) {
                    colour = slotitem.getColor(slot);
                }
                if (result.isItemStackDamageable()) {
                    result.setItemDamage(slot.getItemDamage());
                }
            }
        }
        if (colour != -1 && canColour) {
            item.func_82813_b(result, colour);
        }
        return result;
    }

    private NBTTagCompound getNBT(ItemStack item) {
        if (!item.hasTagCompound()) {
            item.setTagCompound(new NBTTagCompound());
        }
        return item.getTagCompound();
    }

    private void dropItem(ItemStack itemstack) {
        if (itemstack != null) {
            float f = this.rand.nextFloat() * 0.8F + 0.1F;
            float f1 = this.rand.nextFloat() * 0.8F + 0.1F;
            float f2 = this.rand.nextFloat() * 0.8F + 0.1F;

            while (itemstack.stackSize > 0) {
                int j1 = this.rand.nextInt(21) + 10;

                if (j1 > itemstack.stackSize) {
                    j1 = itemstack.stackSize;
                }

                itemstack.stackSize -= j1;
                EntityItem entityitem = new EntityItem(
                        worldObj,
                        xCoord + f,
                        yCoord + f1,
                        zCoord + f2,
                        new ItemStack(itemstack.getItem(), j1, itemstack.getItemDamage()));

                if (itemstack.hasTagCompound()) {
                    entityitem.getEntityItem().setTagCompound((NBTTagCompound) itemstack.getTagCompound().copy());
                }

                float f3 = 0.05F;
                entityitem.motionX = (float) this.rand.nextGaussian() * f3;
                entityitem.motionY = (float) this.rand.nextGaussian() * f3 + 0.2F;
                entityitem.motionZ = (float) this.rand.nextGaussian() * f3;
                worldObj.spawnEntityInWorld(entityitem);
            }
        }
    }

    public void syncData() {

        if (worldObj.isRemote) return;

        NetworkUtils.sendToWatchers(new CarpenterPacket(this).generatePacket(), worldObj, this.xCoord, this.zCoord);

        /*
         * List<EntityPlayer> players = ((WorldServer) worldObj).playerEntities; for (int i = 0; i < players.size();
         * i++) { EntityPlayer player = players.get(i); ((WorldServer)
         * worldObj).getEntityTracker().func_151248_b(player, new CarpenterPacket(this).generatePacket()); }
         */
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

    public void setCraftingSound(String sound) {
        this.craftSound = sound;
    }

    public int getToolTierNeeded() {
        return this.hammerTierRequired;
    }

    public int getCarpenterTierNeeded() {
        return this.CarpenterTierRequired;
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
        if (syncCarpenter == null || craftMatrix == null) {
            return null;
        }

        if (ticksExisted <= 1) return null;

        for (int a = 0; a < getOutputSlotNum(); a++) {
            craftMatrix.setInventorySlotContents(a, inventory[a]);
        }

        return CraftingManagerCarpenter.getInstance().findMatchingRecipe(craftMatrix);
    }

    /** Works out the project for the grid as it is now; null when it makes nothing. */
    private CraftPlan buildProject(GridRecipe.Match match) {
        if (recipe == null) {
            return null;
        }
        RecipeId id = activeRecipe == null ? REPAIR : MFRecipes.CARPENTER.published().idOf(activeRecipe);
        int returns = getSizeInventory() - 4;
        CraftPlan.Builder plan = CraftPlan.builder(
                id == null ? REPAIR : id,
                MFRecipes.CARPENTER.published().getGeneration(),
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
        hammerTierRequired = plan.require(MFRecipeKeys.TOOL_TIER, 0);
        CarpenterTierRequired = plan.require(MFRecipeKeys.TIER, 0);
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
            for (RecipeEntry<GridRecipe> entry : MFRecipes.CARPENTER.published().all()) {
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
        if (candidates.isEmpty() && recipe != null && project != null) {
            candidates.add(new Diagnosis.Candidate(project.getRecipeId(), 0, requirementProblem(player)));
        }
        if (candidates.isEmpty()) {
            return Diagnosis.problem("carpenter", CheckResult.Reason.of("no_match"));
        }
        return Diagnosis.of("carpenter", candidates);
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
        if (!needTool.equalsIgnoreCase(tool)) {
            return CheckResult.Reason.of("tool", needTool, tool);
        }
        if (toolTier < needToolTier) {
            return CheckResult.Reason.tier("tool", toolTier, needToolTier);
        }
        if (tier < needStation) {
            return CheckResult.Reason.tier("bench", tier, needStation);
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
            ItemStack repair = craftMatrix == null ? null
                    : CraftingManagerCarpenter.getInstance().findRepairResult(craftMatrix);
            GridRecipe.Match match = repair != null || craftMatrix == null ? null
                    : CraftingManagerCarpenter.getInstance().match(craftMatrix);
            activeRecipe = match == null ? null : match.getRecipe();
            recipe = repair != null ? repair : match == null ? null : match.getResult();
            skillUsed = activeRecipe == null ? null : activeRecipe.getSkill();
            craftSound = activeRecipe == null ? craftSound : activeRecipe.getSound();
            project = buildProject(match);
            show(project);

            // Progress belongs to one project: another recipe, material, requirement or input starts over
            if (progress > 0 && (!canCraft() || !running.holds(project))) {
                progress = 0;
            }
            running.start(project);
            if (progress > progressMax) progress = progressMax - 1;
            syncData();
        }
    }

    /**
     * Sent by the game when a player starts watching this chunk. syncData only fires when something changes, so without
     * this a returning player saw "No Project Set" over a laid-out recipe until the next hit or grid change.
     */
    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setFloat("Progress", progress);
        nbt.setFloat("ProgressMax", progressMax);
        nbt.setInteger("HammerTier", hammerTierRequired);
        nbt.setInteger("CarpenterTier", CarpenterTierRequired);
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
        hammerTierRequired = nbt.getInteger("HammerTier");
        CarpenterTierRequired = nbt.getInteger("CarpenterTier");
        toolTypeRequired = nbt.getString("ToolNeeded");
        researchRequired = nbt.getString("Research");
        clientResult = nbt.hasKey("Result") ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("Result")) : null;
    }

    public boolean canCraft() {
        if (progressMax > 0 && recipe != null && recipe instanceof ItemStack) {
            return this.canFitResult(recipe);
        }
        return false;
    }

    public void setContainer(ContainerCarpenterMF container) {
        syncCarpenter = container;
        craftMatrix = new InventoryCrafting(syncCarpenter, GridRecipe.Grid.BENCH.width, GridRecipe.Grid.BENCH.height);
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

    // region client sync: the server shows the project; its packets and container fill these on the client

    public void setToolType(String toolType) {
        toolTypeRequired = toolType;
    }

    public void setResearch(String research) {
        researchRequired = research;
    }

    public void setToolTier(int tier) {
        hammerTierRequired = tier;
    }

    public void setRequiredCarpenter(int tier) {
        CarpenterTierRequired = tier;
    }

    // endregion
}
