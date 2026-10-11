package minefantasy.mf2.integration.waila;

import java.util.ArrayList;
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
        body(data, tip, false);
    }

    private static void body(NBTTagCompound data, List<String> tip, boolean advanced) {
        String refusal = data.getString("Reason");
        if (data.getBoolean("Unknown")) tip.add(text("unknown"));
        ItemStack item = data.hasKey("Item") ? ItemStack.loadItemStackFromNBT(data.getCompoundTag("Item")) : null;
        if (item != null)
            tip.add(data.hasKey("Count") ? item.getDisplayName() : text("item", item.getDisplayName(), item.stackSize));
        if (data.hasKey("Count") && !(advanced && data.getTagList("Contents", 10).tagCount() > 0)) {
            int count = data.getInteger("Count");
            if (data.hasKey("Contents")) {
                tip.add(text("storage_occupied", count, data.getInteger("Limit")));
            } else {
                int limit = data.getInteger("Limit");
                if (count <= 0) tip.add(text("empty"));
                else if (limit > 0 && count >= limit) tip.add(text("storage_full", count));
                else tip.add(text("storage_count", count, limit));
            }
        }
        if (advanced && data.hasKey("Material")) {
            String material = StatCollector.translateToLocal(data.getString("Material"));
            if (item == null || !item.getDisplayName().toLowerCase(java.util.Locale.ROOT)
                    .contains(material.toLowerCase(java.util.Locale.ROOT))) {
                tip.add(text("material", material));
            }
        }
        NBTTagList research = data.getTagList("Research", 10);
        for (int i = 0; advanced && i < Math.min(4, research.tagCount()); i++) {
            NBTTagCompound entry = research.getCompoundTagAt(i);
            tip.add(
                    text(
                            entry.getBoolean("Learned") ? "research_learned" : "research_available",
                            StatCollector.translateToLocal(entry.getString("Name"))));
        }
        if (advanced && data.getBoolean("ResearchHidden")) tip.add(text("research_hidden"));
        if (data.hasKey("BerryReady")) tip.add(text(data.getBoolean("BerryReady") ? "berry_ready" : "grow"));
        if (advanced && data.hasKey("Surface")) {
            ItemStack surface = ItemStack.loadItemStackFromNBT(data.getCompoundTag("Surface"));
            if (surface != null) tip.add(text("surface", surface.getDisplayName()));
        }
        if (data.hasKey("Locked")) tip.add(text(data.getBoolean("Locked") ? "locked" : "unlocked"));
        NBTTagList results = data.getTagList(data.hasKey("Contents") ? "Contents" : "Results", 10);
        for (int i = 0; advanced && i < Math.min(4, results.tagCount()); i++) {
            ItemStack output = ItemStack.loadItemStackFromNBT(results.getCompoundTagAt(i));
            if (output != null) tip.add(text("item", output.getDisplayName(), output.stackSize));
        }
        if (data.getBoolean("Ready")) tip.add(text("ready"));
        if (data.hasKey("Built") && (advanced || !data.getBoolean("Built")) && !"not_built".equals(refusal))
            tip.add(text(data.getBoolean("Built") ? "built" : "not_built"));
        float max = data.getFloat("MaxProgress");
        if (max > 0 && (data.hasKey("Item") || data.getBoolean("Unknown") || data.getBoolean("ShowProgress"))) {
            int percent = (int) Math.max(0, Math.min(100, data.getFloat("Progress") * 100D / max));
            tip.add(text(data.getBoolean("BurnProgress") ? "burn_progress" : "progress", percent));
        }
        if (data.hasKey("Burning")) tip.add(text(data.getBoolean("Burning") ? "burning" : "unlit"));
        if (advanced && data.hasKey("FuelSeconds"))
            tip.add(text("fuel", (int) Math.ceil(data.getFloat("FuelSeconds"))));
        if (advanced && data.getInteger("MaxFuel") > 0) {
            int percent = (int) (100L * data.getInteger("Fuel") / data.getInteger("MaxFuel"));
            tip.add(text("fuel_percent", Math.max(0, Math.min(100, percent))));
        }
        if (advanced && data.hasKey("Smoke"))
            tip.add(text("smoke", data.getInteger("Smoke"), data.getInteger("MaxSmoke")));
        if (data.hasKey("Temperature") && (advanced || !data.getBoolean("TemperatureDetails"))
                && !"temperature".equals(refusal))
            tip.add(text("temperature", Math.round(data.getFloat("Temperature"))));
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
        if (advanced && data.hasKey("Dirt"))
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
                values[0] = "tool".equals(what)
                        ? advanced && !data.getString("Tool").isEmpty() ? toolName(data.getString("Tool"))
                                : text("tool_label")
                        : toolName("bench".equals(what) ? "carpenter" : what);
            }
            String key = "minefantasy2.recipe.reason." + reason;
            if ("tool".equals(reason)) {
                key = "minefantasy2.waila.reason.tool";
                if (advanced && data.getInteger("ToolTier") > 0 && values.length > 0) {
                    key = "minefantasy2.waila.reason.tool_tier";
                    values = new Object[] { values[0], data.getInteger("ToolTier") };
                }
            }
            if (!StatCollector.canTranslate(key)) key = "minefantasy2.waila.reason." + reason;
            tip.add(
                    (data.getBoolean("Penalty") ? "\u00a7e" : "\u00a7c")
                            + sentence(StatCollector.translateToLocalFormatted(key, values)));
        }
    }

    /** Waila calls this immediately after body for this provider: replace only our own trailing rows. */
    static void details(NBTTagCompound data, List<String> tip) {
        List<String> compact = new ArrayList<String>();
        body(data, compact, false);
        int start = tip.size() - compact.size();
        if (start >= 0 && tip.subList(start, tip.size()).equals(compact)) {
            tip.subList(start, tip.size()).clear();
        }
        body(data, tip, true);
        requirements(data, tip);
    }

    static boolean hasDetails(NBTTagCompound data) {
        List<String> compact = new ArrayList<String>();
        List<String> expanded = new ArrayList<String>();
        body(data, compact, false);
        body(data, expanded, true);
        requirements(data, expanded);
        return !compact.equals(expanded);
    }

    private static void requirements(NBTTagCompound data, List<String> tip) {
        String reason = data.getString("Reason");
        NBTTagList args = data.getTagList("Args", 8);
        boolean tierReason = "tier".equals(reason) || "harder".equals(reason);
        boolean toolReason = "tool".equals(reason) || tierReason && "tool".equals(args.getStringTagAt(0));
        String tool = data.getString("Tool");
        if (!tool.isEmpty() && !toolReason) {
            int tier = data.getInteger("ToolTier");
            tip.add(tier > 0 ? text("tool", toolName(tool), tier) : text("tool_without_tier", toolName(tool)));
        }
        if (data.hasKey("BenchTier") && data.getInteger("BenchTier") >= 0
                && !(tierReason && !"tool".equals(args.getStringTagAt(0))))
            tip.add(text("bench_tier", data.getInteger("BenchTier")));
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
