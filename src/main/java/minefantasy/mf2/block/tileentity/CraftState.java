package minefantasy.mf2.block.tileentity;

import net.minecraft.nbt.NBTTagCompound;

import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.RunningCraft;

/**
 * The work of a timed station. The station hands over the plan its contents make whenever it looks its recipe up; the
 * progress it keeps belongs to that plan only, and another plan, or none, starts the work over.
 */
final class CraftState {

    private final RunningCraft running = new RunningCraft();

    /**
     * Takes the plan the station's contents make now as the work.
     *
     * @return whether the progress carries on: false when the plan is another than the started one, or there is none
     */
    boolean follow(CraftPlan plan) {
        boolean same = running.holds(plan);
        running.start(plan);
        return same;
    }

    /** Whether the plan is the started one. */
    boolean holds(CraftPlan plan) {
        return running.holds(plan);
    }

    boolean isRunning() {
        return running.isRunning();
    }

    void write(NBTTagCompound nbt) {
        running.write(nbt);
    }

    void read(NBTTagCompound nbt) {
        running.read(nbt);
    }
}
