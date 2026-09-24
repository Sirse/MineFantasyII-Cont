package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

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

    /** Lists every recipe the player may see; handlers without a station GUI never get asked */
    protected void loadAllRecipes() {}

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
