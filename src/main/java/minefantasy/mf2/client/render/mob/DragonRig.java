package minefantasy.mf2.client.render.mob;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.MathHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.entity.mob.DragonAnimation;
import minefantasy.mf2.entity.mob.EntityDragon;

/**
 * What every dragon model moves alike: the body's tilt, roll and breath, the neck, the tail and the curl of the forearm
 * in flight. A model names its joints once; the rig keeps their modelled angles and sets each joint from them and the
 * dragon's pose on every render, so no pose carries over from another dragon.
 */
@SideOnly(Side.CLIENT)
final class DragonRig {

    /** The body's tilt the models have always drawn, whatever their modelled angle. */
    private static final float BODY_TILT = 0.5462880558742251F;
    /** Shares of the look and of the breath-drawing pull for each neck segment; the head takes the rest. */
    private static final float NECK_YAW = 0.2F, NECK_PITCH = 0.15F, NECK_PULL = 0.2F;
    private static final float TWO_PI = (float) (Math.PI * 2.0D);
    /** Strides for each unit of the walk the game counts, the share of a stride a foot is down, and the swing. */
    private static final float STRIDE_RATE = 0.6662F, STANCE = 0.6F, STRIDE = 0.6F, LIFT = 0.7F;
    /** In flight: hip drawn back, knee bent and foot curled, a hind leg further than a foreleg; in radians. */
    private static final float HIND_TUCK_HIP = 1.0F, HIND_TUCK_KNEE = 1.2F, FOOT_CURL = 0.6F;
    private static final float FORE_TUCK_HIP = 0.7F, FORE_TUCK_KNEE = 1.0F;
    /** How far the forearm curls with each beat in flight. */
    private static final float ARM_CURL = 0.3F;
    /** The body rises and falls with each breath at rest. */
    private static final float BREATH = 0.02F, BREATH_RATE = 0.1F;
    /**
     * The tail's waves, each the bend of the whole tail, running from the base to the tip {@code WAVE_LAG} radians a
     * segment behind, little enough that the tail bends as one arc rather than snaking: an idle sway, a swing against
     * the stride as it walks, a rise and fall with the wingbeat in flight. On the ground a wave is spread evenly so the
     * base moves too; in flight it gathers toward the tip, which lashes most. Drawing a breath it lifts its tail, most
     * at the base.
     */
    private static final float SWAY = 0.5F, SWAY_RATE = 0.08F, WALK_SWAY = 0.4F, FLIGHT_WAVE = 0.6F, WAVE_LAG = 0.25F;
    private static final float TAIL_LIFT = 0.6F;
    /** How far the neck, the head and the whole tail sink as the dragon dies, and the ticks it takes. */
    private static final float DEATH_NECK = 0.15F, DEATH_HEAD = 0.2F, DEATH_TAIL = 0.5F;
    /** The fall takes this share of the death; the rest the body lies still. */
    private static final float FALL_SHARE = 0.6F;
    /** As it falls the legs give way and the body sinks to the ground, in model units. */
    private static final float DEATH_HIP = 0.4F, DEATH_KNEE = 0.8F, DEATH_SINK = 4.0F;

    private final ModelRenderer body;
    private final float bodyY;
    private final Joint[] neck, tail;
    private final Joint armRight, armLeft;
    private final List<Leg> legs = new ArrayList<>();

    /** The joints, the neck from the body out and the tail from the body out, the right wing on the -X side. */
    DragonRig(ModelRenderer body, ModelRenderer[] neck, ModelRenderer[] tail, ModelRenderer armRight,
            ModelRenderer armLeft) {
        this.body = body;
        this.bodyY = body.rotationPointY;
        this.neck = joints(neck);
        this.tail = joints(tail);
        this.armRight = new Joint(armRight);
        this.armLeft = new Joint(armLeft);
    }

