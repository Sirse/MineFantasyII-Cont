package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.heating.AmbientTemperature;
import minefantasy.mf2.api.heating.NearbyBlockTemperature;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.integration.enviromine.EnviroMineCompat;

@GameTestHolder(value = "minefantasy2", requiredMods = "enviromine")
public class EnviroMineInstalledTest {

    @GameTest
    public static void installedBlockProfilesReachAmbientTemperature(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.forge);
        helper.setBlock(2, 1, 1, net.minecraft.init.Blocks.lava);
        TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 1, 1, 1);
        EnviroMineCompat.BlockClimate profiles = new EnviroMineCompat.BlockClimate();
        float lava = profiles.temperature(net.minecraft.init.Blocks.lava, 0);
        assertTrue("installed EnviroMine has no finite lava profile", Float.isFinite(lava));
        assertTrue("installed lava profile does not warm air", lava > 0);
        EnviroMineCompat.load();
        float adjustment = NearbyBlockTemperature.get(helper.getWorld(), forge.xCoord, forge.yCoord, forge.zCoord);
        assertTrue(adjustment > 0 && adjustment <= 40);
        float climate = new EnviroMineCompat.Climate().get(helper.getWorld(), forge.xCoord, forge.yCoord, forge.zCoord);
        forge.updateEntity();
        assertEquals(Math.max(-100F, Math.min(100F, climate + adjustment)), forge.temperature, .001F);
        assertEquals(0, forge.getHeat());
        helper.succeed();
    }

    @GameTest
    public static void installedApiSuppliesTheForgeAmbientTemperature(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.forge);
        TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 1, 1, 1);
        EnviroMineCompat.Climate climate = new EnviroMineCompat.Climate();
        float expected = climate.get(helper.getWorld(), forge.xCoord, forge.yCoord, forge.zCoord);
        assertTrue("installed API returned no climate", Float.isFinite(expected));
        // Earlier tests may temporarily replace the provider; install again for this real-mod path.
        EnviroMineCompat.load();
        expected += NearbyBlockTemperature.get(helper.getWorld(), forge.xCoord, forge.yCoord, forge.zCoord);
        assertEquals(
                Math.max(-100F, Math.min(100F, expected)),
                AmbientTemperature.get(helper.getWorld(), forge.xCoord, forge.yCoord, forge.zCoord),
                0.001F);
        forge.updateEntity();
        assertEquals(Math.max(-100F, Math.min(100F, expected)), forge.temperature, 0.001F);
        helper.succeed();
    }
}
