package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeMetadata;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * The shabby tanning rack may fall apart as it finishes a hide, one time in ten, as the book warns; the leather still
 * comes out. A properly made rack never breaks.
 */
@GameTestHolder("minefantasy2")
public class TanningRackTest {

    private TanningRackTest() {}

    /** A rack whose every roll comes out 0: a shabby one always falls apart, into no planks. */
    private static TileEntityTanningRack rack(GameTestHelper helper, Block block) throws Exception {
        reload(
                tx -> tx.add(
                        MFRecipes.TANNING,
                        id("tanning", "shabby"),
                        ProcessRecipe.of(
                                Input.of(seed),
                                new ItemStack(flour),
                                RecipeMetadata.builder().put(MFRecipeKeys.TIME, 0.1F).build()),
                        0));
        helper.setBlock(1, 1, 1, block);
        TileEntityTanningRack rack = helper.assertTileEntityPresent(TileEntityTanningRack.class, 1, 1, 1);
        setField(rack, "rand", new Random() {

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        });
        rack.setInventorySlotContents(0, new ItemStack(seed));
        rack.updateRecipe();
        return rack;
    }

    private static void cut(GameTestHelper helper, TileEntityTanningRack rack) {
        FakePlayer tanner = Modders.fresh(helper, Modders.SMITH);
        tanner.inventory.currentItem = 0;
        tanner.inventory.setInventorySlotContents(0, new ItemStack(ToolListMF.knifeStone));
        rack.interact(tanner, false, false);
    }

    @GameTest
    public static void aShabbyRackMayBreakButGivesTheLeather(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTanningRack rack = rack(helper, BlockListMF.tanner);
            cut(helper, rack);
            TestPos at = helper.absolute(1, 1, 1);
            assertTrue("the shabby rack held", helper.getWorld().isAirBlock(at.x(), at.y(), at.z()));
            assertEquals("the leather was lost with the rack", 1, dropped(flour));
            assertEquals("the hide came back as well", 0, dropped(seed));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aProperRackNeverBreaks(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTanningRack rack = rack(helper, BlockListMF.advTanner);
            cut(helper, rack);
            TestPos at = helper.absolute(1, 1, 1);
            assertSame(
                    "the strong rack broke",
                    BlockListMF.advTanner,
                    helper.getWorld().getBlock(at.x(), at.y(), at.z()));
            assertNotNull("the hide was not finished", rack.getStackInSlot(0));
            assertEquals(flour, rack.getStackInSlot(0).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
