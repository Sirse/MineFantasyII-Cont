package minefantasy.mf2.api.recipe;

/** Where a recipe came from, for diagnostics and bulk removal. */
public final class RecipeSource {

    public enum Kind {
        NATIVE,
        INTEGRATION,
        SCRIPT
    }

    public static final RecipeSource NATIVE = new RecipeSource(Kind.NATIVE, "minefantasy2");

    private final Kind kind;
    private final String detail;

    private RecipeSource(Kind kind, String detail) {
        this.kind = kind;
        this.detail = detail == null ? "" : detail;
    }

    /** A recipe registered by another mod's integration code, named by its mod id. */
    public static RecipeSource integration(String modId) {
        return new RecipeSource(Kind.INTEGRATION, modId);
    }

    /** A recipe from a script; the detail is whatever the script engine exposes, possibly empty. */
    public static RecipeSource script(String detail) {
        return new RecipeSource(Kind.SCRIPT, detail);
    }

    public Kind getKind() {
        return kind;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecipeSource)) {
            return false;
        }
        RecipeSource other = (RecipeSource) o;
        return kind == other.kind && detail.equals(other.detail);
    }

    @Override
    public int hashCode() {
        return 31 * kind.hashCode() + detail.hashCode();
    }

    @Override
    public String toString() {
        return detail.isEmpty() ? kind.name() : kind.name() + "(" + detail + ")";
    }
}
