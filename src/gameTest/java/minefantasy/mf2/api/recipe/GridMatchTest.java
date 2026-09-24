package minefantasy.mf2.api.recipe;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.GridMatch;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.gametest.TestItems;

@GameTestHolder("minefantasy2")
public class GridMatchTest {

    private static final int W = 4;
    private static final int H = 4;
    private static final Item a = TestItems.seed;
    private static final Item b = TestItems.flour;

    /** A crafting grid without a container, filled by column and row. */
    private static InventoryCrafting grid(Object... cells) {
        ItemStack[] slots = new ItemStack[W * H];
        for (int i = 0; i < cells.length; i += 3) {
            slots[(Integer) cells[i] + (Integer) cells[i + 1] * W] = (ItemStack) cells[i + 2];
        }
        return new InventoryCrafting(null, W, H) {

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot >= 0 && slot < slots.length ? slots[slot] : null;
            }

            @Override
            public ItemStack getStackInRowAndColumn(int col, int row) {
                return col >= 0 && col < W && row >= 0 && row < H ? slots[col + row * W] : null;
            }
        };
    }

    private static GridMatch.Cell[] pattern(ItemStack... stacks) {
        GridMatch.Cell[] cells = new GridMatch.Cell[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            cells[i] = GridMatch.stack(stacks[i], stacks[i] == null ? 1 : stacks[i].stackSize);
        }
        return cells;
    }

    @GameTest
    public static void shapedShiftsAndMirrorsWhenAllowed(GameTestHelper helper) throws Exception {
        GridMatch.Cell[] ab = pattern(new ItemStack(a), new ItemStack(b));
        InventoryCrafting shiftedMirrored = grid(2, 3, new ItemStack(b), 3, 3, new ItemStack(a));
        assertNotNull(GridMatch.shaped(shiftedMirrored, W, H, ab, 2, 1, true, true));
        assertNull(GridMatch.shaped(shiftedMirrored, W, H, ab, 2, 1, true, false));
        assertNull(
                GridMatch.shaped(grid(1, 0, new ItemStack(a), 2, 0, new ItemStack(b)), W, H, ab, 2, 1, false, false));
        helper.succeed();
    }

    @GameTest
    public static void shapedRejectsStrayItemsAndShortStacks(GameTestHelper helper) throws Exception {
        GridMatch.Cell[] one = pattern(new ItemStack(a, 3));
        assertNull(
                GridMatch.shaped(grid(0, 0, new ItemStack(a, 3), 3, 3, new ItemStack(b)), W, H, one, 1, 1, true, true));
        assertNull(GridMatch.shaped(grid(0, 0, new ItemStack(a, 2)), W, H, one, 1, 1, true, true));
        int[] amounts = GridMatch.shaped(grid(1, 2, new ItemStack(a, 5)), W, H, one, 1, 1, true, true);
        assertEquals(3, amounts[1 + 2 * W]);
        assertEquals(1, amounts[0]);
        helper.succeed();
    }

    @GameTest
    public static void shapelessFindsAPairingWhateverTheOrder(GameTestHelper helper) throws Exception {
        GridMatch.Cell any = new GridMatch.Cell() {

            @Override
            public boolean accepts(ItemStack stack) {
                return true;
            }

            @Override
            public int amount() {
                return 1;
            }
        };
        // The catch-all cell comes first, so a greedy pass would take the only item the specific cell accepts
        GridMatch.Cell[] cells = { any, GridMatch.stack(new ItemStack(a), 2) };
        int[] amounts = GridMatch.shapeless(grid(0, 0, new ItemStack(a, 2), 1, 0, new ItemStack(b)), W, H, cells);
        assertNotNull(amounts);
        assertEquals(2, amounts[0]);
        assertEquals(1, amounts[1]);
        assertNull(GridMatch.shapeless(grid(0, 0, new ItemStack(a, 2)), W, H, cells));
        helper.succeed();
    }

    @GameTest
    public static void pairAllGivesEveryRowItsOwnColumn(GameTestHelper helper) throws Exception {
        boolean[][] fits = { { true, true }, { true, false } };
        assertArrayEquals(new int[] { 1, 0 }, GridMatch.pairAll(fits, 2));
        assertNull(GridMatch.pairAll(new boolean[][] { { true, false }, { true, false } }, 2));
        helper.succeed();
    }

    @GameTest
    public static void gridRecipesKeepTheirOwnCopies(GameTestHelper helper) throws Exception {
        ItemStack[] pattern = { new ItemStack(a), null };
        ItemStack output = new ItemStack(b);
        GridRecipe recipe = GridRecipe.shaped(GridRecipe.Grid.BENCH, 2, 1, pattern, null, output).time(10).build();
        pattern[0] = new ItemStack(b);
        output.stackSize = 9;
        ((ItemStack) recipe.getEntry(0)).stackSize = 5;

        assertEquals(a, ((ItemStack) recipe.getEntry(0)).getItem());
        assertEquals(1, ((ItemStack) recipe.getEntry(0)).stackSize);
        assertNull(recipe.getEntry(1));
        assertEquals(1, recipe.getRecipeOutput().stackSize);
        helper.succeed();
    }

    @GameTest
    public static void patternMustFillItsSize(GameTestHelper helper) throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            GridRecipe.shaped(GridRecipe.Grid.BENCH, 2, 2, new ItemStack[3], null, new ItemStack(b));
        });
        helper.succeed();
    }

    @GameTest
    public static void matchWorksOutTermsWithoutTouchingTheRecipe(GameTestHelper helper) throws Exception {
        GridRecipe recipe = GridRecipe
                .shaped(GridRecipe.Grid.BENCH, 1, 1, new ItemStack[] { new ItemStack(a, 2) }, null, new ItemStack(b))
                .tool("hands", 1).stationTier(2).time(40).research("tier").tiers(GridRecipe.Tiers.MATERIAL).build();
        GridRecipe.Match match = recipe.match(grid(3, 3, new ItemStack(a, 5)));
        // No material on the parts: the recipe's own terms; "tier" research needs a metal to name one
        assertEquals(40, match.getTime());
        assertEquals(1, match.getToolTier());
        assertEquals(2, match.getStationTier());
        assertEquals("", match.getResearch());
        assertEquals(2, match.getAmounts()[3 + 3 * W]);
        assertEquals(b, match.getResult().getItem());
        assertNull(recipe.match(grid(3, 3, new ItemStack(a, 1))));
        assertEquals(40, recipe.getCraftTime());
        helper.succeed();
    }
}
