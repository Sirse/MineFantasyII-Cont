package minefantasy.mf2.integration.waila;

import static minefantasy.mf2.integration.waila.WailaData.*;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.Requirements;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftInventory;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.block.tileentity.TileEntityBigFurnace;
import minefantasy.mf2.block.tileentity.TileEntityBloomery;
import minefantasy.mf2.block.tileentity.TileEntityQuern;
import minefantasy.mf2.block.tileentity.TileEntityRoast;
import minefantasy.mf2.block.tileentity.TileEntityTanningRack;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;

/** Read-only views of processing stations. No ticking, structure rebuilds, payouts or animation triggers. */
final class WailaProcesses {

    private WailaProcesses() {}

    static NBTTagCompound describe(TileEntity tile, EntityPlayer player) {
        NBTTagCompound data = new NBTTagCompound();
        if (tile instanceof TileEntityBigFurnace) {
            furnace(data, (TileEntityBigFurnace) tile);
        } else if (tile instanceof TileEntityBlastFH) {
            blastHeater(data, (TileEntityBlastFH) tile);
        } else if (tile instanceof TileEntityBlastFC) {
            blastShaft(data, (TileEntityBlastFC) tile);
        } else if (tile instanceof TileEntityBloomery) {
            bloomery(data, (TileEntityBloomery) tile, player);
        } else if (tile instanceof TileEntityRoast) {
            cooking(data, (TileEntityRoast) tile);
        } else if (tile instanceof TileEntityQuern) {
            data.setString("Kind", "craft");
            checked(data, ((TileEntityQuern) tile).inspectWork());
        } else if (tile instanceof TileEntityTanningRack) {
            tanning(data, (TileEntityTanningRack) tile, player);
        } else {
            return WailaWorld.describe(tile, player);
        }
        return data;
    }

    private static void furnace(NBTTagCompound data, TileEntityBigFurnace furnace) {
        data.setString("Kind", "heat");
        data.setBoolean("Built", furnace.built);
        data.setFloat("Temperature", finite(furnace.heat));
        if (!furnace.built) {
            reason(data, CheckResult.Reason.of("not_built"));
        } else if (furnace.isHeater()) {
            data.setBoolean("Burning", furnace.fuel > 0);
            data.setFloat("FuelSeconds", Math.max(0, furnace.fuel) / 20F);
            if (furnace.fuel <= 0 && furnace.heat <= 0) reason(data, CheckResult.Reason.of("no_heat"));
        } else {
            List<CraftPlan> plans = furnace.getShownPlans();
            NBTTagList results = new NBTTagList();
            boolean canWork = false;
            for (CraftPlan plan : plans) {
                ItemStack output = plan.getProduct();
                if (output != null) results.appendTag(output.writeToNBT(new NBTTagCompound()));
                canWork |= plan.canApplySpilling(CraftInventory.of(furnace));
            }
            data.setTag("Results", results);
            progress(data, furnace.progress, furnace.getMaxTime());
            data.setBoolean("ShowProgress", !plans.isEmpty());
            if (plans.isEmpty()) reason(data, CheckResult.Reason.NO_RECIPE);
            else if (!canWork) reason(data, CheckResult.Reason.OUTPUT_FULL);
            else if (furnace.heat < 1) reason(data, CheckResult.Reason.of("no_heat"));
        }
    }

    private static void blastHeater(NBTTagCompound data, TileEntityBlastFH heater) {
        data.setString("Kind", "heat");
        data.setBoolean("Built", heater.isBuilt);
        data.setBoolean("Burning", heater.isBurning());
        data.setInteger("Fuel", Math.max(0, heater.fuel));
        data.setInteger("MaxFuel", Math.max(0, heater.maxFuel));
        progress(data, heater.progress, TileEntityBlastFH.maxProgress);
        data.setBoolean("ShowProgress", heater.isBuilt);
        smoke(data, heater);
        if (!heater.isBuilt) reason(data, CheckResult.Reason.of("not_built"));
        else if (heater.getSmokeValue() >= heater.getMaxSmokeStorage()) reason(data, CheckResult.Reason.of("smoke"));
        else if (!heater.isBurning()) reason(data, CheckResult.Reason.of("no_heat"));
    }

