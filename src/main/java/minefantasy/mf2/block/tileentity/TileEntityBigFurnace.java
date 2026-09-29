package minefantasy.mf2.block.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.Requirements;
import minefantasy.mf2.api.heating.ForgeItemHandler;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.SafeStacks;
import minefantasy.mf2.api.helpers.Sounds;
import minefantasy.mf2.api.helpers.Tiles;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftInventory;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.Diagnosis;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.refine.IBellowsUseable;
import minefantasy.mf2.api.refine.SmokeMechanics;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.refining.BlockBigFurnace;
import minefantasy.mf2.item.food.FoodListMF;

public class TileEntityBigFurnace extends TileEntityStation
        implements IBellowsUseable, ISidedInventory, Diagnosis.Source {

    /** Smelting without a furnace recipe, by vanilla's furnace list. */
    private static final RecipeId VANILLA = RecipeId.of("minefantasy2", "big_furnace/vanilla");

    // UNIVERSAL
    public int fuel;
    public int maxFuel;
    public ItemStack[] inv;
    public int itemMeta;
    public boolean built = false;
    // ANIMATE
    public int numUsers;
    public int doorAngle;
    // HEATER
    public float heat;
    public float maxHeat;
    public int justShared;
    // FURNACE
    public int progress;
    /**
     * CLIENT VAR This is used to determine if the block is able to emit smoke
     */
    public boolean isBurningClient;
    private Random rand = new Random();
    private int aboveType;
    private int ticksExisted;
    private int ticksSinceSync;
    private boolean wasBurning;

    public TileEntityBigFurnace() {
        super();
        inv = new ItemStack[8];
        fuel = maxFuel = progress = 0;
    }

    public TileEntityBigFurnace(int meta) {
        this();
        itemMeta = meta;
    }

    public TileEntityBigFurnace setBlockType(Block block) {
        blockType = block;
        return this;
    }

    @Override
    public void onUsedWithBellows(float powerLevel) {
        if (isHeater()) {
            if (justShared > 0) {
                return;
            }
            justShared = 5;

            if (fuel > 0) {
                int max = (int) (maxHeat * 1.5F);
                if (heat < max) {
                    heat += 50 * powerLevel;
                }

                for (int a = 0; a < 10; a++) {
                    worldObj.spawnParticle(
                            "flame",
                            xCoord + (rand.nextDouble() * 0.8D) + 0.1D,
                            yCoord + 0.4D,
                            zCoord + (rand.nextDouble() * 0.8D) + 0.1D,
                            0,
                            0.01,
                            0);
                }
            }
            pumpBellows(-1, 0, powerLevel * 0.9F);
            pumpBellows(0, -1, powerLevel * 0.9F);
            pumpBellows(0, 1, powerLevel * 0.9F);
            pumpBellows(1, 0, powerLevel * 0.9F);
        }
    }

    private void pumpBellows(int x, int z, float pump) {
        int share = 2;
        TileEntityBigFurnace furn = Tiles.get(worldObj, xCoord + x, yCoord, zCoord + z, TileEntityBigFurnace.class);
        if (furn != null && furn.isHeater()) furn.onUsedWithBellows(pump);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (!worldObj.isRemote) {
            if (numUsers > 0 && doorAngle < 20) {
                doorAngle++;
            }

            if (numUsers <= 0 && doorAngle > 0) {
                doorAngle--;
            }
            if (doorAngle < 0) doorAngle = 0;
            if (doorAngle > 20) doorAngle = 20;
        }

        ++ticksExisted;
        if (justShared > 0) justShared--;
        if (ticksExisted % 10 == 0) {
            built = structureExists();
        }
        if (isHeater()) {
            updateHeater();
        } else {
            updateFurnace();
        }
        // UNIVERSAL
        if (isBurning() != wasBurning || ticksExisted == 20) {
            worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
        }
        if (!worldObj.isRemote) {
            sendPacketToClients();
        }
        wasBurning = isBurning();
    }

    private void updateFurnace() {
        if (isBurning()) {
            if (worldObj.isRemote && rand.nextInt(10) == 0) {
                worldObj.spawnParticle(
                        "flame",
                        xCoord + (rand.nextDouble() * 0.8D) + 0.1D,
                        yCoord + 0.4D,
                        zCoord + (rand.nextDouble() * 0.8D) + 0.1D,
                        0,
                        0.01,
                        0);
            }
        }
        if (worldObj.isRemote) {
            return;
        }
        if (isBurning()) {
            puffSmoke(rand, worldObj, xCoord, yCoord, zCoord);
        }
        TileEntityBigFurnace heater = getHeater();

        if (heater != null) {
            heat = heater.heat;
        } else {
            heat -= 4;
            if (heat < 0) {
                heat = 0;
            }
        }
        boolean canSmelt = false;
        boolean smelted = false;

        if (getSpecialResult() != null) {
            if (!canFitSpecialResult()) {
                canSmelt = false;
            } else {
                canSmelt = true;

                if (progress >= getMaxTime()) {
                    smeltSpecial();
                    smelted = true;
                }
            }
        } else for (int a = 0; a < 4; a++) {
            CraftPlan plan = planFor(a, a + 4);
            if (plan != null) {
                canSmelt = true;

                if (progress >= getMaxTime()) {
                    List<ItemStack> spill = new ArrayList<>();
                    plan.apply(CraftInventory.of(this), spill);
                    for (ItemStack stack : spill) {
                        InventorySlots.drop(worldObj, xCoord, yCoord, zCoord, stack);
                    }
                    smelted = true;
                }
            }
        }

        if (canSmelt) {
            // Whole degrees only: heat is clamped to zero or above, so this drops the same fraction the compound
            // assignment used to drop silently, and says so.
            progress += (int) heat;
        }
        if (!canSmelt || smelted) {
            progress = 0;
        }
    }

    private boolean canFitSpecialResult() {
        ItemStack spec = getSpecialResult();

        if (spec != null) {
            int spaceNeeded = spec.stackSize;
            int spaceLeft = 0;

            for (int a = 4; a < 8; a++) {
                ItemStack item = inv[a];
                if (inv[a] == null) {
                    spaceLeft += 64;
                } else {
                    if (CustomToolHelper.areEqual(inv[a], spec)) {
                        if (inv[a].stackSize < inv[a].getMaxStackSize()) {
                            spaceLeft += inv[a].getMaxStackSize() - inv[a].stackSize;
                        }
                    }
                }
            }
            return spec.stackSize <= spaceLeft;
        }
        return false;
    }

    private void smeltSpecial() {
        ItemStack res = getSpecialResult().copy();

        for (int output = 4; output < 8; output++) {
            if (res.stackSize <= 0) break;

            if (inv[output] == null) {
                int placed = Math.min(res.stackSize, res.getMaxStackSize());
                setInventorySlotContents(output, new ItemStack(res.getItem(), placed, res.getItemDamage()));
                if (res.hasTagCompound()) {
                    inv[output].setTagCompound((NBTTagCompound) res.getTagCompound().copy());
                }
                res.stackSize -= placed;
            } else {
                if (CustomToolHelper.areEqual(inv[output], res)) {
                    int spaceLeft = inv[output].getMaxStackSize() - inv[output].stackSize;

                    if (res.stackSize <= spaceLeft) {
                        inv[output].stackSize += res.stackSize;
                        break;
                    } else {
                        inv[output].stackSize += spaceLeft;
                        res.stackSize -= spaceLeft;
                    }
                }
            }
        }
        for (int input = 0; input < 4; input++) decrStackSize(input, 1);
    }

    public void puffSmoke(Random rand, World world, int x, int y, int z) {
        if (rand.nextInt(5) != 0) {
            return;
        }
        ForgeDirection dir = getBack();
        SmokeMechanics.emitSmokeIndirect(world, x + dir.offsetX, y + (isHeater() ? 2 : 1), z + dir.offsetZ, 1);
    }

    private int getMaxTime() {
        return (int) 1.0E+5;
    }

    /** Each filled input slot's candidates, in lookup order, and why each does or does not smelt there. */
    @Override
    public Diagnosis diagnose(EntityPlayer player) {
        if (isHeater() || !built) {
            return Diagnosis.problem("big_furnace", CheckResult.Reason.of("not_built"));
        }
        List<Diagnosis.Candidate> candidates = new ArrayList<>();
        for (int slot = 0; slot < 4; slot++) {
            ItemStack in = inv[slot];
            if (in == null) {
                continue;
            }
            int output = slot + 4;
            CheckResult.Reason full = planFor(slot, output) == null ? CheckResult.Reason.OUTPUT_FULL : null;
            Diagnosis.Walk<ProcessRecipe> walk = Diagnosis
                    .walk(MFRecipes.BIG_FURNACE.published().candidates(Input.lookupKeys(in)), recipe -> true, entry -> {
                        CheckResult.Reason reason = entry.getRecipe().getInput().explain(in);
                        return reason != null ? reason
                                : Requirements.BIG_FURNACE
                                        .stationProblem(getTier(), entry.getRecipe().get(MFRecipeKeys.TIER, 0));
                    }, entry -> full);
            candidates.addAll(walk.getCandidates());
            if (walk.getChosen() == null && vanillaResult(in) != null) {
                candidates.add(new Diagnosis.Candidate(VANILLA, 0, full));
            }
        }
        return Diagnosis.of("big_furnace", candidates);
    }

    /**
     * The smelt of one input slot into its output slot: a furnace recipe with its own amount and usage, or else vanilla
     * smelting one item. Null when nothing smelts or the output has no room.
     */
    private CraftPlan planFor(int input, int output) {
        ItemStack in = inv[input];
        if (isHeater() || !built || in == null) {
            return null;
        }
        long generation = MFRecipes.BIG_FURNACE.published().getGeneration();
        CraftPlan plan;
        RecipeEntry<ProcessRecipe> entry = MFRecipes.find(
                MFRecipes.BIG_FURNACE,
                in,
                r -> Requirements.BIG_FURNACE.stationFits(getTier(), r.get(MFRecipeKeys.TIER, 0)));
        if (entry != null) {
            plan = CraftPlan.builder(entry.getId(), generation, output).use(input, entry.getRecipe().getInput(), in)
                    .output(entry.getRecipe().getOutput()).build();
        } else {
            ItemStack res = vanillaResult(in);
            if (res == null) {
                return null;
            }
            plan = CraftPlan.builder(VANILLA, generation, output).use(input, Input.of(in).amount(1), in).output(res)
                    .build();
        }
        // Returned containers without room are dropped, so only the product has to fit
        return plan.canApplySpilling(CraftInventory.of(this)) ? plan : null;
    }

    private TileEntityBigFurnace getHeater() {
        TileEntityBigFurnace tile = Tiles.get(worldObj, xCoord, yCoord - 1, zCoord, TileEntityBigFurnace.class);
        return tile != null && tile.isHeater() ? tile : null;
    }

    private TileEntityBigFurnace getFurnace() {
        TileEntityBigFurnace tile = Tiles.get(worldObj, xCoord, yCoord + 1, zCoord, TileEntityBigFurnace.class);
        return tile != null && !tile.isHeater() ? tile : null;
    }

    private void updateHeater() {
        if (worldObj.isRemote) return;

        TileEntityBigFurnace furn = getFurnace();
        if (furn != null) {
            aboveType = furn.getType();
        }
        if (!built) {
            heat = maxHeat = fuel = maxFuel = 0;
            return;
        }
        if (heat < maxHeat) {
            heat++;
        }
        if (heat > maxHeat) {
            heat--;
        }
        if (fuel > 0) {
            fuel--;
        } else {
            if (inv[0] != null && isItemFuel(inv[0])) {
                fuel = maxFuel = getItemBurnTime(inv[0]);
                maxHeat = getItemHeat(inv[0]);
                // One unit is burned, so only one container comes back. Replacing the whole stack with a single
                // container destroyed the rest of it.
                ItemStack cont = SafeStacks.containerOf(inv[0]);

                decrStackSize(0, 1);
                if (cont != null) {
                    if (inv[0] == null) {
                        inv[0] = cont;
                    } else {
                        InventorySlots.drop(worldObj, xCoord, yCoord, zCoord, cont);
                    }
                }
            }
            if (fuel <= 0) {
                if (heat > 0) heat--;
                maxHeat = 0;
            }
        }

    }

    private float getItemHeat(ItemStack itemStack) {
        return ForgeItemHandler.getForgeHeat(itemStack);
    }

    public ItemStack getResult(ItemStack item) {
        if (item == null) return null;

        // SPECIAL SMELTING
        RecipeEntry<ProcessRecipe> recipe = MFRecipes.find(
                MFRecipes.BIG_FURNACE,
                item,
                r -> Requirements.BIG_FURNACE.stationFits(getTier(), r.get(MFRecipeKeys.TIER, 0)));
        if (recipe != null) {
            return recipe.getRecipe().getOutput();
        }

        return vanillaResult(item);
    }

    /** Vanilla smelting, with food burning to a crisp in a furnace this hot. */
    private static ItemStack vanillaResult(ItemStack item) {
        ItemStack res = FurnaceRecipes.smelting().getSmeltingResult(item);
        if (res != null) {
            if (res.getItem() instanceof ItemFood || item.getItem() instanceof ItemFood) {
                return new ItemStack(FoodListMF.burnt_food, 1, 1);
            }
            return res;
        }

        return null;
    }

    public ItemStack getSpecialResult() {
        return null;
        /*
         * ItemStack[] input = new ItemStack[4]; for(int a = 0; a < 4; a ++) { input[a] = inv[a]; } Alloy alloy =
         * SpecialFurnaceRecipes.getResult(input); if(alloy != null) { if(alloy.getLevel() <= getSmeltLevel()) { return
         * SpecialFurnaceRecipes.getResult(input).getRecipeOutput(); } } return null;
         */
    }

    @SideOnly(Side.CLIENT)
    public int getBurnTimeRemainingScaled(int height) {
        if (this.maxFuel == 0) {
            this.maxFuel = 200;
        }

        return this.fuel * height / this.maxFuel;
    }

    @SideOnly(Side.CLIENT)
    public int getHeatScaled(int height) {
        if (heat <= 0) return 0;
        int size = (int) (height / TileEntityForge.maxTemperature * this.heat);

        return Math.min(size, height);
    }

    @SideOnly(Side.CLIENT)
    public int getItemHeatScaled(int height) {
        if (maxHeat <= 0) return 0;
        int size = (int) (height / TileEntityForge.maxTemperature * this.maxHeat);

        return Math.min(size, height);
    }

    public String getInvName() {
        int t = getType();
        String tier = "";

        if (isHeater()) {
            return StatCollector.translateToLocal("tile.furnace.name") + " "
                    + StatCollector.translateToLocal("block.furnace.heater");
        }

        return tier + " " + StatCollector.translateToLocal("tile.furnace.name");
    }

    public int getSmeltLevel() {
        if (isHeater()) {
            return -1;
        }
        return getType() - 1;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        inv = InventorySlots.read(nbt, "Items", inv.length);

        justShared = nbt.getInteger("Shared");
        built = nbt.getBoolean("Built");

        fuel = nbt.getInteger("fuel");
        maxFuel = nbt.hasKey("maxFuel") ? nbt.getInteger("maxFuel") : nbt.getInteger("MaxFuel");

        heat = nbt.getFloat("heat");
        maxHeat = nbt.getFloat("maxHeat");

        progress = nbt.getInteger("progress");
        aboveType = nbt.getInteger("Level");
        // Earlier versions let an unheated furnace accumulate negative heat and progress; clamp on load so a saved
        // debt does not have to be worked off before the first smelt
        if (heat < 0F) heat = 0F;
        if (progress < 0) progress = 0;
    }

    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setInteger("Shared", justShared);
        nbt.setInteger("Level", aboveType);
        nbt.setBoolean("Built", built);

        nbt.setInteger("fuel", fuel);
        nbt.setInteger("maxFuel", maxFuel);

        nbt.setFloat("heat", heat);
        nbt.setFloat("maxHeat", maxHeat);

        nbt.setInteger("progress", progress);

        InventorySlots.write(nbt, "Items", inv);
    }

    public boolean isBurning() {
        if (worldObj == null) {
            return false;
        }
        if (worldObj.isRemote) {
            return isBurningClient;
        }
        if (isHeater()) {
            return heat > 0;
        }
        return progress > 0 && heat > 0;
    }

    public boolean isHeater() {
        Block block = worldObj != null ? getBlockType() : blockType;

        if (block != null && block instanceof BlockBigFurnace) {
            return ((BlockBigFurnace) block).isHeater;
        }
        return false;
    }

    public int getTier() {
        Block block = worldObj != null ? getBlockType() : blockType;

        if (block != null && block instanceof BlockBigFurnace) {
            return ((BlockBigFurnace) block).tier;
        }
        return 0;
    }

    public int getItemBurnTime(ItemStack itemstack) {
        if (itemstack == null) {
            return 0;
        }
        return (int) Math.ceil(ForgeItemHandler.getForgeFuel(itemstack) / 8);
    }

    public void openChest() {
        if (numUsers == 0) {
            Sounds.at(this, "minefantasy2:block.furnace_open", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
        }
        ++numUsers;
    }

    public void closeChest() {
        --numUsers;
        if (numUsers == 0 && doorAngle >= 15) {
            Sounds.at(this, "minefantasy2:block.furnace_close", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
        }
    }

    private void sendPacketToClients() {
        sendState(false);
    }

    public int getBlockMetadata() {
        if (worldObj == null) return itemMeta * 2;

        if (this.blockMetadata == -1) {
            this.blockMetadata = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        }

        return this.blockMetadata;
    }

    /**
     * Heater:0||Stone :1;
     */
    public int getType() {
        return isHeater() ? 0 : 1;
    }

    private int getOnMetadata() {
        return getType() * 2 + 1;
    }

    private int getOffMetadata() {
        return getType() * 2;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (isHeater()) {
            return new int[] { 0 };
        }
        return new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack item, int side) {
        if (isHeater()) {
            return slot == 0 && isItemFuel(item);
        }
        return slot < 4 && item != null
                && (MFRecipes.accepts(MFRecipes.BIG_FURNACE, item) || vanillaResult(item) != null);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return canInsertItem(slot, item, 0);
    }

    public boolean isItemFuel(ItemStack item) {
        return this.getItemBurnTime(item) > 0;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack item, int side) {
        if (!isHeater()) {
            return slot >= 4;
        }
        // Anything left in the fuel slot that no longer burns is spent container, so let automation clear it
        return item != null && !isItemFuel(item);
    }

    public int getCookProgressScaled(int i) {
        return (progress * i) / getMaxTime();
    }

    public boolean isBlockValidForSide(int x, int y, int z) {
        if (worldObj == null) {
            return false;
        }

        Block block = worldObj.getBlock(x, y, z);

        if (block == null) {
            return false;
        }

        if (block == worldObj.getBlock(xCoord, yCoord, zCoord)) {
            return true;
        }
        return block == BlockListMF.firebricks;
    }

    /**
     * Checks a valid block for sides. It must be the required block.
     * <p>
     * HEATER: Requires any stone block for bronze, stone above hardness 2.0(like slate)for iron, and stone above
     * hardness above 5.0(like granite) for steel
     * <p>
     * FURNACES require metal blocks of their material
     */
    public boolean isBlockValidForTop(int x, int y, int z) {
        if (worldObj == null) {
            return false;
        }
        Block block = worldObj.getBlock(x, y, z);

        if (block == null) {
            return false;
        }
        return block == BlockListMF.firebricks;
    }

    public boolean isBlockValidForSide(ForgeDirection side) {
        if (worldObj == null) {
            return false;
        }

        int x = xCoord + side.offsetX;
        int y = yCoord + side.offsetY;
        int z = zCoord + side.offsetZ;

        return isBlockValidForSide(x, y, z);
    }

    public boolean isSolid(ForgeDirection side) {
        if (worldObj == null) {
            return false;
        }

        int x = xCoord + side.offsetX;
        int y = yCoord + side.offsetY;
        int z = zCoord + side.offsetZ;

        Material mat = worldObj.getBlock(x, y, z).getMaterial();

        return mat.isSolid();
    }

    /**
     * Determines if the furnace is built properly HEATER must have sides blocked by stone FURNACES must have walls
     * built to specifications
     */
    private boolean structureExists() {
        if (worldObj == null) {
            return false;
        }

        if (isSolid(getFront())) {
            return false;
        }

        if (!isHeater() && !isBlockValidForTop(xCoord, yCoord + 1, zCoord)) {
            return false;
        }

        if (!isBlockValidForSide(getLeft())) {
            return false;
        }
        if (!isBlockValidForSide(getRight())) {
            return false;
        }
        if (!isBlockValidForSide(getBack())) {
            return false;
        }

        return true;
    }

    /**
     * Gets the direction the face is at(Opposite to the placer)
     */
    public ForgeDirection getFront() {
        int direction = getDirection();
        if (direction == 0)// SOUTH PLACE
        {
            return ForgeDirection.NORTH;
        }
        if (direction == 1)// WEST PLACE
        {
            return ForgeDirection.EAST;
        }
        if (direction == 2)// NORTH PLACE
        {
            return ForgeDirection.SOUTH;
        }
        if (direction == 3)// EAST PLACE
        {
            return ForgeDirection.WEST;
        }
        return ForgeDirection.UNKNOWN;
    }

    /**
     * Gets the direction the back is facing (Same dir as placer)
     */
    public ForgeDirection getBack() {
        int direction = getDirection();
        if (direction == 0)// SOUTH PLACE
        {
            return ForgeDirection.SOUTH;
        }
        if (direction == 1)// WEST PLACE
        {
            return ForgeDirection.WEST;
        }
        if (direction == 2)// NORTH PLACE
        {
            return ForgeDirection.NORTH;
        }
        if (direction == 3)// EAST PLACE
        {
            return ForgeDirection.EAST;
        }
        return ForgeDirection.UNKNOWN;
    }

    /**
     * Gets the direction the left is facing
     */
    public ForgeDirection getLeft() {
        int direction = getDirection();
        if (direction == 0)// SOUTH PLACE
        {
            return ForgeDirection.WEST;
        }
        if (direction == 1)// WEST PLACE
        {
            return ForgeDirection.NORTH;
        }
        if (direction == 2)// NORTH PLACE
        {
            return ForgeDirection.EAST;
        }
        if (direction == 3)// EAST PLACE
        {
            return ForgeDirection.SOUTH;
        }
        return ForgeDirection.UNKNOWN;
    }

    /**
     * Gets the direction the right is facing
     */
    public ForgeDirection getRight() {
        int direction = getDirection();
        if (direction == 0)// SOUTH PLACE
        {
            return ForgeDirection.EAST;
        }
        if (direction == 1)// WEST PLACE
        {
            return ForgeDirection.SOUTH;
        }
        if (direction == 2)// NORTH PLACE
        {
            return ForgeDirection.WEST;
        }
        if (direction == 3)// EAST PLACE
        {
            return ForgeDirection.NORTH;
        }
        return ForgeDirection.UNKNOWN;
    }

    private int getDirection() {
        if (worldObj != null) {
            return worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        }
        return 0;
    }

    public String getTexture() {
        if (isHeater()) {
            return "furnace_heater";
        }
        return "furnace_rock";
    }

    @Override
    public String getInventoryName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected ItemStack[] slots() {
        return inv;
    }

    /**
     * Only the door and the fire show outside the window; fuel and progress change every tick while the furnace runs,
     * and the window gets them through its container.
     */
    @Override
    protected NBTTagCompound describe() {
        NBTTagCompound state = new NBTTagCompound();
        state.setInteger("DoorAngle", doorAngle);
        state.setBoolean("Burning", isBurning());
        return state;
    }

    @Override
    public void show(NBTTagCompound state) {
        doorAngle = state.getInteger("DoorAngle");
        isBurningClient = state.getBoolean("Burning");
    }
}
