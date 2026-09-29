package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.refining.BlockBFH;
import minefantasy.mf2.block.refining.BlockCrucible;
import minefantasy.mf2.block.refining.BlockForge;

/**
 * A forge, crucible or blast heater turns into its lit or unlit twin as it heats and cools. The block is replaced, the
 * station is not: its contents stay inside and nothing drops.
 */
@GameTestHolder("minefantasy2")
public class LitSwapTest {

    private LitSwapTest() {}

    private interface Swap {

        void to(boolean lit, TestPos at);
    }

    private static void swapKeepsTheStation(GameTestHelper helper, Block unlit, Class<? extends TileEntityStation> type,
            Swap swap) {
        helper.setBlock(1, 1, 1, unlit);
        TileEntityStation station = helper.assertTileEntityPresent(type, 1, 1, 1);
        station.setInventorySlotContents(0, new ItemStack(Items.coal, 7));
        TestPos at = helper.absolute(1, 1, 1);

        swap.to(true, at);
        swap.to(false, at);

        TileEntityStation after = helper.assertTileEntityPresent(type, 1, 1, 1);
        assertSame(type.getSimpleName() + ": the swap replaced the station", station, after);
        assertNotNull(type.getSimpleName() + ": the swap emptied the station", after.getStackInSlot(0));
        assertEquals(7, after.getStackInSlot(0).stackSize);
        List<?> dropped = helper.getWorld().getEntitiesWithinAABB(
                EntityItem.class,
                AxisAlignedBB.getBoundingBox(at.x() - 2, at.y() - 2, at.z() - 2, at.x() + 3, at.y() + 3, at.z() + 3));
        assertEquals(type.getSimpleName() + ": the swap dropped the contents", 0, dropped.size());
    }

    @GameTest
    public static void forge(GameTestHelper helper) {
        swapKeepsTheStation(
                helper,
                BlockListMF.forge,
                TileEntityForge.class,
                (lit, at) -> BlockForge.updateFurnaceBlockState(lit, helper.getWorld(), at.x(), at.y(), at.z()));
        helper.succeed();
    }

    @GameTest
    public static void crucible(GameTestHelper helper) {
        swapKeepsTheStation(
                helper,
                BlockListMF.crucible,
                TileEntityCrucible.class,
                (lit, at) -> BlockCrucible.updateFurnaceBlockState(lit, helper.getWorld(), at.x(), at.y(), at.z()));
        helper.succeed();
    }

    @GameTest
    public static void blastHeater(GameTestHelper helper) {
        swapKeepsTheStation(
                helper,
                BlockListMF.blast_heater,
                minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH.class,
                (lit, at) -> BlockBFH.updateFurnaceBlockState(lit, helper.getWorld(), at.x(), at.y(), at.z()));
        helper.succeed();
    }
}
