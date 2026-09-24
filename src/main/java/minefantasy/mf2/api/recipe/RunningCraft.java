package minefantasy.mf2.api.recipe;

import net.minecraft.nbt.NBTTagCompound;

/**
 * The craft a timed station has started, kept as the fingerprint of its plan. The station re-checks its contents while
 * it works and carries on only while the new plan has the same fingerprint, so taking items out or a script replacing
 * the recipe stops the work instead of paying out the old result. Saves from 2.x carry no fingerprint and stop too.
 */
public final class RunningCraft {

    /** Save format; absent in 2.x saves. */
    public static final int FORMAT = 3;

    private NBTTagCompound fingerprint;

    /** Starts, or with null clears, the craft. */
    public void start(CraftPlan plan) {
        fingerprint = plan == null ? null : plan.fingerprint();
    }

    public void clear() {
        fingerprint = null;
    }

    public boolean isRunning() {
        return fingerprint != null;
    }

    /** Whether the check still yields the started plan. */
    public boolean holds(CheckResult result) {
        return fingerprint != null && result.isSuccess() && result.getPlan().matchesFingerprint(fingerprint);
    }

    /** Whether the plan is the started one. */
    public boolean holds(CraftPlan plan) {
        return fingerprint != null && plan != null && plan.matchesFingerprint(fingerprint);
    }

    public void write(NBTTagCompound nbt) {
        nbt.setInteger("RecipeFormat", FORMAT);
        if (fingerprint != null) {
            nbt.setTag("Project", fingerprint);
        }
    }

    public void read(NBTTagCompound nbt) {
        fingerprint = nbt.getInteger("RecipeFormat") == FORMAT && nbt.hasKey("Project") ? nbt.getCompoundTag("Project")
                : null;
    }
}
