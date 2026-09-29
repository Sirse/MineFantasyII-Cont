package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.gadget.ItemBomb;
import minefantasy.mf2.item.list.ComponentListMF;

/**
 * The bomb bench takes one of each component per bomb, puts the bomb into its output and the pots of the powder and the
 * filling into its spare slot: all of it, or nothing when the output has no room.
 */
@GameTestHolder("minefantasy2")
public class BombBenchTest {

    private BombBenchTest() {}

    private static final int CASE = 0, POWDER = 1, FILLING = 2, FUSE = 3, OUTPUT = 4, SPARE = 5;

    private static int size(ItemStack stack) {
        return stack == null ? 0 : stack.stackSize;
    }

    private static TileEntityBombBench loaded(GameTestHelper helper, int each) {
        helper.setBlock(1, 1, 1, BlockListMF.bombBench);
        TileEntityBombBench bench = helper.assertTileEntityPresent(TileEntityBombBench.class, 1, 1, 1);
        bench.setInventorySlotContents(CASE, new ItemStack(ComponentListMF.bomb_casing, each));
        bench.setInventorySlotContents(POWDER, new ItemStack(ComponentListMF.blackpowder, each));
        bench.setInventorySlotContents(FILLING, new ItemStack(ComponentListMF.shrapnel, each));
        bench.setInventorySlotContents(FUSE, new ItemStack(ComponentListMF.bomb_fuse, each));
        return bench;
    }

    /** One press of the bomb press: the whole work at once. */
    private static void press(GameTestHelper helper, TileEntityBombBench bench) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        bench.tryCraft(player, true);
    }

    private static int droppedPots(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 1, 1);
        List<?> items = helper.getWorld().getEntitiesWithinAABB(
                EntityItem.class,
                AxisAlignedBB.getBoundingBox(at.x() - 2, at.y() - 2, at.z() - 2, at.x() + 3, at.y() + 3, at.z() + 3));
        int pots = 0;
        for (Object o : items) {
            ItemStack stack = ((EntityItem) o).getEntityItem();
            if (stack.getItem() == ComponentListMF.clay_pot) pots += stack.stackSize;
        }
        return pots;
    }

    @GameTest
    public static void aBombTakesOneOfEachAndReturnsThePots(GameTestHelper helper) {
        TileEntityBombBench bench = loaded(helper, 2);
        press(helper, bench);
        ItemStack bomb = bench.getStackInSlot(OUTPUT);
        assertNotNull("no bomb was made", bomb);
        assertTrue(bomb.getItem() instanceof ItemBomb);
        assertEquals(1, bomb.stackSize);
        for (int slot : new int[] { CASE, POWDER, FILLING, FUSE }) {
            assertEquals("slot " + slot + " paid the wrong amount", 1, size(bench.getStackInSlot(slot)));
        }
        assertEquals(ComponentListMF.clay_pot, bench.getStackInSlot(SPARE).getItem());
        assertEquals("powder and filling each return a pot", 2, size(bench.getStackInSlot(SPARE)));

        press(helper, bench);
        assertEquals("the second bomb did not stack", 2, size(bench.getStackInSlot(OUTPUT)));
        for (int slot : new int[] { CASE, POWDER, FILLING, FUSE }) {
            assertNull("slot " + slot + " was not used up", bench.getStackInSlot(slot));
        }
        assertEquals(4, size(bench.getStackInSlot(SPARE)));
        helper.succeed();
    }

    @GameTest
    public static void aTakenOutputMakesNothingAndTakesNothing(GameTestHelper helper) {
        TileEntityBombBench bench = loaded(helper, 2);
        bench.setInventorySlotContents(OUTPUT, new ItemStack(Items.stick));
        press(helper, bench);
        assertEquals("the output was replaced", Items.stick, bench.getStackInSlot(OUTPUT).getItem());
        for (int slot : new int[] { CASE, POWDER, FILLING, FUSE }) {
            assertEquals("slot " + slot + " paid for nothing", 2, size(bench.getStackInSlot(slot)));
        }
        assertNull("a pot came back for nothing", bench.getStackInSlot(SPARE));
        assertEquals(0, droppedPots(helper));
        helper.succeed();
    }

    @GameTest
    public static void aFullSpareSlotThrowsThePotsOut(GameTestHelper helper) {
        TileEntityBombBench bench = loaded(helper, 1);
        bench.setInventorySlotContents(SPARE, new ItemStack(Items.stick, 64));
        press(helper, bench);
        assertNotNull("the bomb waited for room for the pot", bench.getStackInSlot(OUTPUT));
        assertEquals(64, size(bench.getStackInSlot(SPARE)));
        assertEquals("the pots were lost", 2, droppedPots(helper));
        helper.succeed();
    }
}
