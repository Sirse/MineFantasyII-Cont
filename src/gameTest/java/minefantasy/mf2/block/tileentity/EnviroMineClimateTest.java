package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import enviromine.core.EM_Settings;
import enviromine.trackers.properties.BiomeProperties;
import enviromine.trackers.properties.DimensionProperties;
import minefantasy.mf2.api.heating.AmbientTemperature;
import minefantasy.mf2.integration.enviromine.EnviroMineCompat;

@GameTestHolder(value = "minefantasy2", requiredMods = "enviromine")
public class EnviroMineClimateTest {

    private static BiomeProperties properties() {
        BiomeProperties biome = new BiomeProperties(new net.minecraft.nbt.NBTTagCompound());
        biome.biomeOveride = true;
        biome.ambientTemp = 30;
        biome.TemperatureShadeDecrease = 0;
        biome.DAWN_TEMPERATURE_DECREASE = 4;
        biome.DAY_TEMPERATURE_DECREASE = 0;
        biome.DUSK_TEMPERATURE_DECREASE = 4;
        biome.NIGHT_TEMPERATURE_DECREASE = 8;
        biome.TemperatureMultiplier = 1;
        biome.EARLY_SPRING_TEMPERATURE_DECREASE = 5;
        biome.MID_SPRING_TEMPERATURE_DECREASE = -2;
        biome.LATE_SPRING_TEMPERATURE_DECREASE = -1;
        biome.EARLY_SUMMER_TEMPERATURE_DECREASE = -1;
        biome.MID_SUMMER_TEMPERATURE_DECREASE = -3;
        biome.LATE_SUMMER_TEMPERATURE_DECREASE = -1;
        biome.EARLY_AUTUMN_TEMPERATURE_DECREASE = 6;
        biome.MID_AUTUMN_TEMPERATURE_DECREASE = 8;
        biome.LATE_AUTUMN_TEMPERATURE_DECREASE = 10;
        biome.EARLY_WINTER_TEMPERATURE_DECREASE = 12;
        biome.MID_WINTER_TEMPERATURE_DECREASE = 22;
        biome.LATE_WINTER_TEMPERATURE_DECREASE = 10;
        return biome;
    }

    private static void restore(int id, BiomeProperties before) {
        if (before == null) EM_Settings.biomeProperties.remove(id);
        else EM_Settings.biomeProperties.put(id, before);
    }

    @GameTest
    public static void blockProfilesRespectMetadataDisabledAndInvalidValues(GameTestHelper helper) {
        boolean enabled = EnviroMineCompat.enabled;
        EnviroMineCompat.enabled = true;
        String name = String
                .valueOf(net.minecraft.block.Block.blockRegistry.getNameForObject(net.minecraft.init.Blocks.stone));
        java.util.Map<String, enviromine.trackers.properties.BlockProperties> before = new java.util.HashMap<>(
                EM_Settings.blockProperties);
        try {
            float[] values = { 12, -5, 500, Float.NaN };
            for (int i = 0; i < 4; i++) {
                enviromine.trackers.properties.BlockProperties profile = new enviromine.trackers.properties.BlockProperties(
                        new net.minecraft.nbt.NBTTagCompound());
                profile.enableTemp = i != 2;
                profile.temp = values[i];
                EM_Settings.blockProperties.put(i == 0 ? name : name + "," + i, profile);
            }
            EnviroMineCompat.BlockClimate profile = new EnviroMineCompat.BlockClimate();
            assertEquals(12F, profile.temperature(net.minecraft.init.Blocks.stone, 0), 0F);
            assertEquals(-5F, profile.temperature(net.minecraft.init.Blocks.stone, 1), 0F);
            assertEquals(0F, profile.temperature(net.minecraft.init.Blocks.stone, 2), 0F);
            assertTrue(Float.isNaN(profile.temperature(net.minecraft.init.Blocks.stone, 3)));
            EnviroMineCompat.enabled = false;
            assertFalse(profile.isActive());
            assertTrue(Float.isNaN(profile.temperature(net.minecraft.init.Blocks.stone, 0)));
        } finally {
            EM_Settings.blockProperties.clear();
            EM_Settings.blockProperties.putAll(before);
            EnviroMineCompat.enabled = enabled;
        }
        helper.succeed();
    }

