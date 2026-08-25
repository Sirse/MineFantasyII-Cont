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
     * Hits required: 1 transforms instantly, >1 increments metadata each hit until outputMeta
     */
    public final int hits;
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
    }

    public boolean matches(Block block, int meta, String heldTool, int heldTier) {
        if (block != input) {
            return false;
        }
        if (inputMeta >= 0 && inputMeta != meta) {
            return false;
        }
        if (heldTier < toolTier) {
            return false;
        }
        return toolType.equalsIgnoreCase(heldTool);
    }
}
