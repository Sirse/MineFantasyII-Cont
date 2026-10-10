package minefantasy.mf2.api.heating;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public interface IQuenchBlock {

    /**
     * Quench an item
     *
     * @return 0 means success, 1-100 means item is damaged, <0 means fail.
     */
    public float quench();

    /** The fluid a piece is quenched in here, asked before {@link #quench()}; water unless told otherwise. */
    default Fluid quenchFluid() {
        return FluidRegistry.WATER;
    }
}
