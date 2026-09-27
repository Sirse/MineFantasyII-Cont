package minefantasy.mf2.api.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.RecipeChecks;

/**
 * What a recipe asks of the tool, the station and the player, and how each station holds to it. The wrong tool or a
 * missing research always stops the work; a tool or station below the recipe's tier stops it or only makes it harder,
 * as the station's {@link Rules} say. Stations, {@code /mf recipes} and NEI all judge through here, so they agree.
 */
public final class Requirements {

    /** How a station treats a tier below the one the recipe needs. */
    public enum Tier {
        /** It cannot work the recipe. */
        REFUSE,
        /** It works the recipe, only harder. */
        PENALTY,
        /** The station has no such tier: a recipe asking for one is refused at registration. */
        NONE
    }

    /** How one kind of station holds to the requirements. */
    public static final class Rules {

        private final String station;
        private final Tier toolTier;
        private final Tier stationTier;
        private final boolean handsTakeAnyTool;

        public Rules(String station, Tier toolTier, Tier stationTier, boolean handsTakeAnyTool) {
            this.station = station;
            this.toolTier = toolTier;
            this.stationTier = stationTier;
            this.handsTakeAnyTool = handsTakeAnyTool;
        }

        public String getStation() {
            return station;
        }

        public Tier getToolTier() {
            return toolTier;
        }

        public Tier getStationTier() {
            return stationTier;
        }

        /** Whether a station of the given tier can work a recipe needing the other; one that only works harder can. */
        public boolean stationFits(int have, int need) {
            return stationTier != Tier.REFUSE || have >= need;
        }

        /** Why a station of the given tier cannot work a recipe needing the other, or null. */
        public CheckResult.Reason stationProblem(int have, int need) {
            return stationFits(have, need) ? null : CheckResult.Reason.tier(station, have, need);
        }

        /** Refuses a recipe asking for a tier the station does not have, which it would otherwise ignore. */
        public void validate(int toolTierNeeded, int stationTierNeeded) {
            RecipeChecks.require(
                    toolTier != Tier.NONE || toolTierNeeded <= 0,
                    "the " + station + " has no tool tier, got " + toolTierNeeded);
            RecipeChecks.require(
                    stationTier != Tier.NONE || stationTierNeeded <= 0,
                    "the " + station + " has no station tier, got " + stationTierNeeded);
        }
    }

    /** Hammers of any tier on any anvil: a weaker one only makes the hits count double and the hit window narrower. */
    public static final Rules ANVIL = new Rules("anvil", Tier.PENALTY, Tier.PENALTY, false);
    public static final Rules CARPENTER = new Rules("bench", Tier.REFUSE, Tier.REFUSE, false);
    /** A recipe for hands takes any tool too. */
    public static final Rules KITCHEN = new Rules("kitchen", Tier.REFUSE, Tier.NONE, true);
    /** The recipe's tier is the tool's; the rack itself has none. */
    public static final Rules TANNING = new Rules("tanning", Tier.REFUSE, Tier.NONE, false);
    public static final Rules CRUCIBLE = new Rules("crucible", Tier.NONE, Tier.REFUSE, false);
    public static final Rules QUERN = new Rules("quern", Tier.NONE, Tier.REFUSE, false);
    public static final Rules BIG_FURNACE = new Rules("furnace", Tier.NONE, Tier.REFUSE, false);

    /** The outcome of a check: what stops the work, else what makes it harder. */
    public static final class Verdict {

        private final CheckResult.Reason refusal;
        private final CheckResult.Reason weakTool;
        private final CheckResult.Reason weakStation;

        private Verdict(CheckResult.Reason refusal, CheckResult.Reason weakTool, CheckResult.Reason weakStation) {
            this.refusal = refusal;
            this.weakTool = weakTool;
            this.weakStation = weakStation;
        }

        public boolean allows() {
            return refusal == null;
        }

        /** What stops the work, or null. */
        public CheckResult.Reason getRefusal() {
            return refusal;
        }

        /** Whether the tool is below the recipe's tier on a station that only penalises it. */
        public boolean isToolWeak() {
            return weakTool != null;
        }

        public boolean isStationWeak() {
            return weakStation != null;
        }

        /** What makes the allowed work harder, or null. */
        public CheckResult.Reason getPenalty() {
            return weakTool != null ? weakTool : weakStation;
        }
    }

    private final String tool;
    private final int toolTier;
    private final int stationTier;
    private final String research;

    /**
     * @param tool     the tool type, or empty for any
     * @param research the research, or empty for none
     */
    public Requirements(String tool, int toolTier, int stationTier, String research) {
        this.tool = tool == null ? "" : tool;
        this.toolTier = toolTier;
        this.stationTier = stationTier;
        this.research = research == null ? "" : research;
    }

    /** What a planned craft asks for. */
    public static Requirements of(CraftPlan plan) {
        return new Requirements(
                plan.require(MFRecipeKeys.TOOL, ""),
                plan.require(MFRecipeKeys.TOOL_TIER, 0),
                plan.require(MFRecipeKeys.TIER, 0),
                plan.require(MFRecipeKeys.RESEARCH, ""));
    }

    public String getTool() {
        return tool;
    }

    public int getToolTier() {
        return toolTier;
    }

    public int getStationTier() {
        return stationTier;
    }

    public String getResearch() {
        return research;
    }

    /** Judges the player's held item, the station's tier and the player's research. */
    public Verdict check(Rules rules, EntityPlayer player, int stationTierHave) {
        ItemStack held = player == null ? null : player.getHeldItem();
        return check(rules, ToolHelper.getCrafterTool(held), ToolHelper.getCrafterTier(held), stationTierHave, player);
    }

    /**
     * @param player whose research counts; null checks no research, for a station working on its own
     */
    public Verdict check(Rules rules, String toolHave, int toolTierHave, int stationTierHave, EntityPlayer player) {
        CheckResult.Reason weakTool = null;
        CheckResult.Reason weakStation = null;
        CheckResult.Reason refusal = null;
        if (!toolFits(rules, toolHave)) {
            refusal = CheckResult.Reason.of("tool", tool, toolHave);
        } else if (toolTierHave < toolTier && rules.toolTier != Tier.NONE) {
            if (rules.toolTier == Tier.REFUSE) {
                refusal = CheckResult.Reason.tier("tool", toolTierHave, toolTier);
            } else {
                weakTool = CheckResult.Reason.harder("tool", toolTierHave, toolTier);
            }
        }
        if (refusal == null && stationTierHave < stationTier && rules.stationTier != Tier.NONE) {
            if (rules.stationTier == Tier.REFUSE) {
                refusal = CheckResult.Reason.tier(rules.station, stationTierHave, stationTier);
            } else {
                weakStation = CheckResult.Reason.harder(rules.station, stationTierHave, stationTier);
            }
        }
        if (refusal == null && player != null
                && !research.isEmpty()
                && !ResearchLogic.hasInfoUnlocked(player, research)) {
            refusal = CheckResult.Reason.of("research", research);
        }
        return new Verdict(refusal, weakTool, weakStation);
    }

    private boolean toolFits(Rules rules, String toolHave) {
        return tool.isEmpty() || tool.equalsIgnoreCase(toolHave)
                || (rules.handsTakeAnyTool && "hands".equalsIgnoreCase(tool));
    }
}