    @GameTest
    public static void seasonalClimateUsesContinuationOverridesExactlyOnce(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 1, 1);
        int id = helper.getWorld().getBiomeGenForCoords(at.x(), at.z()).biomeID;
        BiomeProperties biome = properties();
        BiomeProperties before = EM_Settings.biomeProperties.put(id, biome);
        int dimension = helper.getWorld().provider.dimensionId;
        DimensionProperties dimensionBefore = EM_Settings.dimensionProperties.remove(dimension);
        try {
            float baseline = new EnviroMineCompat.Climate(world -> null).get(helper.getWorld(), at.x(), at.y(), at.z());
            String[] seasons = { "EARLY_SPRING", "MID_SPRING", "LATE_SPRING", "EARLY_SUMMER", "MID_SUMMER",
                    "LATE_SUMMER", "EARLY_AUTUMN", "MID_AUTUMN", "LATE_AUTUMN", "EARLY_WINTER", "MID_WINTER",
                    "LATE_WINTER" };
            float[] decreases = { 5, -2, -1, -1, -3, -1, 6, 8, 10, 12, 22, 10 };
            for (int i = 0; i < seasons.length; i++) {
                final String season = seasons[i];
                float result = new EnviroMineCompat.Climate(world -> season)
                        .get(helper.getWorld(), at.x(), at.y(), at.z());
                assertEquals(decreases[i], baseline - result, .001F);
            }
            assertEquals(22F, biome.MID_WINTER_TEMPERATURE_DECREASE, 0F);
            EM_Settings.dimensionProperties
                    .put(dimension, new DimensionProperties(new net.minecraft.nbt.NBTTagCompound()));
            float disabled = new EnviroMineCompat.Climate(world -> "MID_WINTER")
                    .get(helper.getWorld(), at.x(), at.y(), at.z());
            float noSeason = new EnviroMineCompat.Climate(world -> null).get(helper.getWorld(), at.x(), at.y(), at.z());
            assertEquals(noSeason, disabled, 0F);
        } finally {
            restore(id, before);
            if (dimensionBefore == null) EM_Settings.dimensionProperties.remove(dimension);
            else EM_Settings.dimensionProperties.put(dimension, dimensionBefore);
        }
        helper.succeed();
    }

    @GameTest
    public static void invalidExternalClimateUsesBiomeFallback(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 1, 1);
        int id = helper.getWorld().getBiomeGenForCoords(at.x(), at.z()).biomeID;
        BiomeProperties biome = properties();
        biome.ambientTemp = Float.NaN;
        BiomeProperties before = EM_Settings.biomeProperties.put(id, biome);
        EnviroMineCompat.Climate climate = new EnviroMineCompat.Climate(world -> null);
        AmbientTemperature.setProvider(climate);
        try {
            assertTrue(Float.isNaN(climate.get(helper.getWorld(), at.x(), at.y(), at.z())));
            assertTrue(Float.isFinite(AmbientTemperature.get(helper.getWorld(), at.x(), at.y(), at.z())));
        } finally {
            restore(id, before);
            AmbientTemperature.setProvider(null);
        }
        helper.succeed();
    }

    @GameTest
    public static void continuationReadsBiomeSettingsWithoutChangingThem(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 1, 1);
        int id = helper.getWorld().getBiomeGenForCoords(at.x(), at.z()).biomeID;
        BiomeProperties biome = properties();
        BiomeProperties before = EM_Settings.biomeProperties.put(id, biome);
        try {
            assertTrue(
                    Float.isFinite(
                            new EnviroMineCompat.Climate(world -> null)
                                    .get(helper.getWorld(), at.x(), at.y(), at.z())));
            assertEquals(30F, biome.ambientTemp, 0F);
            assertSame(biome, EM_Settings.biomeProperties.get(id));
            biome.ambientTemp = Float.NaN;
            assertTrue(
                    Float.isNaN(
                            new EnviroMineCompat.Climate(world -> null)
                                    .get(helper.getWorld(), at.x(), at.y(), at.z())));
        } finally {
            restore(id, before);
        }
        helper.succeed();
    }

    @GameTest
    public static void continuationGetsColderAtNight(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 1, 1);
        int id = helper.getWorld().getBiomeGenForCoords(at.x(), at.z()).biomeID;
        BiomeProperties before = EM_Settings.biomeProperties.put(id, properties());
        long time = helper.getWorld().getWorldTime();
        try {
            helper.getWorld().setWorldTime(6000);
            float day = new EnviroMineCompat.Climate(world -> null).get(helper.getWorld(), at.x(), at.y(), at.z());
            helper.getWorld().setWorldTime(18000);
            float night = new EnviroMineCompat.Climate(world -> null).get(helper.getWorld(), at.x(), at.y(), at.z());
            assertEquals(8F, day - night, .001F);
        } finally {
            helper.getWorld().setWorldTime(time);
            restore(id, before);
        }
        helper.succeed();
    }
}
