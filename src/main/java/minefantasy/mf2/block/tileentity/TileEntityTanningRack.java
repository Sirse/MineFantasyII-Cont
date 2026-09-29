package minefantasy.mf2.block.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.Requirements;
import minefantasy.mf2.api.helpers.Sounds;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftInventory;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.Diagnosis;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.block.crafting.BlockEngineerTanner;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.item.list.ComponentListMF;

public class TileEntityTanningRack extends TileEntityStation implements Diagnosis.Source {

    public ItemStack[] items = new ItemStack[2];
    public float progress;
    public float maxProgress;
    public String tex = "";
    /** The rack's own tier; it does not limit what the rack tans. */
    public int tier = 0;
    public String toolType = "knife";
    /** The tool tier the hide on the rack needs. */
    public int toolTier = -1;
    /** The work the progress belongs to, kept across saves, and what watchers last got. */
    private final CraftState craft = new CraftState();
    public float prevAcTime;
    public float acTime;
    private int tempTicksExisted = 0;
    private Random rand = new Random();

    public TileEntityTanningRack() {
        this(0, "Basic");
    }

    public TileEntityTanningRack(int tier, String tex) {
        this.tier = tier;
        this.tex = tex;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        prevAcTime = acTime;
        ++tempTicksExisted;
        if (tempTicksExisted == 10) {
            blockMetadata = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        }
        if (isAutomated()) {
            if (acTime > 0) {
                acTime -= (1F / 20);
            }
        }
    }

    /**
     * Both sides run the interaction so the clicking player sees it at once, but only the server's result is real: it
     * is pushed to every watcher afterwards, which also corrects a client that guessed wrong.
     */
    public boolean interact(EntityPlayer player, boolean leftClick, boolean leverPull) {
        boolean handled = applyInteraction(player, leftClick, leverPull);
        sendState(true);
        return handled;
    }

