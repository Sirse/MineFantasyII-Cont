package minefantasy.mf2.block.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.IHeatSource;
import minefantasy.mf2.api.crafting.IHeatUser;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.CraftInventory;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.block.crafting.BlockRoast;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.network.NetworkUtils;
import minefantasy.mf2.network.packet.TileInventoryPacket;

public class TileEntityRoast extends TileEntity implements IInventory, IHeatUser {

    /**
     * Enable high temperatures ruin cooking
     */
    public static boolean enableOverheat = true;
    public ItemStack[] items = new ItemStack[1];
    public float progress;
    public float maxProgress;
    public String texname = "basic";
    private int tempTicksExisted = 0;
    private Random rand = new Random();
    private int ticksExisted;
    private CookRecipe recipe;
    /** The work the progress belongs to, kept across saves, and what watchers last got. */
    private final CraftState craft = new CraftState();
    private boolean isOvenTemp;

    public TileEntityRoast() {}

    @Override
    public void updateEntity() {
        super.updateEntity();
        ++tempTicksExisted;
        if (tempTicksExisted == 10) {
            blockMetadata = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
            restoreRecipe();
        }
        int temp = getTemp();
        ++ticksExisted;
        if (ticksExisted % 20 == 0 && !worldObj.isRemote) {
            if (temp > 0 && items[0] != null) {
                cook(temp);
            }
        }
        if (temp > 0 && worldObj.isRemote && rand.nextInt(20) == 0) {
            worldObj.spawnParticle(
                    "flame",
                    xCoord + 0.2F + (rand.nextFloat() * 0.6F),
                    yCoord + 0.2F,
                    zCoord + 0.2F + (rand.nextFloat() * 0.6F),
                    0F,
                    0F,
                    0F);
        }
    }

    private int getTemp() {
        TileEntity tile = worldObj.getTileEntity(xCoord, yCoord - 1, zCoord);
        if (tile != null && tile instanceof IHeatSource) {
            return ((IHeatSource) tile).getHeat();
        }
        return 0;
    }

    public TileEntityRoast setInventoryModel(String tex, boolean isOven) {
        this.isOvenTemp = isOven;
        this.texname = tex;
        return this;
    }

    public boolean isOven() {
        if (worldObj == null) return isOvenTemp;
        Block base = worldObj.getBlock(xCoord, yCoord, zCoord);
        if (base instanceof BlockRoast) {
            return ((BlockRoast) base).isOven();
        }
        return false;
    }

