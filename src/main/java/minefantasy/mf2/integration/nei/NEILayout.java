package minefantasy.mf2.integration.nei;

import net.minecraft.item.ItemStack;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;

public class NEILayout {

    public static final Slot COOKING_INPUT = new Slot(31, 15);
    public static final Slot COOKING_OUTPUT = new Slot(102, 16);

    public static final Slot PAINT_OIL_INPUT = new Slot(50, 20);
    public static final Slot PAINT_OIL_OIL = new Slot(50, 43);
    public static final Slot PAINT_OIL_OUTPUT = new Slot(100, 31);

    // Shifted left by 18px (see RecipeHandlerBombBench.X_SHIFT) so the output slot clears NEI's side buttons.
    public static final Slot BOMB_BENCH_CASE = new Slot(59, 74);
    public static final Slot BOMB_BENCH_POWDER = new Slot(59, 48);
    public static final Slot BOMB_BENCH_FILLING = new Slot(34, 23);
    public static final Slot BOMB_BENCH_FUSE = new Slot(84, 23);
    public static final Slot BOMB_BENCH_OUTPUT = new Slot(129, 48);

    // Compact strip so several salvage recipes fit on one page: input on the left, twelve outputs as six columns by
    // two rows. The station is left to the catalyst tab.
    public static final Slot SALVAGE_INPUT = new Slot(8, 12);
    public static final Slot[] SALVAGE_OUTPUTS = new Slot[] { new Slot(52, 3), new Slot(70, 3), new Slot(88, 3),
            new Slot(106, 3), new Slot(124, 3), new Slot(142, 3), new Slot(52, 21), new Slot(70, 21), new Slot(88, 21),
            new Slot(106, 21), new Slot(124, 21), new Slot(142, 21) };

    /**
     * Carpenter and kitchen bench pages: the GUI's table with its 4x4 grid on the left, and on the right a column with
     * the tool above the output and the bench below it, level with the grid's first, middle and last rows. The grid
     * keeps the GUI's 18px pitch, so NEI's stock overlay handler maps it onto the GUI (first cell at 44,54 there) by a
     * fixed offset.
     */
    public static final int BENCH_GRID_X = 26;
    public static final int BENCH_GRID_Y = 24;
    public static final int BENCH_CELL = 18;
    public static final int BENCH_OVERLAY_X = 44 - BENCH_GRID_X;
    public static final int BENCH_OVERLAY_Y = 54 - BENCH_GRID_Y;
    /** The right column sits 4px past the table's edge; its three items share one x. */
    private static final int BENCH_COLUMN_X = BENCH_GRID_X - 22 + 114 + 4 + 5;
    public static final Slot BENCH_TOOL = new Slot(BENCH_COLUMN_X, BENCH_GRID_Y);
    public static final Slot BENCH_OUTPUT = new Slot(BENCH_COLUMN_X, BENCH_GRID_Y + 2 * BENCH_CELL - 8);
    public static final Slot BENCH_STATION = new Slot(BENCH_COLUMN_X, BENCH_GRID_Y + 3 * BENCH_CELL);
    /** Page height: the table ends 93px below the first cell. */
    public static final int BENCH_HEIGHT = BENCH_GRID_Y + 96;

    /**
     * The anvil page draws its 6x4 grid from ANVIL_GRID with the GUI's 18px pitch; the anvil GUI's first cell is at
     * 44,39. NEI's stock overlay handler maps the page onto the GUI by this offset.
     */
    public static final int ANVIL_GRID_X = 31;
    public static final int ANVIL_GRID_Y = 54;
    public static final int ANVIL_OVERLAY_X = 44 - ANVIL_GRID_X;
    public static final int ANVIL_OVERLAY_Y = 39 - ANVIL_GRID_Y;

    /** Draws the bench table with its grid and the output box, cut from a bench GUI texture already bound */
    public static void drawBenchBackground() {
        GuiDraw.drawTexturedModalRect(BENCH_GRID_X - 22, BENCH_GRID_Y - 22, 22, 32, 114, 115);
        GuiDraw.drawTexturedModalRect(BENCH_OUTPUT.x - 5, BENCH_OUTPUT.y - 5, 169, 75, 26, 26);
    }

    private NEILayout() {}

    public static PositionedStack stack(ItemStack stack, Slot slot) {
        return NEIHelper.positionedStack(stack, slot.x, slot.y);
    }

    public static PositionedStack stack(ItemStack stack, int x, int y) {
        return NEIHelper.positionedStack(stack, x, y);
    }

    public static class Slot {

        public final int x;
        public final int y;

        public Slot(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
