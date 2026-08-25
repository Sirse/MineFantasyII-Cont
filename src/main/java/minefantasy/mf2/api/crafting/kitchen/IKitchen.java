package minefantasy.mf2.api.crafting.kitchen;

import minefantasy.mf2.api.crafting.carpenter.ICarpenter;

/**
 * Implemented by the kitchen bench tile entity. Split from {@link ICarpenter} so external implementations of the
 * carpenter interface stay source-compatible.
 */
public interface IKitchen extends ICarpenter {

    /**
     * Called by kitchen recipes so the bench knows how much dirt this craft adds
     */
    void setDirtyAmount(float amount);
}
