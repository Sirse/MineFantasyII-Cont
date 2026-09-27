package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.IUsageHandler;
import codechicken.nei.recipe.RecipeCatalysts;
import codechicken.nei.recipe.TemplateRecipeHandler;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeRegistry;

public abstract class MFNEIRecipeHandler extends TemplateRecipeHandler {

    private final String handlerId;

    protected MFNEIRecipeHandler(String handlerId) {
        this.handlerId = handlerId;
    }

    @Override
    public String getHandlerId() {
        return handlerId;
    }

    @Override
    public String getOverlayIdentifier() {
        return handlerId;
    }

    /** The station GUI's click area asks for this handler's id, meaning every recipe it has */
    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (handlerId.equals(outputId)) {
            loadAllRecipes();
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    /** Lists every recipe the player may see: for the station GUI's click area and for the station as a catalyst */
    protected void loadAllRecipes() {}

    /**
     * Whether the recipe can be made on this station, one of the handler's catalysts: a station below the tier the
     * recipe needs cannot. Every recipe by default.
     */
    protected boolean madeOn(ItemStack station, CachedRecipe recipe) {
        return true;
    }

    /**
     * Looking up the uses of a station lists the recipes it can make. NEI's own lookup goes through a transfer
     * rectangle these handlers do not have, and would list every recipe whatever the station's tier.
     */
    @Override
    public IUsageHandler getUsageAndCatalystHandler(String inputId, Object... ingredients) {
        if ("item".equals(inputId) && ingredients.length > 0
                && ingredients[0] instanceof ItemStack
                && RecipeCatalysts.containsCatalyst(this, (ItemStack) ingredients[0])) {
            ItemStack station = (ItemStack) ingredients[0];
            MFNEIRecipeHandler handler = (MFNEIRecipeHandler) newInstance();
            handler.loadAllRecipes();
            handler.arecipes.removeIf(recipe -> !handler.madeOn(station, recipe));
            return handler;
        }
        return getUsageHandler(inputId, ingredients);
    }

    /** What the station block says about itself, or the fallback when the stack is not such a block. */
    protected static int stationTier(ItemStack station, ToIntFunction<Block> tierOf) {
        Block block = station == null ? null : Block.getBlockFromItem(station.getItem());
        return block == null ? -1 : tierOf.applyAsInt(block);
    }

    /** Output filter for recipe lookups, where a null wanted stack means "all of them" */
    protected static boolean matchesOutput(ItemStack output, ItemStack wanted) {
        return wanted == null || CustomToolHelper.areEqual(output, wanted);
    }

    /** Published one-input recipes producing the wanted stack (all of them for null), in lookup order. */
    protected static List<ProcessRecipe> recipesMaking(RecipeRegistry<ProcessRecipe> registry, ItemStack wanted) {
        List<ProcessRecipe> found = new ArrayList<ProcessRecipe>();
        for (RecipeEntry<ProcessRecipe> entry : registry.published().all()) {
            if (matchesOutput(entry.getRecipe().getOutput(), wanted)) {
                found.add(entry.getRecipe());
            }
        }
        return found;
    }

    /** Published one-input recipes whose input takes the stack, in lookup order. */
    protected static List<ProcessRecipe> recipesUsing(RecipeRegistry<ProcessRecipe> registry, ItemStack ingredient) {
        List<ProcessRecipe> found = new ArrayList<ProcessRecipe>();
        if (!NEIHelper.isValidStack(ingredient)) {
            return found;
        }
        for (RecipeEntry<ProcessRecipe> entry : registry.published().candidates(Input.lookupKeys(ingredient))) {
            if (entry.getRecipe().getInput().matches(ingredient)) {
                found.add(entry.getRecipe());
            }
        }
        return found;
    }
}