    /**
     * Adds a leg, hip to foot ({@code foot} may be null for a foreleg that ends at the forearm). A {@code lead} leg
     * steps with the right hind leg, the others half a stride later, so the legs move in diagonal pairs.
     */
    DragonRig leg(ModelRenderer hip, ModelRenderer knee, ModelRenderer foot, boolean fore, boolean lead) {
        legs.add(new Leg(hip, knee, foot, fore, lead));
        return this;
    }

    /**
     * How far a leg is through its stride, from -1 reaching forward to 1 pushed back: back at an even pace while the
     * foot bears weight, then quickly forward through the air. The second value is how high the foot is lifted.
     */
    static float[] stride(float swing, boolean lead) {
        float cycle = (swing * STRIDE_RATE + (lead ? 0.0F : (float) Math.PI)) / TWO_PI;
        float u = cycle - MathHelper.floor_float(cycle);
        if (u < STANCE) {
            // The planted foot moves back with the ground, at the pace the body moves forward, so it does not slide
            return new float[] { -1.0F + 2.0F * u / STANCE, 0.0F };
        }
        float t = (u - STANCE) / (1.0F - STANCE);
        return new float[] { MathHelper.cos((float) Math.PI * t), MathHelper.sin((float) Math.PI * t) };
    }

    private static Joint[] joints(ModelRenderer[] parts) {
        Joint[] joints = new Joint[parts.length];
        for (int i = 0; i < parts.length; i++) {
            joints[i] = new Joint(parts[i]);
        }
        return joints;
    }

    /** How far the dragon has fallen in death, from 0 alive to 1 down, easing in as its legs give way. */
    static float death(EntityDragon dragon, float partialTicks) {
        if (dragon.deathTime <= 0) return 0.0F;
        float fall = Math.min(1.0F, (dragon.deathTime + partialTicks) / (EntityDragon.DEATH_TICKS * FALL_SHARE));
        return fall * fall * (3.0F - 2.0F * fall);
    }

    /** Head yaw left for the head itself once the neck has taken its share, in radians. */
    float headYaw(DragonAnimation pose, float partialTicks) {
        return (float) Math.toRadians(pose.headYaw(partialTicks)) * (1.0F - NECK_YAW * neck.length);
    }

    /** Head pitch left for the head itself, with the breath-drawing pull and the fall of death, in radians. */
    float headPitch(EntityDragon dragon, DragonAnimation pose, float partialTicks) {
        float look = (float) Math.toRadians(pose.headPitch(partialTicks)) * (1.0F - NECK_PITCH * neck.length);
        float pull = pose.neck(partialTicks) * (1.0F - NECK_PULL * neck.length);
        return look - pull + death(dragon, partialTicks) * DEATH_HEAD;
    }

