package minefantasy.mf2.api.recipe;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Immutable set of typed metadata values attached to a recipe. */
public final class RecipeMetadata {

    public static final RecipeMetadata EMPTY = new RecipeMetadata(Collections.emptyMap());

    private final Map<RecipeMetadataKey<?>, Object> values;

    private RecipeMetadata(Map<RecipeMetadataKey<?>, Object> values) {
        this.values = values;
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> T get(RecipeMetadataKey<T> key) {
        return key.getType().cast(values.get(key));
    }

    public <T> T get(RecipeMetadataKey<T> key, T fallback) {
        T value = get(key);
        return value == null ? fallback : value;
    }

    public boolean has(RecipeMetadataKey<?> key) {
        return values.containsKey(key);
    }

    public Set<RecipeMetadataKey<?>> keys() {
        return values.keySet();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RecipeMetadata && values.equals(((RecipeMetadata) o).values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.toString();
    }

    public static final class Builder {

        private final Map<RecipeMetadataKey<?>, Object> values = new LinkedHashMap<>();

        private Builder() {}

        /** Values must be immutable (numbers, strings, enums); mutable values would leak out of the recipe. */
        public <T> Builder put(RecipeMetadataKey<T> key, T value) {
            values.put(key, key.validate(value));
            return this;
        }

        public RecipeMetadata build() {
            return values.isEmpty() ? EMPTY
                    : new RecipeMetadata(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
        }
    }
}
