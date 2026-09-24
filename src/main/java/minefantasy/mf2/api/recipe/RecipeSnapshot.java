package minefantasy.mf2.api.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Published, immutable state of one registry. Stations and frontends read only snapshots, so a reload running on
 * another thread never exposes a half-built recipe set.
 */
public final class RecipeSnapshot<R> {

    private final long generation;
    private final Map<RecipeId, RecipeEntry<R>> byId;
    private final List<RecipeEntry<R>> ordered;
    private final Map<Object, List<RecipeEntry<R>>> index;
    private final List<RecipeEntry<R>> unindexed;
    private final List<R> recipes;

    RecipeSnapshot(long generation, Collection<RecipeEntry<R>> entries, RecipeRegistry.Indexer<R> indexer) {
        this.generation = generation;
        List<RecipeEntry<R>> sorted = new ArrayList<>(entries);
        sorted.sort(RecipeEntry.ORDER);

        Map<RecipeId, RecipeEntry<R>> ids = new LinkedHashMap<>();
        Map<Object, List<RecipeEntry<R>>> keys = new HashMap<>();
        List<RecipeEntry<R>> loose = new ArrayList<>();
        for (RecipeEntry<R> entry : sorted) {
            ids.put(entry.getId(), entry);
            Collection<?> entryKeys = indexer.keys(entry.getRecipe());
            if (entryKeys == null || entryKeys.isEmpty()) {
                loose.add(entry);
                continue;
            }
            for (Object key : entryKeys) {
                List<RecipeEntry<R>> bucket = keys.computeIfAbsent(key, k -> new ArrayList<>());
                if (bucket.isEmpty() || bucket.get(bucket.size() - 1) != entry) {
                    bucket.add(entry);
                }
            }
        }
        for (Map.Entry<Object, List<RecipeEntry<R>>> bucket : keys.entrySet()) {
            bucket.setValue(Collections.unmodifiableList(bucket.getValue()));
        }
        this.byId = Collections.unmodifiableMap(ids);
        this.ordered = Collections.unmodifiableList(sorted);
        this.index = keys;
        this.unindexed = Collections.unmodifiableList(loose);
        List<R> plain = new ArrayList<>(sorted.size());
        for (RecipeEntry<R> entry : sorted) {
            plain.add(entry.getRecipe());
        }
        this.recipes = Collections.unmodifiableList(plain);
    }

    public long getGeneration() {
        return generation;
    }

    public RecipeEntry<R> get(RecipeId id) {
        return byId.get(id);
    }

    public boolean contains(RecipeId id) {
        return byId.containsKey(id);
    }

    /** All recipes in lookup order. */
    public List<RecipeEntry<R>> all() {
        return ordered;
    }

    /** The recipes themselves, in lookup order. */
    public List<R> recipes() {
        return recipes;
    }

    /** The id the recipe object is published under, or null. */
    public RecipeId idOf(R recipe) {
        for (RecipeEntry<R> entry : ordered) {
            if (entry.getRecipe() == recipe) {
                return entry.getId();
            }
        }
        return null;
    }

    public int size() {
        return ordered.size();
    }

    /**
     * Recipes that may match an input described by the given index keys, in lookup order. Recipes without index keys
     * are always candidates. The caller still checks every candidate; the index only narrows the search.
     */
    public List<RecipeEntry<R>> candidates(Collection<?> keys) {
        Set<RecipeEntry<R>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<RecipeEntry<R>> result = new ArrayList<>(unindexed);
        seen.addAll(unindexed);
        for (Object key : keys) {
            List<RecipeEntry<R>> bucket = index.get(key);
            if (bucket == null) {
                continue;
            }
            for (RecipeEntry<R> entry : bucket) {
                if (seen.add(entry)) {
                    result.add(entry);
                }
            }
        }
        result.sort(RecipeEntry.ORDER);
        return Collections.unmodifiableList(result);
    }
}
