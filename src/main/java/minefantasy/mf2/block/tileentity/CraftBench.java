package minefantasy.mf2.block.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.recipe.CheckResult;

/** A bench worked by hand, as its window shows it: the project, its progress and what it asks of tool and bench. */
public interface CraftBench {

    boolean doesPlayerKnowCraft(EntityPlayer player);

    boolean hasProject();

    ItemStack getShownResult();

    /** The project's name, or a lang key starting with "gui." when it has none of its own. */
    String getResultName();

    /** The tool the project needs, or null for none. */
    String getToolNeeded();

    int getToolTierNeeded();

    float getProgress();

    float getProgressMax();

    /** Reads the current project's refusal or penalty without refreshing recipes or changing progress. */
    CheckResult.Reason getWorkProblem(EntityPlayer player);

    /** The bench tier the project needs; -1 for any. */
    int getBenchTierNeeded();

    /** Whether this bench is good enough for the project; only an anvil of too low a tier is not. */
    default boolean isBenchSufficient() {
        return true;
    }
}
