package minefantasy.mf2.client;

/**
 * How hard a blast shakes the camera. Trauma comes from blasts and fades; the camera turns by its square, so a distant
 * blast barely stirs it, along smooth waves rather than a jitter. Only the view turns: where the player aims stays.
 */
public final class CameraShake {

    /** Ticks a full shake fades over. */
    static final float FADE_TICKS = 12F;
    /** Blasts shake out to this many of their radii. */
    static final float REACH = 3F;
    /** Trauma of a blast right at the camera. */
    static final float STRENGTH = 0.7F;
    private static final float MAX_YAW = 2F;
    private static final float MAX_PITCH = 2F;
    private static final float MAX_ROLL = 3F;

    /** User setting, from 0 for none to 1 for full. */
    public static float scale = 1F;

    private static float trauma;
    private static double since;

    private CameraShake() {}

    /** Trauma a blast of this radius gives at this distance. */
    static float traumaOf(float radius, double distance) {
        double reach = radius * REACH;
        if (distance >= reach) return 0F;
        double near = 1D - distance / reach;
        return (float) (STRENGTH * near * near);
    }

    /**
     * Trauma left of what was there this many ticks ago. Time going back means another world's clock, so nothing of the
     * old shake is left.
     */
    static float faded(float trauma, double ticks) {
        if (ticks < 0F) return 0F;
        return (float) Math.max(0D, trauma - ticks / FADE_TICKS);
    }

    /** A blast at this distance, at this tick. Trauma adds up but never past full. */
    public static void blast(float radius, double distance, double now) {
        float added = traumaOf(radius, distance) * scale;
        if (added <= 0F) return;
        trauma = Math.min(1F, faded(trauma, now - since) + added);
        since = now;
    }

    /**
     * Yaw, pitch and roll in degrees to turn the view by at this moment. Time is a double: an old world's tick count is
     * past where a float keeps the fraction of a tick, and the shake would move in steps.
     */
    public static float[] angles(double now) {
        if (scale <= 0F) return null;
        float left = faded(trauma, now - since);
        if (left <= 0F) return null;
        float shake = left * left;
        return new float[] { MAX_YAW * shake * wave(now, 0F), MAX_PITCH * shake * wave(now, 17.3F),
                MAX_ROLL * shake * wave(now, 41.7F) };
    }

    /** A smooth wave in [-1, 1]: two sines at unrelated speeds never quite repeat. */
    private static float wave(double time, float offset) {
        return (float) (Math.sin((time + offset) * 1.9D) * 0.6D + Math.sin((time + offset) * 3.7D) * 0.4D);
    }
}
