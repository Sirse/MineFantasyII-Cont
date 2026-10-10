package minefantasy.mf2.api.heating;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraftforge.fluids.Fluid;

/**
 * What a piece is quenched in, from the gentlest to the harshest: oil cools slowly, water fast, salt water fastest and
 * most evenly. Fluids are known by their registry names, so those of other mods count too.
 */
public enum QuenchMedium {

    OIL,
    WATER,
    BRINE;

    private static final Map<String, QuenchMedium> FLUIDS = new HashMap<String, QuenchMedium>();

    static {
        register("water", WATER);
        register("seedoil", OIL);
        register("plantoil", OIL);
        register("oliveoil", OIL);
        register("fishoil", OIL);
        register("saltwater", BRINE);
        register("brine", BRINE);
    }

    /** Makes a fluid, by its registry name, quench as the medium; null takes it out. */
    public static void register(String fluid, QuenchMedium medium) {
        if (medium == null) {
            FLUIDS.remove(fluid);
        } else {
            FLUIDS.put(fluid, medium);
        }
    }

    /** The medium registered for the fluid name, or null. */
    public static QuenchMedium registered(String fluid) {
        return FLUIDS.get(fluid);
    }

    /** The medium the fluid quenches as, or null when it does not. */
    public static QuenchMedium of(Fluid fluid) {
        return fluid == null ? null : FLUIDS.get(fluid.getName());
    }

    /** The name kept on a quenched piece and in its tooltip key. */
    public String key() {
        return name().toLowerCase(Locale.ENGLISH);
    }

    public static QuenchMedium byKey(String key) {
        for (QuenchMedium medium : values()) {
            if (medium.key().equals(key)) {
                return medium;
            }
        }
        return null;
    }
}
