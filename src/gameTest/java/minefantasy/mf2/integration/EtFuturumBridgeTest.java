package minefantasy.mf2.integration;

import static minefantasy.mf2.gametest.Assert.*;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.config.ConfigIntegration;
import minefantasy.mf2.integration.etfuturum.EtFuturumCompat;

/**
 * Without Et Futurum Requiem installed, the bridge stays idle: the mod guard and the config toggle both return before
 * any Et Futurum class is touched, so the shared loadComplete path never trips over the missing mod.
 */
@GameTestHolder("minefantasy2")
public class EtFuturumBridgeTest {

    private EtFuturumBridgeTest() {}

    @GameTest
    public static void theBridgeStaysIdleWhenEtFuturumIsAbsent(GameTestHelper helper) {
        boolean integration = ConfigIntegration.etfuturumIntegration;
        boolean blacklist = ConfigIntegration.etfuturumBlastBlacklist;
        try {
            ConfigIntegration.etfuturumIntegration = true;
            ConfigIntegration.etfuturumBlastBlacklist = true;
            EtFuturumCompat.load();
            ConfigIntegration.etfuturumIntegration = false;
            EtFuturumCompat.load();
        } finally {
            ConfigIntegration.etfuturumIntegration = integration;
            ConfigIntegration.etfuturumBlastBlacklist = blacklist;
        }
        helper.succeed();
    }
}
