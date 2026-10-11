package minefantasy.mf2.integration.sereneseasons;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.common.Loader;
import minefantasy.mf2.util.MFLogUtil;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.season.SeasonASMHelper;

/** Read-only bridge to the 1.7.10 backport. No player or client singleton is needed. */
public final class SereneSeasonsCompat {

    public static boolean enabled = true;
    private static Bridge bridge;

    private SereneSeasonsCompat() {}

    public static void load() {
        if (!enabled || !Loader.isModLoaded("sereneseasons")) return;
        try {
            bridge = new Bridge();
            minefantasy.mf2.api.heating.AmbientTemperature.setFallbackProvider((world, x, y, z) -> {
                float climate = biomeTemperature(world, world.getBiomeGenForCoords(x, z), x, y, z);
                return Float.isFinite(climate)
                        ? minefantasy.mf2.api.heating.AmbientTemperature.temperate
                                + (climate - .8F) * minefantasy.mf2.api.heating.AmbientTemperature.biomeScale
                        : Float.NaN;
            });
        } catch (RuntimeException | LinkageError e) {
            warn(e);
        }
    }

    public static String subSeason(World world) {
        return enabled && bridge != null ? bridge.subSeason(world) : null;
    }

    public static float biomeTemperature(World world, BiomeGenBase biome, int x, int y, int z) {
        return enabled && bridge != null ? bridge.biomeTemperature(world, biome, x, y, z) : Float.NaN;
    }

    private static void warn(Throwable e) {
        MFLogUtil.warnOnce(
                "sereneseasons-climate",
                "Serene Seasons climate API unavailable; using climate without seasonal adjustment: {}",
                e.toString());
    }

    public static final class Bridge {

        private volatile boolean failed;

        public String subSeason(World world) {
            if (failed) return null;
            try {
                ISeasonState current = SeasonHelper.getSeasonState(world);
                return current == null || current.getSubSeason() == null ? null : current.getSubSeason().name();
            } catch (RuntimeException | LinkageError e) {
                failed = true;
                warn(e);
                return null;
            }
        }

        public float biomeTemperature(World world, BiomeGenBase biome, int x, int y, int z) {
            if (failed || !world.blockExists(x, y, z)) return Float.NaN;
            try {
                return SeasonASMHelper.getFloatTemperature(world, biome, x, y, z);
            } catch (RuntimeException | LinkageError e) {
                failed = true;
                warn(e);
                return Float.NaN;
            }
        }
    }
}
