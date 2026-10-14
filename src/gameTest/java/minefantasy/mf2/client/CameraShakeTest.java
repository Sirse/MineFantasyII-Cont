package minefantasy.mf2.client;

import static minefantasy.mf2.gametest.Assert.*;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

/** A blast shakes the camera less the farther it is, not at all past its reach, and the shake dies down. */
@GameTestHolder("minefantasy2")
public class CameraShakeTest {

    private CameraShakeTest() {}

    @GameTest
    public static void aBlastShakesLessFartherAway(GameTestHelper helper) {
        float near = CameraShake.traumaOf(4F, 1D);
        float far = CameraShake.traumaOf(4F, 8D);
        assertTrue("a blast close by shook no harder: " + near + " against " + far, near > far && far > 0F);
        assertTrue("even a blast at the camera shakes it full", CameraShake.traumaOf(4F, 0D) < 1F);
        assertEquals("a blast shook past its reach", 0F, CameraShake.traumaOf(4F, 4F * CameraShake.REACH), 0F);
        helper.succeed();
    }

    @GameTest
    public static void aShakeDiesDown(GameTestHelper helper) {
        CameraShake.scale = 1F;
        CameraShake.blast(4F, 0D, 100F);
        assertNotNull("a blast at the camera did not shake it", CameraShake.angles(101F));
        assertNull("the shake never died down", CameraShake.angles(100F + CameraShake.FADE_TICKS));
        helper.succeed();
    }

    @GameTest
    public static void noShakeWhenTurnedOff(GameTestHelper helper) {
        try {
            CameraShake.scale = 0F;
            CameraShake.blast(4F, 0D, 1000F);
            assertNull("the camera shook with shaking turned off", CameraShake.angles(1001F));
        } finally {
            CameraShake.scale = 1F;
        }
        helper.succeed();
    }

    /** A younger world's clock is behind the old one: the old shake must not come back growing. */
    @GameTest
    public static void anOldShakeStaysBehindInAnotherWorld(GameTestHelper helper) {
        CameraShake.scale = 1F;
        CameraShake.blast(4F, 0D, 5000F);
        assertNull("the shake came back in a younger world", CameraShake.angles(10F));
        helper.succeed();
    }

    @GameTest
    public static void turningShakeOffStopsAShakeUnderway(GameTestHelper helper) {
        try {
            CameraShake.scale = 1F;
            CameraShake.blast(4F, 0D, 2000F);
            CameraShake.scale = 0F;
            assertNull("the shake went on after it was turned off", CameraShake.angles(2001F));
        } finally {
            CameraShake.scale = 1F;
        }
        helper.succeed();
    }

    /** An old world counts tens of millions of ticks; the shake still moves between them, not in steps. */
    @GameTest
    public static void aShakeMovesSmoothlyInAnOldWorld(GameTestHelper helper) {
        CameraShake.scale = 1F;
        double old = 30_000_000D;
        CameraShake.blast(4F, 0D, old);
        float[] before = CameraShake.angles(old + 1D);
        float[] between = CameraShake.angles(old + 1.25D);
        assertNotNull("an old world's blast did not shake", before);
        assertTrue("the shake stood still within a tick", before[2] != between[2]);
        helper.succeed();
    }
}
