package minefantasy.mf2.block.tileentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftInventory;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.Diagnosis;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.network.NetworkUtils;
import minefantasy.mf2.network.packet.QuernPacket;

public class TileEntityQuern extends TileEntity implements IInventory, ISidedInventory, Diagnosis.Source {

    public int turnAngle;
    private ItemStack[] inv = new ItemStack[3]; // 0 input, 1 pot, 2 output
    private int postUseTicks;

    public static final int getMaxRevs() {
        return 100;
    }

    public static boolean isInput(ItemStack input) {
        return MFRecipes.accepts(MFRecipes.QUERN, input);
    }

    /**
     * The grind the current contents allow: one input (and the pot, if the recipe uses it up) into the output slot.
     */
    public CheckResult check() {
        ItemStack input = inv[0];
        ItemStack pot = inv[1];
        RecipeEntry<ProcessRecipe> entry = MFRecipes.find(MFRecipes.QUERN, input, recipe -> {
            boolean consumePot = recipe.get(MFRecipeKeys.CONSUME_POT, true);
            return (!consumePot || pot != null) && recipe.get(MFRecipeKeys.TIER, 0) <= getTier();
        });
        if (entry == null) {
            return CheckResult.failure(CheckResult.Reason.NO_RECIPE);
        }
        ProcessRecipe recipe = entry.getRecipe();
        CraftPlan.Builder plan = CraftPlan.builder(entry.getId(), MFRecipes.QUERN.published().getGeneration(), 2)
                .use(0, recipe.getInput(), input).output(recipe.getOutput());
        if (recipe.get(MFRecipeKeys.CONSUME_POT, true)) {
            plan.use(1, Input.of(pot).amount(1), pot);
        }
        CraftPlan built = plan.build();
        return built.canApply(CraftInventory.of(this)) ? CheckResult.success(built)
                : CheckResult.failure(CheckResult.Reason.OUTPUT_FULL);
    }

    /** Every recipe the input could use, in lookup order, and why each is or is not the grind. */
    @Override
    public Diagnosis diagnose(EntityPlayer player) {
        ItemStack input = inv[0];
        ItemStack pot = inv[1];
        if (input == null) {
            return Diagnosis.problem("quern", CheckResult.Reason.MISSING_INPUT);
        }
        List<Diagnosis.Candidate> candidates = new ArrayList<>();
        boolean chosen = false;
        for (RecipeEntry<ProcessRecipe> entry : MFRecipes.QUERN.published().candidates(Input.lookupKeys(input))) {
            ProcessRecipe recipe = entry.getRecipe();
            CheckResult.Reason reason = recipe.getInput().explain(input);
            if (reason == null && recipe.get(MFRecipeKeys.CONSUME_POT, true) && pot == null) {
                reason = CheckResult.Reason.of("pot");
            }
            if (reason == null && recipe.get(MFRecipeKeys.TIER, 0) > getTier()) {
                reason = CheckResult.Reason.tier("quern", getTier(), recipe.get(MFRecipeKeys.TIER, 0));
            }
            if (reason == null && chosen) {
                reason = CheckResult.Reason.of("shadowed");
            }
            if (reason == null) {
                CheckResult result = check();
                reason = result.isSuccess() ? null : result.getReason();
                chosen = true;
            }
            candidates.add(Diagnosis.candidate(entry, reason));
        }
        return Diagnosis.of("quern", candidates);
    }

    public static boolean isPot(ItemStack item) {
        return item != null && item.getItem() == ComponentListMF.clay_pot;
    }

    @Override
    public void updateEntity() {
        int max = getMaxRevs();
        int levels = max / 4;

        if (postUseTicks > 0) {
            --postUseTicks;
        }
        if (postUseTicks > 0
                || !((turnAngle == levels || turnAngle == levels * 2 || turnAngle == levels * 3 || turnAngle == 0))) {
            this.turnAngle++;
            if (!worldObj.isRemote && (turnAngle == levels || turnAngle == levels * 2
                    || turnAngle == levels * 3
                    || turnAngle == max)) {
                worldObj.playSoundEffect(xCoord, yCoord, zCoord, "minefantasy2:block.quern", 1.0F, 1.0F);
                onRevolutionComplete();
            }
            if (turnAngle >= max) {
                turnAngle = 0;
            }
        }
    }

    public boolean onUse(EntityPlayer user) {
        if (!worldObj.isRemote && turnAngle == 0 && postUseTicks == 0) {
            worldObj.playSoundEffect(xCoord, yCoord, zCoord, "minefantasy2:block.quern", 1.0F, 1.0F);
        }
        this.postUseTicks = 10;
        syncAnimation();
        return true;
    }

    public int getPostUseTicks() {
        return postUseTicks;
    }

    public void setPostUseTicks(int postUseTicks) {
        this.postUseTicks = postUseTicks;
    }

    private void syncAnimation() {
        if (worldObj.isRemote) return;
        NetworkUtils.sendToWatchers(new QuernPacket(this).generatePacket(), worldObj, xCoord, zCoord);
    }

    public boolean onRevolutionComplete() {
        CheckResult result = check();
        if (!result.isSuccess()) {
            return false;
        }
        if (worldObj.isRemote) {
            worldObj.spawnParticle("smoke", xCoord + 0.5F, yCoord + 1F, zCoord + 0.5F, 0F, 0.2F, 0F);
            return true;
        }
        worldObj.playSoundEffect(xCoord, yCoord, zCoord, "minefantasy2:block.craftprimitive", 0.5F, 1.2F);
        return result.getPlan().apply(CraftInventory.of(this));
    }

    private int getTier() {
        return 0;
    }

    @Override
    public int getSizeInventory() {
        return inv.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inv[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int num) {
        return InventorySlots.take(inv, slot, num);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return InventorySlots.takeAll(inv, slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack item) {
        inv[slot] = item;
    }

    @Override
    public String getInventoryName() {
        return "gui.quern.name";
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
        if (isInput(item)) {
            return slot == 0;
        }
        if (item != null && item.getItem() == ComponentListMF.clay_pot) {
            return slot == 1;
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        inv = InventorySlots.read(nbt, "Items", inv.length);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        InventorySlots.write(nbt, "Items", inv);
    }

    @SideOnly(Side.CLIENT)
    public String getTextureName() {
        return "quern_basic";
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[] { 0, 1, 2 };
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack item, int side) {
        return isItemValidForSlot(slot, item);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack item, int side) {
        return slot == 2;
    }
}
