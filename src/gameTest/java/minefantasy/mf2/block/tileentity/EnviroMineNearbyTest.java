package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.Ticks.tick;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.heating.NearbyBlockTemperature;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.integration.enviromine.EnviroMineCompat;

@GameTestHolder("minefantasy2")
public class EnviroMineNearbyTest {

    @GameTest
    public static void coldForgeUsesExternalBlocksWithoutSupplyingWorkHeat(GameTestHelper helper) {
        try (Overrides overrides = new Overrides()
                .set(() -> EnviroMineCompat.enabled, v -> EnviroMineCompat.enabled = v, true)
                .set(() -> NearbyBlockTemperature.enabled, v -> NearbyBlockTemperature.enabled = v, true)
                .air((world, x, y, z) -> 20F).run(() -> {}, () -> NearbyBlockTemperature.setProfile(null))) {
            helper.setBlock(1, 1, 1, BlockListMF.forge);
            helper.setBlock(2, 1, 1, Blocks.lava);
            TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 1, 1, 1);
            // Without the installed integration's profile, even lava has no local effect.
            NearbyBlockTemperature.setProfile(null);
            assertEquals(20F, forge.getAmbientTemperature(), 0F);
            NearbyBlockTemperature.setProfile((block, meta) -> block == Blocks.lava ? 30 : Float.NaN);
            forge.setInventorySlotContents(0, new ItemStack(Items.iron_ingot));
            tick(forge, 40);
            assertEquals(50F, forge.temperature, 0F);
            assertEquals(0, forge.getHeat());
            assertEquals(Items.iron_ingot, forge.getStackInSlot(0).getItem());
            NearbyBlockTemperature.setProfile(null);
            assertEquals(20F, forge.getAmbientTemperature(), 0F);
        }
        helper.succeed();
    }

    @GameTest
    public static void movingPiecesCannotEvictStationClimate(GameTestHelper helper) {
        com.gtnewhorizons.horizonqa.api.TestPos at = helper.absolute(1, 1, 1);
        int[] calls = { 0 };
        try (Overrides overrides = new Overrides()
                .set(() -> NearbyBlockTemperature.enabled, v -> NearbyBlockTemperature.enabled = v, true)
                .run(() -> NearbyBlockTemperature.setProfile((block, meta) -> {
                    calls[0]++;
                    return 0;
                }), () -> NearbyBlockTemperature.setProfile(null))) {
            NearbyBlockTemperature.get(helper.getWorld(), at.x(), at.y(), at.z());
            int chunkX = at.x() & ~15, chunkZ = at.z() & ~15;
            for (int i = 0; i < 512; i++) {
                NearbyBlockTemperature.getForEntity(
                        helper.getWorld(),
                        chunkX + (i & 15),
                        at.y() + (i >> 8),
                        chunkZ + ((i >> 4) & 15));
            }
            int scanned = calls[0];
            NearbyBlockTemperature.get(helper.getWorld(), at.x(), at.y(), at.z());
            assertEquals("moving samples evicted the stationary sample", scanned, calls[0]);
        }
        helper.succeed();
    }

    @GameTest
    public static void influenceIsCachedBoundedAndExcludesOurStations(GameTestHelper helper) {
        com.gtnewhorizons.horizonqa.api.TestPos at = helper.absolute(1, 1, 1);
        int[] calls = { 0 };
        try (Overrides overrides = new Overrides()
                .set(() -> EnviroMineCompat.enabled, v -> EnviroMineCompat.enabled = v, true)
                .set(() -> NearbyBlockTemperature.enabled, v -> NearbyBlockTemperature.enabled = v, true)
                .run(() -> NearbyBlockTemperature.setProfile((block, meta) -> {
                    calls[0]++;
                    return block == Blocks.stone ? 30 : 0;
                }), () -> NearbyBlockTemperature.setProfile(null))) {
            helper.setBlock(2, 1, 1, Blocks.stone);
            float close = NearbyBlockTemperature.get(helper.getWorld(), at.x(), at.y(), at.z());
            assertEquals(30F, close, 0F);
            int scanned = calls[0];
            assertEquals(close, NearbyBlockTemperature.get(helper.getWorld(), at.x(), at.y(), at.z()), 0F);
            assertEquals(scanned, calls[0]);
            helper.setBlock(2, 1, 1, Blocks.air);
            helper.setBlock(3, 1, 1, Blocks.stone);
            NearbyBlockTemperature.clearCache();
            float far = NearbyBlockTemperature.get(helper.getWorld(), at.x(), at.y(), at.z());
            assertTrue(far > 0 && far < close);
            NearbyBlockTemperature.setProfile((block, meta) -> block == Blocks.stone ? Float.POSITIVE_INFINITY : 0);
            assertEquals(0F, NearbyBlockTemperature.get(helper.getWorld(), at.x(), at.y(), at.z()), 0F);
            NearbyBlockTemperature.setProfile((block, meta) -> block == Blocks.stone ? 500 : 0);
            assertEquals(40F, NearbyBlockTemperature.get(helper.getWorld(), at.x(), at.y(), at.z()), 0F);
            helper.setBlock(3, 1, 1, Blocks.air);
            helper.setBlock(2, 1, 1, BlockListMF.forge);
            NearbyBlockTemperature.setProfile((block, meta) -> block == BlockListMF.forge ? 500 : 0);
            assertEquals(0F, NearbyBlockTemperature.get(helper.getWorld(), at.x(), at.y(), at.z()), 0F);
            int old = calls[0];
            assertEquals(0F, NearbyBlockTemperature.get(helper.getWorld(), 1000000, 64, 1000000), 0F);
            assertEquals(old, calls[0]);
        }
        helper.succeed();
    }
}
