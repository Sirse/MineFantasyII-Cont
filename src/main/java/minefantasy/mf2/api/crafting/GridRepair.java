package minefantasy.mf2.api.crafting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import minefantasy.mf2.api.helpers.ItemQuality;
import minefantasy.mf2.integration.thaumcraft.TCRepairRules;

/** Joining two worn items of the same kind into one on the anvil or the carpenter's bench. */
public final class GridRepair {

    private GridRepair() {}

    /** The two items of a repair, or null when the grid holds anything else. */
    public static ItemStack[] pair(InventoryCrafting grid) {
        ItemStack first = null;
        ItemStack second = null;
        int filled = 0;
        for (int slot = 0; slot < grid.getSizeInventory(); slot++) {
            ItemStack stack = grid.getStackInSlot(slot);
            if (stack != null) {
                if (filled == 0) first = stack;
                if (filled == 1) second = stack;
                filled++;
            }
        }
        if (filled == 2 && first.getItem() == second.getItem()
                && first.stackSize == 1
                && second.stackSize == 1
                && first.getItem().isRepairable()) {
            return new ItemStack[] { first, second };
        }
        return null;
    }

    /** The repaired item, or null when there is nothing to repair. */
    public static ItemStack result(InventoryCrafting grid) {
        ItemStack[] pair = pair(grid);
        return pair == null ? null : join(pair[0], pair[1]);
    }

    /** Both durabilities plus a tenth of the maximum, keeping the better item's data. */
    private static ItemStack join(ItemStack first, ItemStack second) {
        Item item = first.getItem();
        int remainFirst = first.getMaxDamage() - first.getItemDamageForDisplay();
        int remainSecond = second.getMaxDamage() - second.getItemDamageForDisplay();
        ItemStack nbtSource = ItemQuality.get(first) >= ItemQuality.get(second) ? first : second;
        int maxDamage = nbtSource.getMaxDamage();
        int combined = remainFirst + remainSecond + maxDamage * 10 / 100;
        int damage = Math.max(0, maxDamage - combined);

        ItemStack repaired = new ItemStack(item, 1, damage);
        if (nbtSource.hasTagCompound()) {
            repaired.setTagCompound((NBTTagCompound) nbtSource.getTagCompound().copy());
        }
        if (!TCRepairRules.mergeForRepair(repaired, first, second)) {
            return null;
        }
        return repaired;
    }
}
