package minefantasy.mf2.api.crafting.transformation;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.rpg.Skill;

/**
 * A world-level block transformation: hitting a block with the proper tool turns it into another block, optionally
 * through metadata stages (progressive chopping).
 *
 * Progressive recipes store the stage counter in the block metadata, so they may only target blocks whose metadata
 * range is free. Direct one-hit recipes may transform anything and can carry the input metadata over to the output.
 */
public class TransformationRecipe {

    public final Block input;
    /**
     * Required input metadata, or -1 for any metadata
     */
    public final int inputMeta;
    public final Block output;
    public final int outputMeta;
    /**
     * When true the output keeps the metadata the input block had
     */
    public final boolean copyMeta;
    public final String toolType;
    public final int toolTier;
    /**
     * Hits required: 1 transforms instantly; >1 is a progressive recipe that stores the hit counter in the block
     * metadata (inputMeta + hits taken so far), so it must declare an inputMeta >= 0
     */
    public final int hits;
    /**
     * Bit mask applied to the copied metadata when copyMeta is true, or -1 to copy it unchanged. Use this for inputs
     * whose metadata carries extra state (e.g. log orientation bits)
     */
    public final int copyMetaMask;
    /**
     * Consumed from the player inventory on every hit, or null
     */
    public final ItemStack consumable;
    /**
     * Dropped on every hit, or null
     */
    public final ItemStack dropPerHit;
    public final String sound;
    public final Skill skill;
    public final int skillXp;
    public final String research;
    public final int outputCount;

    public TransformationRecipe(Block input, int inputMeta, Block output, int outputMeta, boolean copyMeta,
            String toolType, int toolTier, int hits, ItemStack consumable, ItemStack dropPerHit, String sound,
            Skill skill, int skillXp, String research, int outputCount) {
        this(
                input,
                inputMeta,
                output,
                outputMeta,
                copyMeta,
                toolType,
                toolTier,
                hits,
                consumable,
                dropPerHit,
                sound,
                skill,
                skillXp,
                research,
                outputCount,
                -1);
    }

    public TransformationRecipe(Block input, int inputMeta, Block output, int outputMeta, boolean copyMeta,
            String toolType, int toolTier, int hits, ItemStack consumable, ItemStack dropPerHit, String sound,
            Skill skill, int skillXp, String research, int outputCount, int copyMetaMask) {
        if (hits > 1 && inputMeta < 0) {
            throw new IllegalArgumentException(
                    "Progressive transformation recipes require a fixed inputMeta to store the stage counter");
        }
        this.input = input;
        this.inputMeta = inputMeta;
        this.output = output;
        this.outputMeta = outputMeta;
        this.copyMeta = copyMeta;
        this.toolType = toolType;
        this.toolTier = toolTier;
        this.hits = Math.max(1, hits);
        this.consumable = consumable;
        this.dropPerHit = dropPerHit;
        this.sound = sound;
        this.skill = skill;
        this.skillXp = skillXp;
        this.research = research;
        this.outputCount = Math.max(1, outputCount);
        this.copyMetaMask = copyMetaMask;
    }

    public boolean matches(Block block, int meta, String heldTool, int heldTier) {
        if (block != input) {
            return false;
        }
        if (hits > 1) {
            // Progressive: metadata holds the hit counter, so accept any stage of this recipe
            if (meta < inputMeta || meta >= inputMeta + hits) {
                return false;
            }
        } else if (inputMeta >= 0 && inputMeta != meta) {
            return false;
        }
        if (heldTier < toolTier) {
            return false;
        }
        return toolType.equalsIgnoreCase(heldTool);
    }

    /**
     * Output metadata for a final hit: either the recipe's fixed meta or the input meta (optionally masked), offset by
     * the recipe's output meta
     */
    public int getOutputMeta(int inputBlockMeta) {
        if (!copyMeta) {
            return outputMeta;
        }
        return outputMeta + (copyMetaMask >= 0 ? inputBlockMeta & copyMetaMask : inputBlockMeta);
    }
}
