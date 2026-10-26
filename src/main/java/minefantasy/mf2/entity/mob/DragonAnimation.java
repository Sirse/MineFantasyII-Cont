package minefantasy.mf2.entity.mob;

import net.minecraft.util.MathHelper;

/**
 * A dragon's pose state on the client, advanced once a client tick and read between ticks. The models only turn it into
 * joint angles, so two dragons drawn with one model never share a pose. Everything eases toward its target, and the
 * wingbeat runs on a continuous phase, so a change of pace never jumps the wings.
 */
public final class DragonAnimation {

    private static final float TWO_PI = (float) (Math.PI * 2.0D);
    /** Ticks to spread or fold the wings, and how fast the flap strength and the pitch follow their targets. */
    private static final float SPREAD_STEP = 0.1F, INTENSITY_EASE = 0.1F, PITCH_EASE = 0.15F, VELOCITY_EASE = 0.3F;
    /** Wingbeats a tick in radians: slow while soaring, fast while hovering or climbing. */
    private static final float SLOW_BEAT = 0.25F, FAST_BEAT = 0.45F;
    /** Flap amplitude in degrees: wide while hovering or climbing, a shiver while gliding. */
    private static final float SMALL_FLAP = 8.0F, WIDE_FLAP = 55.0F, FLAP_CENTRE = 10.0F;
    /** Below this speed a dragon is hovering, not flying somewhere. */
    private static final float HOVER_SPEED = 0.05F;
    /** A step longer than this between ticks is a teleport, not flight. */
    private static final double TELEPORT = 4.0D;
    private static final float MAX_PITCH = (float) Math.toRadians(45.0D);
    /** Room for the longest tail of the models. */
    public static final int TAIL_SEGMENTS = 8;
    private static final float TURN_EASE = 0.2F, BANK_EASE = 0.15F, HEAD_EASE = 0.35F, TAIL_EASE = 0.3F;
    /**
     * Roll and the bend of the whole tail in radians for each degree a tick of turning, and their limits. The tail's
     * segments sit one inside the next, so the limit is on their sum, which the model spreads along the tail.
     */
    private static final float BANK_PER_TURN = 0.06F, MAX_BANK = 0.5F, TAIL_PER_TURN = 0.08F, MAX_TAIL = 0.6F;

    private boolean started;
    private double lastX, lastY, lastZ;
    private double velocityX, velocityY, velocityZ;
    private float phase, prevPhase;
    private float spread, prevSpread;
    private float intensity, prevIntensity;
    private float pitch, prevPitch;
    private float jaw, prevJaw, neck, prevNeck;
    /** How far the tail is lifted as a breath is drawn, from 0 down to 1 raised. */
    private float lift, prevLift;
    private static final float LIFT_EASE = 0.2F;
    /** How far the jaw opens through a breath, in the degrees the jaw is set in. */
    private static final float BREATH_JAW = 20.0F;
    /** Body turn in degrees a tick, smoothed; the roll it brings in flight; where the head looks from the body. */
    private float lastBodyYaw, turn, bank, prevBank;
    private float headYaw, prevHeadYaw, headPitch, prevHeadPitch;
    /**
     * The whole tail's bend as each segment feels it: the first follows the turn, every next one the one before it a
     * tick late, so a turn runs down the tail to its tip.
     */
    private final float[] tail = new float[TAIL_SEGMENTS], prevTail = new float[TAIL_SEGMENTS];

