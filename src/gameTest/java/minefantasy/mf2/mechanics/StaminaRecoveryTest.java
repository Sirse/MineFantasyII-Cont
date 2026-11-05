package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.config.ConfigStamina;
import minefantasy.mf2.gametest.Players;

/** Stamina run down to nothing comes back once the player rests: it does not stay down or keep falling (#13). */
@GameTestHolder("minefantasy2")
public class StaminaRecoveryTest {

    private StaminaRecoveryTest() {}

    @GameTest
    public static void spentStaminaComesBackAtRest(GameTestHelper helper) {
        EntityPlayerMP player = Players.connected(helper, "Tired", null);
        player.setSprinting(false);
        StaminaBar.setStaminaValue(player, 0F);
        // Spent on an action: the pause before it comes back
        StaminaBar.setIdleTime(player, 60F);
        float max = StaminaBar.getTotalMaxStamina(player);

        float last = 0F;
        int ticks = (ConfigStamina.fullRegenSeconds + 10) * 20 + 60;
        for (int tick = 0; tick < ticks; tick++) {
            StaminaMechanics.tickEntity(player);
            float now = StaminaBar.getStaminaValue(player);
            assertTrue("stamina fell at rest: " + last + " > " + now, now >= last);
            last = now;
        }
        assertEquals("stamina did not come back at rest", max, last, 0.01F);
        helper.succeed();
    }
}
