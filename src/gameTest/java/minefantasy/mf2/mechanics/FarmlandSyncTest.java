package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.mechanics.ProtectionFixtures.*;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.farming.FarmingHelper;
import minefantasy.mf2.gametest.DenyAll;
import minefantasy.mf2.gametest.DenyPlacing;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.gametest.Watcher;
import minefantasy.mf2.item.list.CustomToolListMF;

/** Issue #80: ruined farmland must change on the server and be shown so to the player's client. */
@GameTestHolder("minefantasy2")
public class FarmlandSyncTest {

    private FarmlandSyncTest() {}

    /** Every harvest ruins farmland while it runs. */
    private static void alwaysRuin(Runnable action) {
        try (Overrides overrides = new Overrides()) {
            overrides.set(() -> FarmingHelper.isEnabled, v -> FarmingHelper.isEnabled = v, true)
                    .set(() -> FarmingHelper.farmBreakCfg, v -> FarmingHelper.farmBreakCfg = v, 1000F);
            action.run();
        }
    }

    private static Block at(World world, TestPos pos) {
        return world.getBlock(pos.x(), pos.y(), pos.z());
    }

    private static Watcher scytheUser(GameTestHelper helper, TestPos grass) {
        Watcher watcher = new Watcher(helper, grass.x() + 0.5D, grass.y(), grass.z() + 2.5D);
        watcher.player.setCurrentItemOrArmor(0, CustomToolListMF.standard_scythe.construct("Iron", "OakWood"));
        return watcher;
    }

    /** As the server runs a right click on a block. */
    private static void swing(Watcher watcher, World world, TestPos grass) {
        ItemStack scythe = watcher.player.getHeldItem();
        ForgeHooks.onPlaceItemIntoWorld(
                scythe,
                watcher.player,
                world,
                grass.x(),
                grass.y(),
                grass.z(),
                1,
                0.5F,
                0.5F,
                0.5F);
    }

    @GameTest
    public static void aScytheRuinsFarmlandForTheClientToo(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.farmland);
        helper.setBlock(1, 2, 1, Blocks.tallgrass, 1);
        TestPos soil = helper.absolute(1, 1, 1);
        TestPos grass = helper.absolute(1, 2, 1);
        World world = helper.getWorld();
        try (Watcher watcher = scytheUser(helper, grass)) {
            alwaysRuin(() -> swing(watcher, world, grass));
            assertFalse("Forge's capture is left as it was", world.captureBlockSnapshots);
            assertEquals("the grass is cut", Blocks.air, at(world, grass));
            assertEquals("the farmland is ruined", Blocks.dirt, at(world, soil));
            watcher.flush();
            assertEquals("the client sees the grass cut", Blocks.air, watcher.shown(grass.x(), grass.y(), grass.z()));
            assertEquals("the client sees dirt", Blocks.dirt, watcher.shown(soil.x(), soil.y(), soil.z()));
        }
        endOfTick(helper);
        helper.succeed();
    }

    @GameTest
    public static void aProtectedScytheHarvestShowsTheGrassStill(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.farmland);
        helper.setBlock(1, 2, 1, Blocks.tallgrass, 1);
        TestPos soil = helper.absolute(1, 1, 1);
        TestPos grass = helper.absolute(1, 2, 1);
        World world = helper.getWorld();
        try (Overrides overrides = new Overrides().listen(new DenyAll()); Watcher watcher = scytheUser(helper, grass)) {
            alwaysRuin(() -> swing(watcher, world, grass));
            assertEquals(Blocks.tallgrass, at(world, grass));
            assertEquals(Blocks.farmland, at(world, soil));
            watcher.flush();
            assertEquals(
                    "the client is shown the grass back",
                    Blocks.tallgrass,
                    watcher.shown(grass.x(), grass.y(), grass.z()));
            Block soilShown = watcher.shown(soil.x(), soil.y(), soil.z());
            assertTrue("the client sees the farmland", soilShown == null || soilShown == Blocks.farmland);
        }
        endOfTick(helper);
        helper.succeed();
    }

    @GameTest
    public static void aHandHarvestRuinsFarmlandForTheClientToo(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.farmland);
        helper.setBlock(1, 2, 1, Blocks.wheat, 7);
        TestPos soil = helper.absolute(1, 1, 1);
        TestPos crop = helper.absolute(1, 2, 1);
        World world = helper.getWorld();
        try (Watcher watcher = new Watcher(helper, crop.x() + 0.5D, crop.y(), crop.z() + 2.5D)) {
            alwaysRuin(
                    () -> assertTrue(
                            "the crop is harvested",
                            watcher.player.theItemInWorldManager.tryHarvestBlock(crop.x(), crop.y(), crop.z())));
            assertEquals(Blocks.dirt, at(world, soil));
            watcher.flush();
            assertEquals("the client sees dirt", Blocks.dirt, watcher.shown(soil.x(), soil.y(), soil.z()));
        }
        endOfTick(helper);
        helper.succeed();
    }

    @GameTest
    public static void aRefusedReplacementShowsTheBlockBack(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.farmland);
        TestPos soil = helper.absolute(1, 1, 1);
        World world = helper.getWorld();
        DenyPlacing deny = new DenyPlacing();
        try (Overrides overrides = new Overrides().listen(deny);
                Watcher watcher = new Watcher(helper, soil.x() + 0.5D, soil.y() + 1, soil.z() + 2.5D)) {
            assertFalse(
                    ProtectionHelper.replaceBlock(watcher.player, world, soil.x(), soil.y(), soil.z(), Blocks.dirt, 0));
            assertEquals(
                    "the break event showed a hole first",
                    Blocks.air,
                    watcher.shown(soil.x(), soil.y(), soil.z()));
            watcher.flush();
            assertEquals(Blocks.farmland, at(world, soil));
            assertEquals("the client is shown it back", Blocks.farmland, watcher.shown(soil.x(), soil.y(), soil.z()));
        }
        endOfTick(helper);
        helper.succeed();
    }

    @GameTest
    public static void aReplacementInsideAnotherCaptureShowsNoHole(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.farmland);
        TestPos soil = helper.absolute(1, 1, 1);
        World world = helper.getWorld();
        try (Watcher watcher = new Watcher(helper, soil.x() + 0.5D, soil.y() + 1, soil.z() + 2.5D)) {
            world.captureBlockSnapshots = true;
            try {
                assertFalse(
                        ProtectionHelper
                                .replaceBlock(watcher.player, world, soil.x(), soil.y(), soil.z(), Blocks.dirt, 0));
            } finally {
                world.captureBlockSnapshots = false;
                world.capturedBlockSnapshots.clear();
            }
            watcher.flush();
            assertEquals(Blocks.farmland, at(world, soil));
            Block shown = watcher.shown(soil.x(), soil.y(), soil.z());
            assertTrue("the client is not shown a hole", shown == null || shown == Blocks.farmland);
        }
        endOfTick(helper);
        helper.succeed();
    }
}
