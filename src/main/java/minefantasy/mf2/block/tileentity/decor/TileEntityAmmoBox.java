package minefantasy.mf2.block.tileentity.decor;

import minefantasy.mf2.api.archery.AmmoMechanicsMF;
import minefantasy.mf2.api.archery.IAmmo;
import minefantasy.mf2.api.archery.IFirearm;
import minefantasy.mf2.api.crafting.IBasicMetre;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.tool.IStorageBlock;
import minefantasy.mf2.block.decor.BlockAmmoBox;
import minefantasy.mf2.item.ItemBandage;
import minefantasy.mf2.item.gadget.ItemSyringe;
import minefantasy.mf2.network.NetworkUtils;
import minefantasy.mf2.network.packet.AmmoBoxPacket;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;

public class TileEntityAmmoBox extends TileEntityWoodDecor implements IBasicMetre {
    public int angle, stock;
    public ItemStack ammo;
    private byte storageSize = -1;
    private int ticksExisted;

    public TileEntityAmmoBox() {
        super("ammo_box_basic", CustomMaterial.getMaterial("RefinedWood"));
    }

    public TileEntityAmmoBox(String tex, CustomMaterial material, byte size) {
        super(tex, material);
        this.storageSize = size;
    }

    public boolean interact(EntityPlayer user) {
        ItemStack held = user.getHeldItem();

        if (held != null) {
            if (this.getStorageType() == 1 && ammo != null && held.getItem() instanceof IFirearm && loadGun(held)) {
                open();
                syncData();
                return true;
            }
            if (canAcceptItem(held)) {
                int max = this.getMaxAmmo(ammo != null ? ammo : held);
                if (ammo == null) {
                    open();
                    placeInEmpty(user, max);
                } else if (areItemStacksEqual(held, ammo) && stock < max) {
                    open();
                    addToBox(user, max);
                }

                syncData();
                return true;
            }
            return false;
        } else if (ammo != null) {
            open();
            takeStack(user);
            syncData();
            return true;
        }
        syncData();
        return false;
    }

    public boolean canAcceptItem(ItemStack held) {
        if (held == null || held.getItem() instanceof IStorageBlock) {
            return false;
        }
        byte type = this.getStorageType();
        return type == 0 ? isFood(held) : type == 1 ? held.getItem() instanceof IAmmo : type == 2;
    }

    private boolean isFood(ItemStack held) {
        return held.getItem() instanceof ItemFood || held.getItem() instanceof ItemBandage
                || held.getItem() instanceof ItemSyringe;
    }

    private boolean loadGun(ItemStack held) {
        IFirearm gun = (IFirearm) held.getItem();
        if (gun.canAcceptAmmo(held, getAmmoClass())) {
            ItemStack loaded = AmmoMechanicsMF.getAmmo(held);
            if (loaded == null) {
                int ss = Math.min(ammo.getMaxStackSize(), stock);
                ItemStack newloaded = ammo.copy();
                newloaded.stackSize = ss;
                AmmoMechanicsMF.setAmmo(held, newloaded);
                stock -= ss;
                if (stock <= 0) {
                    ammo = null;
                }
                return true;
            } else if (areItemStacksEqual(loaded, ammo)) {
                int room_left = loaded.getMaxStackSize() - loaded.stackSize;
                if (stock > room_left) {
                    stock -= room_left;
                    loaded.stackSize += room_left;
                    AmmoMechanicsMF.setAmmo(held, loaded);
                } else {
                    loaded.stackSize += stock;
                    AmmoMechanicsMF.setAmmo(held, loaded);
                    stock = 0;
                    ammo = null;
                }

                return true;
            }
        }
        return false;
    }

    private void addToBox(EntityPlayer user, int max) {
        ItemStack held = user.getHeldItem();
        if (held == null) return;

        int room_left = max - stock;
        if (room_left <= 0) return;

        int toAdd = Math.min(held.stackSize, room_left);
        if (toAdd <= 0) return;

        stock += toAdd;
        if (!user.capabilities.isCreativeMode) {
            if (held.stackSize <= room_left) {
                user.setCurrentItemOrArmor(0, null);
            } else {
                held.stackSize -= toAdd;
            }
        }
    }

    private void takeStack(EntityPlayer user) {
        if (ammo == null || stock <= 0) return;

        int ss = Math.min(stock, ammo.getMaxStackSize());
        if (ss <= 0) return;

        ItemStack taken = ammo.copy();
        taken.stackSize = ss;
        stock -= ss;
        user.setCurrentItemOrArmor(0, taken);

        if (stock <= 0) {
            stock = 0;
            ammo = null;
        }
    }

