package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;

/**
 * The work on a bench takes many hits; between the first and the last a hopper, another player or a script can change
 * what lies on it. The last hit pays out only for what is there then: no result of the old project, and no input taken
 * for a craft that does not happen.
 */
@GameTestHolder("minefantasy2")
public class LastHitTest {

    private LastHitTest() {}

    private static int size(ItemStack stack) {
        return stack == null ? 0 : stack.stackSize;
    }

    private static void recipe() {
        reload(
                tx -> tx.add(
                        MFRecipes.CARPENTER,
                        id("carpenter", "jar_bar"),
                        GridRecipe.shapeless(
                                GridRecipe.Grid.BENCH,
                                new Object[] { new ItemStack(jar) },
                                null,
                                new ItemStack(bar)).time(10).build(),
                        0));
    }

    private static int output(TileEntityCarpenterMF bench) {
        return bench.getSizeInventory() - 5;
    }

    /** A bench midway through turning a jar into a bar; the change happens, then the last hit lands. */
    private static TileEntityCarpenterMF lastHitAfter(Consumer<TileEntityCarpenterMF> change) throws Exception {
        recipe();
        TileEntityCarpenterMF bench = new TileEntityCarpenterMF();
        place(bench);
        bench.setInventorySlotContents(0, new ItemStack(jar, 2));
        bench.updateCraftingData();
        assertTrue("the bench did not take up the work", bench.hasProject());
        bench.progress = bench.progressMax - 0.01F;
        change.accept(bench);
        bench.progress = bench.progressMax;
        call(bench, "craftItem", new Class<?>[] { EntityPlayer.class }, (Object) null);
        return bench;
    }

    @GameTest
    public static void aSwappedInputGetsNoResultAndIsNotTaken(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityCarpenterMF bench = lastHitAfter(b -> b.setInventorySlotContents(0, new ItemStack(junk, 2)));
            assertNull("the old project paid out", bench.getStackInSlot(output(bench)));
            assertEquals("the swapped-in items were taken", 2, size(bench.getStackInSlot(0)));
            assertEquals(junk, bench.getStackInSlot(0).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aFilledOutputGetsNoResultAndKeepsTheInput(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityCarpenterMF bench = lastHitAfter(
                    b -> b.setInventorySlotContents(output(b), new ItemStack(junk, 64)));
            assertEquals("the result pushed into a full output", junk, bench.getStackInSlot(output(bench)).getItem());
            assertEquals(64, size(bench.getStackInSlot(output(bench))));
            assertEquals("the input was paid for nothing", 2, size(bench.getStackInSlot(0)));
            assertEquals(0, dropped(bar));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aRecipeRemovedBeforeTheLastHitIsNotMade(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityCarpenterMF bench = lastHitAfter(b -> reload(tx -> {}));
            assertNull("a removed recipe was made", bench.getStackInSlot(output(bench)));
            assertEquals("the input was paid for a removed recipe", 2, size(bench.getStackInSlot(0)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void anInputTakenAwayLeavesNoResult(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityCarpenterMF bench = lastHitAfter(b -> b.setInventorySlotContents(0, null));
            assertNull("a result came out of an empty bench", bench.getStackInSlot(output(bench)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
