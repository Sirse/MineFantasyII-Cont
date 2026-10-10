package minefantasy.mf2.integration.waila;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;

import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.block.tileentity.TileEntityComponent;
import minefantasy.mf2.block.tileentity.TileEntityResearch;
import minefantasy.mf2.block.tileentity.TileEntityRoad;
import minefantasy.mf2.block.tileentity.decor.TileEntityAmmoBox;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;

/** Read-only snapshots for storage, study and world blocks. */
final class WailaWorld {

    private WailaWorld() {}

    static NBTTagCompound describe(TileEntity tile, EntityPlayer player) {
        NBTTagCompound data = new NBTTagCompound();
        if (tile instanceof TileEntityComponent) {
            TileEntityComponent pile = (TileEntityComponent) tile;
            data.setString("Kind", "storage");
            storage(data, pile.item, pile.stackSize, pile.max);
            if (pile.material != null) data.setString("Material", "material." + pile.material.getName() + ".name");
        } else if (tile instanceof TileEntityAmmoBox) {
            TileEntityAmmoBox box = (TileEntityAmmoBox) tile;
            data.setString("Kind", "storage");
            storage(data, box.ammo, box.stock, box.ammo == null ? 0 : box.getMaxAmmo(box.ammo.copy()));
        } else if (tile instanceof TileEntityRack) {
            TileEntityRack rack = (TileEntityRack) tile;
            data.setString("Kind", "storage");
            NBTTagList contents = new NBTTagList();
            int occupied = 0;
            for (int i = 0; i < rack.getSizeInventory(); i++) {
                ItemStack stack = rack.getStackInSlot(i);
                if (stack != null && stack.stackSize > 0) {
                    occupied++;
                    if (contents.tagCount() < 4) contents.appendTag(stack.copy().writeToNBT(new NBTTagCompound()));
                }
            }
            data.setTag("Results", contents);
            data.setInteger("Count", occupied);
            data.setInteger("Limit", rack.getSizeInventory());
        } else if (tile instanceof TileEntityResearch) {
            TileEntityResearch table = (TileEntityResearch) tile;
            data.setString("Kind", "research");
            ItemStack artifact = table.getStackInSlot(0);
            List<String> names = TileEntityResearch.getInfo(artifact == null ? null : artifact.copy());
            NBTTagList visible = new NBTTagList();
            boolean hidden = false;
            if (names != null && player != null) for (String name : names) {
                InformationBase base = ResearchLogic.getResearch(name);
                if (base == null) continue;
                if (ResearchLogic.hasInfoUnlocked(player, base) || ResearchLogic.canPurchase(player, base)) {
                    if (visible.tagCount() < 4)
                        visible.appendTag(new NBTTagString("knowledge." + base.getUnlocalisedName()));
                } else hidden = true;
            }
            data.setTag("Research", visible);
            data.setBoolean("Unknown", hidden);
            if (player != null && table.study.belongsTo(artifact, player.getUniqueID().toString())) {
                WailaData.progress(data, table.study.progress, table.maxProgress);
                data.setBoolean("ShowProgress", true);
            }
        } else if (tile instanceof TileEntityRoad) {
            TileEntityRoad road = (TileEntityRoad) tile;
            data.setString("Kind", "world");
            Item item = Item.getItemFromBlock(road.getBaseBlock());
            if (item != null)
                data.setTag("Surface", new ItemStack(item, 1, road.getSurface()[1]).writeToNBT(new NBTTagCompound()));
            data.setBoolean("Locked", road.isLocked);
        }
        return data;
    }

    private static void storage(NBTTagCompound data, ItemStack stack, int count, int limit) {
        if (stack != null) {
            ItemStack shown = stack.copy();
            shown.stackSize = 1;
            WailaData.item(data, shown);
        }
        data.setInteger("Count", Math.max(0, count));
        data.setInteger("Limit", Math.max(0, limit));
    }
}
