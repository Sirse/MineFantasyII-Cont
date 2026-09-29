package minefantasy.mf2.block.tileentity;

import java.util.Random;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftInventory;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.Diagnosis;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeLookup;
import minefantasy.mf2.api.recipe.RunningCraft;
import minefantasy.mf2.api.refine.SmokeMechanics;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.item.heatable.ItemHeated;

public class TileEntityBloomery extends TileEntityStation implements Diagnosis.Source {

    public float progress, progressMax;
    /**
     * Client-side render flag, fed by the described state. On the server the truth is the bloom slot, so always read it
     * through {@link #hasBloom()} rather than touching this directly.
     */
    public boolean hasBloom;
    public boolean isActive;
    private ItemStack[] inv = new ItemStack[3];
    private Random rand = new Random();

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_CARBON = 1;
    private static final int SLOT_BLOOM = 2;

    private final RecipeLookup<BloomRecipe> lookup = new RecipeLookup<>(MFRecipes.BLOOMERY);
    /** The smelt started by {@link #light}; it finishes only if the plan is still the same. */
    private final RunningCraft project = new RunningCraft();

    public static boolean isInput(ItemStack input) {
        if (input == null) {
            return false;
        }
        for (RecipeEntry<BloomRecipe> entry : MFRecipes.BLOOMERY.published().candidates(Input.lookupKeys(input))) {
            if (entry.getRecipe().getInput().matches(input)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks the current contents: the whole input stack smelts into a bloom of as many items, burning exactly the
     * carbon it needs. Research is checked only for a player lighting it ({@code user} may be null).
     */
    public CheckResult check(EntityPlayer user) {
        CheckResult.Reason problem = stationProblem();
        if (problem != null) {
            return CheckResult.failure(problem);
        }
        ItemStack input = inv[SLOT_INPUT];
        return lookup.find(RecipeLookup.keysOf(input), entry -> checkEntry(entry, user));
    }

    /** What stops the bloomery before any recipe is looked at, or null. */
    private CheckResult.Reason stationProblem() {
        if (hasBloom()) {
            return CheckResult.Reason.of("bloom_present");
        }
        if (inv[SLOT_INPUT] == null || inv[SLOT_CARBON] == null) {
            return CheckResult.Reason.MISSING_INPUT;
        }
        if (MineFantasyFuels.getCarbon(inv[SLOT_CARBON]) <= 0) {
            return CheckResult.Reason.of("carbon", 0, 1);
        }
        return null;
    }

    private CheckResult checkEntry(RecipeEntry<BloomRecipe> entry, EntityPlayer user) {
        ItemStack input = inv[SLOT_INPUT];
        ItemStack carbon = inv[SLOT_CARBON];
        int carbonNeeded = (int) Math.ceil((float) input.stackSize / (float) MineFantasyFuels.getCarbon(carbon));
        BloomRecipe recipe = entry.getRecipe();
        ItemStack one = input.copy();
        one.stackSize = 1;
        CheckResult.Reason mismatch = recipe.getInput().explain(one);
        if (mismatch != null) {
            return CheckResult.failure(mismatch);
        }
        if (carbon.stackSize != carbonNeeded) {
            return CheckResult.failure(CheckResult.Reason.of("carbon", carbon.stackSize, carbonNeeded));
        }
        if (user != null && recipe.getResearch() != null
                && !ResearchLogic.hasInfoUnlocked(user, recipe.getResearch())) {
            return CheckResult.failure(CheckResult.Reason.of("research", recipe.getResearch()));
        }
        ItemStack bloom = recipe.getOutput();
        bloom.stackSize = input.stackSize;
        return CheckResult.success(
                CraftPlan.builder(entry.getId(), MFRecipes.BLOOMERY.published().getGeneration(), SLOT_BLOOM)
                        .use(SLOT_INPUT, recipe.getInput().amount(input.stackSize), input)
                        .use(SLOT_CARBON, Input.of(carbon).amount(carbonNeeded), carbon).output(bloom).build());
    }

    @Override
    public Diagnosis diagnose(EntityPlayer player) {
        CheckResult.Reason problem = stationProblem();
        if (problem != null) {
            return Diagnosis.problem("bloomery", problem);
        }
        return Diagnosis.of(
                "bloomery",
                Diagnosis.walk(
                        MFRecipes.BLOOMERY.published().candidates(Input.lookupKeys(inv[SLOT_INPUT])),
                        recipe -> true,
                        entry -> checkEntry(entry, player).getReason(),
                        entry -> null).getCandidates());
    }

    @Override
    public void updateEntity() {
        if (isActive && progressMax > 0) {
            if (!worldObj.canBlockSeeTheSky(xCoord, yCoord + 1, zCoord)) {
                progressMax = progress = 0;
                isActive = false;
                return;
            }
            if (!worldObj.isRemote) {
                if (!projectStillValid()) {
                    return;
                }
                ++progress;
                if (progress >= progressMax) {
                    smeltItem();
                }
                if (rand.nextInt(4) == 0) {
                    SmokeMechanics.spawnSmoke(worldObj, xCoord, yCoord, zCoord, 1);
                }
            }
        }
        if (!worldObj.isRemote) {
            sendState(false);
        }
    }

    public void syncData() {
        sendState(false);
    }

    /**
     * Light the bloomery, starting the process.
     *
     * @return true if it can smelt
     */
    public boolean light(EntityPlayer user) {
        if (isActive || !worldObj.canBlockSeeTheSky(xCoord, yCoord + 1, zCoord)) {
            return false;
        }
        CheckResult result = check(worldObj.isRemote ? null : user);
        if (!result.isSuccess()) {
            return false;
        }
        if (!worldObj.isRemote) {
            isActive = true;
            progress = 0;
            progressMax = inv[SLOT_INPUT].stackSize * BloomRecipe.TICKS_PER_ITEM;
            project.start(result.getPlan());
            worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "fire.ignite", 1.0F, 1.0F);
        }
        return true;
    }

    /**
     * The running smelt stays valid while its plan is unchanged: same inputs, same recipe, same bloom. Anything else (a
     * player took items out, a script replaced the recipe) puts the fire out; the items stay where they are.
     */
    private boolean projectStillValid() {
        if (project.holds(check(null))) {
            return true;
        }
        extinguish();
        return false;
    }

    private void extinguish() {
        isActive = false;
        progress = progressMax = 0;
        project.clear();
    }

    /**
     * Consumes all input and carbon and sets the bloom, if the plan still holds.
     */
    public void smeltItem() {
        CheckResult result = check(null);
        if (project.holds(result)) {
            result.getPlan().apply(CraftInventory.of(this));
        }
        extinguish();
    }

    public boolean tryHammer(EntityPlayer user) {
        if (worldObj.getBlock(xCoord, yCoord + 1, zCoord).getMaterial().isSolid()) {
            return false;
        }
        ItemStack held = user.getHeldItem();
        if (!hasBloom() || isActive) {
            return false;
        }
        String toolType = ToolHelper.getCrafterTool(held);
        float pwr = ToolHelper.getCrafterEfficiency(held);
        if (toolType.equalsIgnoreCase("hammer") || toolType.equalsIgnoreCase("hvyHammer")) {
            if (user.worldObj.isRemote) return true;

            held.damageItem(1, user);
            if (held.getItemDamage() >= held.getMaxDamage()) {
                user.destroyCurrentEquippedItem();
                user.setCurrentItemOrArmor(0, null);
            }

            if (rand.nextFloat() * 10F < pwr) {
                ItemStack drop = inv[2].copy();
                --inv[2].stackSize;
                if (inv[2].stackSize <= 0) {
                    inv[2] = null;
                }
                if (RPGElements.isSystemActive && RPGElements.getLevel(user, SkillList.artisanry) <= 20)// Only gain xp
                // up to level
                // 20
                {
                    SkillList.artisanry.addXP(user, 1);
                }
                drop.stackSize = 1;
                drop = ItemHeated.createHotItem(drop, 1200);
                entityDropItem(worldObj, xCoord, yCoord, zCoord, drop);
                syncData();
            }
            worldObj.playSoundEffect(
                    xCoord + 0.5D,
                    yCoord + 0.5D,
                    zCoord + 0.5D,
                    "minefantasy2:block.anvilsucceed",
                    0.25F,
                    1.0F);

            return true;
        }
        return false;
    }

    public EntityItem entityDropItem(World world, int x, int y, int z, ItemStack item) {
        if (item.stackSize != 0 && item.getItem() != null) {
            EntityItem entityitem = new EntityItem(world, x + 0.5D, y + 1.25F, z + 0.5D, item);
            entityitem.delayBeforeCanPickup = 10;
            world.spawnEntityInWorld(entityitem);
            entityitem.motionX = entityitem.motionY = entityitem.motionZ = 0;
            return entityitem;
        } else {
            return null;
        }
    }

    public boolean hasBloom() {
        // No world when a tile is written on its own, as mods that copy blocks do: the inventory is the truth then too
        if (worldObj != null && worldObj.isRemote) {
            return hasBloom;
        }
        return inv[2] != null;
    }

    @Override
    public String getInventoryName() {
        return "gui.bloomery.name";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        if (item != null && TileEntityBlastFC.isCarbon(item)) {
            return slot == 1;
        }
        if (isInput(item)) {
            return slot == SLOT_INPUT;
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        inv = InventorySlots.read(nbt, "Items", inv.length);
        progress = nbt.getFloat("Progress");
        progressMax = nbt.getFloat("ProgressMax");
        hasBloom = nbt.getBoolean("hasBloom");
        isActive = nbt.getBoolean("isActive");
        project.read(nbt);
        if (isActive && !project.isRunning()) {
            // A 2.x smelt: nothing proves which recipe it ran, so it restarts. The items stay in their slots.
            extinguish();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        InventorySlots.write(nbt, "Items", inv);

        nbt.setFloat("Progress", progress);
        nbt.setFloat("ProgressMax", progressMax);
        nbt.setBoolean("hasBloom", hasBloom());
        nbt.setBoolean("isActive", isActive);
        project.write(nbt);
    }

    @SideOnly(Side.CLIENT)
    public String getTextureName() {
        return "bloomery_basic";
    }

    @Override
    protected ItemStack[] slots() {
        return inv;
    }

    @Override
    protected NBTTagCompound describe() {
        NBTTagCompound state = new NBTTagCompound();
        state.setBoolean("hasBloom", hasBloom());
        state.setBoolean("isActive", isActive);
        return state;
    }

    @Override
    public void show(NBTTagCompound state) {
        hasBloom = state.getBoolean("hasBloom");
        isActive = state.getBoolean("isActive");
    }

    @Override
    public Role role(int slot) {
        return slot == 2 ? Role.OUTPUT : Role.INPUT;
    }
}
