package minefantasy.mf2.api.heating;

import java.util.HashMap;
import java.util.Map;

/**
 * How a metal answers quenching, by medium: the quality a good quench adds and the chance it cracks the piece. Common
 * metals behave as they do in a smithy: only steel hardens, the harder the more brittle, iron barely, pig iron only
 * cracks, and copper, bronze and the precious metals just cool. The mythic metals each have a character of their own.
 * Metals not listed just cool.
 */
public final class QuenchResponse {

    /** Quenching changes nothing: the piece just cools. */
    public static final QuenchResponse NONE = new QuenchResponse(new float[3], new float[3], false, false);

    private static final Map<String, QuenchResponse> METALS = new HashMap<String, QuenchResponse>();

    static {
        QuenchResponse steel = of(8, 2, 15, 8, 22, 15);
        register("Steel", steel);
        register("Encrusted", steel);
        register("StainlessSteel", steel);
        register("Thaumium", steel);
        QuenchResponse hard = of(12, 3, 20, 15, 28, 25);
        register("BlackSteel", hard);
        register("RedSteel", hard);
        register("BlueSteel", hard);
        register("CompositeAlloy", hard);
        register("Iron", of(2, 0, 4, 1, 5, 2));
        register("PigIron", of(0, 10, 0, 35, 0, 50));
        register("Tungsten", of(3, 0, 5, 1, 6, 2));
        // Mithril needs no quench; its alloy keeps its temper and takes only oil
        register("Mithril", NONE);
        register("Mithium", of(15, 0, 0, 0, 0, 0));
        // Adamantium takes only oil and cracks in anything harsher
        register("Adamantium", of(25, 3, 0, 100, 0, 100));
        // Ignotumite rewards a quench from the very middle of its working heat and cracks otherwise
        register("Ignotumite", new QuenchResponse(new float[] { 18, 30, 42 }, new float[] { 3, 15, 25 }, true, false));
        // Ender is unstable: the medium hardly matters, the outcome swings wide
        register("Ender", new QuenchResponse(new float[] { 15, 15, 15 }, new float[] { 20, 20, 20 }, false, true));
    }

    private final float[] bonus;
    private final float[] crack;
    /** Only a quench from the middle of the working heat succeeds. */
    public final boolean narrowWindow;
    /** The quality added swings from nothing to twice the bonus. */
    public final boolean wideSpread;

    public QuenchResponse(float[] bonus, float[] crack, boolean narrowWindow, boolean wideSpread) {
        if (bonus.length != 3 || crack.length != 3) {
            throw new IllegalArgumentException("A quench response gives oil, water and brine");
        }
        this.bonus = bonus.clone();
        this.crack = crack.clone();
        this.narrowWindow = narrowWindow;
        this.wideSpread = wideSpread;
    }

    /** Quality added and percent chance of a crack, in oil, water and brine. */
    public static QuenchResponse of(float oil, float oilCrack, float water, float waterCrack, float brine,
            float brineCrack) {
        return new QuenchResponse(
                new float[] { oil, water, brine },
                new float[] { oilCrack, waterCrack, brineCrack },
                false,
                false);
    }

    /** Sets how the metal of this material name answers quenching; null makes it just cool. */
    public static void register(String material, QuenchResponse response) {
        if (response == null) {
            METALS.remove(material);
        } else {
            METALS.put(material, response);
        }
    }

    /** The response registered for the material name, or null. */
    public static QuenchResponse registered(String material) {
        return METALS.get(material);
    }

    /** How the material answers quenching; {@link #NONE} when unlisted. */
    public static QuenchResponse of(String material) {
        QuenchResponse response = material == null ? null : METALS.get(material);
        return response == null ? NONE : response;
    }

    /** The quality a good quench in the medium adds. */
    public float bonus(QuenchMedium medium) {
        return bonus[medium.ordinal()];
    }

    /** The percent chance a quench in the medium cracks the piece. */
    public float crack(QuenchMedium medium) {
        return crack[medium.ordinal()];
    }

    /** Whether quenching does anything at all. */
    public boolean matters() {
        for (int i = 0; i < 3; i++) {
            if (bonus[i] != 0 || crack[i] != 0) {
                return true;
            }
        }
        return false;
    }
}
