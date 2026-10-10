package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.function.Consumer;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeTransaction;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedIngredients;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;

/**
 * Grid recipes on the real anvil and benches: heat, materials and repairs as the anvil works out its project, the
 * kitchen's dirt, and script recipes built the way the CraftTweaker adapters build them.
 */
@GameTestHolder("minefantasy2")
public class GridStationTest {

    private static int size(ItemStack stack) {
        return stack == null ? 0 : stack.stackSize;
    }

    private static ItemStack product(Object station) throws Exception {
        return (ItemStack) get(station, "recipe");
    }

    private static boolean hasProject(Object station) throws Exception {
        return get(station, "project") != null;
    }

    // region anvil

    private static final int ANVIL_WIDTH = GridRecipe.Grid.ANVIL.width;

    /** The recipes given, plus a heat profile for ore: ore must be worked hot, bars and blades need no heat. */
    private static void anvilRecipes(Consumer<RecipeTransaction> recipes) {
        reload(tx -> {
            tx.add(MFRecipes.HEATING, id("forge_heat", "ore"), Heatable.of(Input.of(ore), 100, 500, 900), 0);
            recipes.accept(tx);
        });
    }

    private static void anvilRecipe(GridRecipe recipe) {
        anvilRecipes(tx -> tx.add(MFRecipes.ANVIL, id("anvil", "test"), recipe, 0));
    }

    private static TileEntityAnvilMF anvil(ItemStack... row) {
        TileEntityAnvilMF anvil = new TileEntityAnvilMF();
        place(anvil);
        for (int i = 0; i < row.length; i++) {
            anvil.setInventorySlotContents(i, row[i]);
        }
        anvil.updateCraftingData();
        return anvil;
    }

    private static GridRecipe.Builder shapeless(GridRecipe.Grid grid, ItemStack output, ItemStack... inputs) {
        return GridRecipe.shapeless(grid, inputs, null, output);
    }

