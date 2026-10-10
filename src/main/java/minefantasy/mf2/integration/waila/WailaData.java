package minefantasy.mf2.integration.waila;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidTankInfo;

import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.QuenchMedium;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.block.tileentity.CraftBench;
import minefantasy.mf2.block.tileentity.TileEntityCrucible;
import minefantasy.mf2.block.tileentity.TileEntityFirepit;
import minefantasy.mf2.block.tileentity.TileEntityForge;
import minefantasy.mf2.block.tileentity.TileEntityKitchenBench;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;

/** Small server snapshots for Waila. Reads state only: no recipe refreshes, interactions or fluid transfers. */
public final class WailaData {

    public static final String KEY = "MineFantasy";

    private WailaData() {}

    public static NBTTagCompound describe(TileEntity tile, EntityPlayer player) {
        NBTTagCompound data = new NBTTagCompound();
        if (tile instanceof CraftBench) {
            CraftBench bench = (CraftBench) tile;
            data.setString("Kind", "craft");
            progress(data, bench.getProgress(), bench.getProgressMax());
            CheckResult.Reason problem = bench.getWorkProblem(player);
            if (problem != null && "checking".equals(problem.getId())) {
                reason(data, problem);
            } else if (bench.hasProject()) {
                boolean known = player != null && bench.doesPlayerKnowCraft(player);
                data.setBoolean("Unknown", !known);
                if (known) {
                    item(data, bench.getShownResult());
                    data.setString("Tool", bench.getToolNeeded() == null ? "" : bench.getToolNeeded());
                    data.setInteger("ToolTier", bench.getToolTierNeeded());
                    data.setInteger("BenchTier", bench.getBenchTierNeeded());
                    reason(data, problem);
                } else {
                    data.setString("Reason", "unknown_research");
                }
            } else {
                reason(data, CheckResult.Reason.NO_RECIPE);
            }
            if (tile instanceof TileEntityKitchenBench) {
                TileEntityKitchenBench kitchen = (TileEntityKitchenBench) tile;
                data.setFloat("Dirt", finite(kitchen.dirtyProgress));
                data.setFloat("DirtMax", finite(kitchen.getDirtyMax()));
                if (kitchen.isDirty()) reason(data, CheckResult.Reason.DIRTY);
            }
        } else if (tile instanceof TileEntityCrucible) {
            TileEntityCrucible crucible = (TileEntityCrucible) tile;
            data.setString("Kind", "heat");
            progress(data, crucible.progress, crucible.progressMax);
            data.setFloat("Temperature", finite(crucible.temperature));
            item(data, crucible.getShownResult());
            reason(data, crucible.getWorkProblem());
        } else if (tile instanceof TileEntityForge) {
            TileEntityForge forge = (TileEntityForge) tile;
            data.setString("Kind", "heat");
            data.setBoolean("Burning", forge.isLit() && forge.fuel > 0);
            data.setFloat("FuelSeconds", finite(forge.getFuelSeconds()));
            data.setFloat("Temperature", finite(forge.temperature));
            ItemStack held = forge.getStackInSlot(0);
            if (held != null) {
                ItemStack shown = held.copy();
                item(data, shown);
                data.setInteger("ItemTemperature", Heatable.getTemp(shown));
                data.setInteger("HeatStage", Heatable.getHeatableStage(shown));
            }
        } else if (tile instanceof TileEntityFirepit) {
            TileEntityFirepit fire = (TileEntityFirepit) tile;
            data.setString("Kind", "heat");
            data.setBoolean("Burning", fire.isBurning());
            data.setFloat("FuelSeconds", Math.max(0, fire.fuel) / 20F);
        } else if (tile instanceof TileEntityTrough) {
            TileEntityTrough trough = (TileEntityTrough) tile;
            FluidTankInfo tank = trough.getTankInfo(ForgeDirection.UNKNOWN)[0];
            data.setString("Kind", "fluids");
            data.setInteger("Capacity", tank.capacity);
            data.setInteger("Amount", tank.fluid == null ? 0 : tank.fluid.amount);
            if (tank.fluid != null) {
                data.setString("Fluid", tank.fluid.getFluid().getName());
                QuenchMedium medium = QuenchMedium.of(tank.fluid.getFluid());
                data.setBoolean("Quench", trough.fill > 0 && medium != null);
            }
        } else {
            return WailaProcesses.describe(tile, player);
        }
        return data;
    }

    static float finite(float value) {
        return Float.isNaN(value) || Float.isInfinite(value) ? 0F : Math.max(0F, value);
    }

    static void progress(NBTTagCompound data, float progress, float max) {
        data.setFloat("Progress", finite(progress));
        data.setFloat("MaxProgress", finite(max));
    }

    static void item(NBTTagCompound data, ItemStack item) {
        if (item != null && item.getItem() != null && item.stackSize > 0) {
            data.setTag("Item", item.copy().writeToNBT(new NBTTagCompound()));
        }
    }

    static void reason(NBTTagCompound data, CheckResult.Reason reason) {
        if (reason == null) return;
        data.setString("Reason", reason.getId());
        data.setBoolean("Penalty", reason.isPenalty());
        NBTTagList args = new NBTTagList();
        for (Object arg : reason.getArgs()) args.appendTag(new NBTTagString(String.valueOf(arg)));
        data.setTag("Args", args);
    }
}
