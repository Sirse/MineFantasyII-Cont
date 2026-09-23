package minefantasy.mf2.integration.nei;

import net.minecraft.item.ItemStack;

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
