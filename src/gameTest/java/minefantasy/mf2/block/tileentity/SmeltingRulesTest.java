package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.mojang.authlib.GameProfile;

import minefantasy.mf2.api.crafting.Requirements;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.refining.BlockCrucible;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

/**
 * What the research book says about crucibles, the blast furnace and bellows, as the code does it.
 */
@GameTestHolder("minefantasy2")
public class SmeltingRulesTest {

    private SmeltingRulesTest() {}

    // region crucibles

    @GameTest
    public static void anAdvancedCrucibleNeedsFirebricksAroundIt(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.lava);
        helper.setBlock(1, 2, 1, BlockListMF.crucibleadv);
        helper.setBlock(5, 1, 1, Blocks.lava);
        helper.setBlock(5, 2, 1, BlockListMF.crucibleadv);
        for (int[] side : new int[][] { { 4, 1 }, { 6, 1 }, { 5, 0 }, { 5, 2 } }) {
            helper.setBlock(side[0], 2, side[1], BlockListMF.firebricks);
        }
        TileEntityCrucible bare = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 2, 1);
        TileEntityCrucible walled = helper.assertTileEntityPresent(TileEntityCrucible.class, 5, 2, 1);
        assertEquals("a crucible without firebricks heats", 0F, bare.getTemperature(), 0F);
        assertEquals("the walled crucible does not take the lava's heat", 750F, walled.getTemperature(), 0F);
        helper.succeed();
    }

    private static FakePlayer holding(GameTestHelper helper, ItemStack held) {
        FakePlayer player = new FakePlayer(helper.getWorld(), new GameProfile(UUID.randomUUID(), Modders.SMITH));
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, held);
        return player;
    }

    /** The talisman trilogy used on a crucible at x; returns the block there afterwards. */
    private static FakePlayer useTrilogy(GameTestHelper helper, int x, BlockCrucible crucible) {
        helper.setBlock(x, 1, 1, crucible);
        TestPos pos = helper.absolute(x, 1, 1);
        FakePlayer player = holding(helper, new ItemStack(ComponentListMF.artefacts, 1, 3));
        crucible.onBlockActivated(helper.getWorld(), pos.x(), pos.y(), pos.z(), player, 1, 0F, 0F, 0F);
        return player;
    }

    @GameTest
    public static void theTrilogyTurnsAWorkingMythicCrucibleIntoAMasterOne(GameTestHelper helper) {
        FakePlayer used = useTrilogy(helper, 1, (BlockCrucible) BlockListMF.cruciblemythic_active);
        assertSame(
                BlockListMF.cruciblemaster_active,
                helper.getWorld().getBlock(
                        helper.absolute(1, 1, 1).x(),
                        helper.absolute(1, 1, 1).y(),
                        helper.absolute(1, 1, 1).z()));
        assertNull("the trilogy was not spent", used.getHeldItem());

        FakePlayer idle = useTrilogy(helper, 5, (BlockCrucible) BlockListMF.cruciblemythic);
        TestPos cold = helper.absolute(5, 1, 1);
        assertSame(
                "a cold mythic crucible changed",
                BlockListMF.cruciblemythic,
                helper.getWorld().getBlock(cold.x(), cold.y(), cold.z()));
        assertNotNull("a cold crucible took the trilogy", idle.getHeldItem());
        helper.succeed();
    }

    @GameTest
    public static void masterMetalsSmeltOnlyInAMasterCrucible(GameTestHelper helper) {
        for (Alloy[] metal : new Alloy[][] { KnowledgeListMF.mithium, KnowledgeListMF.ignotumite,
                KnowledgeListMF.enderforge }) {
            for (Alloy alloy : metal) {
                assertFalse(
                        "a mythic crucible smelts " + alloy.getRecipeOutput(),
                        Requirements.CRUCIBLE.stationFits(2, alloy.getLevel()));
                assertTrue(
                        "a master crucible refuses " + alloy.getRecipeOutput(),
                        Requirements.CRUCIBLE.stationFits(3, alloy.getLevel()));
            }
        }
        helper.succeed();
    }

    // endregion

    // region blast furnace

    /** A random source that always draws the given number. */
    private static Random drawing(int value) {
        return new Random() {

            @Override
            public int nextInt(int bound) {
                return value;
            }
        };
    }

    @GameTest
    public static void hardcoreIngotsKeepAThirdWithoutACrucible(GameTestHelper helper) {
        boolean reduce = ConfigHardcore.HCCreduceIngots;
        try {
            ConfigHardcore.HCCreduceIngots = true;
            assertTrue("the kept third was lost", TileEntityBlastFH.keepsLeftover(drawing(0)));
            assertFalse("a lost ingot was kept", TileEntityBlastFH.keepsLeftover(drawing(1)));
            assertFalse("a lost ingot was kept", TileEntityBlastFH.keepsLeftover(drawing(2)));
            ConfigHardcore.HCCreduceIngots = false;
            assertTrue("an ingot was lost with the rule off", TileEntityBlastFH.keepsLeftover(drawing(1)));
        } finally {
            ConfigHardcore.HCCreduceIngots = reduce;
        }
        helper.succeed();
    }

    @GameTest
    public static void theBlastFurnaceDropsIntoTheCrucibleBelowOrTheWorld(GameTestHelper helper) {
        boolean reduce = ConfigHardcore.HCCreduceIngots;
        try {
            ConfigHardcore.HCCreduceIngots = true;
            helper.setBlock(1, 1, 1, BlockListMF.crucible);
            helper.setBlock(1, 2, 1, BlockListMF.blast_heater);
            TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 1, 1);
            helper.assertTileEntityPresent(TileEntityBlastFH.class, 1, 2, 1).dropItem(new ItemStack(bar, 3));
            ItemStack caught = crucible.getStackInSlot(crucible.getSizeInventory() - 1);
            assertNotNull("the crucible caught nothing", caught);
            assertEquals("the crucible lost ingots", 3, caught.stackSize);

            ConfigHardcore.HCCreduceIngots = false;
            helper.setBlock(5, 2, 1, BlockListMF.blast_heater);
            helper.assertTileEntityPresent(TileEntityBlastFH.class, 5, 2, 1).dropItem(new ItemStack(bar, 2));
            TestPos at = helper.absolute(5, 2, 1);
            List<?> dropped = helper.getWorld().getEntitiesWithinAABB(
                    EntityItem.class,
                    AxisAlignedBB
                            .getBoundingBox(at.x() - 1, at.y() - 1, at.z() - 1, at.x() + 2, at.y() + 2, at.z() + 2));
            assertEquals("the ingots did not come out", 1, dropped.size());
            assertEquals(2, ((EntityItem) dropped.get(0)).getEntityItem().stackSize);
        } finally {
            ConfigHardcore.HCCreduceIngots = reduce;
        }
        helper.succeed();
    }

    // endregion

    // region bellows

    /** A lit forge at x burning fuel of the given heat, already that hot. */
    private static TileEntityForge forge(GameTestHelper helper, int x, net.minecraft.block.Block block, float heat) {
        helper.setBlock(x, 1, 1, block);
        TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, x, 1, 1);
        forge.fuel = 100;
        forge.fuelTemperature = heat;
        forge.temperature = heat;
        return forge;
    }

    private static float pump(TileEntityForge forge) {
        for (int i = 0; i < 20; i++) {
            forge.justShared = 0;
            forge.onUsedWithBellows(1F);
        }
        return forge.temperature;
    }

    @GameTest
    public static void bellowsRaiseAStoneForgeByHalfAndAMetalOneTwofold(GameTestHelper helper) {
        assertEquals("stone forge", 600F, pump(forge(helper, 1, BlockListMF.forge_active, 400F)), 0F);
        assertEquals("metal forge", 800F, pump(forge(helper, 3, BlockListMF.forge_metal_active, 400F)), 0F);
        assertEquals(
                "bellows went past the forge's limit",
                TileEntityForge.maxTemperature,
                pump(forge(helper, 5, BlockListMF.forge_metal_active, 4000F)),
                0F);
        helper.succeed();
    }

    // endregion
}
