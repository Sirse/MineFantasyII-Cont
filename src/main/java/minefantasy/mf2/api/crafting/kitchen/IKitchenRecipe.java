package minefantasy.mf2.api.crafting.kitchen;

import minefantasy.mf2.api.crafting.carpenter.ICarpenterRecipe;

/**
 * A recipe that carries a dirty progress amount for the kitchen bench. Split from {@link ICarpenterRecipe} so external
 * implementations of the carpenter interface stay source-compatible.
 */
public interface IKitchenRecipe extends ICarpenterRecipe {

    /**
     * How much dirty progress this recipe adds to a kitchen bench
     */
    float getDirtyAmount();
}
