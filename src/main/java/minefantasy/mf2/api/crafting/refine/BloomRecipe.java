package minefantasy.mf2.api.crafting.refine;

import java.util.Set;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeChecks;

/**
 * A bloomery recipe: every item of the input stack turns into one output item. Register through
 * {@link minefantasy.mf2.api.crafting.MFRecipes#BLOOMERY}.
 */
public final class BloomRecipe implements RecipeChecks.Validated {

    @Override
    public void validate() {
        RecipeChecks.input("input", input);
        RecipeChecks.output("bloom", output);
        // Every item of the smelted stack gives one bloom; a larger output could not fit the stack's worth
        RecipeChecks.require(output.stackSize == 1, "the bloom must be a single item: each input item gives one");
        RecipeChecks.research("research", research);
    }

    /** Ticks of burning per input item. */
    public static final int TICKS_PER_ITEM = 300;

    private final Input input;
    private final ItemStack output;
    private final String research;

    private BloomRecipe(Input input, ItemStack output, String research) {
        this.input = input;
        this.output = output;
        this.research = research;
    }

    /** The input names one item: the bloomery smelts the whole stack in its input slot. */
    public static BloomRecipe of(Input input, ItemStack output) {
        return of(input, output, null);
    }

    /** With a research the player lighting the bloomery must have unlocked. */
    public static BloomRecipe of(Input input, ItemStack output, String research) {
        if (input == null || output == null || output.getItem() == null) {
            throw new IllegalArgumentException("Bloomery recipe needs an input and an output");
        }
        return new BloomRecipe(input, output.copy(), research == null || research.isEmpty() ? null : research);
    }

    public Input getInput() {
        return input;
    }

    /** One output item, as a copy. */
    public ItemStack getOutput() {
        return output.copy();
    }

    /** Research required to light the bloomery, or null. */
    public String getResearch() {
        return research;
    }

    public Set<Object> indexKeys() {
        return input.indexKeys();
    }
}
