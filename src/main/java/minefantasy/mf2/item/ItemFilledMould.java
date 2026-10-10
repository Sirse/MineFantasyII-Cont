package minefantasy.mf2.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.heating.TongsHelper;
import minefantasy.mf2.api.helpers.Drops;
import minefantasy.mf2.api.helpers.Sounds;
import minefantasy.mf2.item.list.ComponentListMF;

public class ItemFilledMould extends ItemComponentMF {

    private static final String itemNBT = "MF_HeldItem";

    public ItemFilledMould() {
        super("ingot_mould_filled");
        this.setUnlocalizedName("ingot_mould");
    }

    public static ItemStack createMould(ItemStack fill) {
        ItemStack mould = new ItemStack(ComponentListMF.ingot_mould_filled);
        NBTTagCompound nbt = getOrCreateNBT(mould);
        NBTTagCompound save = new NBTTagCompound();
        fill.writeToNBT(save);
        nbt.setTag(itemNBT, save);
        return mould;
    }

    public static NBTTagCompound getOrCreateNBT(ItemStack item) {
        if (!item.hasTagCompound()) {
            item.setTagCompound(new NBTTagCompound());
        }
        return item.getTagCompound();
    }

    public ItemStack getHeldItem(ItemStack item) {
        NBTTagCompound nbt = getOrCreateNBT(item);
        if (nbt.hasKey(itemNBT)) {
            return ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(itemNBT));
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack item, EntityPlayer user, List list, boolean fullInfo) {
        ItemStack held = getHeldItem(item);
        if (held != null) {
            list.add(held.getDisplayName());
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack item, World world, EntityPlayer player) {
        MovingObjectPosition movingobjectposition = this.getMovingObjectPositionFromPlayer(world, player, true);

        if (movingobjectposition == null) {
            return item;
        } else {
            if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                int i = movingobjectposition.blockX;
                int j = movingobjectposition.blockY;
                int k = movingobjectposition.blockZ;

                if (!world.canMineBlock(player, i, j, k)) {
                    return item;
                }

                if (!player.canPlayerEdit(i, j, k, movingobjectposition.sideHit, item)) {
                    return item;
                }

                ItemStack drop = getHeldItem(item);

                if (drop != null && TongsHelper.findQuench(world, i, j, k) != null) {
                    Sounds.quench(player);

                    for (int a = 0; a < 5; a++) {
                        world.spawnParticle("largesmoke", i + 0.5F, j + 1, k + 0.5F, 0, 0.065F, 0);
                    }

                    if (!world.isRemote) {
                        ItemStack mould = new ItemStack(ComponentListMF.ingot_mould);
                        if (!world.getBlock(i, j + 1, k).getMaterial().isSolid()) {
                            Drops.still(world, i + 0.5, j + 1.5, k + 0.5, drop, 20);
                            Drops.still(world, i + 0.5, j + 1.5, k + 0.5, mould, 20);
                        } else {
                            Drops.toPlayer(player, drop);
                            Drops.toPlayer(player, mould);
                        }
                    }

                    --item.stackSize;
                }
            }

            return item;
        }
    }
}
