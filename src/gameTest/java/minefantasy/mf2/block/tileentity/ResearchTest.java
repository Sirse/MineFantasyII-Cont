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
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.ToolListMF;

/** A recipe that needs research is worked only by a player who has it, on the anvil and on the carpenter's bench. */
@GameTestHolder("minefantasy2")
public class ResearchTest {

    private ResearchTest() {}

    /** A research of the mod a new player does not have and can be given alone. */
    private static InformationBase lockedResearch() {
        for (InformationBase base : InformationList.nameMap.values()) {
            if (!base.isPreUnlocked() && base.parentInfo == null) {
                return base;
            }
        }
        fail("the mod has no research a new player lacks");
        return null;
    }

    /** A smith with a hammer, with or without the research. */
    private static FakePlayer smith(GameTestHelper helper, InformationBase research, boolean knows) {
        // Fake players are cached by name: the one who knows and the one who does not must be different players
        FakePlayer player = helper.spawnFakePlayer(knows ? Modders.SCHOLAR : Modders.NOVICE);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, new ItemStack(ToolListMF.hammerStone));
        if (knows) {
            ResearchLogic.forceUnlock(player, research);
        }
        assertEquals(knows, ResearchLogic.hasInfoUnlocked(player, research));
        return player;
    }

    private static GridRecipe recipe(GridRecipe.Grid grid, InformationBase research) {
        return GridRecipe.shaped(grid, 1, 1, new Object[] { new ItemStack(junk) }, null, new ItemStack(bar))
                .tool("hammer", 0).time(2).research(research.getUnlocalisedName()).build();
    }

    private static ItemStack forgeOnAnvil(GameTestHelper helper, boolean knows) {
        InformationBase research = lockedResearch();
        reload(tx -> tx.add(MFRecipes.ANVIL, id("anvil", "research"), recipe(GridRecipe.Grid.ANVIL, research), 0));
        helper.setBlock(1, 1, 1, BlockListMF.anvilStone);
        TileEntityAnvilMF anvil = helper.assertTileEntityPresent(TileEntityAnvilMF.class, 1, 1, 1);
        anvil.setInventorySlotContents(0, new ItemStack(junk));
        anvil.updateCraftingData();
        assertEquals(research.getUnlocalisedName(), anvil.getResearchNeeded());
        FakePlayer player = smith(helper, research, knows);
        int output = anvil.getSizeInventory() - 1;
        for (int hit = 0; hit < 20 && anvil.getStackInSlot(output) == null; hit++) {
            anvil.tryCraft(player, hit % 2 == 0);
        }
        return anvil.getStackInSlot(output);
    }

    @GameTest
    public static void anvilWorksAResearchedRecipeForAPlayerWhoKnowsIt(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            ItemStack made = forgeOnAnvil(helper, true);
            assertNotNull("the anvil made nothing", made);
            assertEquals(bar, made.getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void anvilRefusesAResearchedRecipeToAPlayerWhoDoesNot(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            assertNull("the anvil worked without the research", forgeOnAnvil(helper, false));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    private static ItemStack craftOnBench(GameTestHelper helper, boolean knows) {
        InformationBase research = lockedResearch();
        reload(
                tx -> tx.add(
                        MFRecipes.CARPENTER,
                        id("carpenter", "research"),
                        recipe(GridRecipe.Grid.BENCH, research),
                        0));
        helper.setBlock(1, 1, 1, BlockListMF.carpenter);
        TileEntityCarpenterMF bench = helper.assertTileEntityPresent(TileEntityCarpenterMF.class, 1, 1, 1);
        bench.setInventorySlotContents(0, new ItemStack(junk));
        bench.updateCraftingData();
        FakePlayer player = smith(helper, research, knows);
        int output = bench.getSizeInventory() - 5;
        for (int hit = 0; hit < 20 && bench.getStackInSlot(output) == null; hit++) {
            bench.tryCraft(player);
        }
        return bench.getStackInSlot(output);
    }

    @GameTest
    public static void benchWorksAResearchedRecipeForAPlayerWhoKnowsIt(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            ItemStack made = craftOnBench(helper, true);
            assertNotNull("the bench made nothing", made);
            assertEquals(bar, made.getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void benchRefusesAResearchedRecipeToAPlayerWhoDoesNot(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            assertNull("the bench worked without the research", craftOnBench(helper, false));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