    public boolean interact(EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        ItemStack item = items[0];
        if (item == null) {
            CookRecipe.Found found = held == null || held.getItem() instanceof ItemBlock ? null
                    : CookRecipe.find(held, isOven());
            int amount = found == null ? 0 : found.recipe.getInput().getAmount();
            if (found != null && held.stackSize >= amount) {
                ItemStack item2 = held.copy();
                item2.stackSize = amount;
                setInventorySlotContents(0, item2);
                tryDecrMainItem(player, amount);
                updateRecipe();
                if (!isOven() && this.getTemp() > 0) {
                    worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "random.fizz", 1.0F, 1.0F);
                }
                return true;
            }
        } else {
            if (!player.inventory.addItemStackToInventory(item)) {
                player.entityDropItem(item, 0.0F);
            }
            setInventorySlotContents(0, null);
            updateRecipe();
            return true;
        }
        return false;
    }

    private void tryDecrMainItem(EntityPlayer player, int amount) {
        int held = player.inventory.currentItem;
        if (held >= 0 && held < 9) {
            player.inventory.decrStackSize(held, amount);
        }
    }

    /**
     * One step of cooking at the given heat. The recipe is looked up again first, before its temperatures are read: a
     * reload may have removed or changed it since the food went on, and then the cooking starts over on what the food
     * cooks by now instead of going on by the old recipe.
     */
    private void cook(int temp) {
        CookRecipe.Found found = CookRecipe.find(items[0], isOven());
        CraftPlan plan = plan(found, false);
        if (plan == null && !craft.isRunning()) {
            // Nothing cooks the food, as before: burnt through or its recipe gone. Nothing to restart or resend
            return;
        }
        if (!craft.holds(plan)) {
            updateRecipe();
            return;
        }
        CookRecipe current = found.recipe;
        if (temp <= current.getMinTemperature()) {
            return;
        }
        if (enableOverheat && current.canBurn() && temp > current.getMaxTemperature()) {
            finish(plan(found, true));
            return;
        }
        progress += (temp / 100F);
        if (progress >= maxProgress) {
            finish(plan(found, false));
        }
    }

    private void cacheRecipe() {
        CookRecipe.Found found = CookRecipe.find(getStackInSlot(0), isOven());
        recipe = found == null ? null : found.recipe;
        if (recipe != null) {
            maxProgress = recipe.getTime();
        }
    }

    /**
     * Cooking the food on the station, or burning it, as the found recipe asks: the result takes the food's place, and
     * the cooking terms are part of the plan, so a reload changing them restarts the work.
     */
    private CraftPlan plan(CookRecipe.Found found, boolean burnt) {
        if (found == null || items[0] == null) {
            return null;
        }
        CookRecipe cooking = found.recipe;
        return CraftPlan
                .builder(
                        burnt ? CookRecipe.burntId(found.id) : found.id,
                        MFRecipes.COOKING.published().getGeneration(),
                        0)
                .use(0, cooking.getInput(), items[0]).output(burnt ? cooking.getBurnt() : cooking.getOutput())
                .require(MFRecipeKeys.TIME, (float) cooking.getTime())
                .require(MFRecipeKeys.MIN_TEMPERATURE, cooking.getMinTemperature())
                .require(MFRecipeKeys.MAX_TEMPERATURE, cooking.getMaxTemperature())
                .require(MFRecipeKeys.CAN_BURN, cooking.canBurn()).build();
    }

    /** Pays for the cooking and leaves the result; returned containers without room drop off. */
    private void finish(CraftPlan plan) {
        if (plan != null) {
            List<ItemStack> spill = new ArrayList<>();
            plan.apply(CraftInventory.of(this, 64), spill);
            GridProject.drop(this, spill);
        }
        updateRecipe();
    }

    /**
     * The cooked item changed, so the recipe and the progress both start over.
     */
    public void updateRecipe() {
        cacheRecipe();
        if (!craft.follow(plan(CookRecipe.find(items[0], isOven()), false))) {
            progress = 0;
        }
        sendPacketToClients();
    }

    /**
     * Rebuilds the recipe cache without touching progress. Used after loading, where the saved progress has to survive:
     * the item is the same one that was cooking before the chunk unloaded.
     */
    public void restoreRecipe() {
        cacheRecipe();
        sendPacketToClients();
    }

    public int getProgressBar(int i) {
        if (maxProgress <= 0) return 0;
        return (int) (i / maxProgress * progress);
    }

    public String getResultName() {
        if (recipe != null) {
            return recipe.getOutput().getDisplayName();
        }
        return "";
    }

    /**
     * The saved state, sent when a player starts watching the spit. The inventory packet only goes out on changes, so a
     * returning player otherwise saw an empty spit until the food next changed.
     */
    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        // readFromNBT only fills the slots it finds, so clear first or taken food would linger
        items = new ItemStack[items.length];
        readFromNBT(packet.func_148857_g());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        progress = nbt.getFloat("Progress");
        craft.read(nbt);
        maxProgress = nbt.getFloat("maxProgress");

        items = InventorySlots.read(nbt, "Items", items.length);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setFloat("Progress", progress);
        craft.write(nbt);
        nbt.setFloat("maxProgress", maxProgress);

        InventorySlots.write(nbt, "Items", items);
    }

    // INVENTORY
    public void onInventoryChanged() {}

    @Override
    public int getSizeInventory() {
        return items.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return items[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int num) {
        onInventoryChanged();
        return InventorySlots.take(items, slot, num);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return InventorySlots.takeAll(items, slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack item) {
        onInventoryChanged();
        items[slot] = item;
    }

    @Override
    public String getInventoryName() {
        return "tile.roast.name";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
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
        return false;
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

    @Override
    public boolean canAccept(TileEntity tile) {
        return true;
    }

    /** Sends the station to the watchers, if it changed since they got it last. */
    private void sendPacketToClients() {
        if (worldObj.isRemote) return;
        NBTTagCompound state = new NBTTagCompound();
        writeToNBT(state);
        if (!craft.changed(state)) return;

        NetworkUtils.sendToWatchers(
                new TileInventoryPacket(this, this).generatePacket(),
                worldObj,
                this.xCoord,
                this.zCoord);

        /*
         * List<EntityPlayer> players = ((WorldServer) worldObj).playerEntities; for (int i = 0; i < players.size();
         * i++) { EntityPlayer player = players.get(i); ((WorldServer)
         * worldObj).getEntityTracker().func_151248_b(player, new TileInventoryPacket(this, this).generatePacket()); }
         */
    }

    @SideOnly(Side.CLIENT)
    public String getTexName() {
        if (worldObj == null) return texname;

        Block base = worldObj.getBlock(xCoord, yCoord, zCoord);
        if (base instanceof BlockRoast) {
            return ((BlockRoast) base).tex;
        }
        return "basic";
    }

    @Override
    public Block getBlockType() {
        if (worldObj == null) return BlockListMF.oven_stone;

        return super.getBlockType();
    }

    @Override
    public int getBlockMetadata() {
        if (worldObj == null) return 0;
        return super.getBlockMetadata();
    }
}
