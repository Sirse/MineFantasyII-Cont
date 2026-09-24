package minefantasy.mf2.block.tileentity;

import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.IHotItem;
import minefantasy.mf2.api.recipe.CraftInventory;
import minefantasy.mf2.api.recipe.CraftPlan;

/** The grid part of the anvil, carpenter and kitchen projects: what the grid pays, and paying it. */
final class GridProject {

    private GridProject() {}

    /**
     * Adds every filled grid slot, taking the amount the matched recipe owes there and giving back one container per
     * item. On the anvil a hot stack is described by the item it carries, so cooling does not change the project.
     */
    static CraftPlan.Builder addGrid(CraftPlan.Builder plan, IInventory inventory, int gridSlots, int[] amounts,
            boolean hot) {
        for (int slot = 0; slot < gridSlots; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null) {
                continue;
            }
            int amount = amounts != null && slot < amounts.length ? Math.max(1, amounts[slot]) : 1;
            ItemStack shown = hot ? carried(stack) : stack;
            plan.consume(slot, stack, amount, shown, other -> sameItem(shown, hot ? carried(other) : other));
        }
        return plan;
    }

    /** Drops returned items that found no room on top of the station. */
    static void drop(TileEntity station, List<ItemStack> spill) {
        for (ItemStack stack : spill) {
            if (stack == null || stack.stackSize <= 0 || station.getWorldObj() == null) {
                continue;
            }
            EntityItem entity = new EntityItem(
                    station.getWorldObj(),
                    station.xCoord + 0.5D,
                    station.yCoord + 1.1D,
                    station.zCoord + 0.5D,
                    stack.copy());
            entity.delayBeforeCanPickup = 10;
            station.getWorldObj().spawnEntityInWorld(entity);
        }
    }

    /** Pays the grid; returns without room are dropped by the station. False, with nothing changed, on a mismatch. */
    static boolean pay(CraftPlan plan, IInventory inventory, List<ItemStack> spill) {
        return plan.apply(CraftInventory.of(inventory), spill);
    }

    private static ItemStack carried(ItemStack stack) {
        if (stack.getItem() instanceof IHotItem) {
            ItemStack held = Heatable.getItem(stack);
            if (held != null) {
                return held;
            }
        }
        return stack;
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage()
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    /**
     * The requirements of a matched craft: the match's worked-out time, tiers and research, the recipe's tool and heat.
     */
    static CraftPlan.Builder require(CraftPlan.Builder plan, GridRecipe.Match match) {
        GridRecipe recipe = match.getRecipe();
        return require(
                plan,
                match.getTime(),
                recipe.getToolType(),
                match.getToolTier(),
                match.getStationTier(),
                match.getResearch(),
                recipe.outputHot());
    }

    /**
     * A repair has no recipe; it takes the defaults the managers always started from: 200 ticks with the station's tool
     * and no tiers.
     */
    static CraftPlan.Builder requireRepair(CraftPlan.Builder plan, String tool) {
        return require(plan, 200, tool, 0, 0, "", false);
    }

    private static CraftPlan.Builder require(CraftPlan.Builder plan, int time, String tool, int toolTier,
            int stationTier, String research, boolean hot) {
        return plan.require(MFRecipeKeys.TIME, (float) time).require(MFRecipeKeys.TOOL, tool)
                .require(MFRecipeKeys.TOOL_TIER, Math.max(-1, toolTier))
                .require(MFRecipeKeys.TIER, Math.max(-1, stationTier)).require(MFRecipeKeys.RESEARCH, research)
                .require(MFRecipeKeys.HOT_OUTPUT, hot);
    }
}
