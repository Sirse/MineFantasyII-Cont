package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.world.World;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.integration.sereneseasons.SereneSeasonsCompat;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.season.SeasonTime;

@GameTestHolder(value = "minefantasy2", requiredMods = "sereneseasons")
public class SereneSeasonsClimateTest {

    @GameTest
    public static void seasonalApiReadsWorldAndRecoversFromFailure(GameTestHelper helper) {
        ISeasonState[] current = { SeasonTime.ZERO };
        boolean[] broken = { false };
        int[] calls = { 0 };
        SeasonHelper.ISeasonDataProvider provider = new SeasonHelper.ISeasonDataProvider() {

            public ISeasonState getServerSeasonState(World world) {
                calls[0]++;
                if (broken[0]) throw new IllegalStateException("unsupported seasonal API");
                return current[0];
            }

            public ISeasonState getClientSeasonState() {
                return current[0];
            }
        };
        SereneSeasonsCompat.Bridge bridge = new SereneSeasonsCompat.Bridge();
        try (Overrides overrides = new Overrides()
                .set(() -> SeasonHelper.dataProvider, v -> SeasonHelper.dataProvider = v, provider)) {
            int duration = SeasonTime.ZERO.getSubSeasonDuration();
            current[0] = new SeasonTime(Season.SubSeason.MID_SUMMER.ordinal() * duration);
            assertEquals("MID_SUMMER", bridge.subSeason(helper.getWorld()));
            current[0] = new SeasonTime(Season.SubSeason.MID_WINTER.ordinal() * duration);
            assertEquals("MID_WINTER", bridge.subSeason(helper.getWorld()));
            current[0] = null;
            assertNull(bridge.subSeason(helper.getWorld()));
            broken[0] = true;
            assertNull(bridge.subSeason(helper.getWorld()));
            int checked = calls[0];
            assertNull(bridge.subSeason(helper.getWorld()));
            assertEquals(checked, calls[0]);
        }
        helper.succeed();
    }
}
