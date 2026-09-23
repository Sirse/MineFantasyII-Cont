package minefantasy.mf2.integration.nei;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.TemplateRecipeHandler;
import minefantasy.mf2.api.helpers.CustomToolHelper;

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
}
