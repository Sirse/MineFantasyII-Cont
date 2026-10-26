package minefantasy.mf2.entity;

import static minefantasy.mf2.gametest.Assert.assertFalse;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

/** Smoke thins out: under an open sky a puff is gone in its lifetime, not after rising out of the world. */
@GameTestHolder("minefantasy2")
public class SmokeLifetimeTest {

    private SmokeLifetimeTest() {}

    @GameTest(timeoutTicks = EntitySmoke.MAX_AGE + 100)
    public static void smokeUnderAnOpenSkyThinsOut(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 1, 1);
        EntitySmoke smoke = new EntitySmoke(
                helper.getWorld(),
                at.x() + 0.5D,
                at.y(),
                at.z() + 0.5D,
                0.0D,
                0.001D,
                0.0D);
        helper.getWorld().spawnEntityInWorld(smoke);
        int[] ticks = { 0 };
        helper.succeedWhen(() -> {
            if (++ticks[0] < EntitySmoke.MAX_AGE / 2) {
                assertFalse("the smoke was gone at once", smoke.isDead);
                return false;
            }
            return smoke.isDead;
        });
    }
}
