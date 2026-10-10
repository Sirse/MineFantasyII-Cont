package minefantasy.mf2.integration.waila;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/** Formats server data in the viewing client's language. Never reads a client's station inventory or recipes. */
final class WailaTooltip {

    private WailaTooltip() {}

    static void body(NBTTagCompound data, List<String> tip) {
        if (data.getBoolean("Unknown")) tip.add(text("unknown"));
        ItemStack item = data.hasKey("Item") ? ItemStack.loadItemStackFromNBT(data.getCompoundTag("Item")) : null;
        if (item != null)
            tip.add(data.hasKey("Count") ? item.getDisplayName() : text("item", item.getDisplayName(), item.stackSize));
        if (data.hasKey("Count")) tip.add(text("storage_count", data.getInteger("Count"), data.getInteger("Limit")));
        if (data.hasKey("Material"))
            tip.add(text("material", StatCollector.translateToLocal(data.getString("Material"))));
        NBTTagList research = data.getTagList("Research", 8);
        for (int i = 0; i < Math.min(4, research.tagCount()); i++)
            tip.add(StatCollector.translateToLocal(research.getStringTagAt(i)));
        if (data.hasKey("BerryReady")) tip.add(text(data.getBoolean("BerryReady") ? "berry_ready" : "grow"));
        if (data.hasKey("Surface")) {
            ItemStack surface = ItemStack.loadItemStackFromNBT(data.getCompoundTag("Surface"));
            if (surface != null) tip.add(text("surface", surface.getDisplayName()));
        }
        if (data.hasKey("Locked")) tip.add(text(data.getBoolean("Locked") ? "locked" : "unlocked"));
        NBTTagList results = data.getTagList("Results", 10);
        for (int i = 0; i < Math.min(4, results.tagCount()); i++) {
            ItemStack output = ItemStack.loadItemStackFromNBT(results.getCompoundTagAt(i));
            if (output != null) tip.add(text("item", output.getDisplayName(), output.stackSize));
        }
        if (data.getBoolean("Ready")) tip.add(text("ready"));
        if (data.hasKey("Built")) tip.add(text(data.getBoolean("Built") ? "built" : "not_built"));
        float max = data.getFloat("MaxProgress");
        if (max > 0 && (data.hasKey("Item") || data.getBoolean("Unknown") || data.getBoolean("ShowProgress"))) {
            int percent = (int) Math.max(0, Math.min(100, data.getFloat("Progress") * 100D / max));
            tip.add(text(data.getBoolean("BurnProgress") ? "burn_progress" : "progress", percent));
        }
        if (data.hasKey("Burning")) tip.add(text(data.getBoolean("Burning") ? "burning" : "unlit"));
        if (data.hasKey("FuelSeconds")) tip.add(text("fuel", (int) Math.ceil(data.getFloat("FuelSeconds"))));
        if (data.getInteger("MaxFuel") > 0) {
            int percent = (int) (100L * data.getInteger("Fuel") / data.getInteger("MaxFuel"));
            tip.add(text("fuel_percent", Math.max(0, Math.min(100, percent))));
        }
        if (data.hasKey("Smoke")) tip.add(text("smoke", data.getInteger("Smoke"), data.getInteger("MaxSmoke")));
        if (data.hasKey("Temperature")) tip.add(text("temperature", Math.round(data.getFloat("Temperature"))));
        if (data.hasKey("ItemTemperature")) {
            int stage = data.getInteger("HeatStage");
            String state = stage >= 2 ? "state.unstable" : stage == 1 ? "state.workable" : "minefantasy2.waila.cold";
            tip.add(text("piece", data.getInteger("ItemTemperature"), StatCollector.translateToLocal(state)));
        }
        if (data.hasKey("Capacity")) {
            Fluid fluid = FluidRegistry.getFluid(data.getString("Fluid"));
            tip.add(
                    text(
                            "fluid",
                            fluid == null ? text("empty")
                                    : fluid.getLocalizedName(new FluidStack(fluid, data.getInteger("Amount"))),
                            data.getInteger("Amount"),
                            data.getInteger("Capacity")));
            if (data.getInteger("Amount") > 0) tip.add(text(data.getBoolean("Quench") ? "quench" : "no_quench"));
        }
        if (data.hasKey("Dirt"))
            tip.add(text("dirt", Math.round(data.getFloat("Dirt")), Math.round(data.getFloat("DirtMax"))));
        if (data.hasKey("Reason")) {
            String reason = data.getString("Reason");
            NBTTagList args = data.getTagList("Args", 8);
            Object[] values = new Object[args.tagCount()];
            for (int i = 0; i < values.length; i++) values[i] = args.getStringTagAt(i);
            if ("tool".equals(reason) && values.length == 2) {
                values[0] = toolName(String.valueOf(values[0]));
                values[1] = toolName(String.valueOf(values[1]));
            } else if (("tier".equals(reason) || "harder".equals(reason)) && values.length == 3) {
                String what = String.valueOf(values[0]);
                values[0] = "tool".equals(what) ? text("tool_label")
                        : toolName("bench".equals(what) ? "carpenter" : what);
            }
            String key = "minefantasy2.recipe.reason." + reason;
            if ("tool".equals(reason)) key = "minefantasy2.waila.reason.tool";
            if (!StatCollector.canTranslate(key)) key = "minefantasy2.waila.reason." + reason;
            tip.add(
                    (data.getBoolean("Penalty") ? "\u00a7e" : "\u00a7c")
                            + sentence(StatCollector.translateToLocalFormatted(key, values)));
        }
    }

    static void details(NBTTagCompound data, List<String> tip) {
        String tool = data.getString("Tool");
        if (!tool.isEmpty()) {
            int tier = data.getInteger("ToolTier");
            tip.add(tier > 0 ? text("tool", toolName(tool), tier) : text("tool_without_tier", toolName(tool)));
        }
        if (data.getInteger("BenchTier") >= 0) tip.add(text("bench_tier", data.getInteger("BenchTier")));
    }

    private static String text(String key, Object... args) {
        return StatCollector.translateToLocalFormatted("minefantasy2.waila." + key, args);
    }

    /** Shared recipe reasons are sentence fragments elsewhere, but standalone lines in this HUD. */
    private static String sentence(String text) {
        if (text.isEmpty()) return text;
        int first = text.codePointAt(0);
        return new String(Character.toChars(Character.toUpperCase(first))) + text.substring(Character.charCount(first));
    }

    private static String toolName(String type) {
        String key = "tooltype." + type;
        return StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key) : type;
    }
}
