package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;
import static net.minecraftforge.common.util.ForgeDirection.UP;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;
import minefantasy.mf2.gametest.Templates;

/**
 * Stations turn with the structure they stand in: a template placed turned keeps its furnace built, facing the turned
 * way. The template's furnace faces north; each quarter turns it clockwise seen from above.
 */
@GameTestHolder("minefantasy2")
public class RotationTest {

    private RotationTest() {}

    private static final ForgeDirection[] TURNED = { ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH,
            ForgeDirection.WEST };

    private static void staysBuilt(GameTestHelper helper, int quarters) {
        helper.succeedWhen(() -> {
            TileEntityBigFurnace furnace = Templates.furnace(helper);
            TileEntityBigFurnace heater = Templates.heater(helper);
            assertEquals("the furnace faces the unturned way", TURNED[quarters], furnace.getFront());
            assertEquals("the heater faces the unturned way", TURNED[quarters], heater.getFront());
            // Built once both have looked at their walls; a turned one that is not never passes
            return furnace.built && heater.built;
        });
    }

    @GameTest(timeoutTicks = 60, template = Templates.BIG_FURNACE)
    public static void anUnturnedFurnaceIsBuilt(GameTestHelper helper) {
        staysBuilt(helper, 0);
    }

    @GameTest(timeoutTicks = 60, template = Templates.BIG_FURNACE, rotation = 1)
    public static void aFurnaceTurnedOnceIsBuilt(GameTestHelper helper) {
        staysBuilt(helper, 1);
    }

    @GameTest(timeoutTicks = 60, template = Templates.BIG_FURNACE, rotation = 2)
    public static void aFurnaceTurnedTwiceIsBuilt(GameTestHelper helper) {
        staysBuilt(helper, 2);
    }

    @GameTest(timeoutTicks = 60, template = Templates.BIG_FURNACE, rotation = 3)
    public static void aFurnaceTurnedThriceIsBuilt(GameTestHelper helper) {
        staysBuilt(helper, 3);
    }

    /** A rack turns only to face away from another wall: turned off every wall it would fall at once. */
    @GameTest
    public static void aRackTurnsOnlyOntoAWall(GameTestHelper helper) {
        // Hanging on the wall to its south, so facing north (2); the only other wall is to its west
        helper.setBlock(2, 1, 3, Blocks.stone);
        helper.setBlock(1, 1, 2, Blocks.stone);
        helper.setBlock(2, 1, 2, BlockListMF.rack_wood, 2);
        helper.assertTileEntityPresent(TileEntityRack.class, 2, 1, 2);
        TestPos rack = helper.absolute(2, 1, 2);
        World world = helper.getWorld();
        Block block = world.getBlock(rack.x(), rack.y(), rack.z());

        assertTrue(
                "the rack did not turn onto the west wall",
                block.rotateBlock(world, rack.x(), rack.y(), rack.z(), UP));
        assertEquals(
                "the rack does not face east, off the west wall",
                5,
                world.getBlockMetadata(rack.x(), rack.y(), rack.z()));
        assertFalse("the rack turned off every wall", block.rotateBlock(world, rack.x(), rack.y(), rack.z(), UP));
        assertEquals("a refused turn moved the rack", 5, world.getBlockMetadata(rack.x(), rack.y(), rack.z()));
        assertSame("the rack fell off its wall", BlockListMF.rack_wood, world.getBlock(rack.x(), rack.y(), rack.z()));
        helper.succeed();
    }
}
