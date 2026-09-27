package minefantasy.mf2.api.recipe;

import java.util.Arrays;

/**
 * Outcome of checking a station against a recipe: a plan to execute, or the reason it cannot craft. Reasons carry a
 * translation key and plain arguments, so they can be synced to the client and shown in HUDs or diagnostics.
 */
public final class CheckResult {

    /** Why a station cannot craft. Arguments are numbers or strings only. */
    public static final class Reason {

        public static final Reason NO_RECIPE = new Reason("no_recipe");
        public static final Reason MISSING_INPUT = new Reason("missing_input");
        public static final Reason WRONG_MATERIAL = new Reason("wrong_material");
        public static final Reason RESEARCH = new Reason("research");
        public static final Reason SKILL = new Reason("skill");
        public static final Reason OUTPUT_FULL = new Reason("output_full");
        public static final Reason DIRTY = new Reason("dirty");
        /** Another recipe earlier in the lookup takes the same items. */
        public static final Reason SHADOWED = new Reason("shadowed");

        private final String id;
        private final Object[] args;

        private Reason(String id, Object... args) {
            this.id = id;
            this.args = args;
        }

        public static Reason of(String id, Object... args) {
            for (Object arg : args) {
                if (!(arg instanceof Number) && !(arg instanceof String)) {
                    throw new IllegalArgumentException("Reason arguments must be numbers or strings: " + arg);
                }
            }
            return new Reason(id, args.clone());
        }

        public static Reason temperature(int have, int need) {
            return of("temperature", have, need);
        }

        public static Reason tier(String what, int have, int need) {
            return of("tier", what, have, need);
        }

        /** A tool or station below the tier needed still works, only harder. */
        public static Reason harder(String what, int have, int need) {
            return of("harder", what, have, need);
        }

        public boolean isShadowed() {
            return "shadowed".equals(id);
        }

        /** Whether this only makes the work harder rather than stopping it. */
        public boolean isPenalty() {
            return "harder".equals(id);
        }

        public String getId() {
            return id;
        }

        public String getTranslationKey() {
            return "minefantasy2.recipe.reason." + id;
        }

        public Object[] getArgs() {
            return args.clone();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Reason && id.equals(((Reason) o).id) && Arrays.equals(args, ((Reason) o).args);
        }

        @Override
        public int hashCode() {
            return 31 * id.hashCode() + Arrays.hashCode(args);
        }

        @Override
        public String toString() {
            return args.length == 0 ? id : id + Arrays.toString(args);
        }
    }

    private final CraftPlan plan;
    private final Reason reason;

    private CheckResult(CraftPlan plan, Reason reason) {
        this.plan = plan;
        this.reason = reason;
    }

    public static CheckResult success(CraftPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("A successful check needs a plan");
        }
        return new CheckResult(plan, null);
    }

    public static CheckResult failure(Reason reason) {
        if (reason == null) {
            throw new IllegalArgumentException("A failed check needs a reason");
        }
        return new CheckResult(null, reason);
    }

    public boolean isSuccess() {
        return plan != null;
    }

    /** The plan; null for a failure. */
    public CraftPlan getPlan() {
        return plan;
    }

    /** The reason; null for a success. */
    public Reason getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return isSuccess() ? "success(" + plan.getRecipeId() + ")" : "failure(" + reason + ")";
    }
}
