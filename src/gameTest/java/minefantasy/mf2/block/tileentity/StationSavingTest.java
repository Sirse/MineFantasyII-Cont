package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.chunk.Chunk;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;

/** A station's chunk is marked for saving whenever its slots change, however they change. */
@GameTestHolder("minefantasy2")
public class StationSavingTest {

    private StationSavingTest() {}

    @GameTest
    public static void changingASlotMarksTheChunkForSaving(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.research);
        TestPos at = helper.absolute(1, 1, 1);
        TileEntityResearch table = (TileEntityResearch) helper.getWorld().getTileEntity(at.x(), at.y(), at.z());
        Chunk chunk = helper.getWorld().getChunkFromBlockCoords(at.x(), at.z());

        chunk.isModified = false;
        table.setInventorySlotContents(0, new ItemStack(Items.book));
        assertTrue("putting an item in", chunk.isModified);

        chunk.isModified = false;
        table.decrStackSize(0, 1);
        assertTrue("taking it out", chunk.isModified);
        helper.succeed();
    }
}
