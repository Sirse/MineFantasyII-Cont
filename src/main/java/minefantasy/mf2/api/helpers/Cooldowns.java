package minefantasy.mf2.api.helpers;

import net.minecraft.entity.Entity;

/**
 * Timers kept in an entity's data under a key: countdowns ticked once per tick, and stamps limiting how often a player
 * may do something.
 */
public final class Cooldowns {

    private Cooldowns() {}

    /** Ticks left on the countdown, 0 when it has run out or was never set. */
    public static int left(Entity entity, String key) {
        return entity == null ? 0 : Math.max(0, entity.getEntityData().getInteger(key));
    }

    public static void set(Entity entity, String key, int ticks) {
        if (ticks > 0) {
            entity.getEntityData().setInteger(key, ticks);
        } else {
            entity.getEntityData().removeTag(key);
        }
    }

    /** Counts the countdown down by one tick; the key goes once it runs out. */
    public static void tick(Entity entity, String key) {
        int ticks = left(entity, key);
        if (ticks > 0) {
            set(entity, key, ticks - 1);
        }
    }

    /**
     * Whether the entity may act now, at least {@code interval} ticks after the last time it did; if so, stamps this
     * time. The first time always passes.
     */
    public static boolean pass(Entity entity, String key, long interval) {
        long now = entity.worldObj.getTotalWorldTime();
        if (entity.getEntityData().hasKey(key) && now - entity.getEntityData().getLong(key) < interval) {
            return false;
        }
        entity.getEntityData().setLong(key, now);
        return true;
    }
}