    /** Advances one client tick; returns whether the wings beat down this tick, for the flap sound. */
    boolean tick(EntityDragon dragon) {
        if (!started) {
            started = true;
            // Each dragon starts its beat elsewhere, so a flight does not flap as one
            phase = prevPhase = (dragon.getEntityId() * 1.7F) % TWO_PI;
            spread = prevSpread = dragon.isTerrestrial() ? 0.0F : 1.0F;
            lastX = dragon.posX;
            lastY = dragon.posY;
            lastZ = dragon.posZ;
            lastBodyYaw = dragon.renderYawOffset;
        }
        prevPhase = phase;
        prevBank = bank;
        prevHeadYaw = headYaw;
        prevHeadPitch = headPitch;
        System.arraycopy(tail, 0, prevTail, 0, TAIL_SEGMENTS);
        prevSpread = spread;
        prevIntensity = intensity;
        prevPitch = pitch;
        prevJaw = jaw;
        prevNeck = neck;
        prevLift = lift;

        double dx = dragon.posX - lastX, dy = dragon.posY - lastY, dz = dragon.posZ - lastZ;
        lastX = dragon.posX;
        lastY = dragon.posY;
        lastZ = dragon.posZ;
        if (dx * dx + dy * dy + dz * dz > TELEPORT * TELEPORT) {
            velocityX = velocityY = velocityZ = 0.0D;
        } else {
            velocityX += (dx - velocityX) * VELOCITY_EASE;
            velocityY += (dy - velocityY) * VELOCITY_EASE;
            velocityZ += (dz - velocityZ) * VELOCITY_EASE;
        }
        float horizontal = MathHelper.sqrt_double(velocityX * velocityX + velocityZ * velocityZ);

        // A dying dragon folds its wings as it falls
        spread = approach(spread, dragon.isTerrestrial() || dragon.deathTime > 0 ? 0.0F : 1.0F, SPREAD_STEP);

        turn += (MathHelper.wrapAngleTo180_float(dragon.renderYawOffset - lastBodyYaw) - turn) * TURN_EASE;
        lastBodyYaw = dragon.renderYawOffset;
        bank += (clamp(-turn * BANK_PER_TURN, -MAX_BANK, MAX_BANK) * spread - bank) * BANK_EASE;
        tail[0] += (clamp(-turn * TAIL_PER_TURN, -MAX_TAIL, MAX_TAIL) - tail[0]) * TAIL_EASE;
        for (int i = TAIL_SEGMENTS - 1; i > 0; i--) {
            tail[i] += (prevTail[i - 1] - tail[i]) * TAIL_EASE;
        }

        // The game turns a large head in steps; the pose follows them smoothly
        float lookYaw = MathHelper.wrapAngleTo180_float(dragon.rotationYawHead - dragon.renderYawOffset);
        // Eased the short way round and never wrapped, so crossing behind the body is no spin
        headYaw += MathHelper.wrapAngleTo180_float(lookYaw - headYaw) * HEAD_EASE;
        headPitch += (dragon.rotationPitch - headPitch) * HEAD_EASE;

        // Hovering and climbing take full beats, cruising less, gliding down hardly any
        float cruise = 1.0F - 0.4F * clamp(horizontal / 0.3F, 0.0F, 1.0F);
        float climb = clamp((float) velocityY * 10.0F, -0.85F, 0.4F);
        intensity += (clamp(cruise + climb, 0.0F, 1.0F) - intensity) * INTENSITY_EASE;

        float pitchTarget = horizontal < HOVER_SPEED ? 0.0F
                : clamp((float) Math.atan2(velocityY, horizontal), -MAX_PITCH, MAX_PITCH);
        pitch += (pitchTarget * spread - pitch) * PITCH_EASE;

        // Open all through a breath, however long it is drawn or let out; else as a bite or a roar opens it
        jaw = (float) Math.toRadians(Math.max(dragon.getJawMove(), dragon.showsBreath() ? BREATH_JAW : 0.0F));
        neck = (float) Math.toRadians(4.5F * dragon.getNeckAngle());
        lift += ((dragon.showsDrawingBreath() ? 1.0F : 0.0F) - lift) * LIFT_EASE;

        phase += SLOW_BEAT + (FAST_BEAT - SLOW_BEAT) * intensity;
        boolean downbeat = false;
        if (phase >= TWO_PI) {
            phase -= TWO_PI;
            prevPhase -= TWO_PI;
            downbeat = spread > 0.5F && intensity > 0.3F;
        }
        return downbeat;
    }

    /** The roll into a turn, in radians. */
    public float bank(float partialTicks) {
        return lerp(prevBank, bank, partialTicks);
    }

    /** Where the head looks from the body, in degrees: yaw and pitch. */
    public float headYaw(float partialTicks) {
        return lerp(prevHeadYaw, headYaw, partialTicks);
    }

    public float headPitch(float partialTicks) {
        return lerp(prevHeadPitch, headPitch, partialTicks);
    }

    /** The whole tail's bend as a segment feels it, in radians, never past the limit; the first segment is nearest. */
    public float tail(int segment, float partialTicks) {
        int i = Math.min(segment, TAIL_SEGMENTS - 1);
        return lerp(prevTail[i], tail[i], partialTicks);
    }

    /** How far the wings are spread, from 0 folded to 1 open. */
    public float spread(float partialTicks) {
        return lerp(prevSpread, spread, partialTicks);
    }

    /** The wing angle in radians from its flight rest, up positive. */
    public float flap(float partialTicks) {
        float strength = lerp(prevIntensity, intensity, partialTicks);
        float amplitude = SMALL_FLAP + (WIDE_FLAP - SMALL_FLAP) * strength;
        return (float) Math.toRadians(FLAP_CENTRE + amplitude * MathHelper.sin(lerp(prevPhase, phase, partialTicks)));
    }

    /** How far the body tips nose up, in radians; level when hovering or on the ground. */
    public float pitch(float partialTicks) {
        return lerp(prevPitch, pitch, partialTicks);
    }

    /** How far the jaw is open, in radians. */
    public float jaw(float partialTicks) {
        return lerp(prevJaw, jaw, partialTicks);
    }

    /**
     * Where the wings are in their beat, from -1 to 1, {@code lag} radians behind them: the tail rises and falls with
     * the beat a little after it.
     */
    public float beat(float partialTicks, float lag) {
        return MathHelper.sin(lerp(prevPhase, phase, partialTicks) - lag);
    }

    /** How far the tail is lifted as a breath is drawn, from 0 to 1. */
    public float tailLift(float partialTicks) {
        return lerp(prevLift, lift, partialTicks);
    }

    /** How far the neck is drawn back to breathe, in radians. */
    public float neck(float partialTicks) {
        return lerp(prevNeck, neck, partialTicks);
    }

    private static float approach(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : value > max ? max : value;
    }

    private static float lerp(float from, float to, float partialTicks) {
        return from + (to - from) * partialTicks;
    }
}
