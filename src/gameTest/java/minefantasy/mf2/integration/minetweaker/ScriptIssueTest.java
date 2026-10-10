package minefantasy.mf2.integration.minetweaker;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.gametest.TestItems;

/** Reported script problems: removals that ignored a tag, and carbon a reload could not take back. */
@GameTestHolder(value = "minefantasy2", requiredMods = "MineTweaker3")
public class ScriptIssueTest {

    private ScriptIssueTest() {}

    private static String metal(ItemStack bar) {
        return bar.hasTagCompound() ? bar.getTagCompound().getCompoundTag("MF_CustomMaterials").getString("main_metal")
                : "";
    }

    /** Issue 76: a tag given to removeByOutput names only the outputs that carry it. */
    @GameTest
    public static void removeByOutputKeepsOutputsWithAnotherTag(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    "the script failed",
                    0,
                    Scripts.run(
                            "mods.minefantasy.Bloomery.removeByOutput(<minefantasy2:custom_bar>"
                                    + ".withTag({MF_CustomMaterials: {main_metal: \"Copper\"}}));")
                            .size());
            int copper = 0;
            for (RecipeEntry<BloomRecipe> entry : MFRecipes.BLOOMERY.published().all()) {
                if ("Copper".equals(metal(entry.getRecipe().getOutput()))) {
                    copper++;
                }
            }
            assertEquals("copper bars are still made", 0, copper);
            assertNotNull(
                    "iron went with the copper",
                    MFRecipes.BLOOMERY.published().get(RecipeId.parse("minefantasy2:bloomery/minecraft.iron_ore")));
        });
    }

    /** Issue 77: carbon a script adds leaves with the script. */
    @GameTest
    public static void scriptCarbonLeavesWithTheReload(GameTestHelper helper) throws Exception {
        ItemStack stick = new ItemStack(Items.stick);
        Scripts.around(helper, () -> {
            assertEquals(0, MineFantasyFuels.getCarbon(stick));
            assertEquals(
                    "the script failed",
                    0,
                    Scripts.run("mods.minefantasy.Fuel.addCarbon(<minecraft:stick>, 3);").size());
            assertEquals("the script's carbon is missing", 3, MineFantasyFuels.getCarbon(stick));
            Scripts.run("");
            assertEquals("the carbon outlived its script", 0, MineFantasyFuels.getCarbon(stick));
        });
    }

    /**
     * Carbon stands in for carbon only where the script asks for it with MF.carbon(): a named carbon item is that item,
     * and its conditions hold.
     */
    @GameTest
    public static void carbonIsInterchangeableOnlyWhenAsked(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Crucible.add(\"pure_carbon\", <minecraft:diamond>, 0, 1,"
                                    + " [<minefantasy2tests:carbon>.onlyWithTag({grade: \"pure\"})]);",
                            "mods.minefantasy.Crucible.add(\"named_carbon\", <minecraft:emerald>, 0, 1,"
                                    + " [<minefantasy2tests:carbon>]);",
                            "mods.minefantasy.Crucible.add(\"any_carbon\", <minecraft:gold_ingot>, 0, 1,"
                                    + " [mods.minefantasy.MF.carbon()]);")
                            .size());
            Alloy pure = MFRecipes.ALLOY.published().get(RecipeId.parse("crafttweaker:alloy/pure_carbon")).getRecipe();
            assertFalse(
                    "untagged carbon passed onlyWithTag",
                    pure.matches(new ItemStack[] { new ItemStack(TestItems.carbon) }));
            Alloy named = MFRecipes.ALLOY.published().get(RecipeId.parse("crafttweaker:alloy/named_carbon"))
                    .getRecipe();
            assertFalse(
                    "coal stood in for a named carbon item",
                    named.matches(new ItemStack[] { new ItemStack(Items.coal) }));
            assertTrue(named.matches(new ItemStack[] { new ItemStack(TestItems.carbon) }));
            Alloy any = MFRecipes.ALLOY.published().get(RecipeId.parse("crafttweaker:alloy/any_carbon")).getRecipe();
            assertTrue(
                    "coal does not stand in for MF.carbon()",
                    any.matches(new ItemStack[] { new ItemStack(Items.coal) }));
            assertFalse(
                    "MF.carbon() took something that is not carbon",
                    any.matches(new ItemStack[] { new ItemStack(Items.stick) }));
        });
    }

    /** Salvage cannot keep a condition, so a script giving one is refused rather than applied to every item. */
    @GameTest
    public static void salvageRefusesConditions(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertFalse(
                    "a condition was accepted",
                    Scripts.run(
                            "mods.minefantasy.Salvage.set(<minefantasy2tests:seed>.onlyWithTag({grade: \"pure\"}),"
                                    + " [<minecraft:diamond>]);")
                            .isEmpty());
            Object[] parts = Salvage.getSalvage(new ItemStack(TestItems.seed));
            assertTrue(
                    "the untagged seed salvages into the diamond",
                    parts == null || parts.length == 0 || ((ItemStack) parts[0]).getItem() != Items.diamond);
            assertEquals(
                    0,
                    Scripts.run("mods.minefantasy.Salvage.set(<minefantasy2tests:seed>, [<minecraft:diamond>]);")
                            .size());
            parts = Salvage.getSalvage(new ItemStack(TestItems.seed));
            assertSame("plain items no longer salvage", Items.diamond, ((ItemStack) parts[0]).getItem());
        });
    }

    /** The burn stage of a tagged result takes that result only, whatever other tags it gained. */
    @GameTest
    public static void aTaggedResultBurnsAlone(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Cooking.add(\"graded\", <minecraft:stick>.withTag({grade: \"cooked\"}),"
                                    + " <minefantasy2tests:seed>, 100, 300, 20, 10, false, true);")
                            .size());
            Input burn = MFRecipes.COOKING.published().get(RecipeId.parse("crafttweaker:cooking/graded_burnt"))
                    .getRecipe().getInput();
            assertFalse("an untagged stick burns", burn.matches(new ItemStack(Items.stick)));
            ItemStack cooked = new ItemStack(Items.stick);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("grade", "cooked");
            tag.setInteger("heat", 50);
            cooked.setTagCompound(tag);
            assertTrue("the cooked stick no longer burns", burn.matches(cooked));
        });
    }

    /**
     * removeAccepting takes the recipes that would take the stack, whatever condition they check, and leaves the
     * neighbour that would not.
     */
    @GameTest
    public static void removeAcceptingTakesWhatWouldTakeTheStack(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Quern.add(\"graded_stick\", <minecraft:diamond>,"
                                    + " <minecraft:stick>.onlyWithTag({grade: \"pure\"}));",
                            "mods.minefantasy.Quern.add(\"plain_bone\", <minecraft:diamond>, <minecraft:bone>);",
                            "mods.minefantasy.Quern.removeAccepting(<minecraft:stick>.withTag({grade: \"pure\", extra: 1}));")
                            .size());
            assertNull(
                    "the recipe taking the stack survived",
                    MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/graded_stick")));
            assertNotNull(
                    "the neighbour went too",
                    MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/plain_bone")));
        });
    }

    /** A removal finding another number of recipes than the script expected removes none of them. */
    @GameTest
    public static void anUnexpectedCountRemovesNothing(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            List<String> errors = Scripts.run(
                    "mods.minefantasy.Quern.add(\"one\", <minecraft:emerald>, <minecraft:bone>);",
                    "mods.minefantasy.Quern.add(\"two\", <minecraft:emerald>, <minecraft:stick>);",
                    "mods.minefantasy.Quern.removeByOutput(<minecraft:emerald>, 1);");
            assertTrue(
                    "the count mismatch was not reported: " + errors,
                    errors.stream().anyMatch(e -> e.contains("expected 1 recipes, found 2")));
            assertNotNull(MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/one")));
            assertNotNull(MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/two")));
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Quern.add(\"one\", <minecraft:emerald>, <minecraft:bone>);",
                            "mods.minefantasy.Quern.add(\"two\", <minecraft:emerald>, <minecraft:stick>);",
                            "mods.minefantasy.Quern.removeByOutput(<minecraft:emerald>, 2);").size());
            assertNull(MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/one")));
        });
    }

    /** removeAccepting asks an anvil recipe about the cold stack a player would heat for it. */
    @GameTest
    public static void removeAcceptingFindsAnvilRecipesByTheColdItem(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Anvil.addShapeless(\"cold_bone\", <minecraft:diamond>, \"artisanry\","
                                    + " \"\", false, \"hammer\", 0, 0, 10, [<minecraft:bone>]);",
                            "mods.minefantasy.Anvil.removeAccepting(<minecraft:bone>, 1);").size());
            assertNull(MFRecipes.ANVIL.published().get(RecipeId.parse("crafttweaker:anvil/cold_bone")));
        });
    }

    /** An ingredient listing no items, as {@code <*>} may, is a script error rather than a crash. */
    @GameTest
    public static void anIngredientWithoutItemsIsAnError(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            List<String> errors = Scripts.run("mods.minefantasy.Salvage.set(<*>, [<minecraft:diamond>]);");
            assertFalse("<*> was accepted", errors.isEmpty());
            assertTrue("<*> crashed: " + errors, errors.stream().noneMatch(e -> e.contains("NullPointerException")));
        });
    }

    /** A refusal names the recipe, the input and the reason, and says the recipe was not registered. */
    @GameTest
    public static void errorsSayWhatToFix(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            String[][] cases = {
                    { "mods.minefantasy.Anvil.addShaped(\"reused\", <minecraft:diamond>, \"\", \"\", false,"
                            + " \"hammer\", 0, 0, 10, [[<minecraft:stick>, <minecraft:bone>.reuse()]]);",
                            "crafttweaker:anvil/reused", "input at row 1, column 2", "transformers" },
                    { "mods.minefantasy.CarpenterBench.addShapeless(\"reused\", <minecraft:diamond>, \"\", \"\", \"\","
                            + " 0.0, \"hands\", 0, 0, 10, [<minecraft:stick>, <minecraft:bone>.reuse()]);",
                            "crafttweaker:carpenter/reused", "ingredient 2", "transformers" },
                    { "mods.minefantasy.Crucible.add(\"reused\", <minecraft:diamond>, 0, 1,"
                            + " [<minecraft:stick>, <minecraft:bone>.reuse()]);", "crafttweaker:alloy/reused",
                            "ingredient 2", "transformers" },
                    { "mods.minefantasy.Quern.add(\"reused\", <minecraft:diamond>, <minecraft:bone>.reuse());",
                            "crafttweaker:quern/reused", "input:", "transformers" },
                    { "mods.minefantasy.Anvil.addShapeless(\"skilled\", <minecraft:diamond>, \"juggling\", \"\","
                            + " false, \"hammer\", 0, 0, 10, [<minecraft:stick>]);", "crafttweaker:anvil/skilled",
                            "unknown skill juggling", "" },
                    { "mods.minefantasy.Bloomery.add(\"studied\", <minecraft:diamond>, <minecraft:stick>,"
                            + " \"noSuchResearch\");", "crafttweaker:bloomery/studied", "is not a known research", "" },
                    { "mods.minefantasy.Quern.add(\"stel\", <minefantasy2:custom_bar>.withTag("
                            + "{MF_CustomMaterials: {main_metal: \"stel\"}}), <minecraft:stick>);",
                            "crafttweaker:quern/stel", "unknown material 'stel'", "" } };
            for (String[] c : cases) {
                List<String> errors = Scripts.run(c[0]);
                assertTrue(
                        c[1] + " did not say " + c[2] + ": " + errors,
                        errors.stream().anyMatch(
                                e -> e.contains(c[1]) && e.contains(c[2])
                                        && e.contains(c[3])
                                        && e.endsWith("Nothing changed.")));
            }
        });
    }
}
