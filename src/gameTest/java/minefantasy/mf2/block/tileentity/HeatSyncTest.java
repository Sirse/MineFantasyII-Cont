package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.Ticks.tickUntil;

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.gametest.Overrides;

/** What a forge tells clients about its heat: a crucible on it draws its flame and gauge from that alone. */
@GameTestHolder("minefantasy2")
public class HeatSyncTest {

    private HeatSyncTest() {}

    @GameTest
    public static void aForgeShowsItsWorkingHeatUntilItCools(GameTestHelper helper) {
        float[] air = { 40F };
        try (Overrides overrides = new Overrides().air((world, x, y, z) -> air[0])) {
            helper.setBlock(1, 1, 1, BlockListMF.forge_metal_active);
            TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 1, 1, 1);
            forge.updateEntity();
            forge.fuel = 100;
            forge.fuelTemperature = 600F;
            forge.onUsedWithBellows(1F);
            NBTTagCompound lit = forge.describe();
            assertEquals(forge.getWorkingHeat(), lit.getFloat("WorkingHeat"), 0F);
            assertTrue("a fired forge shows heat", lit.getFloat("WorkingHeat") > 0F);

            forge.fuel = 0;
            tickUntil(forge, () -> forge.getWorkingHeat() == 0F, 600);
            assertEquals(0F, forge.getWorkingHeat(), 0F);
            NBTTagCompound cold = forge.describe();
            assertEquals("a cold forge shows no heat", 0F, cold.getFloat("WorkingHeat"), 0F);
            assertTrue("the warm air still shows on the thermometer", cold.getFloat("Temperature") > 30F);
        }
        helper.succeed();
    }
}
