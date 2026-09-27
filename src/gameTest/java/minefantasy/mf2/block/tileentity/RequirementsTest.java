package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.Requirements;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeMetadata;
import minefantasy.mf2.api.recipe.RecipeRegistrationException;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * The shared judgement of what a recipe asks of the tool, the station and the player: which station refuses what and
 * which only works harder, and the stations, diagnostics and registration holding to it.
 */
@GameTestHolder("minefantasy2")
public class RequirementsTest {

    private RequirementsTest() {}

    private static final Requirements HAMMER_3 = new Requirements("hammer", 3, 3, "");

    // region rules

    @GameTest
    public static void anvilOnlyPenalisesAWeakHammerAndAnvil(GameTestHelper helper) throws Exception {
        Requirements.Verdict verdict = HAMMER_3.check(Requirements.ANVIL, "hammer", 1, 1, null);
        assertTrue("a weaker anvil refused the work", verdict.allows());
        assertTrue(verdict.isToolWeak());
        assertTrue(verdict.isStationWeak());
        assertEquals(CheckResult.Reason.of("harder", "tool", 1, 3), verdict.getPenalty());
        assertTrue(Requirements.ANVIL.stationFits(0, 5));

        Requirements.Verdict anvilOnly = HAMMER_3.check(Requirements.ANVIL, "hammer", 3, 1, null);
        assertFalse(anvilOnly.isToolWeak());
        assertEquals(CheckResult.Reason.of("harder", "anvil", 1, 3), anvilOnly.getPenalty());
        helper.succeed();
    }

    @GameTest
    public static void benchRefusesAWeakToolThenAWeakBench(GameTestHelper helper) throws Exception {
        assertEquals(
                CheckResult.Reason.tier("tool", 1, 3),
                HAMMER_3.check(Requirements.CARPENTER, "hammer", 1, 1, null).getRefusal());
        assertEquals(
                CheckResult.Reason.tier("bench", 1, 3),
                HAMMER_3.check(Requirements.CARPENTER, "hammer", 3, 1, null).getRefusal());
        assertFalse(Requirements.CARPENTER.stationFits(1, 3));
        assertNull(HAMMER_3.check(Requirements.CARPENTER, "hammer", 3, 3, null).getPenalty());
        helper.succeed();
    }

    @GameTest
    public static void theWrongToolStopsEveryStation(GameTestHelper helper) throws Exception {
        for (Requirements.Rules rules : new Requirements.Rules[] { Requirements.ANVIL, Requirements.CARPENTER,
                Requirements.KITCHEN, Requirements.TANNING }) {
            assertEquals(
                    rules.getStation(),
                    CheckResult.Reason.of("tool", "hammer", "knife"),
                    HAMMER_3.check(rules, "knife", 5, 5, null).getRefusal());
        }
        helper.succeed();
    }

    @GameTest
    public static void onlyTheKitchenTakesAnyToolForHands(GameTestHelper helper) throws Exception {
        Requirements hands = new Requirements("hands", 0, 0, "");
        assertTrue(hands.check(Requirements.KITCHEN, "knife", 0, 0, null).allows());
        assertFalse(hands.check(Requirements.CARPENTER, "knife", 0, 0, null).allows());
        helper.succeed();
    }

    @GameTest
    public static void aStationWithoutATierIgnoresNone(GameTestHelper helper) throws Exception {
        Requirements knife = new Requirements("knife", 2, 5, "");
        assertTrue(knife.check(Requirements.TANNING, "knife", 2, 0, null).allows());
        assertEquals(
                CheckResult.Reason.tier("tool", 1, 2),
                knife.check(Requirements.TANNING, "knife", 1, 0, null).getRefusal());
        assertThrows(IllegalArgumentException.class, () -> Requirements.KITCHEN.validate(0, 1));
        assertThrows(IllegalArgumentException.class, () -> Requirements.CRUCIBLE.validate(1, 0));
        Requirements.KITCHEN.validate(3, -1);
        helper.succeed();
    }

