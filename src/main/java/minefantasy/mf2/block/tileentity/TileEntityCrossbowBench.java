package minefantasy.mf2.block.tileentity;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import minefantasy.mf2.api.crafting.IBasicMetre;
import minefantasy.mf2.api.crafting.engineer.ICrossbowPart;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.item.list.ToolListMF;

public class TileEntityCrossbowBench extends TileEntityStation implements IBasicMetre {

    public float progress;
    public float maxProgress = 25F;
    public boolean hasRecipe;
    /**
     * Stock, Head, Mod, Muzzle, Result
     */
    private ItemStack[] inv = new ItemStack[5];
    private Random rand = new Random();
    private int ticksExisted;

    private static ICrossbowPart getCrossbowPart(ItemStack item) {
        if (item != null && item.getItem() instanceof ICrossbowPart) {
            return (ICrossbowPart) item.getItem();
        }
        return null;
    }

    public static boolean isMatch(ItemStack item, String var) {
        ICrossbowPart part = getCrossbowPart(item);
        if (part != null) {
            return part.getComponentType().equalsIgnoreCase(var);
        }
        return false;
    }

    @Override
    public void updateEntity() {
        ++ticksExisted;
        if (worldObj.isRemote) {
            return;
        }
        sendState(false);
    }

    public boolean tryCraft(EntityPlayer user) {
        ItemStack result = findResult();
        hasRecipe = result != null;

        if (result == null) {
            progress = 0F;
        } else if (ToolHelper.getCrafterTool(user.getHeldItem()).equalsIgnoreCase("spanner")) {
            if (user.getHeldItem() != null) {
                worldObj.playSoundEffect(
                        xCoord + 0.5,
                        yCoord,
                        zCoord + 0.5,
                        "minefantasy2:block.twistbolt",
                        0.25F,
                        1.0F);
                ToolHelper.wearCrafterTool(user);
            }
            float efficiency = ToolHelper.getCrafterEfficiency(user.getHeldItem());

            if (user.swingProgress > 0 && user.swingProgress <= 1.0) {
                efficiency *= (0.5F - user.swingProgress);
            }

            if (!worldObj.isRemote) {
                progress += efficiency;
            }
            if ((progress >= maxProgress && craftItem(result))) {
                worldObj.playSoundEffect(xCoord + 0.5, yCoord, zCoord + 0.5, "random.door_open", 0.35F, 0.5F);
                progress = 0;
                if (user != null) {
                    SkillList.engineering.addXP(user, 10);
                }
                for (int a = 0; a < 4; a++) {
                    decrStackSize(a, 1);
                }
            }
            return true;
        }
        return false;
    }

    private boolean craftItem(ItemStack result) {
        if (inv[4] == null) {
            this.setInventorySlotContents(4, result);
            return true;
        } else {
            return false;
        }
    }

    public void syncData() {
        sendState(false);
    }

    // INVENORY

    private ItemStack findResult() {
        ICrossbowPart stock = getCrossbowPart(inv[0]);
        ICrossbowPart head = getCrossbowPart(inv[1]);
        ICrossbowPart mod = getCrossbowPart(inv[2]);
        ICrossbowPart muzzle = getCrossbowPart(inv[3]);

        if (stock == null || head == null) return null;

        return ToolListMF.crossbow_custom.constructCrossbow(stock, head, mod, muzzle);
    }

    @Override
    public String getInventoryName() {
        return "gui.crossbowcraftmf.name";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        if (this.isMatch(item, "stock")) {
            return slot == 0;
        }
        if (this.isMatch(item, "mechanism")) {
            return slot == 1;
        }
        if (this.isMatch(item, "mod")) {
            return slot == 2;
        }
        if (this.isMatch(item, "muzzle")) {
            return slot == 3;
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        inv = InventorySlots.read(nbt, "Items", inv.length);
        progress = nbt.getFloat("progress");
        maxProgress = nbt.getFloat("maxProgress");
        hasRecipe = nbt.getBoolean("hasRecipe");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        InventorySlots.write(nbt, "Items", inv);
        nbt.setFloat("progress", progress);
        nbt.setFloat("maxProgress", maxProgress);
        nbt.setBoolean("hasRecipe", hasRecipe);
    }

    @Override
    public int getMetreScale(int size) {
        return (int) Math.min(size, size / maxProgress * progress);
    }

    @Override
    public boolean shouldShowMetre() {
        return true;
    }

    @Override
    public String getLocalisedName() {
        return StatCollector.translateToLocal("tile.crossbowBench.name");
    }

    @Override
    protected ItemStack[] slots() {
        return inv;
    }

    @Override
    protected NBTTagCompound describe() {
        NBTTagCompound state = new NBTTagCompound();
        state.setFloat("Progress", progress);
        state.setFloat("MaxProgress", maxProgress);
        state.setBoolean("HasRecipe", hasRecipe);
        return state;
    }

    @Override
    public void show(NBTTagCompound state) {
        progress = state.getFloat("Progress");
        maxProgress = state.getFloat("MaxProgress");
        hasRecipe = state.getBoolean("HasRecipe");
    }

    @Override
    public Role role(int slot) {
        return slot == 4 ? Role.OUTPUT : Role.INPUT;
    }
}
