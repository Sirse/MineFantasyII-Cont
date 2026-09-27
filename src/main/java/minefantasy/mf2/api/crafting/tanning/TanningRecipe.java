package minefantasy.mf2.api.crafting.tanning;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeChecks;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeMetadata;

/** Native tanning recipes; see {@link MFRecipes#TANNING}. */
public final class TanningRecipe {

    private TanningRecipe() {}

    /** A native recipe tanning the input, an item, block, stack or ore name, into the output. */
    public static Builder recipe(Object input, ItemStack output) {
        return new Builder(input, output);
    }

    /** A native tanning recipe: worked with a knife of any tier unless said otherwise. The time must be given. */
    public static final class Builder {

        private final Object input;
        private final ItemStack output;
        private Float time;
        private String tool = "knife";
        private int toolTier = -1;

        private Builder(Object input, ItemStack output) {
            this.input = input;
            this.output = output;
        }

        /** The work it takes, in the units of the rack. */
        public Builder time(float time) {
            this.time = time;
            return this;
        }

        /** The tool to work it with and the tier it needs; -1 for any. */
        public Builder tool(String type, int tier) {
            this.tool = type;
            this.toolTier = tier;
            return this;
        }

        public RecipeEntry<ProcessRecipe> register() {
            RecipeChecks.require(time != null, "a tanning recipe needs its time");
            return NativeRecipes.addNative(MFRecipes.TANNING, input, recipe(input, time, toolTier, tool, output));
        }
    }

    private static ProcessRecipe recipe(Object input, float time, int tier, String toolType, ItemStack output) {
        return ProcessRecipe.of(
                NativeRecipes.input(input),
                output,
                RecipeMetadata.builder().put(MFRecipeKeys.TIME, time).put(MFRecipeKeys.TOOL_TIER, tier)
                        .put(MFRecipeKeys.TOOL, toolType).build());
    }
}
