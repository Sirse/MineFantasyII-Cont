package minefantasy.mf2.block.tileentity;

import java.util.Random;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import minefantasy.mf2.api.crafting.carpenter.CarpenterCraftMatrix;
import minefantasy.mf2.api.crafting.kitchen.CraftingManagerKitchen;
import minefantasy.mf2.api.crafting.kitchen.IKitchen;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.config.ConfigKitchen;
import minefantasy.mf2.container.ContainerKitchenBench;

public class TileEntityKitchenBench extends TileEntity implements IInventory, IKitchen {

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
    private CarpenterCraftMatrix craftMatrix;
    private String lastPlayerHit = "";
    private String toolTypeRequired = "hands";
    private String craftSound = "step.wood";
    private String researchRequired = "";
    private Skill skillUsed;
    private boolean resetRecipe = false;
    private ItemStack recipe;

    public TileEntityKitchenBench() {
        inventory = new ItemStack[width * height + 5];
        setContainer(new ContainerKitchenBench(this));
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        NBTTagList savedItems = nbt.getTagList("Items", 10);
        this.inventory = new ItemStack[this.getSizeInventory()];

        for (int i = 0; i < savedItems.tagCount(); ++i) {
            NBTTagCompound savedSlot = savedItems.getCompoundTagAt(i);
            byte slotNum = savedSlot.getByte("Slot");

            if (slotNum >= 0 && slotNum < this.inventory.length) {
                this.inventory[slotNum] = ItemStack.loadItemStackFromNBT(savedSlot);
            }
        }
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
        if (this.inventory[slot] != null) {
            ItemStack itemstack;

            if (this.inventory[slot].stackSize <= num) {
                itemstack = this.inventory[slot];
                this.inventory[slot] = null;
                return itemstack;
            } else {
                itemstack = this.inventory[slot].splitStack(num);
                if (this.inventory[slot].stackSize == 0) {
                    this.inventory[slot] = null;
                }
                return itemstack;
            }
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (this.inventory[slot] != null) {
            ItemStack itemstack = this.inventory[slot];
            this.inventory[slot] = null;
            return itemstack;
        }
        return null;
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
        if (!worldObj.isRemote && ticksExisted % 20 == 0) {
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
            if (held != null && !recipeRequiresHands()) {
                held.damageItem(1, user);
                if (held.getItemDamage() >= held.getMaxDamage()) {
                    user.destroyCurrentEquippedItem();
                }
            }

            if (isDirty()) {
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
        if (this.canCraft() && !isDirty()) {
            addXP(user);
            addDirtyProgress(user);
            ItemStack result = recipe.copy();
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
            consumeResources();
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

    public String getResultName() {
        if (recipe != null && recipe.getItem() != null && recipe.getDisplayName() != null) {
            return recipe.getDisplayName();
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

    public void consumeResources() {
        resetRecipe = true;
        for (int slot = 0; slot < getOutputSlotNum(); slot++) {
            ItemStack item = getStackInSlot(slot);
            if (item != null && item.getItem() != null && item.getItem().getContainerItem(item) != null) {
                if (item.stackSize == 1) {
                    setInventorySlotContents(slot, item.getItem().getContainerItem(item));
                } else {
                    ItemStack drop = processSurplus(item.getItem().getContainerItem(item));
                    if (drop != null) {
                        dropItem(drop);
                    }
                    decrStackSize(slot, 1);
                }
            } else {
                decrStackSize(slot, 1);
            }
        }
        resetRecipe = false;
        onInventoryChanged();
    }

    private ItemStack processSurplus(ItemStack item) {
        for (int a = 0; a < 4; a++) {
            if (item == null) {
                return null;
            }
            int s = getSizeInventory() - 4 + a;
            ItemStack slot = inventory[s];
            if (slot == null) {
                setInventorySlotContents(s, item);
                return null;
            } else {
                if (slot.isItemEqual(item) && ItemStack.areItemStackTagsEqual(slot, item)
                        && slot.stackSize < slot.getMaxStackSize()) {
                    int roomLeft = slot.getMaxStackSize() - slot.stackSize;
                    int toMove = Math.min(roomLeft, item.stackSize);
                    slot.stackSize += toMove;
                    item.stackSize -= toMove;
                    if (item.stackSize <= 0) {
                        return null;
                    }
                }
            }
        }
        return item;
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
        return CraftingManagerKitchen.getInstance().findMatchingRecipe(this, craftMatrix);
    }

    public void updateCraftingData() {
        if (!worldObj.isRemote) {
            ItemStack oldRecipe = recipe;
            recipe = getResult();

            if ((!canCraft() || isDirty()) && progress > 0) {
                progress = 0;
            }
            if (recipe != null && oldRecipe != null && !recipe.isItemEqual(oldRecipe)) {
                progress = 0;
            }
            if (progress > progressMax) progress = progressMax - 1;
        }
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
        float max = ConfigKitchen.dirtyProgressMax;
        if (max <= 0) {
            return 0;
        }
        return (int) Math.ceil((i * dirtyProgress) / max);
    }

    @Override
    public void setForgeTime(int i) {
        progressMax = i;
    }

    @Override
    public void setToolTier(int i) {}

    @Override
    public void setRequiredCarpenter(int i) {}

    @Override
    public void setHotOutput(boolean hot) {}

    @Override
    public void setToolType(String toolType) {
        this.toolTypeRequired = toolType;
    }

    @Override
    public void setCraftingSound(String sound) {
        this.craftSound = sound;
    }

    @Override
    public void setResearch(String research) {
        this.researchRequired = research;
    }

    @Override
    public void setSkill(Skill skill) {
        skillUsed = skill;
    }

    @Override
    public void setDirtyAmount(float amount) {
        pendingDirtyAmount = amount;
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
            float baseXP = this.progressMax / 10F;
            skillUsed.addXP(smith, (int) baseXP + 1);
        }
    }

    public void setContainer(ContainerKitchenBench container) {
        syncBench = container;
        craftMatrix = new CarpenterCraftMatrix(this, syncBench, width, height);
    }
}
