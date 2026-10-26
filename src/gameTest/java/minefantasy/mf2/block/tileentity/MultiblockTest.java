package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.gametest.Templates;

/** The multiblocks as their templates lay them out: built whole, and no longer once a part is gone. */
@GameTestHolder("minefantasy2")
public class MultiblockTest {

    private MultiblockTest() {}

    /** A heater over its crucible with firebricks at its corners, two chambers walled in firebrick above it. */
    @GameTest(template = Templates.BLAST_FURNACE)
    public static void aBlastFurnaceIsBuiltWhileItsWallsStand(GameTestHelper helper) {
        TileEntityBlastFH heater = helper.assertTileEntityPresent(TileEntityBlastFH.class, Templates.HEATER);
        TileEntityBlastFC chamber = helper.assertTileEntityPresent(TileEntityBlastFC.class, "chamber");
        TileEntityBlastFC top = helper.assertTileEntityPresent(TileEntityBlastFC.class, "chamber_top");
        helper.assertTileEntityPresent(TileEntityCrucible.class, "catcher");
        for (TileEntityBlastFC part : new TileEntityBlastFC[] { heater, chamber, top }) part.updateBuild();
        assertTrue("the heater is not built", heater.isBuilt);
        assertTrue("the chamber is not built", chamber.isBuilt);
        assertTrue("the top chamber is not built", top.isBuilt);

        helper.setBlock("heater_corner", Blocks.air);
        helper.setBlock("chamber_top_wall", Blocks.air);
        for (TileEntityBlastFC part : new TileEntityBlastFC[] { heater, chamber, top }) part.updateBuild();
        assertFalse("a heater missing a corner is built", heater.isBuilt);
        assertTrue("the lower chamber lost nothing", chamber.isBuilt);
        assertFalse("a chamber missing a wall is built", top.isBuilt);
        helper.succeed();
    }

    /** A big furnace and its heater loaded from a save that says they are not built look at their walls at once. */
    @GameTest(template = Templates.BIG_FURNACE)
    public static void aLoadedBigFurnaceChecksItsWallsOnTheFirstTick(GameTestHelper helper) {
        for (TileEntityBigFurnace part : new TileEntityBigFurnace[] { Templates.furnace(helper),
                Templates.heater(helper) }) {
            NBTTagCompound saved = new NBTTagCompound();
            part.writeToNBT(saved);
            assertFalse("the built flag is saved", saved.hasKey("Built"));
            saved.setBoolean("Built", false);
            TileEntityBigFurnace loaded = (TileEntityBigFurnace) TileEntity.createAndLoadEntity(saved);
            helper.getWorld().setTileEntity(part.xCoord, part.yCoord, part.zCoord, loaded);
            loaded.updateEntity();
            assertTrue("a loaded part is not built after its first tick", loaded.built);
        }
        helper.succeed();
    }

    /** A chamber loaded from a save that says it is not built looks at its bricks on its first tick. */
    @GameTest(template = Templates.BLAST_FURNACE)
    public static void aLoadedChamberChecksItsBricksOnTheFirstTick(GameTestHelper helper) {
        for (String label : new String[] { Templates.HEATER, "chamber" }) {
            TileEntityBlastFC part = helper.assertTileEntityPresent(TileEntityBlastFC.class, label);
            NBTTagCompound saved = new NBTTagCompound();
            part.writeToNBT(saved);
            assertFalse("the built flag is saved", saved.hasKey("isBuilt"));
            saved.setBoolean("isBuilt", false);
            TileEntityBlastFC loaded = (TileEntityBlastFC) TileEntity.createAndLoadEntity(saved);
            helper.getWorld().setTileEntity(part.xCoord, part.yCoord, part.zCoord, loaded);
            loaded.updateEntity();
            assertTrue("the " + label + " is not built after its first tick", loaded.isBuilt);
        }
        helper.succeed();
    }

}
