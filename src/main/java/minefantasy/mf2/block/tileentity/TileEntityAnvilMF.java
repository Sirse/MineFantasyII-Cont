package minefantasy.mf2.block.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.IQualityBalance;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.Requirements;
import minefantasy.mf2.api.crafting.anvil.CraftingManagerAnvil;
import minefantasy.mf2.api.crafting.exotic.SpecialForging;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.IHotItem;
import minefantasy.mf2.api.helpers.Sounds;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.Diagnosis;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.container.ContainerAnvilMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

public class TileEntityAnvilMF extends TileEntityStation
        implements GridProject.Bench, CraftBench, IQualityBalance, Diagnosis.Source {

    private final Random rand = new Random();
    public int tier;
    public float progressMax;
    public float progress;
    public String texName = "";
    public float qualityBalance = 0F;
    public float thresholdPosition = 0.1F;
    public float leftHit = 0F;
    public float rightHit = 0F;
    private ItemStack[] inventory;
    private int ticksExisted;
    private ContainerAnvilMF syncAnvil;
    private InventoryCrafting craftMatrix;
    private String lastPlayerHit = "";
    private String toolTypeRequired = "hammer";
    private String researchRequired = "";
    private boolean outputHot = false;
    private Skill skillUsed;
    private boolean resetRecipe = false;
    private boolean craftingDataDirty = true;
    /** Saved progress outlives the derived recipe cache, so rebuild it before the first canCraft check. */
    private boolean needsRecipeRestore;
    private boolean isFakeAnvil = false;
    private ItemStack recipe;
    private GridRecipe activeRecipe;
    /** The id of the recipe the grid holds, as the lookup found it. */
    private RecipeId activeId;
    /** The craft the grid holds, worked out in full; the HUD, the save and finishing all read it. */
    private CraftPlan project;
    /** The project the progress belongs to, kept across saves, and what watchers last got. */
    private final CraftState craft = new CraftState();
    private static final RecipeId REPAIR = RecipeId.of("minefantasy2", "anvil/repair");
    private int hammerTierRequired;
    private int anvilTierRequired;
    /** How wide the hit window stays on an anvil below the recipe's tier; a lower anvil still works, only harder */
    public static final float LOW_TIER_HIT_WINDOW = 0.25F;

    public TileEntityAnvilMF() {
        this(0, "Iron");
    }

    public TileEntityAnvilMF(int tier, String name) {
        inventory = new ItemStack[25];
        this.tier = tier;
        texName = name;
        setContainer(new ContainerAnvilMF(this));
    }

    public TileEntityAnvilMF setDisplay() {
        isFakeAnvil = true;
        return this;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        craft.read(nbt);
        tier = nbt.getInteger("tier");
        inventory = InventorySlots.read(nbt, "Items", inventory.length);
        progress = nbt.getFloat("Progress");
        progressMax = nbt.getFloat("ProgressMax");
        needsRecipeRestore = progressMax > 0;
        toolTypeRequired = nbt.getString("toolTypeRequired");
        researchRequired = nbt.getString("researchRequired");
        texName = nbt.getString("TextureName");
        outputHot = nbt.getBoolean("outputHot");
        qualityBalance = nbt.getFloat("Quality");
        leftHit = nbt.getFloat("leftHit");
        rightHit = nbt.getFloat("rightHit");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        craft.write(nbt);
        nbt.setInteger("tier", tier);

        InventorySlots.write(nbt, "Items", inventory);

        nbt.setFloat("Progress", progress);
        nbt.setFloat("ProgressMax", progressMax);
        nbt.setString("toolTypeRequired", toolTypeRequired);
        nbt.setString("researchRequired", researchRequired);
        nbt.setString("TextureName", texName);
        nbt.setBoolean("outputHot", outputHot);
        nbt.setFloat("Quality", qualityBalance);
        nbt.setFloat("leftHit", leftHit);
        nbt.setFloat("rightHit", rightHit);
    }

    @Override
    public String getInventoryName() {
        return "gui.anvilmf.name";
    }

    @Override
    public void updateEntity() {
        ++ticksExisted;
        if (!worldObj.isRemote && (leftHit == 0 || rightHit == 0)) {
            reassignHitValues();
        }
        super.updateEntity();
        if (!worldObj.isRemote) {
            if (needsRecipeRestore) {
                needsRecipeRestore = false;
                if (!isFakeAnvil) {
                    updateCraftingData();
                    craftingDataDirty = false;
                }
            }
            if (!isFakeAnvil && craftingDataDirty && ticksExisted % 20 == 0) {
                updateCraftingData();
                craftingDataDirty = false;
            }
            if (!canCraft() && ticksExisted > 1) {
                progress = progressMax = 0;
                this.recipe = null;
            }
        }
        if (!worldObj.isRemote) {
            updateThreshold();
        }
        resetRecipe = false;
    }

    public void onInventoryChanged() {
        if (!resetRecipe) {
            updateCraftingData();
            craftingDataDirty = false;
            resetRecipe = true;
        } else {
            craftingDataDirty = true;
        }
    }

    public boolean tryCraft(EntityPlayer user, boolean rightClick) {
        if (user == null) return false;

        String toolType = ToolHelper.getCrafterTool(user.getHeldItem());
        if (toolType.equalsIgnoreCase("hammer") || toolType.equalsIgnoreCase("hvyHammer")) {
            ToolHelper.wearCrafterTool(user);
            if (worldObj.isRemote) return true;

            Requirements.Verdict verdict = verdict(user);
            if (verdict != null && verdict.allows() && canCraft()) {
                float mod = 1.0F;
                if (verdict.isToolWeak()) {
                    mod = 2.0F;
                    if (rand.nextInt(5) == 0) {
                        reassignHitValues();
                    }
                }
                if (rightClick) {
                    this.qualityBalance += (rightHit * mod);
                } else {
                    this.qualityBalance -= (leftHit * mod);
                }
                if (qualityBalance >= 1.0F || qualityBalance <= -1.0F) {
                    ruinCraft();
                }

                Sounds.at(this, "minefantasy2:block.anvilsucceed", 0.25F, rightClick ? 1.2F : 1.0F);
                float efficiency = ToolHelper.getCrafterEfficiency(user.getHeldItem()) * (rightClick ? 0.75F : 1.0F);

                if (user.swingProgress > 0 && user.swingProgress <= 1.0) {
                    efficiency *= (0.5F - user.swingProgress);
                }

                progress += Math.max(0.2F, efficiency);
                if (progress >= progressMax) {
                    craftItem(user);
                }
            } else {
                Sounds.at(this, "minefantasy2:block.anvilfail", 0.25F, 1.0F);
            }
            lastPlayerHit = user.getCommandSenderName();
            updateCraftingData();

            return true;
        }
        updateCraftingData();
        return false;
    }

    private void ruinCraft() {
        if (!worldObj.isRemote) {
            payGrid();
            reassignHitValues();
            progress = progressMax = qualityBalance = 0;
        }
        worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.8, zCoord + 0.5, "random.break", 1.0F, 0.8F);
    }

    public boolean doesPlayerKnowCraft(EntityPlayer user) {
        return getResearchNeeded().isEmpty() || ResearchLogic.hasInfoUnlocked(user, getResearchNeeded());
    }

    private void craftItem(EntityPlayer lastHit) {
        if (!GridProject.stillMakes(this)) {
            progress = 0;
            return;
        }
        if (this.canCraft()) {
            ItemStack result = AnvilFinish
                    .finish(this, project.getProduct(), lastPlayerHit, project.require(MFRecipeKeys.HOT_OUTPUT, false));

            addXP(lastHit);
            // The grid pays first: nothing is produced if it no longer holds what the project takes
            if (payGrid()) {
                placeResult(result);
            }
        }
        onInventoryChanged();
        progress = 0;
        reassignHitValues();
        qualityBalance = 0;
    }

    private void placeResult(ItemStack result) {
        int outputSlot = getSizeInventory() - 1;

        if (inventory[outputSlot] == null || SpecialForging.getItemDesign(inventory[outputSlot]) != null) {
            inventory[outputSlot] = result;
        } else {
            if (inventory[outputSlot].isItemEqual(result)
                    && ItemStack.areItemStackTagsEqual(inventory[outputSlot], result)) {
                if (inventory[outputSlot].stackSize + result.stackSize <= getStackSize(inventory[outputSlot])) {
                    inventory[outputSlot].stackSize += result.stackSize;
                } else {
                    InventorySlots.drop(worldObj, xCoord, yCoord, zCoord, result);
                }
            } else {
                InventorySlots.drop(worldObj, xCoord, yCoord, zCoord, result);
            }
        }
    }

    private void addXP(EntityPlayer smith) {
        if (skillUsed == null) return;

        float baseXP = project.require(MFRecipeKeys.TIME, 0F) / 10F;
        baseXP /= (1.0F + getAbsoluteBalance());

        skillUsed.addXP(smith, (int) baseXP + 1);
    }

    /**
     * A forge within four blocks holding a dragon heart, for a player who has learned dragonforging; null for none. The
     * craft that finds it takes that one heart.
     */
    TileEntityForge heartedForge(EntityPlayer player) {
        if (player == null || !ResearchLogic.hasInfoUnlocked(player, KnowledgeListMF.smeltDragonforge)) {
            return null;
        }
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    TileEntity tile = worldObj.getTileEntity(xCoord + x, yCoord + y, zCoord + z);
                    if (tile instanceof TileEntityForge && ((TileEntityForge) tile).dragonHeartPower > 0) {
                        return (TileEntityForge) tile;
                    }
                }
            }
        }
        return null;
    }

    public void syncData() {
        sendState(false);
    }

    /** Result as the server last sent it; clients never run the recipe lookup themselves */
    private ItemStack clientResult;

    /** The recipe result, or on the client the copy the server synced for display */
    public ItemStack getShownResult() {
        return worldObj != null && worldObj.isRemote ? clientResult : recipe;
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
        return "gui.noproject";
    }

    public String getResearchNeeded() {
        return researchRequired;
    }

    public String getToolNeeded() {
        return toolTypeRequired;
    }

    public int getToolTierNeeded() {
        return this.hammerTierRequired;
    }

    public int getAnvilTierNeeded() {
        return this.anvilTierRequired;
    }

    /** Takes what the project owes from the grid; containers without room are dropped. False on a mismatch. */
    private boolean payGrid() {
        if (project == null) {
            return false;
        }
        List<ItemStack> spill = new ArrayList<>();
        resetRecipe = true;
        boolean paid = GridProject.pay(project, this, spill);
        resetRecipe = false;
        for (ItemStack stack : spill) {
            InventorySlots.drop(worldObj, xCoord, yCoord, zCoord, stack);
        }
        this.onInventoryChanged();
        return paid;
    }

    private boolean canFitResult(ItemStack result) {
        ItemStack resSlot = inventory[getSizeInventory() - 1];
        if (resSlot != null && result != null) {
            int maxStack = getStackSize(resSlot);
            if (resSlot.stackSize + result.stackSize > maxStack) {
                return false;
            }
            if (resSlot.getItem() instanceof IHotItem) {
                ItemStack heated = Heatable.getItem(resSlot);
                if (heated != null) {
                    resSlot = heated;
                }
            }
            return (resSlot.isItemEqual(result) && ItemStack.areItemStackTagsEqual(resSlot, result))
                    || (SpecialForging.getItemDesign(resSlot) != null && resSlot.stackSize <= 1);
        }
        return true;
    }

    private int getStackSize(ItemStack slot) {
        if (slot == null) return 0;

        if (slot.getItem() instanceof IHotItem) {
            ItemStack held = Heatable.getItem(slot);
            if (held != null) return held.getMaxStackSize();
        }
        return slot.getMaxStackSize();
    }

    // CRAFTING CODE
    public ItemStack getResult() {
        if (syncAnvil == null || craftMatrix == null) {
            return null;
        }

        if (ticksExisted <= 1) return null;

        for (int a = 0; a < getSizeInventory() - 1; a++) {
            craftMatrix.setInventorySlotContents(a, inventory[a]);
        }

        return CraftingManagerAnvil.getInstance().findMatchingRecipe(craftMatrix);
    }

    /** Works out the project for the grid as it is now; null when it makes nothing. */
    private CraftPlan buildProject(GridRecipe.Match match) {
        if (recipe == null) {
            return null;
        }
        RecipeId id = activeRecipe == null ? REPAIR : activeId;
        CraftPlan.Builder plan = CraftPlan
                .builder(id == null ? REPAIR : id, MFRecipes.ANVIL.published().getGeneration(), getSizeInventory() - 1);
        GridProject.addGrid(plan, this, getSizeInventory() - 1, match == null ? null : match.getAmounts(), true);
        if (match == null) {
            GridProject.requireRepair(plan, "hammer");
        } else {
            GridProject.require(plan, match);
        }
        return plan.product(recipe).build();
    }

    /** Shows the project on the station: the HUD and the hit checks read these. */
    private void show(CraftPlan plan) {
        if (plan == null) {
            return;
        }
        progressMax = plan.require(MFRecipeKeys.TIME, 0F);
        toolTypeRequired = plan.require(MFRecipeKeys.TOOL, "hammer");
        hammerTierRequired = plan.require(MFRecipeKeys.TOOL_TIER, 0);
        anvilTierRequired = plan.require(MFRecipeKeys.TIER, 0);
        researchRequired = plan.require(MFRecipeKeys.RESEARCH, "");
        outputHot = plan.require(MFRecipeKeys.HOT_OUTPUT, false);
    }

    /**
     * Every recipe matching the grid, in lookup order: the first is crafted unless the player or the bench stops it,
     * the rest are shadowed by it.
     */
    @Override
    public Diagnosis diagnose(EntityPlayer player) {
        updateCraftingData();
        List<Diagnosis.Candidate> candidates = craftMatrix != null && findRepair() == null ? Diagnosis.walk(
                MFRecipes.ANVIL.published().all(),
                recipe -> recipe.matches(craftMatrix),
                entry -> null,
                entry -> requirementProblem(player)).getCandidates() : new ArrayList<>();
        if (candidates.isEmpty() && recipe != null && project != null) {
            candidates.add(new Diagnosis.Candidate(project.getRecipeId(), 0, requirementProblem(player)));
        }
        if (candidates.isEmpty()) {
            return Diagnosis.problem("anvil", CheckResult.Reason.of("no_match"));
        }
        return Diagnosis.of("anvil", candidates);
    }

    /** How the project's requirements judge the player and this anvil; null without a project. */
    private Requirements.Verdict verdict(EntityPlayer player) {
        return project == null ? null : Requirements.of(project).check(Requirements.ANVIL, player, tier);
    }

    /** What stops the player crafting the project, or null; a penalty means it only works harder. */
    private CheckResult.Reason requirementProblem(EntityPlayer player) {
        Requirements.Verdict verdict = verdict(player);
        if (verdict == null) {
            return CheckResult.Reason.NO_RECIPE;
        }
        if (!verdict.allows()) {
            return verdict.getRefusal();
        }
        return canCraft() ? verdict.getPenalty() : CheckResult.Reason.OUTPUT_FULL;
    }

    private ItemStack findRepair() {
        return craftMatrix == null ? null : CraftingManagerAnvil.getInstance().findRepairResult(craftMatrix);
    }

    public void updateCraftingData() {
        if (!worldObj.isRemote) {
            if (craftMatrix != null) {
                for (int a = 0; a < getSizeInventory() - 1; a++) {
                    craftMatrix.setInventorySlotContents(a, inventory[a]);
                }
            }
            ItemStack repair = findRepair();
            GridRecipe.Found found = repair != null || craftMatrix == null ? null
                    : CraftingManagerAnvil.getInstance().find(craftMatrix);
            GridRecipe.Match match = found == null ? null : found.getMatch();
            activeRecipe = match == null ? null : match.getRecipe();
            activeId = found == null ? null : found.getId();
            skillUsed = activeRecipe == null ? null : activeRecipe.getSkill();
            recipe = repair != null ? repair : match == null ? null : match.getResult();
            project = buildProject(match);
            show(project);

            // Progress belongs to one project: another recipe, material, requirement or input starts over
            boolean carriesOn = craft.follow(project);
            if (progress > 0 && (!canCraft() || !carriesOn)) {
                progress = 0;
                reassignHitValues();
                qualityBalance = 0;
            }
            if (progress > progressMax) progress = progressMax - 1;
            syncData();
        }
    }

    /**
     * What watchers are shown of the station: the description packet and the state packet carry the same, and
     * {@link #show} reads it back.
     */
    @Override
    protected NBTTagCompound describe() {
        NBTTagCompound nbt = new NBTTagCompound();
        CraftHud.write(nbt, progress, progressMax, toolTypeRequired, researchRequired, recipe);
        nbt.setFloat("QualityBalance", qualityBalance);
        nbt.setFloat("ThresholdPosition", thresholdPosition);
        nbt.setFloat("LeftHit", leftHit);
        nbt.setFloat("RightHit", rightHit);
        nbt.setInteger(CraftHud.TOOL_TIER, hammerTierRequired);
        nbt.setInteger("AnvilTier", anvilTierRequired);
        return nbt;
    }

    @Override
    public void show(NBTTagCompound state) {
        CraftHud hud = CraftHud.read(state);
        progress = hud.progress;
        progressMax = hud.progressMax;
        toolTypeRequired = hud.tool;
        researchRequired = hud.research;
        clientResult = hud.result;
        qualityBalance = state.getFloat("QualityBalance");
        thresholdPosition = state.getFloat("ThresholdPosition");
        leftHit = state.getFloat("LeftHit");
        rightHit = state.getFloat("RightHit");
        hammerTierRequired = state.getInteger(CraftHud.TOOL_TIER);
        anvilTierRequired = state.getInteger("AnvilTier");
    }

    public boolean canCraft() {
        if (progressMax > 0 && recipe instanceof ItemStack) {
            return this.canFitResult(recipe);
        }
        return false;
    }

    public boolean isOutputHot() {
        return this.outputHot;
    }

    public void setContainer(ContainerAnvilMF container) {
        syncAnvil = container;
        craftMatrix = new InventoryCrafting(syncAnvil, GridRecipe.Grid.ANVIL.width, GridRecipe.Grid.ANVIL.height);
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

    public String getTextureName() {
        return texName;
    }

    @Override
    public float getMarkerPosition() {
        return qualityBalance;
    }

    @Override
    public boolean shouldShowMetre() {
        // Hit quality only means something while there is a project to hit
        return hasProject();
    }

    @Override
    public float getThresholdPosition() {
        return thresholdPosition;
    }

    @Override
    public float getSuperThresholdPosition() {
        return thresholdPosition / 3.5F;
    }

    float getAbsoluteBalance() {
        return qualityBalance < 0 ? -qualityBalance : qualityBalance;
    }

    private void updateThreshold() {
        float modifier = 1.0F;
        if (tier < anvilTierRequired) {
            modifier *= LOW_TIER_HIT_WINDOW;
        }

        float baseThreshold = worldObj.difficultySetting.getDifficultyId() >= 2 ? 7.5F : 10F;
        thresholdPosition = (baseThreshold / 100F) * modifier;
    }

    public void upset(EntityPlayer user) {
        if (this.progress > 0 && this.progressMax > 0) {
            Sounds.at(this, "minefantasy2:block.anvilsucceed", 0.25F, 0.75F);
            if (!worldObj.isRemote) {
                progress -= (progressMax / 10F);
                if (progress < 0) {
                    ruinCraft();
                }
            }
        }
    }

    private void reassignHitValues() {
        if (!worldObj.isRemote) {
            leftHit = 0.1F + (0.01F * rand.nextInt(11));
            rightHit = 0.1F + (0.01F * rand.nextInt(11));
        }
    }

    // region client sync: the server shows the project; its packets and container fill these on the client

    public void setHammerUsed(int tier) {
        hammerTierRequired = tier;
    }

    public void setRequiredAnvil(int tier) {
        anvilTierRequired = tier;
    }

    public void setHotOutput(boolean hot) {
        outputHot = hot;
    }

    public int getRecipeHammer() {
        return hammerTierRequired;
    }

    public int getRecipeAnvil() {
        return anvilTierRequired;
    }

    // endregion

    @Override
    public CraftPlan currentProject() {
        return project;
    }

    @Override
    protected ItemStack[] slots() {
        return inventory;
    }

    @Override
    public Role role(int slot) {
        return slot == getSizeInventory() - 1 ? Role.OUTPUT : Role.INPUT;
    }

    @Override
    public float getProgress() {
        return progress;
    }

    @Override
    public float getProgressMax() {
        return progressMax;
    }

    @Override
    public int getBenchTierNeeded() {
        return getAnvilTierNeeded();
    }

    @Override
    public boolean isBenchSufficient() {
        return tier >= getAnvilTierNeeded();
    }
}