    private boolean applyInteraction(EntityPlayer player, boolean leftClick, boolean leverPull) {
        if (leverPull && acTime > 0) {
            return true;
        }

        ItemStack held = player.getHeldItem();

        // Interaction
        if (items[1] != null && (leverPull || ToolHelper.getCrafterTool(held).equalsIgnoreCase(toolType))) {
            if (leverPull || requirements().check(Requirements.TANNING, player, 0).allows()) {
                if (!leverPull) {
                    held.damageItem(1, player);
                    if (held.getItemDamage() >= held.getMaxDamage()) {
                        player.destroyCurrentEquippedItem();
                    }
                } else {
                    Sounds.at(this, "tile.piston.out", 0.75F, 0.85F);
                    acTime = 1.0F;
                    syncAnimation();
                }

                float efficiency = leverPull ? 100F : ToolHelper.getCrafterEfficiency(held);
                if (!leverPull && player.swingProgress > 0 && player.swingProgress <= 1.0) {
                    efficiency *= (0.5F - player.swingProgress);
                }

                if (efficiency > 0) {
                    progress += efficiency;
                }
                if (toolType.equalsIgnoreCase("shears")) {
                    Sounds.at(this, "mob.sheep.shear", 1.0F, 1.0F);
                } else {
                    Sounds.at(this, "dig.cloth", 1.0F, 1.0F);
                }
                CraftPlan plan = progress >= maxProgress ? currentPlan() : null;
                if (plan != null && finish(plan)) {
                    if (RPGElements.isSystemActive) {
                        SkillList.artisanry.addXP(player, 1);
                    }
                    if (isShabbyRack() && rand.nextInt(10) == 0 && !worldObj.isRemote) {
                        for (int a = 0; a < rand.nextInt(10); a++) {
                            ItemStack plank = ComponentListMF.plank.construct("ScrapWood");
                            Sounds.at(this, "mob.zombie.woodbreak", 1.0F, 1.5F);
                            InventorySlots.drop(worldObj, xCoord, yCoord, zCoord, plank);
                        }
                        worldObj.setBlockToAir(xCoord, yCoord, zCoord);
                        return true;
                    }
                }
            }
            return true;
        }
        if (!leftClick && (ToolHelper.getCrafterTool(held).equalsIgnoreCase("nothing")
                || ToolHelper.getCrafterTool(held).equalsIgnoreCase("hands"))) {
            // Item placement
            ItemStack item = items[0];
            if (item == null) {
                RecipeEntry<ProcessRecipe> entry = held == null || held.getItem() instanceof ItemBlock ? null
                        : MFRecipes.find(MFRecipes.TANNING, held);
                if (entry != null) {
                    int amount = entry.getRecipe().getInput().getAmount();
                    ItemStack item2 = held.copy();
                    item2.stackSize = amount;
                    setInventorySlotContents(0, item2);
                    tryDecrMainItem(player, amount);
                    updateRecipe();
                    Sounds.at(this, "mob.horse.leather", 1.0F, 1.0F);
                    return true;
                }
            } else {
                if (!player.inventory.addItemStackToInventory(item)) {
                    player.entityDropItem(item, 0.0F);
                }
                setInventorySlotContents(0, null);
                updateRecipe();
                Sounds.at(this, "mob.horse.leather", 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    /** The lever was pulled: the swing goes out once, then plays on its own on every side. */
    private void syncAnimation() {
        NBTTagCompound moment = new NBTTagCompound();
        moment.setFloat("Swing", acTime);
        sendMoment(moment);
    }

    public boolean isAutomated() {
        if (worldObj == null) {
            return tex.equalsIgnoreCase("metal");
        }
        return worldObj.getBlock(xCoord, yCoord, zCoord) instanceof BlockEngineerTanner;
    }

    private void tryDecrMainItem(EntityPlayer player, int amount) {
        int held = player.inventory.currentItem;
        if (held >= 0 && held < 9) {
            player.inventory.decrStackSize(held, amount);
        }
    }

    /** The candidates for the item on the rack, in lookup order, checked against the tool the player holds. */
    @Override
    public Diagnosis diagnose(EntityPlayer player) {
        if (items[0] == null) {
            return Diagnosis.problem("tanning", CheckResult.Reason.MISSING_INPUT);
        }
        return Diagnosis.of(
                "tanning",
                Diagnosis.walk(
                        MFRecipes.TANNING.published().candidates(Input.lookupKeys(items[0])),
                        recipe -> true,
                        entry -> entry.getRecipe().getInput().explain(items[0]),
                        entry -> isAutomated() ? null
                                : requirements(entry.getRecipe()).check(Requirements.TANNING, player, 0).getRefusal())
                        .getCandidates());
    }

    /** What the hide on the rack asks of the tool; a lever pull or a machine works it with none. */
    private Requirements requirements() {
        return new Requirements(toolType, toolTier, 0, "");
    }

    private static Requirements requirements(ProcessRecipe recipe) {
        return new Requirements(recipe.get(MFRecipeKeys.TOOL, "knife"), recipe.get(MFRecipeKeys.TOOL_TIER, -1), 0, "");
    }

    public void updateRecipe() {
        RecipeEntry<ProcessRecipe> entry = MFRecipes.find(MFRecipes.TANNING, items[0]);
        if (entry == null) {
            setInventorySlotContents(1, null);
            progress = maxProgress = 0;
            toolTier = -1;
        } else {
            ProcessRecipe recipe = entry.getRecipe();
            setInventorySlotContents(1, recipe.getOutput());
            toolTier = recipe.get(MFRecipeKeys.TOOL_TIER, -1);
            maxProgress = recipe.get(MFRecipeKeys.TIME, 0F);
            toolType = recipe.get(MFRecipeKeys.TOOL, "knife");
        }
        if (!craft.follow(plan(entry))) {
            progress = 0;
        }
    }

    /** The work on the rack as the recipe asks for it now: what it takes from the rack and what it leaves there. */
    private CraftPlan plan(RecipeEntry<ProcessRecipe> entry) {
        if (entry == null || items[0] == null) {
            return null;
        }
        ProcessRecipe recipe = entry.getRecipe();
        return CraftPlan.builder(entry.getId(), MFRecipes.TANNING.published().getGeneration(), 0)
                .use(0, recipe.getInput(), items[0]).output(recipe.getOutput())
                .require(MFRecipeKeys.TIME, recipe.get(MFRecipeKeys.TIME, 0F))
                .require(MFRecipeKeys.TOOL, recipe.get(MFRecipeKeys.TOOL, "knife"))
                .require(MFRecipeKeys.TOOL_TIER, recipe.get(MFRecipeKeys.TOOL_TIER, -1)).build();
    }

    /**
     * The work as the recipe asks for it now, if it is still the work the progress belongs to. A reload may have
     * replaced or removed the recipe since the item went on the rack; then the work restarts instead of paying out the
     * old result.
     */
    private CraftPlan currentPlan() {
        CraftPlan plan = plan(MFRecipes.find(MFRecipes.TANNING, items[0]));
        if (craft.holds(plan)) {
            return plan;
        }
        updateRecipe();
        return null;
    }

    /** Pays for the work and leaves the product on the rack; returned containers without room drop off it. */
    private boolean finish(CraftPlan plan) {
        List<ItemStack> spill = new ArrayList<>();
        boolean paid = plan.apply(CraftInventory.of(this, 64), spill);
        GridProject.drop(this, spill);
        updateRecipe();
        return paid;
    }

    public boolean doesPlayerKnowCraft(EntityPlayer thePlayer) {
        return true;
    }

    public int getProgressBar(int i) {
        if (maxProgress <= 0) return 0;
        return (int) (i / maxProgress * progress);
    }

    public String getResultName() {
        if (items[1] != null) {
            return items[1].getDisplayName();
        }
        return "";
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        acTime = nbt.getFloat("acTime");
        prevAcTime = acTime;
        tex = nbt.getString("tex");
        tier = nbt.getInteger("tier");
        progress = nbt.getFloat("Progress");
        craft.read(nbt);
        maxProgress = nbt.getFloat("maxProgress");
        toolType = nbt.getString("toolType");
        toolTier = nbt.getInteger("ToolTier");

        items = InventorySlots.read(nbt, "Items", items.length);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setFloat("acTime", acTime);
        nbt.setString("tex", tex);
        nbt.setInteger("tier", tier);
        nbt.setFloat("Progress", progress);
        craft.write(nbt);
        nbt.setFloat("maxProgress", maxProgress);
        nbt.setString("toolType", toolType);
        nbt.setInteger("ToolTier", toolTier);

        InventorySlots.write(nbt, "Items", items);
    }

    // INVENTORY
    public void onInventoryChanged() {
        sendState(false);
    }

    @Override
    public String getInventoryName() {
        return "tile.tanner.name";
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return false;
    }

    private boolean isShabbyRack() {
        return worldObj.getBlock(xCoord, yCoord, zCoord) == BlockListMF.tanner;
    }

    @Override
    protected ItemStack[] slots() {
        return items;
    }

    @Override
    protected NBTTagCompound describe() {
        NBTTagCompound state = new NBTTagCompound();
        writeToNBT(state);
        // The swing runs down on its own every tick; carried, it would make every tick a new state
        state.removeTag("acTime");
        return state;
    }

    /** readFromNBT only fills the slots it finds, so they are cleared first, or a removed hide would linger. */
    @Override
    public void show(NBTTagCompound state) {
        if (state.hasKey("Swing")) {
            acTime = prevAcTime = state.getFloat("Swing");
            return;
        }
        float swing = acTime;
        items = new ItemStack[items.length];
        readFromNBT(state);
        acTime = prevAcTime = swing;
    }

    @Override
    public Role role(int slot) {
        // Slot 1 only shows what the hide becomes; it is no item to take
        return slot == 0 ? Role.WORK : Role.NONE;
    }
}
