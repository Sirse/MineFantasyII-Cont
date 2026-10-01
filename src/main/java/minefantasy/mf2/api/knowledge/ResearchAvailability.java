package minefantasy.mf2.api.knowledge;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Where an entry stands for a player, worked out once so that everything showing it agrees: the book's card and learn
 * button, the list of what can be learned now, the glow on the map and the flame on a category's ribbon.
 */
public enum ResearchAvailability {

    /** The player knows it. */
    KNOWN,
    /** Its parent is known, its skills are met, and it is bought from the book: it can be learned right now. */
    BUYABLE,
    /** Bought from the book and its parent is known, but a skill it needs is not yet high enough. */
    NEEDS_SKILL,
    /** Its parent is known, but it is learned by study at a research table, not bought from the book. */
    AT_TABLE,
    /** Its parent is not known yet. */
    LOCKED;

    public static ResearchAvailability of(EntityPlayer player, InformationBase entry) {
        if (ResearchLogic.hasInfoUnlocked(player, entry)) {
            return KNOWN;
        }
        if (!ResearchLogic.canUnlockInfo(player, entry)) {
            return LOCKED;
        }
        if (!entry.isEasy()) {
            return AT_TABLE;
        }
        if (!entry.hasSkillsUnlocked(player)) {
            return NEEDS_SKILL;
        }
        return ResearchLogic.canPurchase(player, entry) ? BUYABLE : LOCKED;
    }

    /** Whether its parent is known, so that it is within reach, whatever else it still needs. */
    public boolean isReachable() {
        return this == BUYABLE || this == NEEDS_SKILL || this == AT_TABLE;
    }
}
