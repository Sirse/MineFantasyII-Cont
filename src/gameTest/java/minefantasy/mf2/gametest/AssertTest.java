package minefantasy.mf2.gametest;

import static minefantasy.mf2.gametest.Assert.*;

import com.gtnewhorizons.horizonqa.api.GameTestAssertException;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

/** The assertions the tests rely on fail when they should, NaN and infinities included. */
@GameTestHolder("minefantasy2")
public class AssertTest {

    private AssertTest() {}

    private static boolean fails(Runnable assertion) {
        try {
            assertion.run();
            return false;
        } catch (GameTestAssertException expected) {
            return true;
        }
    }

    @GameTest
    public static void aDeltaComparisonFailsOnNaNAndInfinities(GameTestHelper helper) {
        if (!fails(() -> assertEquals(0F, Float.NaN, 0F))) fail("NaN passed for 0");
        if (!fails(() -> assertEquals(Float.NaN, 0F, 0F))) fail("0 passed for NaN");
        if (!fails(() -> assertEquals(0F, Float.POSITIVE_INFINITY, 1F))) fail("infinity passed for 0");
        if (!fails(() -> assertEquals(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1F))) {
            fail("-infinity passed for +infinity");
        }
        if (!fails(() -> assertEquals(1F, 1.5F, 0.4F))) fail("a difference over the delta passed");

        assertEquals(Float.NaN, Float.NaN, 0F);
        assertEquals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0F);
        assertEquals(0F, -0F, 0F);
        assertEquals(1F, 1.25F, 0.25F);
        helper.succeed();
    }
}