    @GameTest
    public static void aMissingResearchStopsThePlayerButNotAMachine(GameTestHelper helper) throws Exception {
        InformationBase locked = null;
        for (InformationBase base : InformationList.nameMap.values()) {
            if (!base.isPreUnlocked() && base.parentInfo == null) {
                locked = base;
                break;
            }
        }
        assertNotNull("the mod has no research a new player lacks", locked);
        Requirements needs = new Requirements("hammer", 0, 0, locked.getUnlocalisedName());
        FakePlayer novice = helper.spawnFakePlayer(Modders.NOVICE);
        assertEquals(
                CheckResult.Reason.of("research", locked.getUnlocalisedName()),
                needs.check(Requirements.CARPENTER, "hammer", 0, 0, novice).getRefusal());
        assertTrue(needs.check(Requirements.CARPENTER, "hammer", 0, 0, null).allows());
        helper.succeed();
    }

    // endregion

    // region stations and registration

    @GameTest
    public static void kitchenRecipesAskingForABenchTierAreRefused(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            assertThrows(
                    RecipeRegistrationException.class,
                    () -> reload(
                            tx -> tx.add(
                                    MFRecipes.KITCHEN,
                                    id("kitchen", "tiered"),
                                    GridRecipe.shaped(
                                            GridRecipe.Grid.BENCH,
                                            1,
                                            1,
                                            new Object[] { new ItemStack(junk) },
                                            null,
                                            new ItemStack(bar)).tool("hands", 0).stationTier(1).time(2).build(),
                                    0)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** A player holding a stone hammer, below tier 2. */
    private static FakePlayer weakSmith(GameTestHelper helper) {
        FakePlayer player = helper.spawnFakePlayer(Modders.NOVICE);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, new ItemStack(ToolListMF.hammerStone));
        assertTrue(ToolHelper.getCrafterTier(player.getHeldItem()) < 2);
        return player;
    }

    @GameTest
    public static void kitchenDiagnosisReportsAToolTooWeak(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.KITCHEN,
                            id("kitchen", "strong_hammer"),
                            GridRecipe.shaped(
                                    GridRecipe.Grid.BENCH,
                                    1,
                                    1,
                                    new Object[] { new ItemStack(junk) },
                                    null,
                                    new ItemStack(bar)).tool("hammer", 2).time(2).build(),
                            0));
            TileEntityKitchenBench bench = place(new TileEntityKitchenBench());
            bench.setInventorySlotContents(0, new ItemStack(junk));
            bench.updateCraftingData();
            FakePlayer player = weakSmith(helper);
            CheckResult.Reason reason = bench.diagnose(player).getCandidates().get(0).getReason();
            assertNotNull("the diagnosis let a hammer the bench refuses through", reason);
            assertEquals("tier", reason.getId());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    private static TileEntityTanningRack rack(int toolTier) {
        reload(
                tx -> tx.add(
                        MFRecipes.TANNING,
                        id("tanning", "tool_tier_" + toolTier),
                        ProcessRecipe.of(
                                Input.of(seed),
                                new ItemStack(flour),
                                RecipeMetadata.builder().put(MFRecipeKeys.TIME, 100F).put(MFRecipeKeys.TOOL, "hammer")
                                        .put(MFRecipeKeys.TOOL_TIER, toolTier).build()),
                        0));
        TileEntityTanningRack rack = place(new TileEntityTanningRack());
        rack.setInventorySlotContents(0, new ItemStack(seed));
        rack.updateRecipe();
        return rack;
    }

    @GameTest
    public static void rackWorksWithATierLowRackForTheToolItNeeds(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTanningRack rack = rack(0);
            assertEquals("the recipe's tier is the tool's", 0, rack.toolTier);
            rack.interact(weakSmith(helper), true, false);
            assertTrue("a basic rack refused a recipe for any tool", rack.progress > 0);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void rackRefusesAToolBelowTheRecipesTier(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityTanningRack rack = rack(3);
            FakePlayer player = weakSmith(helper);
            rack.interact(player, true, false);
            assertEquals("a weak tool worked the hide", 0F, rack.progress, 0F);
            CheckResult.Reason reason = rack.diagnose(player).getCandidates().get(0).getReason();
            assertEquals(CheckResult.Reason.tier("tool", ToolHelper.getCrafterTier(player.getHeldItem()), 3), reason);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion
}
