package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.heating.AmbientTemperature;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.integration.enviromine.EnviroMineCompat;
import minefantasy.mf2.integration.sereneseasons.SereneSeasonsCompat;

@GameTestHolder(value = "minefantasy2", requiredMods = "sereneseasons")
public class SereneSeasonsInstalledTest {

    @GameTest
    public static void installedSeasonalClimateReachesColdForge(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.forge);
        TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 1, 1, 1);
        SereneSeasonsCompat.Bridge bridge = new SereneSeasonsCompat.Bridge();
        assertNotNull(bridge.subSeason(helper.getWorld()));
        float climate = bridge.biomeTemperature(
                helper.getWorld(),
                helper.getWorld().getBiomeGenForCoords(forge.xCoord, forge.zCoord),
                forge.xCoord,
                forge.yCoord,
                forge.zCoord);
        assertTrue("real seasonal API failed", Float.isFinite(climate));
        SereneSeasonsCompat.load();
        AmbientTemperature.setProvider(null);
        try {
            float expected = Math.max(
                    -100F,
                    Math.min(100F, AmbientTemperature.temperate + (climate - .8F) * AmbientTemperature.biomeScale));
            forge.updateEntity();
            assertEquals(expected, forge.temperature, .001F);
        } finally {
            EnviroMineCompat.load();
        }
        helper.succeed();
    }
}
