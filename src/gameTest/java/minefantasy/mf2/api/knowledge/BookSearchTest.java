package minefantasy.mf2.api.knowledge;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.knowledge.client.BookSearch;

/**
 * The research book's search on its own: typed, then browsed through its finds beside the chosen entry's button, back
 * to typing from the glass, ended by Escape or by a choice made elsewhere, and its finds kept until they can change.
 */
@GameTestHolder("minefantasy2")
public class BookSearchTest {

    private BookSearchTest() {}

    private static final List<String> NAMES = Arrays.asList("iron ingot", "iron plate", "bronze", "iron hilt");

    /** A search over the names above, counting how often it really looks. */
    private static final class Names implements BookSearch.Finder<String> {

        int looks;

        @Override
        public List<String> find(String query) {
            looks++;
            List<String> found = new ArrayList<String>();
            for (String name : NAMES) {
                if (name.contains(query)) {
                    found.add(name);
                }
            }
            return found;
        }
    }

    private static BookSearch<String> typed(String query) {
        BookSearch<String> search = new BookSearch<String>(new Names());
        search.toggle();
        for (char c : query.toCharArray()) {
            search.type(c);
        }
        return search;
    }

    @GameTest
    public static void typingCountsTheFindsBeforeGoingToAny(GameTestHelper helper) {
        BookSearch<String> search = typed("iron");
        assertTrue(search.isTyping());
        assertEquals("3", search.counter(""));
        helper.succeed();
    }

    @GameTest
    public static void goingToAFindBrowsesAndCountsWhichOfHowMany(GameTestHelper helper) {
        BookSearch<String> search = typed("iron");
        assertEquals("iron ingot", search.step(1, ""));
        assertTrue("browsing: the field gives way to the chosen entry's button", search.isBrowsing());
        assertEquals("1/3", search.counter(""));
        assertEquals("iron plate", search.step(1, ""));
        assertEquals("iron hilt", search.step(1, ""));
        assertEquals("round from the last to the first", "iron ingot", search.step(1, ""));
        helper.succeed();
    }

    @GameTest
    public static void backFromNoneGoesToTheLast(GameTestHelper helper) {
        assertEquals("iron hilt", typed("iron").step(-1, ""));
        helper.succeed();
    }

    @GameTest
    public static void theGlassWhileBrowsingBringsTheQueryBack(GameTestHelper helper) {
        BookSearch<String> search = typed("iron");
        search.step(1, "");
        search.step(1, "");
        search.toggle();
        assertTrue(search.isTyping());
        assertEquals("iron", search.query());
        assertEquals("the place among the finds is kept", "2/3", search.counter(""));
        helper.succeed();
    }

    @GameTest
    public static void theGlassWhileTypingCloses(GameTestHelper helper) {
        BookSearch<String> search = typed("iron");
        search.toggle();
        assertFalse(search.isOpen());
        helper.succeed();
    }

    @GameTest
    public static void escapeEndsTheSearchWhetherTypingOrBrowsing(GameTestHelper helper) {
        BookSearch<String> search = typed("iron");
        search.close();
        assertFalse(search.isOpen());
        assertEquals("", search.query());
        search = typed("iron");
        search.step(1, "");
        search.close();
        assertFalse(search.isOpen());
        assertEquals(-1, search.selected());
        helper.succeed();
    }

    @GameTest
    public static void reopeningAfterItEndedStartsAfresh(GameTestHelper helper) {
        BookSearch<String> search = typed("iron");
        search.step(1, "");
        search.close();
        search.toggle();
        assertTrue(search.isTyping());
        assertEquals("", search.query());
        assertEquals("0", search.counter(""));
        helper.succeed();
    }

    @GameTest
    public static void aChoiceElsewhereEndsBrowsingButNotTyping(GameTestHelper helper) {
        BookSearch<String> search = typed("iron");
        search.choseElsewhere();
        assertTrue("a search being typed stays open whatever card is chosen", search.isTyping());
        search.step(1, "");
        search.choseElsewhere();
        assertFalse("another category or entry chosen: only its button stays", search.isOpen());
        helper.succeed();
    }

    @GameTest
    public static void editingTheQueryForgetsWhichFindWasShown(GameTestHelper helper) {
        BookSearch<String> search = typed("iron");
        search.step(1, "");
        search.toggle();
        search.type(' ');
        assertEquals(-1, search.selected());
        helper.succeed();
    }

    @GameTest
    public static void findsAreLookedForOnlyWhenTheyCanHaveChanged(GameTestHelper helper) {
        Names names = new Names();
        BookSearch<String> search = new BookSearch<String>(names);
        search.toggle();
        search.type('i');
        search.results("en_US:3");
        search.results("en_US:3");
        search.counter("en_US:3");
        assertEquals(1, names.looks);
        search.results("en_US:4");
        assertEquals("something learned", 2, names.looks);
        search.results("ru_RU:4");
        assertEquals("the language changed", 3, names.looks);
        search.type('r');
        search.results("ru_RU:4");
        assertEquals("the query changed", 4, names.looks);
        helper.succeed();
    }
}
