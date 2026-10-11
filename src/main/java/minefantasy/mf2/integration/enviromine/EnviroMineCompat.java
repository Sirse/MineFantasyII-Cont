package minefantasy.mf2.integration.enviromine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.common.Loader;
import enviromine.core.EM_Settings;
import enviromine.trackers.properties.BiomeProperties;
import enviromine.trackers.properties.BlockProperties;
import enviromine.trackers.properties.DimensionProperties;
import enviromine.utils.EnviroUtils;
import minefantasy.mf2.api.heating.AmbientTemperature;
import minefantasy.mf2.util.MFLogUtil;

/** Optional, read-only climate bridge. Never creates a living entity or runs EnviroMine's body/armour scan. */
public final class EnviroMineCompat {

    public static boolean enabled = true;

    private EnviroMineCompat() {}

    public static void load() {
        minefantasy.mf2.api.heating.NearbyBlockTemperature.setExcludedBlocks(
                block -> block instanceof minefantasy.mf2.block.refining.BlockForge
                        || block instanceof minefantasy.mf2.block.refining.BlockCrucible
                        || block instanceof minefantasy.mf2.block.refining.BlockBigFurnace
                        || block instanceof minefantasy.mf2.block.crafting.BlockFirepit);
        if (!enabled || !Loader.isModLoaded("enviromine")) {
            minefantasy.mf2.api.heating.NearbyBlockTemperature.setProfile(null);
            return;
        }
        try {
            AmbientTemperature.setProvider(new Climate());
        } catch (RuntimeException | LinkageError e) {
            MFLogUtil.warnOnce(
                    "enviromine-climate",
                    "EnviroMine climate API unavailable; using biome temperature: {}",
                    e.toString());
        }
        try {
            minefantasy.mf2.api.heating.NearbyBlockTemperature.setProfile(new BlockClimate());
        } catch (RuntimeException | LinkageError e) {
            minefantasy.mf2.api.heating.NearbyBlockTemperature.setProfile(null);
            MFLogUtil.warnOnce(
                    "enviromine-block-climate",
                    "EnviroMine block profiles unavailable; skipping nearby block climate: {}",
                    e.toString());
        }
    }

    /** Reads only temperature profiles, with the same metadata priority as EnviroMine. */
    public static final class BlockClimate implements minefantasy.mf2.api.heating.NearbyBlockTemperature.BlockProfile {

        private boolean failed;

        @Override
        public synchronized boolean isActive() {
            return enabled && !failed;
        }

        @Override
        public synchronized float temperature(net.minecraft.block.Block block, int metadata) {
            if (failed || !enabled) return Float.NaN;
            try {
                BlockProperties properties = BlockProperties.base.getProperty(block, metadata);
                return properties == null ? Float.NaN : properties.enableTemp ? properties.temp : 0;
            } catch (RuntimeException | LinkageError e) {
                failed = true;
                MFLogUtil.warnOnce(
                        "enviromine-block-climate",
                        "EnviroMine block profiles unavailable; skipping nearby block climate: {}",
                        e.toString());
                return Float.NaN;
            }
        }
    }

    /** Climate API for EnviroMine continuation only. */
    public static final class Climate implements AmbientTemperature.Provider {

        private final java.util.function.Function<World, String> season;
        private final Map<World, Map<Long, Sample>> cache = new WeakHashMap<>();
        private final Map<World, Map<Long, Sample>> movingCache = new WeakHashMap<>();
        private boolean failed;

        public Climate() {
            this(minefantasy.mf2.integration.sereneseasons.SereneSeasonsCompat::subSeason);
        }

        public Climate(java.util.function.Function<World, String> season) {
            this.season = season;
        }

        @Override
        public synchronized float get(World world, int x, int y, int z) {
            return get(world, x, y, z, false);
        }

        @Override
        public synchronized float getForEntity(World world, int x, int y, int z) {
            return get(world, x, y, z, true);
        }

        private float get(World world, int x, int y, int z, boolean moving) {
            if (failed || !world.blockExists(x, y, z)) return Float.NaN;
            Map<World, Map<Long, Sample>> selected = moving ? movingCache : cache;
            int capacity = moving ? 256 : 4096;
            Map<Long, Sample> samples = selected.get(world);
            if (samples == null) {
                samples = new LinkedHashMap<Long, Sample>(256, .75F, true) {

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Long, Sample> entry) {
                        return size() > capacity;
                    }
                };
                selected.put(world, samples);
            }
            long key = minefantasy.mf2.api.heating.NearbyBlockTemperature.positionKey(x, y, z);
            long now = world.getTotalWorldTime();
            Sample sample = samples.get(key);
            if (sample != null && now >= sample.time && now - sample.time < 20) return sample.temperature;
            try {
                float value = climate(world, x, y, z);
                samples.put(key, new Sample(now, value));
                return value;
            } catch (RuntimeException | LinkageError e) {
                failed = true;
                cache.clear();
                movingCache.clear();
                MFLogUtil.warnOnce(
                        "enviromine-climate",
                        "EnviroMine climate calculation unavailable; using biome temperature: {}",
                        e.toString());
                return Float.NaN;
            }
        }

