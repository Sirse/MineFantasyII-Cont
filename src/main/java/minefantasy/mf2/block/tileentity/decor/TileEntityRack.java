package minefantasy.mf2.block.tileentity.decor;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import minefantasy.mf2.api.helpers.BlockPositionHelper;
import minefantasy.mf2.api.weapon.IRackItem;
import minefantasy.mf2.block.tileentity.InventorySlots;

public class TileEntityRack extends TileEntityWoodDecor implements IInventory {

    private ItemStack inv[];
    private int ticksExisted;

    public TileEntityRack() {
        super("rack_wood");
        inv = new ItemStack[4];
    }

    @Override
    public int getSizeInventory() {
        return inv.length;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return inv[i];
    }

    @Override
    public ItemStack decrStackSize(int i, int j) {
        ItemStack taken = InventorySlots.take(inv, i, j);
        if (taken != null) {
            onContentsChanged();
        } else {
            syncItems();
            updateInventory();
        }
        return taken;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        inv[i] = itemstack;
        updateInventory();
        onContentsChanged();
    }

    /**
     * Single exit point for content changes: marks the chunk so the new state is saved, then pushes it to clients.
     * Without markDirty a rack change can be lost when the chunk unloads without any other edit dirtying it.
     */
    public void onContentsChanged() {
        markDirty();
        syncItems();
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

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void updateEntity() {
        ++ticksExisted;
        if (ticksExisted == 10 || ticksExisted % 50 == 0) {
            syncItems();
        }
    }

    public void syncItems() {
        sendState(false);
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        return entityplayer.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64D;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int var1) {
        return null;
    }

    private int getEnchantment(int i) {
        ItemStack is = this.getStackInSlot(i);
        if (is == null) return 0;

        if (is.isItemEnchanted()) return 1;

        return 0;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        return false;
    }

    /*
     * North: WEST: 0=2 South: SOUTH: 1=1 West: EAST: 2=3 East: NORTH: 3=0
     */
    public int getSlotFor(float x, float y) {
        int direction = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        ForgeDirection FD = ForgeDirection.getOrientation(direction);
        float offset = 1F / 16F;

        float x1 = 0.0F + offset;
        float x2 = 1.0F - offset;
        float y1 = 0.0F;
        float y2 = 1.0F;
        if (FD == ForgeDirection.EAST || FD == ForgeDirection.WEST) {
            x1 = 0.0F;
            x2 = 1.0F;
            y1 = 0.0F + offset;
            y2 = 1.0F - offset;
        }
        int[] coord = BlockPositionHelper.getCoordsFor(x, y, x1, x2, y1, y2, 4, 4, direction);

        if (coord == null) {
            return -1;
        }
        return coord[0];

    }

    public boolean canHang(ItemStack item, int slot) {
        if (item == null || item.getItem() == null) {
            return false;
        }
        if (item.getItem() instanceof ItemBlock) {
            return false;
        }
        if (item.getItem() instanceof IRackItem) {
            return ((IRackItem) item.getItem()).canHang(this, item, slot);
        }
        if (item.getItem() instanceof ItemArmor) return false;
        // if(item.getItem() instanceof ItemCrossbow || item.getItem() instanceof
        // ItemBomb || item.getItem() instanceof ItemMine)return false;
        return item.getItem().isItemTool(item);
    }

    @Override
    public String getInventoryName() {
        return null;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {}

    public boolean hasRackAbove(int slot) {
        TileEntity side = worldObj.getTileEntity(xCoord, yCoord + 1, zCoord);
        return side != null && side instanceof TileEntityRack;// && ((TileEntityRack)side).getStackInSlot(slot) == null;
    }

    public boolean hasRackBelow(int slot) {
        TileEntity side = worldObj.getTileEntity(xCoord, yCoord - 1, zCoord);
        return side != null && side instanceof TileEntityRack;// && ((TileEntityRack)side).getStackInSlot(slot) == null;
    }

    public void updateInventory() {
        if (!worldObj.isRemote) {
            for (int x = 0; x < getSizeInventory(); x++) {
                ItemStack item = this.getStackInSlot(x);
                if (item != null && !canHang(item, x)) {
                    EntityItem drop = new EntityItem(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, item);
                    worldObj.spawnEntityInWorld(drop);
                    setInventorySlotContents(x, null);
                }
            }
        }
    }

    @Override
    protected NBTTagCompound describe() {
        NBTTagCompound state = super.describe();
        InventorySlots.write(state, "Items", inv);
        return state;
    }

    @Override
    public void show(NBTTagCompound state) {
        super.show(state);
        inv = InventorySlots.read(state, "Items", inv.length);
    }
}
