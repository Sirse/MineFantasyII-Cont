package minefantasy.mf2.integration.minetweaker.tweakers;

import net.minecraftforge.fluids.FluidRegistry;

import minefantasy.mf2.api.heating.QuenchMedium;
import minefantasy.mf2.api.heating.QuenchResponse;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * What pieces are quenched in and how metals answer it. A reload puts back what was there before the script.
 *
 * <pre>
 * mods.minefantasy.Quench.fluid("creosote", "oil"); // oil, water, brine, or none to take it out
 * mods.minefantasy.Quench.metal("Titanium", 5, 0, 10, 2, 12, 4); // quality added and crack % in oil, water, brine
 * mods.minefantasy.Quench.justCools("Steel");
 * </pre>
 */
@ZenClass("mods.minefantasy.Quench")
public class QuenchTweaker {

    private QuenchTweaker() {}

    @ZenMethod
    public static void fluid(String fluid, String medium) {
        String line = "Quench.fluid(\"" + fluid + "\", \"" + medium + "\")";
        if (fluid == null || fluid.isEmpty()) {
            MineTweakerAPI.logError(line + ": no fluid named. " + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        QuenchMedium kind = null;
        if (!"none".equals(medium)) {
            kind = QuenchMedium.byKey(medium);
            if (kind == null) {
                MineTweakerAPI
                        .logError(line + ": the medium is oil, water, brine or none. " + ScriptRecipes.NOTHING_CHANGED);
                return;
            }
        }
        if (FluidRegistry.getFluid(fluid) == null) {
            MineTweakerAPI.logWarning(line + ": no fluid of that name is registered now");
        }
        final QuenchMedium set = kind;
        MineTweakerAPI.apply(new Change<QuenchMedium>(line, QuenchMedium.registered(fluid)) {

            @Override
            void put(QuenchMedium value) {
                QuenchMedium.register(fluid, value);
            }

            @Override
            QuenchMedium value() {
                return set;
            }
        });
    }

    @ZenMethod
    public static void metal(String material, float oil, float oilCrack, float water, float waterCrack, float brine,
            float brineCrack) {
        String line = "Quench.metal(\"" + material + "\", ...)";
        if (material == null || material.isEmpty()) {
            MineTweakerAPI.logError(line + ": no material named. " + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        float[] bonus = { oil, water, brine };
        float[] crack = { oilCrack, waterCrack, brineCrack };
        for (int i = 0; i < 3; i++) {
            if (!finite(bonus[i]) || bonus[i] < -200F || bonus[i] > 200F) {
                MineTweakerAPI.logError(line + ": quality added is from -200 to 200. " + ScriptRecipes.NOTHING_CHANGED);
                return;
            }
            if (!finite(crack[i]) || crack[i] < 0F || crack[i] > 100F) {
                MineTweakerAPI.logError(line + ": a crack chance is from 0 to 100. " + ScriptRecipes.NOTHING_CHANGED);
                return;
            }
        }
        setMetal(line, material, new QuenchResponse(bonus, crack, false, false));
    }

    @ZenMethod
    public static void justCools(String material) {
        String line = "Quench.justCools(\"" + material + "\")";
        if (material == null || material.isEmpty()) {
            MineTweakerAPI.logError(line + ": no material named. " + ScriptRecipes.NOTHING_CHANGED);
            return;
        }
        setMetal(line, material, QuenchResponse.NONE);
    }

    private static void setMetal(String line, final String material, final QuenchResponse response) {
        MineTweakerAPI.apply(new Change<QuenchResponse>(line, QuenchResponse.registered(material)) {

            @Override
            void put(QuenchResponse value) {
                QuenchResponse.register(material, value);
            }

            @Override
            QuenchResponse value() {
                return response;
            }
        });
    }

    private static boolean finite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    /** Sets a value, and on undo puts back what was there. */
    private abstract static class Change<T> implements IUndoableAction {

        private final String line;
        private final T before;

        Change(String line, T before) {
            this.line = line;
            this.before = before;
        }

        abstract void put(T value);

        abstract T value();

        @Override
        public void apply() {
            put(value());
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            put(before);
        }

        @Override
        public String describe() {
            return line;
        }

        @Override
        public String describeUndo() {
            return "Undoing " + line;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
