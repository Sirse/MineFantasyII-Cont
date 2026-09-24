package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.Weather;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * Stations built from their blocks and worked the way a player works them: the anvil under the hammer, the spit over a
 * fire, the blast furnace put together from firebricks.
 */
@GameTestHolder("minefantasy2")
public class WorldStationTest {

    private WorldStationTest() {}

    private static FakePlayer holding(GameTestHelper helper, ItemStack held) {
        FakePlayer player = helper.spawnFakePlayer(Modders.SMITH);
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, held);
        return player;
    }

    // region anvil

    /** Strikes until the anvil pays out, left and right in turn so the quality balance never ruins the work. */
    private static ItemStack forge(TileEntityAnvilMF anvil, FakePlayer smith) {
        int output = anvil.getSizeInventory() - 1;
        for (int hit = 0; hit < 40 && anvil.getStackInSlot(output) == null; hit++) {
            anvil.tryCraft(smith, hit % 2 == 0);
        }
        return anvil.getStackInSlot(output);
    }

    private static TileEntityAnvilMF anvil(GameTestHelper helper, GridRecipe recipe, ItemStack onGrid) {
        reload(tx -> {
            tx.add(MFRecipes.HEATING, id("forge_heat", "ore"), Heatable.of(Input.of(ore), 100, 500, 900), 0);
            tx.add(MFRecipes.ANVIL, id("anvil", "test"), recipe, 0);
        });
        helper.setBlock(1, 1, 1, BlockListMF.anvilStone);
        TileEntityAnvilMF anvil = helper.assertTileEntityPresent(TileEntityAnvilMF.class, 1, 1, 1);
        anvil.setInventorySlotContents(0, onGrid);
        anvil.updateCraftingData();
        assertTrue("the anvil found no project", anvil.hasProject());
        return anvil;
    }

    private static GridRecipe.Builder oneCell(ItemStack input, ItemStack output) {
        return GridRecipe.shaped(GridRecipe.Grid.ANVIL, 1, 1, new Object[] { input }, null, output).tool("hammer", 0)
                .time(2);
    }

    @GameTest
    public static void anvilForgesUnderTheHammer(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityAnvilMF anvil = anvil(
                    helper,
                    oneCell(new ItemStack(junk, 2), new ItemStack(bar)).build(),
                    new ItemStack(junk, 3));
            ItemStack made = forge(anvil, holding(helper, new ItemStack(ToolListMF.hammerStone)));
            assertNotNull("the anvil made nothing", made);
            assertEquals(bar, made.getItem());
            assertEquals("the grid paid two", 1, anvil.getStackInSlot(0).stackSize);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void anvilWithoutAHammerDoesNotWork(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityAnvilMF anvil = anvil(
                    helper,
                    oneCell(new ItemStack(junk, 2), new ItemStack(bar)).build(),
                    new ItemStack(junk, 3));
            assertNull(forge(anvil, holding(helper, new ItemStack(seed))));
            assertEquals(3, anvil.getStackInSlot(0).stackSize);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void hotRecipeLeavesTheForgedPieceHot(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityAnvilMF anvil = anvil(
                    helper,
                    oneCell(new ItemStack(ore), new ItemStack(bar)).hot(true).build(),
                    heated(new ItemStack(ore), 300));
            ItemStack made = forge(anvil, holding(helper, new ItemStack(ToolListMF.hammerStone)));
            assertNotNull("the anvil made nothing", made);
            assertEquals("the result comes off the anvil hot", ComponentListMF.hotItem, made.getItem());
            assertEquals(bar, Heatable.getItem(made).getItem());
            assertTrue("it keeps the work heat", Heatable.getTemp(made) > 0);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region spit

    /** A lit firepit with a spit over it, the food put on from a player's hand. */
    private static TileEntityRoast spitOverFire(GameTestHelper helper, ItemStack food) {
        helper.setWeather(Weather.CLEAR);
        helper.setBlock(1, 1, 1, BlockListMF.firepit);
        TileEntityFirepit fire = helper.assertTileEntityPresent(TileEntityFirepit.class, 1, 1, 1);
        fire.fuel = 6000;
        fire.setLit(true);
        helper.setBlock(1, 2, 1, BlockListMF.roast);
        TileEntityRoast spit = helper.assertTileEntityPresent(TileEntityRoast.class, 1, 2, 1);
        FakePlayer cook = holding(helper, food);
        assertTrue("the spit took nothing", spit.interact(cook));
        return spit;
    }

    private static void cookingRecipe(String name, Input input, ItemStack output, int max, boolean canBurn) {
        reload(
                tx -> tx.add(
                        MFRecipes.COOKING,
                        id("cooking", name),
                        CookRecipe.of(input, output, new ItemStack(junk), 50, max, 3, 3, false, canBurn),
                        0));
    }

    @GameTest(timeoutTicks = 600)
    public static void spitCooksOverALitFire(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        cookingRecipe("seed", Input.of(seed).amount(2), new ItemStack(flour), 1000, false);
        TileEntityRoast spit = spitOverFire(helper, new ItemStack(seed, 3));
        assertEquals(
                "the spit takes what the recipe asks for; it cooks by "
                        + CookRecipe.find(new ItemStack(seed, 3), false).id,
                2,
                spit.getStackInSlot(0).stackSize);
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> spit.getStackInSlot(0) != null && spit.getStackInSlot(0).getItem() == flour);
    }

    @GameTest(timeoutTicks = 600)
    public static void spitBurnsFoodOverTooMuchHeat(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        // The fire gives 100 to 200; this food burns above 60
        cookingRecipe("ore", Input.of(ore), new ItemStack(bar), 60, true);
        TileEntityRoast spit = spitOverFire(helper, new ItemStack(ore));
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> spit.getStackInSlot(0) != null && spit.getStackInSlot(0).getItem() == junk);
    }

    private static CookRecipe potRecipe(int min) {
        return CookRecipe.of(Input.of(pot), new ItemStack(flour), null, min, 2000, 3, 3, false, false);
    }

    @GameTest(timeoutTicks = 800)
    public static void spitTakesALowerHeatAfterAReload(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        // The fire gives 100 to 200: too little for this recipe until a reload lowers what it needs
        reload(tx -> tx.add(MFRecipes.COOKING, id("cooking", "pot"), potRecipe(1000), 0));
        TileEntityRoast spit = spitOverFire(helper, new ItemStack(pot));
        Stations.keepUntilFinished();
        helper.startSequence().thenIdle(60).thenExecute(() -> {
            assertEquals("it cooked below the recipe's heat", pot, spit.getStackInSlot(0).getItem());
            assertEquals(0F, spit.progress, 0F);
            Stations.reload(helper, tx -> tx.add(MFRecipes.COOKING, id("cooking", "pot"), potRecipe(50), 0));
        }).thenWaitUntil(() -> {
            ItemStack food = spit.getStackInSlot(0);
            assertTrue("the food did not cook after the reload", food != null && food.getItem() == flour);
        }).thenSucceed();
    }

    @GameTest
    public static void aRecipeForSeveralDoesNotBlockTheOneForOne(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(tx -> {
                tx.add(
                        MFRecipes.COOKING,
                        id("cooking", "four"),
                        CookRecipe.of(
                                Input.of(metaBar).amount(4),
                                new ItemStack(flour),
                                null,
                                50,
                                500,
                                3,
                                3,
                                false,
                                false),
                        10);
                tx.add(
                        MFRecipes.COOKING,
                        id("cooking", "one"),
                        CookRecipe.of(Input.of(metaBar), new ItemStack(bar), null, 50, 500, 3, 3, false, false),
                        0);
            });
            assertEquals(bar, CookRecipe.find(new ItemStack(metaBar, 1), false).recipe.getOutput().getItem());
            assertEquals(flour, CookRecipe.find(new ItemStack(metaBar, 4), false).recipe.getOutput().getItem());

            TileEntityRoast spit = place(new TileEntityRoast());
            assertTrue("one item was refused", spit.interact(holding(helper, new ItemStack(metaBar))));
            assertEquals(1, spit.getStackInSlot(0).stackSize);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region blast furnace

    @GameTest(timeoutTicks = 200)
    public static void blastFurnaceSmeltsIntoTheCrucibleBelow(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        reload(
                tx -> tx.add(
                        MFRecipes.BLAST_FURNACE,
                        id("blast_furnace", "ore"),
                        ProcessRecipe.of(Input.of(ore), new ItemStack(bar)),
                        0));
        // Crucible, heater with firebricks on its corners, chamber with firebricks on its sides
        helper.setBlock(2, 1, 2, BlockListMF.crucible);
        helper.setBlock(2, 2, 2, BlockListMF.blast_heater);
        helper.setBlock(2, 3, 2, BlockListMF.blast_chamber);
        for (int[] corner : new int[][] { { 1, 1 }, { 3, 1 }, { 1, 3 }, { 3, 3 } }) {
            helper.setBlock(corner[0], 2, corner[1], BlockListMF.firebricks);
        }
        for (int[] side : new int[][] { { 1, 2 }, { 3, 2 }, { 2, 1 }, { 2, 3 } }) {
            helper.setBlock(side[0], 3, side[1], BlockListMF.firebricks);
        }
        TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 2, 1, 2);
        TileEntityBlastFH heater = helper.assertTileEntityPresent(TileEntityBlastFH.class, 2, 2, 2);
        TileEntityBlastFC chamber = helper.assertTileEntityPresent(TileEntityBlastFC.class, 2, 3, 2);
        heater.updateBuild();
        chamber.updateBuild();
        assertTrue("the heater is not built", heater.isBuilt);
        assertTrue("the chamber is not built", chamber.isBuilt);

        chamber.setInventorySlotContents(0, new ItemStack(carbon));
        chamber.setInventorySlotContents(1, new ItemStack(ore));
        heater.fuel = 10000;
        // Skip most of the long smelt; the last ticks run for real
        heater.progress = TileEntityBlastFH.maxProgress - 5;
        Stations.keepUntilFinished();

        int output = crucible.getSizeInventory() - 1;
        helper.succeedWhen(() -> {
            ItemStack made = crucible.getStackInSlot(output);
            if (made == null) {
                return false;
            }
            assertEquals(bar, made.getItem());
            assertNull("the chamber paid its ore", chamber.getStackInSlot(1));
            assertNull("the carbon item is spent on its first use", chamber.getStackInSlot(0));
            assertEquals("three uses of the carbon are left", 3, chamber.tempUses);
            return true;
        });
    }

    // endregion
}
