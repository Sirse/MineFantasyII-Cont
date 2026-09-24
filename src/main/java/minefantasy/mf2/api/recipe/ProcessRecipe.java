package minefantasy.mf2.api.recipe;

import java.util.Set;

import net.minecraft.item.ItemStack;

/**
 * One input turning into one output, with station-specific parameters as metadata: the shape shared by the blast
 * furnace, quern, tanning rack, big furnace and paint oil. Immutable.
 */
public final class ProcessRecipe implements RecipeChecks.Validated {

    @Override
    public void validate() {
        RecipeChecks.input("input", input);
        RecipeChecks.output("output", output);
    }

    private final Input input;
    private final ItemStack output;
    private final RecipeMetadata metadata;

    private ProcessRecipe(Input input, ItemStack output, RecipeMetadata metadata) {
        this.input = input;
        this.output = output;
        this.metadata = metadata;
    }

    public static ProcessRecipe of(Input input, ItemStack output) {
        return of(input, output, RecipeMetadata.EMPTY);
    }

    public static ProcessRecipe of(Input input, ItemStack output, RecipeMetadata metadata) {
        if (input == null || output == null || output.getItem() == null) {
            throw new IllegalArgumentException("A recipe needs an input and an output");
        }
        return new ProcessRecipe(input, output.copy(), metadata == null ? RecipeMetadata.EMPTY : metadata);
    }

    public Input getInput() {
        return input;
    }

    /** The output, as a copy. */
    public ItemStack getOutput() {
        return output.copy();
    }

    public RecipeMetadata getMetadata() {
        return metadata;
    }

    public <T> T get(RecipeMetadataKey<T> key, T fallback) {
        return metadata.get(key, fallback);
    }

    public Set<Object> indexKeys() {
        return input.indexKeys();
    }
}