    @GameTest
    public static void anvilWorksHeatablePiecesOnlyAtWorkingHeat(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            anvilRecipe(
                    shapeless(GridRecipe.Grid.ANVIL, new ItemStack(bar), new ItemStack(ore)).tool("hammer", 0).build());
            assertFalse("cold", hasProject(anvil(new ItemStack(ore))));
            assertFalse("not yet workable", hasProject(anvil(heated(new ItemStack(ore), 50))));
            assertFalse("unstable", hasProject(anvil(heated(new ItemStack(ore), 600))));
            TileEntityAnvilMF working = anvil(heated(new ItemStack(ore), 300));
            assertTrue(hasProject(working));
            assertEquals(bar, product(working).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void hotPieceIsPaidWithItsOwnStackSize(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            anvilRecipe(
                    GridRecipe.shaped(
                            GridRecipe.Grid.ANVIL,
                            1,
                            1,
                            new Object[] { new ItemStack(ore, 2) },
                            null,
                            new ItemStack(bar)).build());
            // The carried stack was one ore when heated; the hot stack now holds two
            ItemStack piece = heated(new ItemStack(ore, 1), 300);
            assertFalse(hasProject(anvil(piece)));
            piece.stackSize = 2;
            assertTrue(hasProject(anvil(piece)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void hotRecipeAsksForAHotResult(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            anvilRecipe(shapeless(GridRecipe.Grid.ANVIL, new ItemStack(bar), new ItemStack(junk)).hot(true).build());
            assertTrue(anvil(new ItemStack(junk)).isOutputHot());
            anvilRecipe(shapeless(GridRecipe.Grid.ANVIL, new ItemStack(bar), new ItemStack(junk)).build());
            assertFalse(anvil(new ItemStack(junk)).isOutputHot());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void metalSetsTimeTiersResearchAndTheResultMaterial(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            anvilRecipe(
                    shapeless(GridRecipe.Grid.ANVIL, new ItemStack(blade), new ItemStack(bar)).tool("hammer", -1)
                            .time(10).research("tier").tiers(GridRecipe.Tiers.MATERIAL).build());
            TileEntityAnvilMF anvil = anvil(of(bar, steel));
            assertTrue(hasProject(anvil));
            assertEquals(10 * steel.craftTimeModifier, anvil.progressMax, 0.5F);
            assertEquals(3, anvil.getToolTierNeeded());
            assertEquals(3, anvil.getAnvilTierNeeded());
            assertEquals("smeltteststeel", anvil.getResearchNeeded());
            assertEquals(steel, CustomMaterial.getMaterialFor(product(anvil), CustomToolHelper.slot_main));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void fixedTiersIgnoreTheMetal(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            anvilRecipe(
                    shapeless(GridRecipe.Grid.ANVIL, new ItemStack(blade), new ItemStack(bar)).tool("hammer", 1)
                            .stationTier(1).time(10).build());
            TileEntityAnvilMF anvil = anvil(of(bar, steel));
            assertEquals(10F, anvil.progressMax, 0F);
            assertEquals(1, anvil.getToolTierNeeded());
            assertEquals(1, anvil.getAnvilTierNeeded());
            assertNull(CustomMaterial.getMaterialFor(product(anvil), CustomToolHelper.slot_main));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void mixedMetalsMakeNothing(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            anvilRecipe(
                    shapeless(GridRecipe.Grid.ANVIL, new ItemStack(blade), new ItemStack(bar), new ItemStack(bar))
                            .tiers(GridRecipe.Tiers.MATERIAL).build());
            assertTrue(hasProject(anvil(of(bar, steel), of(bar, steel))));
            assertFalse(hasProject(anvil(of(bar, steel), of(bar, bronze))));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void woodGoesToTheHaftOnTheAnvilAndIsTheMainMaterialOnTheBench(GameTestHelper helper)
            throws Exception {
        Stations.begin(helper);
        try {
            GridRecipe onAnvil = shapeless(
                    GridRecipe.Grid.ANVIL,
                    new ItemStack(blade),
                    new ItemStack(bar),
                    new ItemStack(junk)).tiers(GridRecipe.Tiers.MATERIAL).build();
            GridRecipe onBench = shapeless(GridRecipe.Grid.BENCH, new ItemStack(blade), new ItemStack(junk))
                    .tiers(GridRecipe.Tiers.MATERIAL).build();
            anvilRecipes(tx -> {
                tx.add(MFRecipes.ANVIL, id("anvil", "tool"), onAnvil, 0);
                tx.add(MFRecipes.CARPENTER, id("carpenter", "club"), onBench, 0);
            });
            ItemStack tool = product(anvil(of(bar, steel), of(junk, oak)));
            assertEquals(steel, CustomMaterial.getMaterialFor(tool, CustomToolHelper.slot_main));
            assertEquals(oak, CustomMaterial.getMaterialFor(tool, CustomToolHelper.slot_haft));

            TileEntityCarpenterMF bench = new TileEntityCarpenterMF();
            place(bench);
            bench.setInventorySlotContents(0, of(junk, oak));
            bench.updateCraftingData();
            assertEquals(oak, CustomMaterial.getMaterialFor(product(bench), CustomToolHelper.slot_main));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    private static ItemStack worn(int damage) {
        return new ItemStack(blade, 1, damage);
    }

    @GameTest
    public static void twoWornPiecesRepairIntoOne(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            anvilRecipes(tx -> {});
            TileEntityAnvilMF anvil = anvil(worn(60), worn(70));
            // 40 and 30 left, plus a tenth of the maximum
            assertEquals(20, product(anvil).getItemDamage());
            assertEquals(200F, anvil.progressMax, 0F);
            assertEquals("hammer", anvil.getToolNeeded());
            assertEquals(0, anvil.getAnvilTierNeeded());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void repairNeedsExactlyTwoOfTheSameItem(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            anvilRecipes(tx -> {});
            assertFalse(hasProject(anvil(worn(60))));
            assertFalse(hasProject(anvil(worn(60), worn(70), worn(10))));
            assertFalse(hasProject(anvil(worn(60), new ItemStack(junk))));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void repairGoesBeforeARecipeForTheSamePair(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            anvilRecipe(
                    shapeless(GridRecipe.Grid.ANVIL, new ItemStack(bar), new ItemStack(blade), new ItemStack(blade))
                            .build());
            TileEntityAnvilMF anvil = anvil(worn(60), worn(70));
            assertEquals(blade, product(anvil).getItem());
            assertEquals(20, product(anvil).getItemDamage());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region kitchen

    private static TileEntityKitchenBench kitchen(float dirt) {
        reload(
                tx -> tx.add(
                        MFRecipes.KITCHEN,
                        id("kitchen", "flour"),
                        shapeless(GridRecipe.Grid.BENCH, new ItemStack(flour), new ItemStack(seed)).tool("hands", 0)
                                .time(1).dirtyAmount(dirt).build(),
                        0));
        TileEntityKitchenBench bench = new TileEntityKitchenBench();
        place(bench);
        bench.setDirtyMax(50F);
        bench.setInventorySlotContents(0, new ItemStack(seed, 3));
        bench.updateCraftingData();
        return bench;
    }

    private static void craft(TileEntityKitchenBench bench) throws Exception {
        call(bench, "craftItem", new Class<?>[] { net.minecraft.entity.player.EntityPlayer.class }, (Object) null);
    }

    private static int kitchenOutput(TileEntityKitchenBench bench) {
        return bench.getSizeInventory() - 5;
    }

    @GameTest
    public static void eachCraftDirtiesTheBenchUntilItStops(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = kitchen(30F);
            craft(bench);
            assertEquals(30F, bench.dirtyProgress, 0F);
            assertFalse(bench.isDirty());
            craft(bench);
            assertTrue(bench.isDirty());
            assertEquals(2, size(bench.getStackInSlot(kitchenOutput(bench))));

            craft(bench);
            assertEquals("a dirty bench takes nothing", 1, size(bench.getStackInSlot(0)));
            assertEquals(2, size(bench.getStackInSlot(kitchenOutput(bench))));
            assertEquals(60F, bench.dirtyProgress, 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void cleanRecipeLeavesNoDirt(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = kitchen(0F);
            craft(bench);
            assertEquals(0F, bench.dirtyProgress, 0F);
            assertEquals(1, size(bench.getStackInSlot(kitchenOutput(bench))));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void dirtyBenchDropsItsProgress(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = kitchen(30F);
            bench.progress = 5;
            bench.dirtyProgress = 50F;
            bench.updateCraftingData();
            assertEquals(0F, bench.progress, 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void dirtSurvivesSaving(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = kitchen(30F);
            craft(bench);
            NBTTagCompound nbt = new NBTTagCompound();
            bench.writeToNBT(nbt);
            TileEntityKitchenBench loaded = new TileEntityKitchenBench();
            loaded.readFromNBT(nbt);
            assertEquals(30F, loaded.dirtyProgress, 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region scripts

    private static IItemStack script(ItemStack stack) {
        return MineTweakerMC.getIItemStack(stack);
    }

    private static TileEntityCarpenterMF bench(GridRecipe recipe, int... slotsAndCounts) {
        reload(tx -> tx.add(MFRecipes.CARPENTER, id("carpenter", "script"), recipe, 0));
        TileEntityCarpenterMF bench = new TileEntityCarpenterMF();
        place(bench);
        for (int i = 0; i < slotsAndCounts.length; i += 3) {
            bench.setInventorySlotContents(
                    slotsAndCounts[i],
                    new ItemStack(slotsAndCounts[i + 1] == 0 ? seed : ore, slotsAndCounts[i + 2]));
        }
        bench.updateCraftingData();
        return bench;
    }

    @GameTest
    public static void scriptShapedSitsInTheCornerAndIsNeverMirrored(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            GridRecipe recipe = TweakedIngredients.shaped(
                    GridRecipe.Grid.BENCH,
                    new IIngredient[][] { { script(new ItemStack(seed)), script(new ItemStack(ore)) } },
                    script(new ItemStack(bar)),
                    true).build();
            // slot, 0 = seed / 1 = ore, count
            assertTrue(hasProject(bench(recipe, 0, 0, 1, 1, 1, 1)));
            assertFalse("shifted", hasProject(bench(recipe, 5, 0, 1, 6, 1, 1)));
            assertFalse("mirrored", hasProject(bench(recipe, 0, 1, 1, 1, 0, 1)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void scriptAmountIsWhatTheSlotPays(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            GridRecipe recipe = TweakedIngredients.shapeless(
                    GridRecipe.Grid.BENCH,
                    new IIngredient[] { script(new ItemStack(ore)).amount(3) },
                    script(new ItemStack(bar))).time(1).build();
            assertFalse(hasProject(bench(recipe, 0, 1, 2)));
            TileEntityCarpenterMF bench = bench(recipe, 0, 1, 5);
            assertTrue(hasProject(bench));
            bench.progress = bench.progressMax;
            call(bench, "craftItem", new Class<?>[] { net.minecraft.entity.player.EntityPlayer.class }, (Object) null);
            assertEquals(2, size(bench.getStackInSlot(0)));
            assertEquals(bar, bench.getStackInSlot(bench.getSizeInventory() - 5).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void scriptAnvilCellJudgesAHotPieceByTheItemItCarries(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            anvilRecipe(
                    TweakedIngredients.shapeless(
                            GridRecipe.Grid.ANVIL,
                            new IIngredient[] { script(new ItemStack(ore)) },
                            script(new ItemStack(bar))).build());
            assertTrue(hasProject(anvil(heated(new ItemStack(ore), 300))));
            assertFalse("cooled past working heat", hasProject(anvil(heated(new ItemStack(ore), 50))));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void scriptInputKeepsItsAmountAndCondition(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            Input input = ScriptInputs.toInput(script(new ItemStack(ore, 1, 2)).amount(4));
            assertEquals(4, input.getAmount());
            assertTrue(input.matches(new ItemStack(ore, 4, 2)));
            assertFalse("other damage", input.matches(new ItemStack(ore, 4, 3)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void takesSeesStacksAndScriptEntries(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            GridRecipe nativeRecipe = shapeless(GridRecipe.Grid.BENCH, new ItemStack(bar), new ItemStack(ore)).build();
            GridRecipe scriptRecipe = TweakedIngredients.shapeless(
                    GridRecipe.Grid.BENCH,
                    new IIngredient[] { script(new ItemStack(seed)) },
                    script(new ItemStack(bar))).build();
            assertTrue(nativeRecipe.takes(new ItemStack(ore)));
            assertFalse(nativeRecipe.takes(new ItemStack(seed)));
            assertTrue(scriptRecipe.takes(new ItemStack(seed)));
            assertNotNull(scriptRecipe.getEntries().get(0));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion
}
