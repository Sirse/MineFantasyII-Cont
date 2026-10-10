package minefantasy.mf2.integration.minetweaker;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.gametest.TestItems;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.CustomToolListMF;

/** {@code mods.minefantasy.MF} material helpers, and the warnings for ingredients that do not mean what they say. */
@GameTestHolder(value = "minefantasy2", requiredMods = "MineTweaker3")
public class MaterialScriptTest {

    private MaterialScriptTest() {}

    private static ItemStack bar(String metal) {
        return ComponentListMF.bar.createComm(metal);
    }

    private static String main(ItemStack stack) {
        CustomMaterial material = CustomToolHelper.getCustomPrimaryMaterial(stack);
        return material == null ? null : material.name.toLowerCase();
    }

    @GameTest
    public static void aMaterialInputTakesThatMaterialOnly(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    "the script failed",
                    0,
                    Scripts.run(
                            "mods.minefantasy.Quern.add(\"steel_dust\", <minecraft:redstone>,"
                                    + " mods.minefantasy.MF.input(<minefantasy2:custom_bar>, \"Steel\"));")
                            .size());
            Input input = MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/steel_dust")).getRecipe()
                    .getInput();
            assertTrue("a steel bar is refused", input.matches(bar("steel")));
            assertFalse("a copper bar is taken", input.matches(bar("copper")));
            assertEquals("NEI would show another bar", "steel", main(input.examples().get(0)));
        });
    }

    @GameTest
    public static void aMaterialStackIsMadeOfItsMaterials(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    "the script failed",
                    0,
                    Scripts.run(
                            "mods.minefantasy.Quern.add(\"steel_bar\","
                                    + " mods.minefantasy.MF.stack(<minefantasy2:custom_bar>, \"steel\"), <minecraft:stick>);")
                            .size());
            ProcessRecipe recipe = MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/steel_bar"))
                    .getRecipe();
            assertEquals("steel", main(recipe.getOutput()));
            assertFalse(
                    "an unknown material was accepted",
                    Scripts.run("val x = mods.minefantasy.MF.stack(<minefantasy2:custom_bar>, \"nomaterial\");")
                            .isEmpty());
        });
    }

    @GameTest
    public static void withTagOnAnInputIsWarnedAbout(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            List<String> tagged = Scripts.warnings(
                    "mods.minefantasy.Quern.add(\"tagged\", <minecraft:redstone>, <minefantasy2:custom_bar>"
                            + ".withTag({MF_CustomMaterials: {main_metal: \"steel\"}}));");
            assertTrue(
                    "no warning for withTag on an input: " + tagged,
                    tagged.stream().anyMatch(w -> w.contains("withTag")));
            List<String> material = Scripts.warnings(
                    "mods.minefantasy.Quern.add(\"material\", <minecraft:redstone>,"
                            + " mods.minefantasy.MF.input(<minefantasy2:custom_bar>, \"steel\"));");
            assertFalse(
                    "MF.input was warned about: " + material,
                    material.stream().anyMatch(w -> w.contains("withTag")));
        });
    }

    @GameTest
    public static void specialForgingWarnsWhatItIgnores(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            List<String> warnings = Scripts.warnings(
                    "mods.minefantasy.SpecialForging.setOrnate(<minecraft:stick>.withTag({grade: \"fine\"}),"
                            + " <minecraft:diamond>);");
            assertTrue(
                    "SpecialForging dropped the tag quietly: " + warnings,
                    warnings.stream().anyMatch(w -> w.contains("SpecialForging") && w.contains("tag")));
        });
    }

    /** A script bar is the bar MineFantasy makes: same tag, so the two stack, however the material is spelt. */
    @GameTest
    public static void aScriptBarStacksWithACraftedOne(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Quern.add(\"spelt\","
                                    + " mods.minefantasy.MF.stack(<minefantasy2:custom_bar>, \"sTeEl\"), <minecraft:stick>);")
                            .size());
            ItemStack scripted = MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/spelt")).getRecipe()
                    .getOutput();
            assertTrue(
                    "the script bar does not stack with a crafted one",
                    ItemStack.areItemStackTagsEqual(scripted, bar("Steel")));
        });
    }

    /** Bars asked for in lower case, as the crucible's alloys are, are the same bars as the rest. */
    @GameTest
    public static void barsAreNamedAsTheirMaterialWasRegistered(GameTestHelper helper) {
        assertTrue(
                "a bronze bar from the crucible does not stack with one from elsewhere",
                ItemStack.areItemStackTagsEqual(ComponentListMF.bar("bronze"), ComponentListMF.bar("Bronze")));
        helper.succeed();
    }

    /** MF.carbon() keeps its amount and mark: coal stands in for two carbon. */
    @GameTest
    public static void carbonStandsInForAnAmount(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Crucible.add(\"two_carbon\", <minecraft:emerald>, 0, 1,"
                                    + " [mods.minefantasy.MF.carbon().marked(\"c\") * 2]);")
                            .size());
            Alloy alloy = MFRecipes.ALLOY.published().get(RecipeId.parse("crafttweaker:alloy/two_carbon")).getRecipe();
            assertTrue(
                    "coal no longer stands in for two carbon",
                    alloy.matches(new ItemStack[] { new ItemStack(Items.coal, 2) }));
        });
    }

    /** "any" is how MineFantasy names an item without a material, never a material a script can ask for. */
    @GameTest
    public static void anyIsNotAMaterial(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            for (String line : new String[] { "val a = mods.minefantasy.MF.stack(<minefantasy2:custom_bar>, \"any\");",
                    "val b = mods.minefantasy.MF.input(<minefantasy2:custom_bar>, \"Any\");",
                    "val c = mods.minefantasy.MF.input(<minefantasy2:standard_sword>, \"steel\", \"ANY\");",
                    "val d = mods.minefantasy.MF.inputNoHaft(<minefantasy2:standard_sword>, \"any\");" }) {
                List<String> errors = Scripts.run(line);
                assertTrue(
                        "any was taken as a material: " + line + " " + errors,
                        errors.stream().anyMatch(e -> e.contains("not a material")));
            }
        });
    }

    /** An input without a haft takes any haft, none included; inputNoHaft takes none only. */
    @GameTest
    public static void anUnnamedHaftIsAnyHaft(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Quern.add(\"any_haft\", <minecraft:redstone>,"
                                    + " mods.minefantasy.MF.input(<minefantasy2:standard_sword>, \"steel\"));",
                            "mods.minefantasy.Quern.add(\"no_haft\", <minecraft:glowstone_dust>,"
                                    + " mods.minefantasy.MF.inputNoHaft(<minefantasy2:standard_sword>, \"steel\"));",
                            "mods.minefantasy.Quern.add(\"oak_haft\", <minecraft:sugar>,"
                                    + " mods.minefantasy.MF.input(<minefantasy2:standard_sword>, \"steel\", \"oakwood\"));")
                            .size());
            ItemStack bare = sword("steel", null);
            ItemStack oak = sword("steel", "oakwood");
            ItemStack spruce = sword("steel", "sprucewood");
            ItemStack iron = sword("iron", "oakwood");
            Input anyHaft = quern("any_haft");
            assertTrue("a bare steel sword was refused", anyHaft.matches(bare));
            assertTrue("an oak hafted steel sword was refused", anyHaft.matches(oak));
            assertFalse("an iron sword was taken", anyHaft.matches(iron));
            Input noHaft = quern("no_haft");
            assertTrue(noHaft.matches(bare));
            assertFalse("inputNoHaft took a hafted sword", noHaft.matches(oak));
            Input oakHaft = quern("oak_haft");
            assertTrue(oakHaft.matches(oak));
            assertFalse("a spruce haft passed for oak", oakHaft.matches(spruce));
            assertFalse("no haft passed for oak", oakHaft.matches(bare));
        });
    }

    private static ItemStack sword(String main, String haft) {
        ItemStack sword = new ItemStack(CustomToolListMF.standard_sword);
        CustomMaterial.addMaterial(sword, CustomToolHelper.slot_main, main);
        if (haft != null) {
            CustomMaterial.addMaterial(sword, CustomToolHelper.slot_haft, haft);
        }
        return sword;
    }

    private static Input quern(String name) {
        return MFRecipes.QUERN.published().get(RecipeId.parse("crafttweaker:quern/" + name)).getRecipe().getInput();
    }

    /** A plain item is that item in any material: nothing is guessed from a listed example. */
    @GameTest
    public static void aPlainItemTakesAnyMaterial(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Quern.add(\"any_bar\", <minecraft:redstone>, <minefantasy2:custom_bar>);")
                            .size());
            Input input = quern("any_bar");
            assertTrue(input.matches(bar("steel")));
            assertTrue(input.matches(bar("copper")));
        });
    }

    /** MF.input counts its amount; transformers on it are refused. */
    @GameTest
    public static void aMaterialInputKeepsAmountAndRefusesTransformers(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Quern.add(\"three_steel\", <minecraft:redstone>,"
                                    + " mods.minefantasy.MF.input(<minefantasy2:custom_bar>, \"steel\") * 3);")
                            .size());
            Input input = quern("three_steel");
            assertEquals(3, input.getAmount());
            assertTrue("a stack of four steel bars was refused", input.matches(ComponentListMF.bar("Steel", 4)));
            assertFalse(
                    "a reused material input was accepted",
                    Scripts.run(
                            "mods.minefantasy.Quern.add(\"reuse\", <minecraft:redstone>,"
                                    + " mods.minefantasy.MF.input(<minefantasy2:custom_bar>, \"steel\").reuse());")
                            .isEmpty());
        });
    }

    /** Special forging keeps the item only, so a material input is refused rather than widened to every material. */
    @GameTest
    public static void specialForgingRefusesAMaterialInput(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            List<String> errors = Scripts.run(
                    "mods.minefantasy.SpecialForging.setOrnate(mods.minefantasy.MF.input(<minefantasy2:custom_bar>,"
                            + " \"steel\"), <minecraft:diamond>);");
            assertTrue(
                    "SpecialForging took a material input: " + errors,
                    errors.stream().anyMatch(e -> e.contains("keeps items only")));
        });
    }

    /** MF.carbon() counts carbon a later script line adds, wherever the line stands. */
    @GameTest
    public static void carbonAddedAfterTheRecipeCounts(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Quern.add(\"any_carbon\", <minecraft:redstone>, mods.minefantasy.MF.carbon());",
                            "mods.minefantasy.Fuel.addCarbon(<minecraft:stick>, 2);").size());
            Input input = quern("any_carbon");
            assertTrue("carbon added after the recipe is refused", input.matches(new ItemStack(Items.stick)));
            assertTrue(input.matches(new ItemStack(Items.coal)));
            assertFalse(input.matches(new ItemStack(Items.bone)));
            assertNotNull(
                    "the station lookup does not find the later carbon",
                    MFRecipes.find(MFRecipes.QUERN, new ItemStack(Items.stick)));
        });
    }

    /** Salvage keeps an item and its materials, so "no haft" cannot be kept and is refused. */
    @GameTest
    public static void salvageRefusesNoHaft(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            List<String> errors = Scripts.run(
                    "mods.minefantasy.Salvage.set(mods.minefantasy.MF.inputNoHaft(<minefantasy2:standard_sword>,"
                            + " \"steel\"), [<minecraft:diamond>]);");
            assertTrue(
                    "no haft was taken as any haft: " + errors,
                    errors.stream().anyMatch(e -> e.contains("keeps items only")));
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Salvage.set(mods.minefantasy.MF.input(<minefantasy2:standard_sword>,"
                                    + " \"steel\", \"oakwood\"), [<minecraft:diamond>]);")
                            .size());
        });
    }

    /** Item-only APIs take one item per line: alternatives are refused rather than read out of CraftTweaker. */
    @GameTest
    public static void itemOnlyApisRefuseAlternatives(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            for (String line : new String[] {
                    "mods.minefantasy.Salvage.set(<minecraft:stick> | <minecraft:bone>, [<minecraft:diamond>]);",
                    "mods.minefantasy.SpecialForging.setOrnate(<minecraft:stick> | <minecraft:bone>, <minecraft:diamond>);" }) {
                List<String> errors = Scripts.run(line);
                assertTrue(line + ": " + errors, errors.stream().anyMatch(e -> e.contains("one line per item")));
            }
        });
    }

    /** Alternatives with MF.carbon(), either way round, still count carbon added after the recipe. */
    @GameTest
    public static void carbonInAlternativesCountsLaterCarbon(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.Quern.add(\"carbon_or_bone\", <minecraft:redstone>,"
                                    + " mods.minefantasy.MF.carbon() | <minecraft:bone>);",
                            "mods.minefantasy.Quern.add(\"bone_or_carbon\", <minecraft:glowstone_dust>,"
                                    + " <minecraft:bone> | mods.minefantasy.MF.carbon());",
                            "mods.minefantasy.Fuel.addCarbon(<minecraft:stick>, 2);").size());
            for (String name : new String[] { "carbon_or_bone", "bone_or_carbon" }) {
                Input input = quern(name);
                assertTrue(name + " refused carbon added later", input.matches(new ItemStack(Items.stick)));
                assertTrue(name, input.matches(new ItemStack(Items.bone)));
                assertTrue(name, input.matches(new ItemStack(Items.coal)));
                assertFalse(name, input.matches(new ItemStack(Items.apple)));
            }
            assertNotNull(
                    "the station lookup does not find later carbon through alternatives",
                    MFRecipes.find(MFRecipes.QUERN, new ItemStack(Items.stick)));
        });
    }

    /**
     * A grid recipe is asked by its own rule which items it takes, as NEI's usage pages ask it: withTag restricts
     * nothing, so any bar counts, while MF.input keeps to its material.
     */
    @GameTest
    public static void aGridRecipeTakesByItsRuleNotItsExamples(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.CarpenterBench.shapeless(\"tagged\", <minecraft:redstone>)"
                                    + ".ingredients([<minefantasy2:custom_bar>.withTag({MF_CustomMaterials: {main_metal: \"steel\"}})])"
                                    + ".tool(\"hands\", 0).time(10).register();",
                            "mods.minefantasy.CarpenterBench.shapeless(\"material\", <minecraft:glowstone_dust>)"
                                    + ".ingredients([mods.minefantasy.MF.input(<minefantasy2:custom_bar>, \"steel\")])"
                                    + ".tool(\"hands\", 0).time(10).register();")
                            .size());
            minefantasy.mf2.api.crafting.GridRecipe tagged = MFRecipes.CARPENTER.published()
                    .get(RecipeId.parse("crafttweaker:carpenter/tagged")).getRecipe();
            minefantasy.mf2.api.crafting.GridRecipe material = MFRecipes.CARPENTER.published()
                    .get(RecipeId.parse("crafttweaker:carpenter/material")).getRecipe();
            assertTrue("withTag was taken as a restriction", tagged.takes(bar("copper")));
            assertTrue(material.takes(bar("steel")));
            assertFalse("MF.input took another material", material.takes(bar("copper")));
        });
    }

    /** Carbon added for one metadata is that metadata only; the wildcard adds every one. */
    @GameTest
    public static void carbonKeepsToItsMetadata(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(0, Scripts.run("mods.minefantasy.Fuel.addCarbon(<minefantasy2tests:meta_bar:3>, 2);").size());
            assertTrue(minefantasy.mf2.api.crafting.MineFantasyFuels.isCarbon(new ItemStack(TestItems.metaBar, 1, 3)));
            assertFalse(
                    "another metadata became carbon",
                    minefantasy.mf2.api.crafting.MineFantasyFuels.isCarbon(new ItemStack(TestItems.metaBar, 1, 2)));
            assertEquals(0, Scripts.run("mods.minefantasy.Fuel.addCarbon(<minefantasy2tests:meta_bar:*>, 2);").size());
            assertTrue(minefantasy.mf2.api.crafting.MineFantasyFuels.isCarbon(new ItemStack(TestItems.metaBar, 1, 2)));
        });
    }

    /** A counted script ingredient takes its count out of a bigger stack, as CraftTweaker counts it exactly. */
    @GameTest
    public static void aCountedIngredientTakesFromABiggerStack(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run("mods.minefantasy.Quern.add(\"two_iron\", <minecraft:redstone>, <ore:ingotIron> * 2);")
                            .size());
            Input input = quern("two_iron");
            assertTrue("two ingots were refused", input.matches(new ItemStack(Items.iron_ingot, 2)));
            assertTrue("a stack of three was refused", input.matches(new ItemStack(Items.iron_ingot, 3)));
            assertFalse("one ingot is enough", input.hasEnough(new ItemStack(Items.iron_ingot, 1)));
            assertFalse(input.matches(new ItemStack(Items.gold_ingot, 3)));
        });
    }
}
