package minefantasy.mf2.api.recipe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Typed key for station-specific recipe data (tiers, research, time, temperature). The key holds the value type, a
 * validator and a translation key for describing the value; rendering belongs to client-side frontends, so this class
 * never touches client code.
 */
public final class RecipeMetadataKey<T> {

    private static final Map<String, RecipeMetadataKey<?>> KEYS = new ConcurrentHashMap<>();

    private final String id;
    private final Class<T> type;
    private final Predicate<? super T> validator;
    private final String translationKey;

    private RecipeMetadataKey(String id, Class<T> type, Predicate<? super T> validator, String translationKey) {
        this.id = id;
        this.type = type;
        this.validator = validator;
        this.translationKey = translationKey;
    }

    /** Creates a key; ids are global, so registering the same id twice is an error. */
    public static <T> RecipeMetadataKey<T> create(String id, Class<T> type, Predicate<? super T> validator,
            String translationKey) {
        RecipeMetadataKey<T> key = new RecipeMetadataKey<>(id, type, validator, translationKey);
        if (KEYS.putIfAbsent(id, key) != null) {
            throw new IllegalArgumentException("Recipe metadata key already exists: " + id);
        }
        return key;
    }

    public static <T> RecipeMetadataKey<T> create(String id, Class<T> type, String translationKey) {
        return create(id, type, value -> true, translationKey);
    }

    public static RecipeMetadataKey<?> byId(String id) {
        return KEYS.get(id);
    }

    public String getId() {
        return id;
    }

    public Class<T> getType() {
        return type;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    /** Checks the type and the validator; returns the value typed, or throws with the key named. */
    public T validate(Object value) {
        if (value == null || !type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Recipe metadata " + id + " expects " + type.getSimpleName() + ", got " + value);
        }
        T typed = type.cast(value);
        if (!validator.test(typed)) {
            throw new IllegalArgumentException("Invalid value for recipe metadata " + id + ": " + value);
        }
        return typed;
    }

    @Override
    public String toString() {
        return id;
    }
}
