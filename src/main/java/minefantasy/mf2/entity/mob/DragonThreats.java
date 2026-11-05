package minefantasy.mf2.entity.mob;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * The harm each attacker has done a dragon, fading as the fight goes on, a shot from afar counting for more. The dragon
 * turns on an attacker that has done clearly more than its target: a scratch does not draw it off the one it fights.
 */
final class DragonThreats {

    static final float RANGED = 1.5F, SWITCH_MARGIN = 1.5F, FADE = 0.9F;
    /** The target counts as having done at least this share of the dragon's health, even before it has struck. */
    static final float HOLD_SHARE = 0.05F;

    private final Map<Entity, Float> harm = new WeakHashMap<>();

    /**
     * Counts the harm and says whether the dragon should turn from {@code current} on the attacker: only on one whose
     * harm is half as much again as the target's, the target counting at least {@link #HOLD_SHARE} of
     * {@code maxHealth}, so a target found but yet to strike, or whose harm has faded, is not lost to a scratch.
     */
    boolean note(Entity attacker, float done, boolean fromAfar, Entity current, float maxHealth) {
        float threat = harm.merge(attacker, done * (fromAfar ? RANGED : 1.0F), Float::sum);
        if (attacker == current) return false;
        if (current == null || current.isDead) return true;
        if (attacker instanceof EntityPlayer && !(current instanceof EntityPlayer)) return true;
        float held = Math.max(harm.getOrDefault(current, 0.0F), maxHealth * HOLD_SHARE);
        return threat > held * SWITCH_MARGIN;
    }

    /** Old harm fades, a second's worth, and is forgotten once it is next to none. */
    void fade() {
        if (harm.isEmpty()) return;
        harm.replaceAll((attacker, threat) -> threat * FADE);
        harm.values().removeIf(threat -> threat < 0.5F);
    }
}