    /**
     * Place ammo in empty box
     */
    private void placeInEmpty(EntityPlayer user, int max) {
        ItemStack held = user.getHeldItem();
        if (held == null) {
            return;
        }

        ammo = held.copy();
        int amount_to_place = Math.min(held.stackSize, max);
        stock = amount_to_place;

        if (!user.capabilities.isCreativeMode) {
            held.stackSize -= amount_to_place;
            if (held.stackSize <= 0) {
                user.setCurrentItemOrArmor(0, null);
            }
        }
    }

    private void open() {
        angle = 16;
        this.worldObj.playSoundEffect(this.xCoord + 0.5D, this.yCoord + 0.25D, this.zCoord + 0.5D, "random.chestclosed",
                0.5F, this.worldObj.rand.nextFloat() * 0.1F + 1.4F);
    }

    @Override
    public void updateEntity() {
        ++ticksExisted;
        if (ticksExisted == 20 || ticksExisted % 100 == 0) {
            syncData();
        }
        if (angle > 0)
            --angle;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("stock", stock);
        if (ammo != null) {
            NBTTagCompound itemsave = new NBTTagCompound();
            ammo.writeToNBT(itemsave);
            nbt.setTag("storage", itemsave);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        stock = nbt.getInteger("stock");
        if (nbt.hasKey("storage")) {
            NBTTagCompound itemsave = nbt.getCompoundTag("storage");
            ammo = ItemStack.loadItemStackFromNBT(itemsave);
        } else {
            ammo = null;
        }
    }

    public void syncData() {
        if (worldObj.isRemote) return;
        // Validate current contents before sync
        setContentsValidated(ammo, stock);

        NetworkUtils.sendToWatchers(new AmmoBoxPacket(this).generatePacket(), (WorldServer) worldObj, this.xCoord, this.zCoord);
    }

    /**
     * Validates and applies ammo/stock ensuring type acceptance and capacity bounds.
     * Returns true if state changed.
     */
    public boolean setContentsValidated(ItemStack newAmmo, int newStock) {
        ItemStack validatedAmmo = newAmmo;
        int validatedStock = newStock;

        if (validatedAmmo != null && !canAcceptItem(validatedAmmo)) {
            validatedAmmo = null;
            validatedStock = 0;
        }
        int max = (validatedAmmo != null) ? getMaxAmmo(validatedAmmo) : 0;
        if (validatedStock < 0) validatedStock = 0;
        if (validatedStock > max) validatedStock = max;
        if (validatedAmmo != null && validatedAmmo.stackSize > validatedAmmo.getMaxStackSize()) {
            validatedAmmo = validatedAmmo.copy();
            validatedAmmo.stackSize = validatedAmmo.getMaxStackSize();
        }

        boolean changed = this.ammo != validatedAmmo || this.stock != validatedStock || this.ammo != null && !areItemStacksEqual(this.ammo, validatedAmmo);
        this.ammo = validatedAmmo;
        this.stock = validatedStock;
        return changed;
    }

    public int getMaxAmmo(ItemStack ammo) {
        return ammo.getMaxStackSize() * getCapacity(getMaterial().tier);
    }

    @Override
    public int getMetreScale(int size) {
        if (ammo != null) {
            return (int) Math.min(size, (float) size / (float) getMaxAmmo(ammo) * stock);
        }
        return 0;
    }

    @Override
    public boolean shouldShowMetre() {
        return ammo != null;
    }

    @Override
    public String getLocalisedName() {
        if (ammo != null) {
            return ammo.getDisplayName() + " x" + stock;
        }
        return "";
    }

    private boolean areItemStacksEqual(ItemStack i1, ItemStack i2) {
        if (i1 == null || i2 == null)
            return false;

        return i1.isItemEqual(i2) && ItemStack.areItemStackTagsEqual(i1, i2);
    }

    private String getAmmoClass() {
        if (ammo != null && ammo.getItem() instanceof IAmmo) {
            return ((IAmmo) ammo.getItem()).getAmmoType(ammo);
        }
        return "null";
    }

    public byte getStorageType() {
        if (worldObj != null) {
            Block block = worldObj.getBlock(xCoord, yCoord, zCoord);
            if (block instanceof BlockAmmoBox) {
                return ((BlockAmmoBox) block).storageType;
            }
        }
        return storageSize;
    }
}
