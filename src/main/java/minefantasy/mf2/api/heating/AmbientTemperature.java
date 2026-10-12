package minefantasy.mf2.api.heating;

import net.minecraft.world.World;

/** Temperature of the environment in Celsius, independent of a station's heat source. */
public final class AmbientTemperature {

    public static float temperate = 20F;
    public static float biomeScale = 25F;
    private static volatile Provider provider;
    private static volatile Provider fallbackProvider;

    private AmbientTemperature() {}

    /** Optional integrations supply air temperature, never a player's body temperature. NaN uses the biome fallback. */
    public interface Provider {

        float get(World world, int x, int y, int z);

        default float getForEntity(World world, int x, int y, int z) {
            return get(world, x, y, z);
        }
    }

    /** The integration that supplies air temperature now, or null for the biome default. */
    public static Provider getProvider() {
        return provider;
    }

    public static void setProvider(Provider value) {
        provider = value;
        NearbyBlockTemperature.clearCache();
    }

    public static void setFallbackProvider(Provider value) {
        fallbackProvider = value;
    }

    public static float get(World world, int x, int y, int z) {
        return get(world, x, y, z, false);
    }

    public static float getForEntity(World world, int x, int y, int z) {
        return get(world, x, y, z, true);
    }

    private static float get(World world, int x, int y, int z, boolean moving) {
        if (world == null || !world.blockExists(x, y, z)) return temperate;
        Provider primary = provider;
        float value = primary == null ? Float.NaN
                : moving ? primary.getForEntity(world, x, y, z) : primary.get(world, x, y, z);
        Provider fallback = fallbackProvider;
        if (!Float.isFinite(value) && fallback != null)
            value = moving ? fallback.getForEntity(world, x, y, z) : fallback.get(world, x, y, z);
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            float climate = world.getBiomeGenForCoords(x, z).getFloatTemperature(x, y, z);
            value = temperate + (climate - 0.8F) * biomeScale;
        }
        if (!Float.isFinite(value)) value = temperate;
        return Math.max(
                -100F,
                Math.min(
                        100F,
                        value + (moving ? NearbyBlockTemperature.getForEntity(world, x, y, z)
                                : NearbyBlockTemperature.get(world, x, y, z))));
    }

    /** Moves towards the environment without overshooting, including below zero. */
    public static float approach(float temperature, float ambient, float step) {
        return temperature > ambient ? Math.max(ambient, temperature - step) : Math.min(ambient, temperature + step);
    }
}
