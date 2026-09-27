package minefantasy.mf2.block.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
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
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.ItemQuality;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.Diagnosis;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.container.ContainerAnvilMF;
import minefantasy.mf2.entity.EntityItemUnbreakable;
import minefantasy.mf2.item.armour.ItemArmourMF;
import minefantasy.mf2.item.heatable.ItemHeated;
import minefantasy.mf2.knowledge.KnowledgeListMF;
import minefantasy.mf2.mechanics.PlayerTickHandlerMF;
import minefantasy.mf2.network.NetworkUtils;
import minefantasy.mf2.network.packet.StationStatePacket;

public class TileEntityAnvilMF extends TileEntity
        implements StationStatePacket.Shown, IInventory, IQualityBalance, Diagnosis.Source {

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
    public int getSizeInventory() {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int num) {
        ItemStack taken = InventorySlots.take(inventory, slot, num);
        if (taken != null) {
            onInventoryChanged();
        }
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return InventorySlots.takeAll(inventory, slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack item) {
        inventory[slot] = item;
        onInventoryChanged();
    }

    @Override
    public String getInventoryName() {
        return "gui.anvilmf.name";
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
            if (user.getHeldItem() != null) {
                user.getHeldItem().damageItem(1, user);
                if (user.getHeldItem().getItemDamage() >= user.getHeldItem().getMaxDamage()) {
                    if (worldObj.isRemote) user.renderBrokenItemStack(user.getHeldItem());

                    user.destroyCurrentEquippedItem();
                }
            }
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

                worldObj.playSoundEffect(
                        xCoord + 0.5D,
                        yCoord + 0.5D,
                        zCoord + 0.5D,
                        "minefantasy2:block.anvilsucceed",
                        0.25F,
                        rightClick ? 1.2F : 1.0F);
                float efficiency = ToolHelper.getCrafterEfficiency(user.getHeldItem()) * (rightClick ? 0.75F : 1.0F);

                if (user.swingProgress > 0 && user.swingProgress <= 1.0) {
                    efficiency *= (0.5F - user.swingProgress);
                }

                progress += Math.max(0.2F, efficiency);
                if (progress >= progressMax) {
                    craftItem(user);
                }
            } else {
                worldObj.playSoundEffect(
                        xCoord + 0.5D,
                        yCoord + 0.5D,
                        zCoord + 0.5D,
                        "minefantasy2:block.anvilfail",
                        0.25F,
                        1.0F);
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
        // Re-read the grid before paying out: inventory changes after the first one only refresh the recipe on a
        // timer, so the project may no longer match what is actually on the anvil
        CraftPlan crafting = project;
        updateCraftingData();
        if (crafting == null || !crafting.sameAs(project)) {
            progress = 0;
            return;
        }
        if (this.canCraft()) {
            ItemStack result = modifySpecials(project.getProduct());
            if (result == null) {
                return;
            }

            if (result.getItem() instanceof ItemArmourMF) {
                result = modifyArmour(result);
            }

            if (result.getMaxStackSize() == 1 && !lastPlayerHit.isEmpty()) {
                getNBT(result).setString("MF_CraftedByName", lastPlayerHit);
            }

            int temp = this.averageTemp();
            if (project.require(MFRecipeKeys.HOT_OUTPUT, false) && temp > 0) {
                result = ItemHeated.createHotItem(result, temp);
            }

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
                    dropItem(result);
                }
            } else {
                dropItem(result);
            }
        }
    }

    private void addXP(EntityPlayer smith) {
        if (skillUsed == null) return;

        float baseXP = project.require(MFRecipeKeys.TIME, 0F) / 10F;
        baseXP /= (1.0F + getAbsoluteBalance());

        skillUsed.addXP(smith, (int) baseXP + 1);
    }

    private ItemStack modifyArmour(ItemStack result) {
        ItemArmourMF item = (ItemArmourMF) result.getItem();
        boolean canColour = item.canColour();
        int colour = -1;
        for (int a = 0; a < getSizeInventory() - 1; a++) {
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

    private ItemStack modifySpecials(ItemStack result) {
        boolean hasHeart = false;
        boolean isTool = result.getMaxStackSize() == 1 && result.isItemStackDamageable();
        EntityPlayer player = worldObj.getPlayerEntityByName(lastPlayerHit);

        Item DF = SpecialForging.getDragonCraft(result);

        if (DF != null) {
            // DRAGONFORGE
            for (int x = -4; x <= 4; x++) {
                for (int y = -4; y <= 4; y++) {
                    for (int z = -4; z <= 4; z++) {
                        TileEntity tile = worldObj.getTileEntity(xCoord + x, yCoord + y, zCoord + z);
                        if (player != null && ResearchLogic.hasInfoUnlocked(player, KnowledgeListMF.smeltDragonforge)
                                && tile != null
                                && tile instanceof TileEntityForge) {
                            if (((TileEntityForge) tile).dragonHeartPower > 0) {
                                hasHeart = true;
                                ((TileEntityForge) tile).dragonHeartPower = 0;
                                worldObj.createExplosion(player, xCoord + x, yCoord + y, zCoord + z, 1F, false);
                                PlayerTickHandlerMF.spawnDragon(player);
                                PlayerTickHandlerMF.addDragonEnemyPts(player, 2);

                                break;
                            }
                        }
                    }
                }
            }
        }
        if (hasHeart) {
            NBTBase nbt = !(result.hasTagCompound()) ? null : result.getTagCompound().copy();
            result = new ItemStack(DF, result.stackSize, result.getItemDamage());
            if (nbt != null) {
                result.setTagCompound((NBTTagCompound) nbt);
            }
        } else {
            Item special = SpecialForging
                    .getSpecialCraft(SpecialForging.getItemDesign(inventory[getSizeInventory() - 1]), result);
            if (special != null) {
                NBTBase nbt = !(result.hasTagCompound()) ? null : result.getTagCompound().copy();
                result = new ItemStack(special, result.stackSize, result.getItemDamage());
                if (nbt != null) {
                    result.setTagCompound((NBTTagCompound) nbt);
                }
            }
        }

        if (isPerfectItem() && !isMythicRecipe()) {
            grade(result, ItemQuality.Grade.SUPERIOR);
            if (CustomToolHelper.isMythic(result)) {
                ToolHelper.setUnbreakable(result, true);
                result.getTagCompound().setBoolean(EntityItemUnbreakable.persistNBT, true);
            } else {
                ItemQuality.set(result, ItemQuality.MAX);
            }
            return result;
        }
        if (isTool) {
            result = modifyQualityComponents(result);
        }
        return damageItem(result);
    }

    private ItemStack modifyQualityComponents(ItemStack result) {
        float totalPts = 0F;
        int totalItems = 0;
        for (ItemStack item : inventory) {
            ItemQuality.Grade grade = ItemQuality.getGrade(item);
            if (grade != ItemQuality.Grade.ORDINARY) {
                ++totalItems;
                totalPts += grade == ItemQuality.Grade.INFERIOR ? -50F : 100F;
            }
        }
        if (totalItems > 0 && totalPts > 0) {
            totalPts /= totalItems;
            ItemQuality.set(result, ItemQuality.get(result) + totalPts);
            if (totalPts <= -85F) {
                grade(result, ItemQuality.Grade.INFERIOR);
            }
            if (totalPts >= 80) {
                grade(result, ItemQuality.Grade.SUPERIOR);
            }
        }
        return result;
    }

    private int averageTemp() {
        float totalTemp = 0;
        int itemCount = 0;
        for (int a = 0; a < getSizeInventory() - 1; a++) {
            ItemStack item = getStackInSlot(a);
            if (item != null && item.getItem() instanceof IHotItem) {
                ++itemCount;
                totalTemp += Heatable.getTemp(item);
            }
        }
        if (totalTemp > 0 && itemCount > 0) {
            return (int) (totalTemp / itemCount);
        }
        return 0;
    }

    private NBTTagCompound getNBT(ItemStack item) {
        if (!item.hasTagCompound()) {
            item.setTagCompound(new NBTTagCompound());
        }
        return item.getTagCompound();
    }

    private void dropItem(ItemStack itemstack) {
        if (itemstack != null) {
            float f = this.rand.nextFloat() * 0.4F + 0.3F;
            float f1 = this.rand.nextFloat() * 0.4F + 0.3F;
            float f2 = this.rand.nextFloat() * 0.4F + 0.3F;

            while (itemstack.stackSize > 0) {
                int j1 = this.rand.nextInt(21) + 10;

                if (j1 > itemstack.stackSize) {
                    j1 = itemstack.stackSize;
                }

                boolean delay = true;
                double[] positions = new double[] { xCoord + f, yCoord + f1 + 1, zCoord + f2 };
                if (worldObj.getBlock(xCoord, yCoord + 1, zCoord).getMaterial().isSolid() && lastPlayerHit != null) {
                    EntityPlayer smith = worldObj.getPlayerEntityByName(lastPlayerHit);
                    if (smith != null) {
                        delay = false;
                        positions = new double[] { smith.posX, smith.posY, smith.posZ };
                    }
                }

                itemstack.stackSize -= j1;
                EntityItem entityitem = new EntityItem(
                        worldObj,
                        positions[0],
                        positions[1],
                        positions[2],
                        new ItemStack(itemstack.getItem(), j1, itemstack.getItemDamage()));
                if (itemstack.hasTagCompound()) {
                    entityitem.getEntityItem().setTagCompound((NBTTagCompound) itemstack.getTagCompound().copy());
                }

                entityitem.delayBeforeCanPickup = delay ? 20 : 0;
                entityitem.motionX = entityitem.motionY = entityitem.motionZ = 0;

                worldObj.spawnEntityInWorld(entityitem);
            }
        }
    }

    public boolean hasItems(EntityPlayer user, ItemStack[] items) {
        for (ItemStack check : items) {
            if (!hasItems(user, check)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasItems(EntityPlayer user, ItemStack item) {
        return hasItems(user, item, item.stackSize);
    }

    public boolean hasItems(EntityPlayer user, ItemStack item, int number) {
        int count = 0;
        for (int a = 0; a < user.inventory.getSizeInventory(); a++) {
            ItemStack slot = user.inventory.getStackInSlot(a);
            if (slot != null && slot.isItemEqual(item)) {
                count += slot.stackSize;
            }
        }
        return count >= number;
    }

    /** Sends the state to the watchers, if it changed since they got it last. */
    public void syncData() {
        if (worldObj.isRemote) return;
        NBTTagCompound state = describe();
        if (craft.changed(state)) {
            NetworkUtils.sendToWatchers(
                    new StationStatePacket(this, state).generatePacket(),
                    worldObj,
                    this.xCoord,
                    this.zCoord);
        }
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
            dropItem(stack);
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
     * Sent by the game when a player starts watching this chunk. syncData only fires when something changes, so without
     * this a returning player saw "No Project Set" over a laid-out recipe until the next hit or grid change.
     */
    @Override
    public Packet getDescriptionPacket() {
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, describe());
    }

    /**
     * What watchers are shown of the station: the description packet and the state packet carry the same, and
     * {@link #show} reads it back.
     */
    private NBTTagCompound describe() {
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
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        show(packet.func_148857_g());
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
        if (this.isMythicRecipe() && !this.isMythicReady()) {
            return false;
        }

        if (progressMax > 0 && recipe != null && recipe instanceof ItemStack) {
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

    private boolean isMythicRecipe() {
        return false;// this.hammerTierRequired >= 6;
    }

    private boolean isMythicReady() {
        return true;
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

    private float getAbsoluteBalance() {
        return qualityBalance < 0 ? -qualityBalance : qualityBalance;
    }

    private float getItemDamage() {
        int threshold = (int) (100F * thresholdPosition / 2F);
        int total = (int) (100F * getAbsoluteBalance() - threshold);

        if (total > threshold) {
            float percent = ((float) total - (float) threshold) / (100F - threshold);
            return percent;
        }
        return 0F;
    }

    private boolean isPerfectItem() {
        int threshold = (int) (100F * getSuperThresholdPosition() / 2F);
        int total = (int) (100F * getAbsoluteBalance() - threshold);

        return total <= threshold;
    }

    private ItemStack damageItem(ItemStack item) {
        float itemdam = getItemDamage();
        if (itemdam > 0.5F) {
            grade(item, ItemQuality.Grade.INFERIOR);
            float q = 100F * (0.75F - (itemdam - 0.5F));
            ItemQuality.set(item, Math.max(10F, q));
        }
        float damage = itemdam * item.getMaxDamage();
        if (item.isItemStackDamageable()) {
            if (damage > 0) {
                item.setItemDamage((int) (damage));
                if (isMythicRecipe()) {
                    grade(item, ItemQuality.Grade.INFERIOR);
                }
            } else if (isMythicRecipe()) {
                ToolHelper.setUnbreakable(item, true);
            }
        }
        return item;
    }

    /** Grades a forged item; only one that wears down has a grade. */
    private void grade(ItemStack item, ItemQuality.Grade grade) {
        if (item != null && item.isItemStackDamageable()) {
            ItemQuality.setGrade(item, grade);
        }
    }

    private void updateThreshold() {
        float modifier = 1.0F;
        if (tier < anvilTierRequired) {
            modifier *= LOW_TIER_HIT_WINDOW;
        }

        float baseThreshold = worldObj.difficultySetting.getDifficultyId() >= 2 ? 7.5F : 10F;
        thresholdPosition = (isMythicRecipe() ? 0.05F : baseThreshold / 100F) * modifier;
    }

    public void upset(EntityPlayer user) {
        if (this.progress > 0 && this.progressMax > 0) {
            worldObj.playSoundEffect(
                    xCoord + 0.5D,
                    yCoord + 0.5D,
                    zCoord + 0.5D,
                    "minefantasy2:block.anvilsucceed",
                    0.25F,
                    0.75F);
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
}
