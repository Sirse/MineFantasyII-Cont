package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.mojang.authlib.GameProfile;

import minefantasy.mf2.api.crafting.Requirements;
import minefantasy.mf2.api.heating.AmbientTemperature;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.refining.BlockCrucible;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.gametest.Templates;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

/**
 * What the research book says about crucibles, the blast furnace and bellows, as the code does it.
 */
@GameTestHolder("minefantasy2")
public class SmeltingRulesTest {

    private SmeltingRulesTest() {}

    // region crucibles

    /** A fireclay crucible walled in firebrick on an advanced forge stays coated only while all four walls stand. */
    @GameTest(template = Templates.CRUCIBLE_COATED)
    public static void aCrucibleLosesItsCoatingWithAWall(GameTestHelper helper) {
        TileEntityCrucible crucible = Templates.crucible(helper);
        assertTrue("the walled crucible is not coated", crucible.isCoated());
        helper.setBlock(Templates.CRUCIBLE_WALLS[0], Blocks.air);
        assertFalse("a crucible with a wall gone stayed coated", crucible.isCoated());
        helper.succeed();
    }

    /**
     * The same crucible on lava without walls takes no source heat, and is coated and takes the lava's heat only once
     * all four firebricks are laid around it.
     */
    @GameTest(template = Templates.CRUCIBLE_NO_WALLS)
    public static void anAdvancedCrucibleNeedsFirebricksAroundIt(GameTestHelper helper) {
        TileEntityCrucible crucible = Templates.crucible(helper);
        helper.setBlock(Templates.HEAT_SOURCE, Blocks.lava);
        assertFalse("a bare crucible is coated", crucible.isCoated());
        assertEquals(
                "a crucible without firebricks takes source heat",
                AmbientTemperature.get(helper.getWorld(), crucible.xCoord, crucible.yCoord, crucible.zCoord),
                crucible.getTemperature(),
                0F);
        for (int wall = 0; wall < Templates.CRUCIBLE_WALLS.length; wall++) {
            helper.setBlock(Templates.CRUCIBLE_WALLS[wall], BlockListMF.firebricks);
            assertEquals(
                    "coated with " + (wall + 1) + " walls",
                    wall == Templates.CRUCIBLE_WALLS.length - 1,
                    crucible.isCoated());
        }
        assertEquals("the walled crucible does not take the lava's heat", 750F, crucible.getTemperature(), 0F);
        helper.succeed();
    }

    /** A stone crucible needs no walls: it takes the heat of lava under it as it stands. */
    @GameTest(template = Templates.CRUCIBLE_STONE)
    public static void aStoneCrucibleTakesHeatWithoutWalls(GameTestHelper helper) {
        TileEntityCrucible crucible = Templates.crucible(helper);
        assertEquals("an unlit forge gave heat", 0F, crucible.getWorkingHeat(), 0F);
        helper.setBlock(Templates.HEAT_SOURCE, Blocks.lava);
        assertEquals("the stone crucible did not take the lava's heat", 750F, crucible.getWorkingHeat(), 0F);
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
    public static void aCrucibleBecomesAMasterOneWithAllItHolds(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.cruciblemythic_active);
        TestPos at = helper.absolute(1, 1, 1);
        TileEntityCrucible before = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 1, 1);
        before.setInventorySlotContents(0, new ItemStack(Items.iron_ingot, 5));
        before.setInventorySlotContents(before.getSizeInventory() - 1, new ItemStack(Items.gold_ingot, 3));
        assertEquals(2, before.getTier());
        FakePlayer player = holding(helper, new ItemStack(ComponentListMF.artefacts, 1, 3));
        ((BlockCrucible) BlockListMF.cruciblemythic_active)
                .onBlockActivated(helper.getWorld(), at.x(), at.y(), at.z(), player, 1, 0F, 0F, 0F);
        assertSame(BlockListMF.cruciblemaster_active, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        TileEntityCrucible after = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 1, 1);
        assertSame("the crucible was replaced", before, after);
        assertEquals("the crucible kept its old tier", 3, after.getTier());
        assertEquals(5, after.getStackInSlot(0).stackSize);
        assertEquals(3, after.getStackInSlot(after.getSizeInventory() - 1).stackSize);
        AxisAlignedBB around = AxisAlignedBB
                .getBoundingBox(at.x() - 3, at.y() - 3, at.z() - 3, at.x() + 4, at.y() + 4, at.z() + 4);
        assertTrue(
                "its contents were spilled",
                helper.getWorld().getEntitiesWithinAABB(EntityItem.class, around).isEmpty());
        helper.succeed();
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
        try (Overrides overrides = new Overrides()
                .set(() -> ConfigHardcore.HCCreduceIngots, v -> ConfigHardcore.HCCreduceIngots = v, true)) {
            assertTrue("the kept third was lost", TileEntityBlastFH.keepsLeftover(drawing(0)));
            assertFalse("a lost ingot was kept", TileEntityBlastFH.keepsLeftover(drawing(1)));
            assertFalse("a lost ingot was kept", TileEntityBlastFH.keepsLeftover(drawing(2)));
            overrides.set(() -> ConfigHardcore.HCCreduceIngots, v -> ConfigHardcore.HCCreduceIngots = v, false);
            assertTrue("an ingot was lost with the rule off", TileEntityBlastFH.keepsLeftover(drawing(1)));
        }
        helper.succeed();
    }

    @GameTest
    public static void theBlastFurnaceDropsIntoTheCrucibleBelowOrTheWorld(GameTestHelper helper) {
        try (Overrides overrides = new Overrides()
                .set(() -> ConfigHardcore.HCCreduceIngots, v -> ConfigHardcore.HCCreduceIngots = v, true)) {
            helper.setBlock(1, 1, 1, BlockListMF.crucible);
            helper.setBlock(1, 2, 1, BlockListMF.blast_heater);
            TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 1, 1);
            helper.assertTileEntityPresent(TileEntityBlastFH.class, 1, 2, 1).dropItem(new ItemStack(bar, 3));
            ItemStack caught = crucible.getStackInSlot(crucible.getSizeInventory() - 1);
            assertNotNull("the crucible caught nothing", caught);
            assertEquals("the crucible lost ingots", 3, caught.stackSize);

            overrides.set(() -> ConfigHardcore.HCCreduceIngots, v -> ConfigHardcore.HCCreduceIngots = v, false);
            helper.setBlock(5, 2, 1, BlockListMF.blast_heater);
            helper.assertTileEntityPresent(TileEntityBlastFH.class, 5, 2, 1).dropItem(new ItemStack(bar, 2));
            TestPos at = helper.absolute(5, 2, 1);
            List<?> dropped = helper.getWorld().getEntitiesWithinAABB(
                    EntityItem.class,
                    AxisAlignedBB
                            .getBoundingBox(at.x() - 1, at.y() - 1, at.z() - 1, at.x() + 2, at.y() + 2, at.z() + 2));
            assertEquals("the ingots did not come out", 1, dropped.size());
            assertEquals(2, ((EntityItem) dropped.get(0)).getEntityItem().stackSize);
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
