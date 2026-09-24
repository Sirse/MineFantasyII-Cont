package minefantasy.mf2.api.recipe;

/** A recipe operation that contradicts the registry state: duplicate id, missing id, closed registry. */
public class RecipeRegistrationException extends RuntimeException {

    public RecipeRegistrationException(String message) {
        super(message);
    }
}
