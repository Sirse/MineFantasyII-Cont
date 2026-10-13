package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

/** Stamina flowing reaches the client every fourth tick; what the player must notice at once goes at once. */
@GameTestHolder("minefantasy2")
public class StaminaSyncTest {

    private StaminaSyncTest() {}

    private static float[] bar(float stamina, float max, float flash, float bonus) {
        return new float[] { stamina, max, flash, bonus };
    }

    @GameTest
    public static void flowingStaminaIsSentEveryFourthTick(GameTestHelper helper) {
        float[] last = bar(50, 100, 0, 0);
        assertTrue("the first", StaminaMechanics.shouldSync(null, last, 1));
        assertFalse("unchanged", StaminaMechanics.shouldSync(last, bar(50, 100, 0, 0), 4));
        assertFalse("flowing between", StaminaMechanics.shouldSync(last, bar(51, 100, 0, 0), 5));
        assertTrue("flowing on the fourth", StaminaMechanics.shouldSync(last, bar(51, 100, 0, 0), 8));
        assertTrue("the heartbeat", StaminaMechanics.shouldSync(last, bar(50, 100, 0, 0), 60));
        helper.succeed();
    }

    @GameTest
    public static void whatMattersIsSentAtOnce(GameTestHelper helper) {
        float[] last = bar(50, 100, 0, 0);
        assertTrue("ran out", StaminaMechanics.shouldSync(last, bar(0, 100, 0, 0), 5));
        assertTrue("filled up", StaminaMechanics.shouldSync(bar(99, 100, 0, 0), bar(100, 100, 0, 0), 5));
        assertTrue("max", StaminaMechanics.shouldSync(last, bar(50, 110, 0, 0), 5));
        assertTrue("bonus", StaminaMechanics.shouldSync(last, bar(50, 100, 0, 10), 5));
        assertTrue("flash starts", StaminaMechanics.shouldSync(last, bar(50, 100, 20, 0), 5));
        assertTrue("flash ends", StaminaMechanics.shouldSync(bar(50, 100, 1, 0), last, 5));
        assertFalse("flash counts down", StaminaMechanics.shouldSync(bar(50, 100, 20, 0), bar(50, 100, 19, 0), 5));
        assertTrue("out after a refill", StaminaMechanics.shouldSync(bar(0, 100, 0, 0), bar(1, 100, 0, 0), 5));
        helper.succeed();
    }
}
