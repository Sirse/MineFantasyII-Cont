package minefantasy.mf2.api.recipe;

import java.util.regex.Pattern;

/**
 * Stable identifier of a recipe: {@code namespace:station/path}, for example {@code minefantasy2:bloomery/iron_ore}.
 * The first path segment names the station the recipe belongs to.
 */
public final class RecipeId implements Comparable<RecipeId> {

    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_.-]+(/[a-z0-9_.-]+)+");

    private final String namespace;
    private final String path;

    private RecipeId(String namespace, String path) {
        if (namespace == null || !NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid recipe id namespace: " + namespace);
        }
        if (path == null || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid recipe id path (expected station/name): " + path);
        }
        this.namespace = namespace;
        this.path = path;
    }

    public static RecipeId of(String namespace, String path) {
        return new RecipeId(namespace, path);
    }

    /** Parses {@code namespace:station/path}. */
    public static RecipeId parse(String id) {
        int colon = id == null ? -1 : id.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("Invalid recipe id (expected namespace:station/path): " + id);
        }
        return new RecipeId(id.substring(0, colon), id.substring(colon + 1));
    }

    /** The same id with text appended to the path, such as {@code _burnt} or {@code .alias}. */
    public RecipeId withSuffix(String suffix) {
        return new RecipeId(namespace, path + suffix);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    public String getStation() {
        return path.substring(0, path.indexOf('/'));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecipeId)) {
            return false;
        }
        RecipeId other = (RecipeId) o;
        return namespace.equals(other.namespace) && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }

    @Override
    public int compareTo(RecipeId o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
