package minefantasy.mf2.api.knowledge;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Items;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.gametest.Modders;

/**
 * An entry's standing tells apart what can be learned right now from what only looks within reach: one a skill short,
 * one studied at a research table, one whose parent is not known.
 */
@GameTestHolder("minefantasy2")
public class ResearchAvailabilityTest {

    private ResearchAvailabilityTest() {}

    /** An entry of the test's own, not registered, so no book or player already holds it. */
    private static InformationBase entry(String name, int artefacts, InformationBase parent) {
        return new InformationBase("availability_test_" + name, 0, 0, artefacts, Items.book, parent);
    }

    @GameTest
    public static void anEntryWithAKnownParentAndNoNeedsCanBeBought(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
        InformationBase parent = entry("parent", 0, null);
        InformationBase child = entry("child", 0, parent);

        assertEquals(ResearchAvailability.BUYABLE, ResearchAvailability.of(player, parent));
        assertEquals(ResearchAvailability.LOCKED, ResearchAvailability.of(player, child));
        ResearchLogic.forceUnlock(player, parent);
        assertEquals(ResearchAvailability.KNOWN, ResearchAvailability.of(player, parent));
        assertEquals(ResearchAvailability.BUYABLE, ResearchAvailability.of(player, child));
        helper.succeed();
    }

    @GameTest
    public static void anEntryStudiedAtATableIsNotOneToBuy(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
        boolean easy = InformationBase.easyResearch;
        InformationBase.easyResearch = false;
        try {
            ResearchAvailability state = ResearchAvailability.of(player, entry("table", 2, null));
            assertEquals(ResearchAvailability.AT_TABLE, state);
            assertTrue("a table entry counts as out of reach", state.isReachable());
        } finally {
            InformationBase.easyResearch = easy;
        }
        helper.succeed();
    }

    @GameTest
    public static void anEntryASkillShortIsNotOneToBuy(GameTestHelper helper) {
        if (!RPGElements.isSystemActive) {
            // Without skills there is nothing to fall short of
            helper.succeed();
            return;
        }
        FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
        InformationBase entry = entry("skill", 0, null).addSkill(SkillList.artisanry, 1000);
        assertEquals(ResearchAvailability.NEEDS_SKILL, ResearchAvailability.of(player, entry));
        helper.succeed();
    }
}
