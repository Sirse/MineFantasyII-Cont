package minefantasy.mf2.api.crafting.carpenter;

/**
 * A recipe that can require more than one item in a grid cell.
 * <p>
 * The benches ask for this per slot when they consume the inputs. Without it a recipe that matched on a stacked
 * ingredient still only took a single item out of the cell.
 */
public interface IStackedCarpenterRecipe {

    /**
     * Items to take from each grid slot, indexed the same way as the craft matrix, or null when every filled cell needs
     * exactly one item.
     */
    int[] getRequiredAmounts(CarpenterCraftMatrix matrix);
}
