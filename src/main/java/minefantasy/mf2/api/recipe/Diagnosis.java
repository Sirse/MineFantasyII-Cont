package minefantasy.mf2.api.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
