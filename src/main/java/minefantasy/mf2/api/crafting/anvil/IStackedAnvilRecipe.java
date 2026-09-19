package minefantasy.mf2.api.crafting.anvil;

/**
 * A recipe that can require more than one item in a grid cell.
 * <p>
 * The anvil asks for this per slot when it consumes the inputs. Without it a recipe that matched on a stacked
 * ingredient still only took a single item out of the cell.
 */
public interface IStackedAnvilRecipe {

    /**
     * Items to take from each grid slot, indexed the same way as the craft matrix, or null when every filled cell needs
     * exactly one item.
     */
    int[] getRequiredAmounts(AnvilCraftMatrix matrix);
}
