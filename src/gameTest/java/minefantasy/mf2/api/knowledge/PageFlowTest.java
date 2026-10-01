package minefantasy.mf2.api.knowledge;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.knowledge.client.PageFlow;

/**
 * An entry's text runs on over as many pages as it needs, the first shorter for the heading, and other pages keep their
 * place between the runs of text.
 */
@GameTestHolder("minefantasy2")
public class PageFlowTest {

    private PageFlowTest() {}

    private static List<String> lines(int count) {
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            lines.add("line " + i);
        }
        return lines;
    }

    @SafeVarargs
    private static List<PageFlow.Page> lay(int firstRoom, int room, List<String>... sources) {
        return PageFlow.lay(Arrays.asList(sources), firstRoom, room);
    }

    @GameTest
    public static void textThatFillsThePageExactlyTakesOnePage(GameTestHelper helper) {
        assertEquals(1, lay(5, 5, lines(5)).size());
        List<PageFlow.Page> pages = lay(5, 5, lines(6));
        assertEquals(2, pages.size());
        assertEquals(Arrays.asList("line 5"), pages.get(1).lines);
        helper.succeed();
    }

    @GameTest
    public static void theFirstPageLeavesRoomForTheHeading(GameTestHelper helper) {
        List<PageFlow.Page> pages = lay(3, 5, lines(8));
        assertEquals(2, pages.size());
        assertEquals(3, pages.get(0).lines.size());
        assertEquals(5, pages.get(1).lines.size());
        helper.succeed();
    }

    @GameTest
    public static void aCarriedOnPageStartsWithTextNotABlankLine(GameTestHelper helper) {
        List<PageFlow.Page> pages = lay(2, 2, Arrays.asList("a", "b", "", "", "c"));
        assertEquals(2, pages.size());
        assertEquals(Arrays.asList("c"), pages.get(1).lines);
        helper.succeed();
    }

    @GameTest
    public static void aRecipeKeepsItsPlaceBetweenRunsOfText(GameTestHelper helper) {
        List<PageFlow.Page> pages = lay(2, 2, lines(3), null, lines(1));
        assertEquals(4, pages.size());
        assertFalse("the text runs on", pages.get(0).endsText);
        assertTrue("the text ends before the recipe", pages.get(1).endsText);
        assertNull(pages.get(2).lines);
        assertEquals(1, pages.get(2).source);
        assertTrue("the last text ends the entry", pages.get(3).endsText);
        assertEquals(2, pages.get(3).source);
        helper.succeed();
    }

    @GameTest
    public static void textPagesInARowEndTheirTextOnlyAtTheLast(GameTestHelper helper) {
        List<PageFlow.Page> pages = lay(5, 5, lines(2), lines(2));
        assertEquals(2, pages.size());
        assertFalse("more text follows", pages.get(0).endsText);
        assertTrue(pages.get(1).endsText);
        helper.succeed();
    }

    @GameTest
    public static void emptyTextTakesNoPage(GameTestHelper helper) {
        assertEquals(0, lay(5, 5, Arrays.asList("", " ")).size());
        helper.succeed();
    }
}
