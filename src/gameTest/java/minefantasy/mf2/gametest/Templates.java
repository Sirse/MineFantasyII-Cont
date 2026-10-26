package minefantasy.mf2.gametest;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;

import minefantasy.mf2.block.tileentity.TileEntityBigFurnace;
import minefantasy.mf2.block.tileentity.TileEntityCrucible;

/**
 * The structure templates under {@code assets/minefantasy2/horizonqastructures}, and the labels they share. A test
 * names a block of its template by label, so a template exported again with another layout keeps its tests right.
 */
public final class Templates {

    /** A heater and a furnace on it, walled in firebrick and roofed, front open to the north. */
    public static final String BIG_FURNACE = "furnace/big";
    /** The same furnace saved mid-smelt: iron ore in, bars out, coal in a hot heater. */
    public static final String BIG_FURNACE_WORKING = "furnace/big_working";
    /** A fireclay crucible on an advanced forge, walled in firebrick. */
    public static final String CRUCIBLE_COATED = "crucible/coated";
    /** The same crucible and forge without walls; the walls' places are labelled. */
    public static final String CRUCIBLE_NO_WALLS = "crucible/no_walls";
    /** A stone crucible on a stone forge. */
    public static final String CRUCIBLE_STONE = "crucible/stone";
    /** A mythic crucible over a ring of eight end portal frames holding eyes. */
    public static final String CRUCIBLE_ENDER = "crucible/ender";
    /** A crucible catching under a heater, two chambers over it, all walled in firebrick. */
    public static final String BLAST_FURNACE = "blast/furnace";
    /** A frame with three frames beside it, ready for a cogwork station. */
    public static final String COGWORK_FRAME = "cogwork/frame";
    /** The same frames with the station built. */
    public static final String COGWORK_STATION = "cogwork/station";

    public static final String HEATER = "heater", FURNACE = "furnace", ROOF = "roof";
    public static final String CRUCIBLE = "crucible", HEAT_SOURCE = "heat_source";
    /** The firebricks around a crucible, or their places. */
    public static final String[] CRUCIBLE_WALLS = { "wall_west", "wall_east", "wall_north", "wall_south" };

    private Templates() {}

    public static TileEntityBigFurnace furnace(GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileEntityBigFurnace.class, FURNACE);
    }

    public static TileEntityBigFurnace heater(GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileEntityBigFurnace.class, HEATER);
    }

    public static TileEntityCrucible crucible(GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileEntityCrucible.class, CRUCIBLE);
    }
}
