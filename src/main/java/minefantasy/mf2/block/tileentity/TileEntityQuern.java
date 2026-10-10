package minefantasy.mf2.block.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.Requirements;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftInventory;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.Diagnosis;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.item.list.ComponentListMF;

public class TileEntityQuern extends TileEntityStation implements Diagnosis.Source {

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
            return (!consumePot || pot != null)
                    && Requirements.QUERN.stationFits(getTier(), recipe.get(MFRecipeKeys.TIER, 0));
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
        return Diagnosis.of(
                "quern",
                Diagnosis.walk(
                        MFRecipes.QUERN.published().candidates(Input.lookupKeys(input)),
                        recipe -> true,
                        entry -> {
                            ProcessRecipe recipe = entry.getRecipe();
                            CheckResult.Reason reason = recipe.getInput().explain(input);
                            if (reason == null && recipe.get(MFRecipeKeys.CONSUME_POT, true) && pot == null) {
                                reason = CheckResult.Reason.of("pot");
                            }
                            return reason != null ? reason
                                    : Requirements.QUERN.stationProblem(getTier(), recipe.get(MFRecipeKeys.TIER, 0));
                        },
                        entry -> {
                            CheckResult result = check();
                            return result.isSuccess() ? null : result.getReason();
                        }).getCandidates());
    }

    public static boolean isPot(ItemStack item) {
        return item != null && item.getItem() == ComponentListMF.clay_pot;
    }

    /** The same valid grind as check(), with an explanation when a pot or station tier prevents it. */
    public CheckResult inspectWork() {
        if (inv[0] == null) return CheckResult.failure(CheckResult.Reason.MISSING_INPUT);
        CheckResult result = check();
        if (result.isSuccess() || result.getReason() != CheckResult.Reason.NO_RECIPE) return result;
        RecipeEntry<ProcessRecipe> entry = MFRecipes.find(MFRecipes.QUERN, inv[0]);
        if (entry == null) return result;
        if (entry.getRecipe().get(MFRecipeKeys.CONSUME_POT, true) && inv[1] == null) {
            return CheckResult.failure(CheckResult.Reason.of("pot"));
        }
        CheckResult.Reason problem = Requirements.QUERN
                .stationProblem(getTier(), entry.getRecipe().get(MFRecipeKeys.TIER, 0));
        return problem == null ? result : CheckResult.failure(problem);
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
        sendState(false);
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
    public String getInventoryName() {
        return "gui.quern.name";
    }

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
    protected ItemStack[] slots() {
        return inv;
    }

    @Override
    protected NBTTagCompound describe() {
        NBTTagCompound state = new NBTTagCompound();
        state.setInteger("TurnAngle", turnAngle);
        state.setInteger("PostUseTicks", postUseTicks);
        return state;
    }

    @Override
    public void show(NBTTagCompound state) {
        turnAngle = Math.max(0, state.getInteger("TurnAngle"));
        postUseTicks = Math.max(0, state.getInteger("PostUseTicks"));
    }

    @Override
    public Role role(int slot) {
        return slot == 2 ? Role.OUTPUT : Role.INPUT;
    }
}
