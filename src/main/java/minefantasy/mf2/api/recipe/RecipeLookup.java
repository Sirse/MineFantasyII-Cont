package minefantasy.mf2.api.recipe;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.item.ItemStack;

/**
 * Finds the recipe a station should use. Candidates come from the index (optionally cached per station), and every
 * candidate is checked in priority order with the current context each time, so a cached lookup always agrees with a
 * full one: the cache holds candidates, never a result.
 */
public final class RecipeLookup<R> {

    /** Checks one candidate against the station's current state. */
    @FunctionalInterface
    public interface Checker<R> {

        CheckResult check(RecipeEntry<R> entry);
    }

    private final RecipeRegistry<R> registry;

    private long cachedGeneration = -1;
    private Set<Object> cachedKeys;
    private List<RecipeEntry<R>> cachedCandidates;

    public RecipeLookup(RecipeRegistry<R> registry) {
        this.registry = registry;
    }

    /** Lookup keys of the stacks in the given slots. */
    public static Set<Object> keysOf(ItemStack... stacks) {
        Set<Object> keys = new LinkedHashSet<>();
        for (ItemStack stack : stacks) {
            keys.addAll(Input.lookupKeys(stack));
        }
        return keys;
    }

    /**
     * Returns the first successful check in lookup order. Without a success, returns the failure of the first candidate
     * that failed for a reason other than {@link CheckResult.Reason#NO_RECIPE}, so a player learns "too cold" rather
     * than "no recipe"; otherwise {@code NO_RECIPE}.
     */
    public CheckResult find(Set<Object> keys, Checker<R> checker) {
        CheckResult firstFailure = null;
        for (RecipeEntry<R> entry : candidates(keys)) {
            CheckResult result = checker.check(entry);
            if (result.isSuccess()) {
                return result;
            }
            if (firstFailure == null && result.getReason() != CheckResult.Reason.NO_RECIPE) {
                firstFailure = result;
            }
        }
        return firstFailure != null ? firstFailure : CheckResult.failure(CheckResult.Reason.NO_RECIPE);
    }

    /** Every candidate with its check result, for diagnostics. */
    public List<String> explain(Set<Object> keys, Checker<R> checker, Function<RecipeEntry<R>, String> label) {
        List<String> lines = new ArrayList<>();
        for (RecipeEntry<R> entry : candidates(keys)) {
            lines.add(label.apply(entry) + ": " + checker.check(entry));
        }
        return lines;
    }

    public List<RecipeEntry<R>> candidates(Set<Object> keys) {
        RecipeSnapshot<R> snapshot = registry.published();
        if (snapshot.getGeneration() != cachedGeneration || !keys.equals(cachedKeys)) {
            cachedCandidates = snapshot.candidates(keys);
            cachedKeys = new LinkedHashSet<>(keys);
            cachedGeneration = snapshot.getGeneration();
        }
        return cachedCandidates;
    }
}