    void apply(EntityDragon dragon, DragonAnimation pose, float partialTicks, float age, float swing,
            float swingAmount) {
        float spread = pose.spread(partialTicks), rest = 1.0F - spread, flap = pose.flap(partialTicks);
        float death = death(dragon, partialTicks), alive = 1.0F - death;
        float breath = MathHelper.sin(age * BREATH_RATE) * rest * alive;

        body.rotateAngleX = BODY_TILT - pose.pitch(partialTicks) + breath * BREATH;
        body.rotateAngleZ = pose.bank(partialTicks);
        body.rotationPointY = bodyY + death * DEATH_SINK;

        float yaw = (float) Math.toRadians(pose.headYaw(partialTicks)) * NECK_YAW;
        float pitch = (float) Math.toRadians(pose.headPitch(partialTicks)) * NECK_PITCH;
        float pull = pose.neck(partialTicks) * NECK_PULL;
        for (Joint segment : neck) {
            segment.set(pitch - pull + death * DEATH_NECK + breath * BREATH, yaw, 0.0F);
        }

        // Each segment turns inside the one before, so a bend of the whole tail is shared out: for the turn and the
        // trail the base takes the least, the tip the most, and the shares add up to the whole
        int segments = tail.length;
        float shares = segments * (segments + 1) / 2.0F, even = 1.0F / segments;
        float whips = segments * (segments + 1) * (2 * segments + 1) / 6.0F;
        float trail = pose.pitch(partialTicks) * 0.25F;
        float walk = rest * Math.min(1.0F, swingAmount) * alive;
        float stride = swing * STRIDE_RATE;
        float lift = pose.tailLift(partialTicks) * alive * TAIL_LIFT;
        for (int i = 0; i < segments; i++) {
            float share = (i + 1) / shares, lag = i * WAVE_LAG;
            // Grows with the square of the way along, so the tip takes the most and the shares still add to the whole
            float whip = (i + 1) * (i + 1) / whips, spreadOut = even + (whip - even) * spread;
            float side = MathHelper.sin(age * SWAY_RATE - lag) * SWAY * alive
                    // Against the stride, as a counterweight to the swing of the hips
                    + MathHelper.sin(stride - lag) * WALK_SWAY * walk;
            float rise = pose.beat(partialTicks, lag) * FLIGHT_WAVE * spread * alive;
            // Lifted most at the base, so the whole tail comes up rather than only its tip curling
            float lifted = lift * (segments - i) / shares;
            // In flight the tail trails level behind a body that climbs or dives
            tail[i].set(
                    (trail + death * DEATH_TAIL) * share + rise * whip - lifted,
                    pose.tail(i, partialTicks) * share + side * spreadOut,
                    0.0F);
        }

        for (Leg leg : legs) {
            leg.apply(spread, swing, swingAmount, death);
        }

        // On the ground the wing keeps its modelled bend; in flight the forearm curls a little with each beat
        float curl = flap * ARM_CURL * spread;
        armRight.set(0.0F, 0.0F, curl);
        armLeft.set(0.0F, 0.0F, -curl);
    }

    /**
     * A leg: on the ground it strides, bearing weight while its foot is down and bending at the knee to carry the foot
     * forward; in flight it is drawn up, knee bent and foot curled, and held still.
     */
    private static final class Leg {

        private final Joint hip, knee, foot;
        private final boolean fore, lead;

        Leg(ModelRenderer hip, ModelRenderer knee, ModelRenderer foot, boolean fore, boolean lead) {
            this.hip = new Joint(hip);
            this.knee = new Joint(knee);
            this.foot = foot == null ? null : new Joint(foot);
            this.fore = fore;
            this.lead = lead;
        }

        void apply(float spread, float swing, float swingAmount, float death) {
            float walk = (1.0F - spread) * Math.min(1.0F, swingAmount) * (1.0F - death);
            float[] stride = stride(swing, lead);
            float reach = stride[0] * STRIDE * walk, lift = stride[1] * LIFT * walk;
            float tuckHip = (fore ? FORE_TUCK_HIP : HIND_TUCK_HIP) * spread;
            float tuckKnee = (fore ? FORE_TUCK_KNEE : HIND_TUCK_KNEE) * spread;
            hip.set(reach + tuckHip + death * DEATH_HIP, 0.0F, 0.0F);
            knee.set(-lift - tuckKnee - death * DEATH_KNEE, 0.0F, 0.0F);
            if (foot != null) foot.set(lift * 0.5F + FOOT_CURL * spread, 0.0F, 0.0F);
        }
    }

    /** A joint and the angles it was modelled with; it is always set as an offset from those. */
    private static final class Joint {

        private final ModelRenderer part;
        private final float x, y, z;

        Joint(ModelRenderer part) {
            this.part = part;
            this.x = part.rotateAngleX;
            this.y = part.rotateAngleY;
            this.z = part.rotateAngleZ;
        }

        void set(float dx, float dy, float dz) {
            part.rotateAngleX = x + dx;
            part.rotateAngleY = y + dy;
            part.rotateAngleZ = z + dz;
        }
    }
}
