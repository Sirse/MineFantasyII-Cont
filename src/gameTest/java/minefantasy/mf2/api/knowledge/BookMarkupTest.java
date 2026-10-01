package minefantasy.mf2.api.knowledge;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.knowledge.client.BookMarkup;

/** The book's markup turns into the font's codes, and a stray code, even the last character, never shows as text. */
@GameTestHolder("minefantasy2")
public class BookMarkupTest {

    private BookMarkupTest() {}

    private static final String BOLD = EnumChatFormatting.BOLD.toString();
    private static final String RESET = EnumChatFormatting.RESET.toString();

    @GameTest
    public static void aHeadingIsBoldAndAParagraphBreakIsABlankLine(GameTestHelper helper) {
        assertEquals(BOLD + "Title" + RESET + "\n\nText", BookMarkup.parse("$hTitle$r^Text"));
        helper.succeed();
    }

    @GameTest
    public static void theLastCharacterIsReadLikeAnyOther(GameTestHelper helper) {
        assertEquals("Ends\n\n", BookMarkup.parse("Ends^"));
        assertEquals("Ends", BookMarkup.parse("Ends$"));
        assertEquals("Ends" + RESET, BookMarkup.parse("Ends$r"));
        helper.succeed();
    }

    @GameTest
    public static void anUnknownCodeIsDropped(GameTestHelper helper) {
        assertEquals("ab", BookMarkup.parse("a$qb"));
        helper.succeed();
    }
}
