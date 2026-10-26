package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldSettings;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.helpers.PlayerModeHelper;
import minefantasy.mf2.entity.mob.EntityDragon;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.gametest.Players;

/** A dragon hunts players at play: a creative player or a spectator is never called on. */
@GameTestHolder("minefantasy2")
public class DragonSpawnTest {

    /** Stands in for Et Futurum's spectator gametype, as in {@code SpectatorModeTest}. */
    private static final WorldSettings.GameType FAKE_SPECTATOR = WorldSettings.GameType.ADVENTURE;
    private static final int HEIGHT = 3;

    private DragonSpawnTest() {}

    /** Calls a dragon over a player in the given gametype; returns how many came, and sends them away. */
    private static int dragonsCalled(GameTestHelper helper, WorldSettings.GameType type) {
        EntityPlayerMP player = Players.connected(helper, "Hunted", null);
        // Connected, as the warning the dragon brings is sent to the player
        player.theItemInWorldManager.setGameType(type);
        TestPos at = helper.absolute(1, 1, 1);
        player.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        // A step since the last call, which the spawn waits for
        player.distanceWalkedOnStepModified = player.getEntityData().getInteger("MF_LastStep") + 1;
        Set<Object> before = new HashSet<>(helper.getWorld().loadedEntityList);
        PlayerTickHandlerMF.spawnDragon(player, HEIGHT);
        int called = 0;
        for (Object entity : new ArrayList<>(helper.getWorld().loadedEntityList)) {
            if (entity instanceof EntityDragon && !before.contains(entity)) {
                ((EntityDragon) entity).setDead();
                called++;
            }
        }
        return called;
    }

    @GameTest
    public static void noDragonHuntsACreativePlayer(GameTestHelper helper) {
        assertTrue("a dragon came for a creative player", dragonsCalled(helper, WorldSettings.GameType.CREATIVE) == 0);
        assertTrue("no dragon came for a survival player", dragonsCalled(helper, WorldSettings.GameType.SURVIVAL) == 1);
        helper.succeed();
    }

    @GameTest
    public static void noDragonHuntsASpectator(GameTestHelper helper) {
        try (Overrides overrides = new Overrides().run(
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(FAKE_SPECTATOR),
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(null))) {
            assertTrue("a dragon came for a spectator", dragonsCalled(helper, FAKE_SPECTATOR) == 0);
        }
        helper.succeed();
    }
}
