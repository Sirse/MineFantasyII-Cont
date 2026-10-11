package minefantasy.mf2.api.heating;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/** Bounded air-temperature adjustment; never reads station heat or loads neighbouring chunks. */
public final class NearbyBlockTemperature {

    public static boolean enabled = true;
    private static BlockProfile profile;
    private static final Map<World, Map<Long, Sample>> cache = new WeakHashMap<>();

    private static final Map<World, Map<Long, Sample>> movingCache = new WeakHashMap<>();
    private static java.util.function.Predicate<Block> excluded = block -> false;

    public static synchronized void setExcludedBlocks(java.util.function.Predicate<Block> value) {
        excluded = value;
        clearCache();
    }

    private NearbyBlockTemperature() {}

    public interface BlockProfile {

        float temperature(Block block, int metadata);

        default boolean isActive() {
            return true;
        }
    }

    public static synchronized void setProfile(BlockProfile value) {
        profile = value;
        cache.clear();
        movingCache.clear();
    }

    public static synchronized void clearCache() {
        cache.clear();
        movingCache.clear();
    }

    public static synchronized float get(World world, int x, int y, int z) {
        return get(world, x, y, z, false);
    }

    public static synchronized float getForEntity(World world, int x, int y, int z) {
        return get(world, x, y, z, true);
    }

    private static float get(World world, int x, int y, int z, boolean moving) {
        if (!enabled || profile == null || !profile.isActive() || world == null || !world.blockExists(x, y, z))
            return 0;
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
        long key = positionKey(x, y, z);
        long now = world.getTotalWorldTime();
        Sample sample = samples.get(key);
        if (sample != null && now >= sample.time && now - sample.time < 20) return sample.value;
        int reach = 3;
        float warm = 0, cold = 0;
        for (int dx = -reach; dx <= reach; dx++)
            for (int dy = -reach; dy <= reach; dy++) for (int dz = -reach; dz <= reach; dz++) {
                int distanceSquared = dx * dx + dy * dy + dz * dz;
                if (distanceSquared == 0 || distanceSquared > reach * reach) continue;
                int bx = x + dx, by = y + dy, bz = z + dz;
                if (!world.blockExists(bx, by, bz)) continue;
                Block block = world.getBlock(bx, by, bz);
                // These already heat their workpieces directly. Including them creates thermal feedback between
                // stations.
                if (excluded.test(block)) continue;
                float influence = profile.temperature(block, world.getBlockMetadata(bx, by, bz));
                if (!Float.isFinite(influence)) continue;
                influence *= (reach + 1F - (float) Math.sqrt(distanceSquared)) / reach;
                // Strongest source of each sign prevents large pools or walls of ice multiplying without bound.
                warm = Math.max(warm, influence);
                cold = Math.min(cold, influence);
            }
        float limit = 40;
        float value = Math.max(-limit, Math.min(limit, warm + cold));
        samples.put(key, new Sample(now, value));
        return value;
    }

    /** Vanilla world coordinate ranges fit 26 bits per horizontal axis and 12 bits vertically. */
    public static long positionKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (y & 0xFFFL);
    }

    private static final class Sample {

        final long time;
        final float value;

        Sample(long time, float value) {
            this.time = time;
            this.value = value;
        }
    }
}
