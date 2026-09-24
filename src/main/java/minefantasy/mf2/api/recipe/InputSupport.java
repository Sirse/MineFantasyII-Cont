package minefantasy.mf2.api.recipe;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

/**
 * The parts of an {@link Input} a station honours when it pays for a craft: amounts above one, and which {@link Usage
 * usages}. A registry refuses a recipe asking for anything else when it is registered, rather than the station quietly
 * taking one item regardless.
 */
public final class InputSupport {

    /** Stations that pay through a {@link CraftPlan}: any amount, any usage. */
    public static final InputSupport PLANNED = new InputSupport(true, EnumSet.allOf(Usage.Kind.class));
    /** Stations that turn exactly one item into the result. */
    public static final InputSupport ONE_CONSUMED = new InputSupport(false, EnumSet.of(Usage.Kind.CONSUME));

    private final boolean amounts;
    private final Set<Usage.Kind> usages;

    public InputSupport(boolean amounts, Set<Usage.Kind> usages) {
        this.amounts = amounts;
        this.usages = Collections.unmodifiableSet(EnumSet.copyOf(usages));
    }

    /** Throws IllegalArgumentException naming what the station cannot do. */
    public void check(Input input) {
        if (!amounts && input.getAmount() != 1) {
            throw new IllegalArgumentException("takes one item per input, not " + input.getAmount());
        }
        if (!usages.contains(input.getUsage().getKind())) {
            throw new IllegalArgumentException("does not support usage " + input.getUsage() + "; supported: " + usages);
        }
    }

    /** A registry validator checking the recipe's input. */
    public <R> RecipeRegistry.Validator<R> of(Function<R, Input> input) {
        return recipe -> check(input.apply(recipe));
    }
}
