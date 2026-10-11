package minefantasy.mf2.integration.minetweaker;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;

/** Fluent station calls run through the actual ZenScript compiler and reload, including guarded replacement. */
@GameTestHolder(value = "minefantasy2", requiredMods = "MineTweaker3")
public class StationBuilderScriptTest {

    private StationBuilderScriptTest() {}

    @GameTest
    public static void stationBuildersNameTheirOptions(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            List<String> errors = Scripts.run(
                    "mods.minefantasy.Cooking.recipe(\"built_bread\", <minecraft:bread>, <minecraft:wheat> * 3).heat(150, 250).time(60).burnsAfter(20).oven().priority(7).register();",
                    "mods.minefantasy.Quern.recipe(\"built_dust\", <minecraft:redstone>, <minecraft:stick>).tier(2).consumePot().register();",
                    "mods.minefantasy.TanningRack.recipe(\"built_leather\", <minecraft:leather>, <minecraft:rotten_flesh> * 4).time(2.0).tool(\"shears\", 1).register();",
                    "mods.minefantasy.Crucible.recipe(\"built_alloy\", <minecraft:diamond>, [<minecraft:iron_ingot>]).tier(1).ratio(2).register();",
                    "mods.minefantasy.PaintOil.recipe(\"built_paint\", <minecraft:stone>, <minecraft:cobblestone>).register();");
            assertTrue("builder scripts failed: " + errors, errors.isEmpty());
            CookRecipe cooking = MFRecipes.COOKING.published().get(RecipeId.parse("crafttweaker:cooking/built_bread"))
                    .getRecipe();
            assertEquals(150, cooking.getMinTemperature());
            assertEquals(250, cooking.getMaxTemperature());
            assertEquals(60, cooking.getTime());
            assertEquals(20, cooking.getBurnTime());
            assertTrue(cooking.isBaking());
            assertTrue(cooking.getInput().hasEnough(new ItemStack(Items.wheat, 3)));
            assertFalse(cooking.getInput().hasEnough(new ItemStack(Items.wheat, 2)));
            assertNotNull(MFRecipes.COOKING.published().get(RecipeId.parse("crafttweaker:cooking/built_bread_burnt")));
            ProcessRecipe quern = MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/built_dust"))
                    .getRecipe();
            assertEquals(2, quern.get(MFRecipeKeys.TIER, 0).intValue());
            assertTrue(quern.get(MFRecipeKeys.CONSUME_POT, false));
            ProcessRecipe tanning = MFRecipes.TANNING.published()
                    .get(RecipeId.parse("crafttweaker:tanning/built_leather")).getRecipe();
            assertEquals(2F, tanning.get(MFRecipeKeys.TIME, 0F), 0F);
            assertEquals("shears", tanning.get(MFRecipeKeys.TOOL, ""));
            assertEquals(1, tanning.get(MFRecipeKeys.TOOL_TIER, 0).intValue());
            assertNotNull(MFRecipes.ALLOY.published().get(RecipeId.parse("crafttweaker:alloy/built_alloy_x2")));
            ProcessRecipe paint = MFRecipes.PAINT_OIL.published()
                    .get(RecipeId.parse("crafttweaker:paint_oil/built_paint")).getRecipe();
            assertTrue(paint.getInput().matches(new ItemStack(Blocks.cobblestone)));
            assertEquals(new ItemStack(Blocks.stone).getItem(), paint.getOutput().getItem());
            Scripts.run("");
            assertNull(MFRecipes.COOKING.published().get(RecipeId.parse("crafttweaker:cooking/built_bread")));
            assertNull(MFRecipes.ALLOY.published().get(RecipeId.parse("crafttweaker:alloy/built_alloy_x2")));
        });
    }

    @GameTest
    public static void acceptingReplacementKeepsNativeIdsAndReloadRestoresThem(GameTestHelper helper) throws Exception {
        RecipeId id = Scripts.natives(MFRecipes.QUERN, 1).get(0);
        RecipeEntry<ProcessRecipe> original = MFRecipes.QUERN.published().get(id);
        // Replace all recipes accepting this example, rather than depending on a native id's spelling.
        ItemStack input = original.getRecipe().getInput().examples().get(0);
        int count = 0;
        for (RecipeEntry<ProcessRecipe> entry : MFRecipes.QUERN.published().all()) {
            if (entry.getRecipe().getInput().matches(input)) {
                count++;
            }
        }
        int expected = count;
        Scripts.around(helper, () -> {
            List<String> errors = Scripts.run(
                    "mods.minefantasy.Quern.recipe(\"replacement\", <minecraft:diamond>, <minecraft:bone>).replaceAccepting("
                            + Scripts.bracket(input)
                            + ", "
                            + expected
                            + ");");
            assertTrue("replaceAccepting failed: " + errors, errors.isEmpty());
            RecipeEntry<ProcessRecipe> replaced = MFRecipes.QUERN.published().get(id);
            assertEquals(original.getOrder(), replaced.getOrder());
            assertEquals(Items.diamond, replaced.getRecipe().getOutput().getItem());
            assertTrue(replaced.getRecipe().getInput().matches(new ItemStack(Items.bone)));
            assertNull(MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/replacement")));
        });
        assertSame(original.getRecipe(), MFRecipes.QUERN.published().get(id).getRecipe());
    }

    @GameTest
    public static void guardedReplacementAndInvalidBuildersLeaveNoPartialChanges(GameTestHelper helper)
            throws Exception {
        Scripts.around(helper, () -> {
            String a = "mods.minefantasy.Cooking.recipe(\"a\", <minecraft:bread>, <minecraft:stick>).heat(100, 300).time(20).burnsAfter(10).register();";
            String b = "mods.minefantasy.Cooking.recipe(\"b\", <minecraft:apple>, <minecraft:stick>).heat(100, 300).time(20).burnsAfter(10).register();";
            String change = "mods.minefantasy.Cooking.recipe(\"unused\", <minecraft:diamond>, <minecraft:bone>).heat(100, 300).time(30).noBurn()";
            assertFalse(Scripts.run(a, b, change + ".replaceAccepting(<minecraft:stick>);").isEmpty());
            RecipeId id = RecipeId.parse("crafttweaker:cooking/a");
            assertEquals(Items.bread, MFRecipes.COOKING.published().get(id).getRecipe().getOutput().getItem());
            assertNotNull(MFRecipes.COOKING.published().get(CookRecipe.burntId(id)));
            assertEquals(0, Scripts.run(a, b, change + ".replaceAccepting(<minecraft:stick>, 2);").size());
            assertEquals(Items.diamond, MFRecipes.COOKING.published().get(id).getRecipe().getOutput().getItem());
            assertNull(MFRecipes.COOKING.published().get(CookRecipe.burntId(id)));
            // The second action sees the first action's changed input, so finds nothing and changes nothing.
            assertFalse(
                    Scripts.run(
                            a,
                            change + ".replaceAccepting(<minecraft:stick>);",
                            change + ".replaceAccepting(<minecraft:stick>);").isEmpty());
            assertEquals(Items.diamond, MFRecipes.COOKING.published().get(id).getRecipe().getOutput().getItem());
            assertFalse(
                    Scripts.run(
                            a,
                            "mods.minefantasy.Cooking.recipe(\"unused\", <minecraft:diamond>, <minecraft:bone>).heat(300, 100).time(20).replaceAccepting(<minecraft:stick>);")
                            .isEmpty());
            assertEquals(Items.bread, MFRecipes.COOKING.published().get(id).getRecipe().getOutput().getItem());
            assertNotNull(MFRecipes.COOKING.published().get(CookRecipe.burntId(id)));
            assertFalse(
                    Scripts.run(
                            "val r = mods.minefantasy.Quern.recipe(\"repeat\", <minecraft:diamond>, <minecraft:stick>); r.register(); r.register();")
                            .isEmpty());
            assertNotNull(MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/repeat")));
            assertFalse(
                    Scripts.run(
                            "mods.minefantasy.Cooking.recipe(\"missing_heat\", <minecraft:bread>, <minecraft:wheat>).time(20).register();")
                            .isEmpty());
            assertNull(MFRecipes.COOKING.published().get(RecipeId.parse("crafttweaker:cooking/missing_heat")));
        });
    }

    @GameTest
    public static void stationBuildersRejectMissingOptionsAndRepeatedRegistration(GameTestHelper helper)
            throws Exception {
        Scripts.around(helper, () -> {
            assertFalse(
                    Scripts.run(
                            "mods.minefantasy.TanningRack.recipe(\"missing_time\", <minecraft:leather>, <minecraft:rotten_flesh>).register();")
                            .isEmpty());
            assertNull(MFRecipes.TANNING.published().get(RecipeId.parse("crafttweaker:tanning/missing_time")));
            assertFalse(
                    Scripts.run(
                            "mods.minefantasy.Crucible.recipe(\"bad_ratio\", <minecraft:diamond>, [<minecraft:iron_ingot>]).ratio(0).register();")
                            .isEmpty());
            assertNull(MFRecipes.ALLOY.published().get(RecipeId.parse("crafttweaker:alloy/bad_ratio")));
            assertFalse(
                    Scripts.run(
                            "val r = mods.minefantasy.Crucible.recipe(\"repeat_alloy\", <minecraft:diamond>, [<minecraft:iron_ingot>]).ratio(2); r.register(); r.register();")
                            .isEmpty());
            assertNotNull(MFRecipes.ALLOY.published().get(RecipeId.parse("crafttweaker:alloy/repeat_alloy_x2")));
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Cooking.recipe(\"default_burn\", <minecraft:bread>, <minecraft:stick>).heat(100, 300).time(60).register();")
                            .size());
            assertEquals(
                    30,
                    MFRecipes.COOKING.published().get(RecipeId.parse("crafttweaker:cooking/default_burn")).getRecipe()
                            .getBurnTime());
        });
    }

    @GameTest
    public static void unknownDesignRefusesSetAndRemoveWithoutChangingCraftsOrSalvage(GameTestHelper helper)
            throws Exception {
        Scripts.around(helper, () -> {
            String valid = "mods.minefantasy.SpecialForging.set(\"ornate\", <minecraft:iron_sword>, <minecraft:diamond>);";
            String[] invalid = {
                    "mods.minefantasy.SpecialForging.set(\"ornat\", <minecraft:iron_sword>, <minecraft:emerald>);",
                    "mods.minefantasy.SpecialForging.remove(\"ornat\", <minecraft:iron_sword>);" };
            for (String line : invalid) {
                List<String> errors = Scripts.run(valid, line);
                assertFalse("unknown design accepted", errors.isEmpty());
                assertTrue(errors.toString().contains("unknown special forging design 'ornat'"));
                assertTrue(errors.toString().contains("ornate"));
                assertTrue(errors.toString().contains("dragonforge"));
                assertTrue(errors.toString().contains("Nothing changed."));
                assertEquals(
                        Items.diamond,
                        minefantasy.mf2.api.crafting.exotic.SpecialForging
                                .getSpecialCraft("ornate", new ItemStack(Items.iron_sword)));
                assertNull(
                        minefantasy.mf2.api.crafting.exotic.SpecialForging
                                .getSpecialCraft("ornat", new ItemStack(Items.iron_sword)));
                assertNull(
                        MFRecipes.SALVAGE.published().get(minefantasy.mf2.api.crafting.Salvage.aliasId(Items.emerald)));
                assertEquals(
                        Items.iron_sword,
                        MFRecipes.SALVAGE.published().get(minefantasy.mf2.api.crafting.Salvage.aliasId(Items.diamond))
                                .getRecipe().getAlias());
            }
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.SpecialForging.set(\"dragonforge\", <minecraft:iron_sword>, <minecraft:diamond>);")
                            .size());
            assertEquals(
                    Items.diamond,
                    minefantasy.mf2.api.crafting.exotic.SpecialForging
                            .getSpecialCraft("dragonforge", new ItemStack(Items.iron_sword)));
        });
    }

    @GameTest
    public static void designDiscoveryRunsOnceForManyScriptActionsAndRefreshesOnReload(GameTestHelper helper)
            throws Exception {
        Scripts.around(helper, () -> {
            String[] lines = new String[100];
            java.util.Arrays.fill(
                    lines,
                    "mods.minefantasy.SpecialForging.set(\"qa_design\", <minecraft:iron_sword>, <minecraft:diamond>);");
            int before = minefantasy.mf2.gametest.TestItems.designProbe.reads;
            assertEquals(0, Scripts.run(lines).size());
            assertEquals(
                    "each script line rescanned the item registry",
                    1,
                    minefantasy.mf2.gametest.TestItems.designProbe.reads - before);
            assertEquals(
                    Items.diamond,
                    minefantasy.mf2.api.crafting.exotic.SpecialForging
                            .getSpecialCraft("qa_design", new ItemStack(Items.iron_sword)));
            before = minefantasy.mf2.gametest.TestItems.designProbe.reads;
            assertEquals(0, Scripts.run(lines[0]).size());
            assertEquals(
                    "reload did not rediscover registered designs",
                    1,
                    minefantasy.mf2.gametest.TestItems.designProbe.reads - before);
            assertEquals(
                    0,
                    Scripts.run(
                            lines[0],
                            "mods.minefantasy.SpecialForging.remove(\"qa_design\", <minecraft:iron_sword>);").size());
            assertNull(
                    minefantasy.mf2.api.crafting.exotic.SpecialForging
                            .getSpecialCraft("qa_design", new ItemStack(Items.iron_sword)));
        });
    }

    @GameTest
    public static void unnamedDesignListsChoicesAndChangesNeitherCraftNorSalvage(GameTestHelper helper)
            throws Exception {
        Scripts.around(helper, () -> {
            List<String> errors = Scripts
                    .run("mods.minefantasy.SpecialForging.set(\"\", <minecraft:stick>, <minecraft:diamond>);");
            assertFalse(errors.isEmpty());
            assertTrue(errors.toString().contains("ornate"));
            assertTrue(errors.toString().contains("dragonforge"));
            assertFalse(
                    MFRecipes.SPECIAL_FORGING.published().all().stream()
                            .anyMatch(entry -> entry.getRecipe().getBase() == Items.stick));
            assertNull(MFRecipes.SALVAGE.published().get(minefantasy.mf2.api.crafting.Salvage.aliasId(Items.diamond)));
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.SpecialForging.set(\"ornate\", <minecraft:stick>, <minecraft:diamond>);")
                            .size());
            assertNotNull(
                    MFRecipes.SPECIAL_FORGING.published()
                            .get(minefantasy.mf2.api.crafting.exotic.SpecialForging.idFor("ornate", Items.stick)));
            assertEquals(
                    Items.stick,
                    MFRecipes.SALVAGE.published().get(minefantasy.mf2.api.crafting.Salvage.aliasId(Items.diamond))
                            .getRecipe().getAlias());
        });
    }

    @GameTest
    public static void positionalPaintKeepsItsOrderAndWarnsOncePerLoad(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            List<String> warnings = Scripts.warnings(
                    "mods.minefantasy.PaintOil.add(\"old_paint\", <minecraft:cobblestone>, <minecraft:stone>);",
                    "mods.minefantasy.PaintOil.add(\"other_paint\", <minecraft:dirt>, <minecraft:stone>);");
            assertEquals(1, warnings.size());
            ProcessRecipe paint = MFRecipes.PAINT_OIL.published()
                    .get(RecipeId.parse("crafttweaker:paint_oil/old_paint")).getRecipe();
            assertTrue(paint.getInput().matches(new ItemStack(Blocks.cobblestone)));
            assertEquals(new ItemStack(Blocks.stone).getItem(), paint.getOutput().getItem());
            assertEquals(
                    1,
                    Scripts.warnings(
                            "mods.minefantasy.PaintOil.add(\"old_paint\", <minecraft:cobblestone>, <minecraft:stone>);")
                            .size());
        });
    }
}
