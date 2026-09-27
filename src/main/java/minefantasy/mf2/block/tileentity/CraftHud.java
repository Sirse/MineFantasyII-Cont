package minefantasy.mf2.block.tileentity;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * What every grid station shows of its work: the progress, the tool and research it needs, and what it makes. A station
 * adds its own fields beside these in the same tag, which goes out as the description packet and the state packet
 * alike; the keys are those saved worlds and clients already use.
 */
final class CraftHud {

    static final String PROGRESS = "Progress";
    static final String PROGRESS_MAX = "ProgressMax";
    static final String TOOL = "ToolNeeded";
    static final String RESEARCH = "Research";
    static final String RESULT = "Result";
    /** The tool tier; anvil and bench kept the name from when only hammers counted. */
    static final String TOOL_TIER = "HammerTier";

    float progress;
    float progressMax;
    String tool = "";
    String research = "";
    ItemStack result;

    private CraftHud() {}

    static void write(NBTTagCompound nbt, float progress, float progressMax, String tool, String research,
            ItemStack result) {
        nbt.setFloat(PROGRESS, progress);
        nbt.setFloat(PROGRESS_MAX, Math.max(0F, progressMax));
        nbt.setString(TOOL, tool == null ? "" : tool);
        nbt.setString(RESEARCH, research == null ? "" : research);
        if (result != null) {
            nbt.setTag(RESULT, result.writeToNBT(new NBTTagCompound()));
        }
    }

    static CraftHud read(NBTTagCompound nbt) {
        CraftHud hud = new CraftHud();
        hud.progress = nbt.getFloat(PROGRESS);
        hud.progressMax = Math.max(0F, nbt.getFloat(PROGRESS_MAX));
        hud.tool = nbt.getString(TOOL);
        hud.research = nbt.getString(RESEARCH);
        hud.result = nbt.hasKey(RESULT) ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(RESULT)) : null;
        return hud;
    }
}
