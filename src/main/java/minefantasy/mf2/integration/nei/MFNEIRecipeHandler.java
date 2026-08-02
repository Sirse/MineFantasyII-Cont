package minefantasy.mf2.integration.nei;

import codechicken.nei.recipe.TemplateRecipeHandler;

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
}
