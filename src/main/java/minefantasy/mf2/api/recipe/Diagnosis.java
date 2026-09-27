package minefantasy.mf2.api.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * What a station finds when it looks its recipes up, for the {@code /mf recipes} command: every candidate in lookup
 * order with its priority and why it is not the one crafted, and a problem of the station itself (no input, no fuel).
 */
public final class Diagnosis {

    /** Implemented by stations that can explain their lookup. */
    public interface Source {

        Diagnosis diagnose(net.minecraft.entity.player.EntityPlayer player);
    }

    /** One candidate recipe; a null reason means it is the one the station crafts. */
    public static final class Candidate {

        private final RecipeId id;
        private final int priority;
        private final CheckResult.Reason reason;

        public Candidate(RecipeId id, int priority, CheckResult.Reason reason) {
            this.id = id;
            this.priority = priority;
            this.reason = reason;
        }

        public RecipeId getId() {
            return id;
        }

        public int getPriority() {
            return priority;
        }

        public CheckResult.Reason getReason() {
            return reason;
        }
    }

    private final String station;
    private final CheckResult.Reason problem;
    private final List<Candidate> candidates;

    private Diagnosis(String station, CheckResult.Reason problem, List<Candidate> candidates) {
        this.station = station;
        this.problem = problem;
        this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
    }

    public static Diagnosis of(String station, List<Candidate> candidates) {
        return new Diagnosis(station, null, candidates);
    }

    /** The station cannot look anything up, for the given reason. */
    public static Diagnosis problem(String station, CheckResult.Reason problem) {
        return new Diagnosis(station, problem, Collections.<Candidate>emptyList());
    }

    /** What a lookup made of its candidates: each with its reason, and the one it crafts, if any. */
    public static final class Walk<R> {

        private final List<Candidate> candidates;
        private final RecipeEntry<R> chosen;

        private Walk(List<Candidate> candidates, RecipeEntry<R> chosen) {
            this.candidates = candidates;
            this.chosen = chosen;
        }

        public List<Candidate> getCandidates() {
            return candidates;
        }

        /** The recipe the lookup takes, even if the station still cannot craft it; null for none. */
        public RecipeEntry<R> getChosen() {
            return chosen;
        }
    }

    /**
     * Walks candidates the way a station looks them up. Recipes outside the station's context, such as an oven recipe
     * at a spit, are left out: they never compete. Of the rest, the first the lookup takes is the one crafted, and
     * {@code stops} says what still stops the station crafting it; any later one it would take is shadowed by it.
     *
     * @param inContext whether the station looks at the recipe at all
     * @param passes    why the lookup passes over the recipe, or null when it takes it
     * @param stops     what stops the station crafting the recipe it took, or null
     */
    public static <R> Walk<R> walk(Iterable<RecipeEntry<R>> entries, Predicate<R> inContext,
            Function<RecipeEntry<R>, CheckResult.Reason> passes, Function<RecipeEntry<R>, CheckResult.Reason> stops) {
        List<Candidate> candidates = new ArrayList<>();
        RecipeEntry<R> chosen = null;
        for (RecipeEntry<R> entry : entries) {
            if (!inContext.test(entry.getRecipe())) {
                continue;
            }
            CheckResult.Reason reason = passes.apply(entry);
            if (reason == null) {
                if (chosen != null) {
                    reason = CheckResult.Reason.SHADOWED;
                } else {
                    chosen = entry;
                    reason = stops.apply(entry);
                }
            }
            candidates.add(candidate(entry, reason));
        }
        return new Walk<>(candidates, chosen);
    }

    public static <R> Candidate candidate(RecipeEntry<R> entry, CheckResult.Reason reason) {
        return new Candidate(entry.getId(), entry.getPriority(), reason);
    }

    public String getStation() {
        return station;
    }

    public CheckResult.Reason getProblem() {
        return problem;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }
}