    private static void blastShaft(NBTTagCompound data, TileEntityBlastFC shaft) {
        data.setString("Kind", "heat");
        data.setBoolean("Built", shaft.isBuilt);
        smoke(data, shaft);
        CheckResult check = shaft.inspectWork();
        checked(data, check);
        if (!check.isSuccess()) return;
        TileEntityBlastFH heater = null;
        // Bounded vertical lookup inside this loaded chunk. A tooltip never loads chunks or scans firebrick walls.
        for (int down = 1; down <= Math.min(8, TileEntityBlastFH.maxFurnaceHeight); down++) {
            int y = shaft.yCoord - down;
            if (!shaft.getWorldObj().blockExists(shaft.xCoord, y, shaft.zCoord)) break;
            TileEntity below = shaft.getWorldObj().getTileEntity(shaft.xCoord, y, shaft.zCoord);
            if (below instanceof TileEntityBlastFH) {
                heater = (TileEntityBlastFH) below;
                break;
            }
            if (!(below instanceof TileEntityBlastFC)) break;
        }
        if (heater == null || !heater.isBuilt) reason(data, CheckResult.Reason.of("not_built"));
        else {
            progress(data, heater.progress, TileEntityBlastFH.maxProgress);
            if (heater.getSmokeValue() >= heater.getMaxSmokeStorage()) reason(data, CheckResult.Reason.of("smoke"));
            else if (!heater.isBurning()) reason(data, CheckResult.Reason.of("no_heat"));
        }
    }

    private static void smoke(NBTTagCompound data, TileEntityBlastFC furnace) {
        data.setInteger("Smoke", Math.max(0, furnace.getSmokeValue()));
        data.setInteger("MaxSmoke", furnace.getMaxSmokeStorage());
    }

    private static void bloomery(NBTTagCompound data, TileEntityBloomery bloomery, EntityPlayer player) {
        data.setString("Kind", "heat");
        if (bloomery.hasBloom()) {
            item(data, bloomery.getStackInSlot(2));
            data.setBoolean("Ready", true);
            return;
        }
        CheckResult result = bloomery.inspectWork(player);
        checked(data, result);
        if (bloomery.isActive) progress(data, bloomery.progress, bloomery.progressMax);
        if (result.isSuccess()
                && !bloomery.getWorldObj().canBlockSeeTheSky(bloomery.xCoord, bloomery.yCoord + 1, bloomery.zCoord)) {
            reason(data, CheckResult.Reason.of("open_sky"));
        } else if (result.isSuccess() && !bloomery.isActive) {
            reason(data, CheckResult.Reason.of("unlit"));
        }
    }

    private static void cooking(NBTTagCompound data, TileEntityRoast roast) {
        data.setString("Kind", "heat");
        int heat = roast.getWorkTemperature();
        data.setFloat("Temperature", heat);
        CookRecipe.Found found = roast.getShownCooking();
        if (found == null) {
            if (roast.getStackInSlot(0) == null) reason(data, CheckResult.Reason.MISSING_INPUT);
            else if (roast.hasCookingProject() || CookRecipe.find(roast.getStackInSlot(0), roast.isOven()) != null) {
                reason(data, CheckResult.Reason.of("checking"));
            } else {
                item(data, roast.getStackInSlot(0));
                data.setBoolean("Ready", true);
            }
            return;
        }
        CookRecipe recipe = found.recipe;
        boolean burnStage = found.id.getPath().endsWith(CookRecipe.BURNT_SUFFIX);
        item(data, burnStage ? roast.getStackInSlot(0) : recipe.getOutput());
        progress(data, roast.progress, roast.maxProgress);
        data.setBoolean("Ready", burnStage);
        data.setBoolean("BurnProgress", burnStage);
        if (burnStage) reason(data, CheckResult.Reason.of("burning_food"));
        else if (heat <= recipe.getMinTemperature())
            reason(data, CheckResult.Reason.temperature(heat, recipe.getMinTemperature() + 1));
        else if (TileEntityRoast.enableOverheat && recipe.canBurn() && heat > recipe.getMaxTemperature()) {
            reason(data, CheckResult.Reason.of("overheat"));
        }
    }

    private static void tanning(NBTTagCompound data, TileEntityTanningRack rack, EntityPlayer player) {
        data.setString("Kind", "craft");
        CraftPlan plan = rack.getShownPlan();
        if (plan == null) {
            if (rack.getStackInSlot(0) == null) reason(data, CheckResult.Reason.MISSING_INPUT);
            else if (rack.hasWorkProject()) reason(data, CheckResult.Reason.of("checking"));
            else {
                item(data, rack.getStackInSlot(0));
                data.setBoolean("Ready", true);
            }
            return;
        }
        item(data, plan.getProduct());
        progress(data, rack.progress, rack.maxProgress);
        data.setString("Tool", rack.isAutomated() ? "" : plan.require(MFRecipeKeys.TOOL, "knife"));
        data.setInteger("ToolTier", plan.require(MFRecipeKeys.TOOL_TIER, -1));
        data.setInteger("BenchTier", -1);
        if (!rack.isAutomated())
            reason(data, Requirements.of(plan).check(Requirements.TANNING, player, 0).getRefusal());
    }

    private static void checked(NBTTagCompound data, CheckResult result) {
        if (result.isSuccess()) item(data, result.getPlan().getProduct());
        else if ("research".equals(result.getReason().getId())) {
            data.setBoolean("Unknown", true);
            data.setString("Reason", "unknown_research");
        } else reason(data, result.getReason());
    }
}
