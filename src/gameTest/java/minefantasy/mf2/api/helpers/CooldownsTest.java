package minefantasy.mf2.api.helpers;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.gametest.Modders;

/** Countdowns run down to nothing and go; a stamp lets the first action through, then waits out its interval. */
@GameTestHolder("minefantasy2")
public class CooldownsTest {

    private CooldownsTest() {}

    @GameTest
    public static void aCountdownRunsOutAndGoes(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        assertEquals(0, Cooldowns.left(player, "test"));
        Cooldowns.set(player, "test", 2);
        Cooldowns.tick(player, "test");
        assertEquals(1, Cooldowns.left(player, "test"));
        Cooldowns.tick(player, "test");
        Cooldowns.tick(player, "test");
        assertEquals(0, Cooldowns.left(player, "test"));
        assertFalse("a run-out countdown stayed in the data", player.getEntityData().hasKey("test"));
        player.getEntityData().setInteger("test", -5);
        assertEquals("a forged negative countdown counted", 0, Cooldowns.left(player, "test"));
        helper.succeed();
    }

    @GameTest
    public static void aStampWaitsOutItsInterval(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        assertTrue("the first action was held back", Cooldowns.pass(player, "stamp", 2));
        assertFalse("a second action in the same tick passed", Cooldowns.pass(player, "stamp", 2));
        long now = player.worldObj.getTotalWorldTime();
        player.getEntityData().setLong("stamp", now - 2);
        assertTrue("the action was held back past its interval", Cooldowns.pass(player, "stamp", 2));
        helper.succeed();
    }
}