        private float climate(World world, int x, int y, int z) {
            BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
            BiomeProperties properties = EM_Settings.biomeProperties.get(biome.biomeID);
            if (properties != null && !properties.biomeOveride) properties = null;
            DimensionProperties dimension = EM_Settings.dimensionProperties.get(world.provider.dimensionId);
            boolean dimensionOverride = dimension != null && dimension.override;
            float value = properties == null ? EnviroUtils.getBiomeTemp(x, y, z, biome) : properties.ambientTemp;
            if (!world.provider.hasNoSky) {
                float low = EM_Settings.SurfaceYPosition;
                float high = EM_Settings.SkyYPositionLowerBound;
                float top = EM_Settings.SkyYPositionUpperBound;
                float lowTemp = EM_Settings.MinLowAltitudeTemp;
                float highTemp = EM_Settings.MaxHighAltitudeTemp;
                if (y < low && low > 0 && value < lowTemp) value += (lowTemp - value) * (1 - y / low);
                else if (y >= top) value = Math.min(value, highTemp);
                else if (y > high && value > highTemp)
                    value -= (value - highTemp) * (y - high) / Math.max(1F, EM_Settings.SkyYPositionLowerBoundDivider);
            }
            if (!dimensionOverride || dimension.weatherAffectsTemp) {
                if (world.isRaining() && biome.rainfall != 0 && (properties != null && properties.TemperatureRainBool))
                    value -= (properties == null ? 6 : properties.TemperatureRainDecrease);
                else if (world.isThundering() && biome.rainfall != 0
                        && (properties != null && properties.TemperatureThunderBool))
                    value -= (properties == null ? 8 : properties.TemperatureThunderDecrease);
            }
            if (!world.canBlockSeeTheSky(x, y, z) && world.isDaytime() && !world.isRaining())
                value -= (properties == null ? 2.5F : properties.TemperatureShadeDecrease);
            if (dimension == null ? !world.provider.hasNoSky : dimensionOverride && dimension.dayNightTemp) {
                long time = Math.floorMod(world.getWorldTime(), 24000L);
                float[] phases = { properties == null ? 4 : properties.DAWN_TEMPERATURE_DECREASE,
                        properties == null ? 0 : properties.DAY_TEMPERATURE_DECREASE,
                        properties == null ? 4 : properties.DUSK_TEMPERATURE_DECREASE,
                        properties == null ? 8 : properties.NIGHT_TEMPERATURE_DECREASE };
                int phase = (int) (time / 6000);
                float blend = time % 6000 / 6000F;
                value -= (phases[phase] + (phases[(phase + 1) % 4] - phases[phase]) * blend)
                        * (properties == null ? 1 : properties.TemperatureMultiplier);
                String current = season.apply(world);
                if (current != null) {
                    switch (current) {
                        case "EARLY_SPRING":
                            value -= properties == null ? 5 : properties.EARLY_SPRING_TEMPERATURE_DECREASE;
                            break;
                        case "MID_SPRING":
                            value -= properties == null ? -2 : properties.MID_SPRING_TEMPERATURE_DECREASE;
                            break;
                        case "LATE_SPRING":
                            value -= properties == null ? -1 : properties.LATE_SPRING_TEMPERATURE_DECREASE;
                            break;
                        case "EARLY_SUMMER":
                            value -= properties == null ? -1 : properties.EARLY_SUMMER_TEMPERATURE_DECREASE;
                            break;
                        case "MID_SUMMER":
                            value -= properties == null ? -3 : properties.MID_SUMMER_TEMPERATURE_DECREASE;
                            break;
                        case "LATE_SUMMER":
                            value -= properties == null ? -1 : properties.LATE_SUMMER_TEMPERATURE_DECREASE;
                            break;
                        case "EARLY_AUTUMN":
                            value -= properties == null ? 6 : properties.EARLY_AUTUMN_TEMPERATURE_DECREASE;
                            break;
                        case "MID_AUTUMN":
                            value -= properties == null ? 8 : properties.MID_AUTUMN_TEMPERATURE_DECREASE;
                            break;
                        case "LATE_AUTUMN":
                            value -= properties == null ? 10 : properties.LATE_AUTUMN_TEMPERATURE_DECREASE;
                            break;
                        case "EARLY_WINTER":
                            value -= properties == null ? 12 : properties.EARLY_WINTER_TEMPERATURE_DECREASE;
                            break;
                        case "MID_WINTER":
                            value -= properties == null ? 16 : properties.MID_WINTER_TEMPERATURE_DECREASE;
                            break;
                        case "LATE_WINTER":
                            value -= properties == null ? 10 : properties.LATE_WINTER_TEMPERATURE_DECREASE;
                            break;
                        default:
                            break;
                    }
                }
            }
            return value;
        }

    }

    private static final class Sample {

        final long time;
        final float temperature;

        Sample(long time, float temperature) {
            this.time = time;
            this.temperature = temperature;
        }
    }
}
