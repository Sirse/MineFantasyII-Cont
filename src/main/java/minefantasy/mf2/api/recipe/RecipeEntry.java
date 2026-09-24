package minefantasy.mf2.api.recipe;

import java.util.Comparator;

/** A registered recipe with its id, source and ordering data. Immutable. */
public final class RecipeEntry<R> {

    /** Higher priority first; equal priorities keep registration order. */
    public static final Comparator<RecipeEntry<?>> ORDER = (a, b) -> {
        if (a.priority != b.priority) {
            return a.priority > b.priority ? -1 : 1;
        }
        return Long.compare(a.order, b.order);
    };

    private final RecipeId id;
    private final R recipe;
    private final RecipeSource source;
    private final int priority;
    private final long order;

    RecipeEntry(RecipeId id, R recipe, RecipeSource source, int priority, long order) {
        this.id = id;
        this.recipe = recipe;
        this.source = source;
        this.priority = priority;
        this.order = order;
    }

    public RecipeId getId() {
        return id;
    }

    public R getRecipe() {
        return recipe;
    }

    public RecipeSource getSource() {
        return source;
    }

    public int getPriority() {
        return priority;
    }

    /** Registration position; kept by {@code replace}, so a replaced recipe does not move in the order. */
    public long getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return id + " [" + source + ", priority " + priority + "]";
    }
}
