package minefantasy.mf2.integration.minetweaker;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.config.ConfigKitchen;

/**
 * A script that exercises the CraftTweaker API of every station must load without errors and register what it says.
 */
@GameTestHolder(value = "minefantasy2", requiredMods = "MineTweaker3")
public class ScriptExampleTest {

    private ScriptExampleTest() {}

    /** One script over every station: hot output, custom materials, woods and metals, alloys, removal. */
    private static final String[] SCRIPT = { "import mods.minefantasy.Anvil;", "import mods.minefantasy.Forge;",
            "import mods.minefantasy.CarpenterBench;", "import mods.minefantasy.KitchenBench;",
            "import mods.minefantasy.Crucible;", "import mods.minefantasy.Bloomery;",
            "import mods.minefantasy.BigFurnace;", "import mods.minefantasy.Fuel;", "import mods.minefantasy.Cooking;",
            "import mods.minefantasy.Quern;", "import mods.minefantasy.TanningRack;",
            "import mods.minefantasy.Salvage;", "import mods.minefantasy.SpecialForging;", "",
            "val metals = [\"bronze\", \"iron\", \"steel\"] as string[];", "val tiers = [1, 2, 3] as int[];",
            "val woods = [\"oakwood\", \"sprucewood\", \"yewwood\"] as string[];", "", "for i, metal in metals {",
            "    val bar = <minefantasy2:custom_bar>.withTag({MF_CustomMaterials: {main_metal: metal}});",
            "    for wood in woods {",
            "        val plank = <minefantasy2:MF_Com_plank>.withTag({MF_CustomMaterials: {main_metal: wood}});",
            "        Anvil.addShaped(\"sword_\" + metal + \"_\" + wood,",
            "            <minefantasy2:standard_sword>.withTag({MF_CustomMaterials: {main_metal: metal, haft_wood: wood}}),",
            "            \"artisanry\", \"craftWeapons\", true, \"hammer\", tiers[i], tiers[i], 300,",
            "            [[bar, bar, bar, plank]]);", "    }", "}", "",
            "Forge.add(\"iron_bars\", <minecraft:iron_bars>, 600, 1000, 1400);",
            "Anvil.addShapeless(\"ingot_from_bars\", <minecraft:iron_ingot>, \"artisanry\", \"\", false, \"hammer\", 0, 0, 100,",
            "    [<minecraft:iron_bars> * 4]);", "",
            "for wood in [\"oakwood\", \"birchwood\", \"refinedwood\"] as string[] {",
            "    CarpenterBench.addShapeless(\"cut_\" + wood,",
            "        <minefantasy2:MF_Com_plank_cut>.withTag({MF_CustomMaterials: {main_metal: wood}}) * 2,",
            "        \"construction\", \"\", \"dig.wood\", 0.0, \"knife\", 0, 0, 20,",
            "        [<minefantasy2:MF_Com_plank>.withTag({MF_CustomMaterials: {main_metal: wood}})]);", "}", "",
            "KitchenBench.addShaped(\"apple_mash\", <minecraft:mushroom_stew>, \"provisioning\", \"\", \"step.wood\", \"spoon\", 40, 0,",
            "    [[<minecraft:apple>, <minecraft:apple>], [<minecraft:bowl>, null]]);", "",
            "Crucible.add(\"rich_bronze\", <minefantasy2:custom_bar>.withTag({MF_CustomMaterials: {main_metal: \"bronze\"}}) * 4, 1, 2,",
            "    [<minefantasy2:custom_bar>.withTag({MF_CustomMaterials: {main_metal: \"copper\"}}) * 3,",
            "     <minefantasy2:custom_bar>.withTag({MF_CustomMaterials: {main_metal: \"tin\"}})]);", "",
            "Bloomery.add(\"iron_from_rails\", <minefantasy2:custom_bar>.withTag({MF_CustomMaterials: {main_metal: \"iron\"}}),",
            "    <minecraft:rail>, \"smeltIron\");", "",
            "BigFurnace.add(\"glass_panes\", <minecraft:glass_pane> * 4, <minecraft:sand>, 1);", "",
            "Fuel.addCarbon(<minecraft:blaze_powder>, 4);", "",
            "Cooking.add(\"baked_potato_spit\", <minecraft:baked_potato>, <minecraft:potato>, 100, 300, 20, 10, false, true);",
            "Cooking.add(\"bread_oven\", <minecraft:bread>, <minecraft:wheat> * 3, 150, 250, 60, 20, true, true);", "",
            "Quern.add(\"bone_meal\", <minecraft:dye:15> * 4, <minecraft:bone>, 0, false);", "",
            "TanningRack.add(\"flesh_leather\", <minecraft:leather>, <minecraft:rotten_flesh> * 4, 2.0, 0, \"shears\");",
            "", "Anvil.remove(\"minefantasy2:anvil/minecraft.flint_and_steel.0\");",
            "Anvil.removeByOutput(<minecraft:bucket>);",
            "Cooking.remove(\"minefantasy2:cooking/minefantasy2.mf2_food_generic_meat_chunk_uncooked.0\");", "",
            "Salvage.set(<minecraft:bucket>, [<minecraft:iron_ingot> * 2]);",
            "SpecialForging.setOrnate(<minecraft:iron_ingot>, <minecraft:gold_ingot>);" };

    private static <R> R recipe(RecipeRegistry<R> registry, String id) {
        RecipeId recipeId = RecipeId.of(id.substring(0, id.indexOf(':')), id.substring(id.indexOf(':') + 1));
        assertTrue(id + " is not registered", registry.published().contains(recipeId));
        return registry.published().get(recipeId).getRecipe();
    }

    private static String material(ItemStack item, String slot) {
        CustomMaterial material = CustomMaterial.getMaterialFor(item, slot);
        return material == null ? null : material.getName();
    }

    @GameTest
    public static void theExampleScriptRunsAndDoesWhatItSays(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            List<String> errors = Scripts.run(SCRIPT);
            assertTrue("the script logs errors: " + errors, errors.isEmpty());

            GridRecipe sword = recipe(MFRecipes.ANVIL, "crafttweaker:anvil/sword_steel_yewwood");
            assertTrue("the sword does not come out hot", sword.outputHot());
            assertEquals("steel", material(sword.getRecipeOutput(), CustomToolHelper.slot_main));
            assertEquals("yewwood", material(sword.getRecipeOutput(), CustomToolHelper.slot_haft));
            assertEquals(3, sword.getAnvil());
            assertEquals(9, countFrom(MFRecipes.ANVIL, "crafttweaker:anvil/sword_"));

            recipe(MFRecipes.HEATING, "crafttweaker:forge_heat/iron_bars");
            GridRecipe cut = recipe(MFRecipes.CARPENTER, "crafttweaker:carpenter/cut_refinedwood");
            assertEquals("refinedwood", material(cut.getRecipeOutput(), CustomToolHelper.slot_main));
            assertEquals(2, cut.getRecipeOutput().stackSize);
            recipe(
                    ConfigKitchen.enableBench ? MFRecipes.KITCHEN : MFRecipes.CARPENTER,
                    (ConfigKitchen.enableBench ? "crafttweaker:kitchen/" : "crafttweaker:carpenter/") + "apple_mash");

            recipe(MFRecipes.ALLOY, "crafttweaker:alloy/rich_bronze");
            recipe(MFRecipes.ALLOY, "crafttweaker:alloy/rich_bronze_x2");
            recipe(MFRecipes.BLOOMERY, "crafttweaker:bloomery/iron_from_rails");
            recipe(MFRecipes.BIG_FURNACE, "crafttweaker:big_furnace/glass_panes");

            assertTrue(recipe(MFRecipes.COOKING, "crafttweaker:cooking/bread_oven").isBaking());
            assertFalse(recipe(MFRecipes.COOKING, "crafttweaker:cooking/baked_potato_spit").isBaking());
            CookRecipe burnt = recipe(MFRecipes.COOKING, "crafttweaker:cooking/baked_potato_spit_burnt");
            assertNotNull(burnt);
            recipe(MFRecipes.QUERN, "crafttweaker:quern/bone_meal");
            recipe(MFRecipes.TANNING, "crafttweaker:tanning/flesh_leather");

            assertFalse(
                    "a removed recipe stayed",
                    MFRecipes.ANVIL.published()
                            .contains(RecipeId.of("minefantasy2", "anvil/minecraft.flint_and_steel.0")));
        });
    }

    private static int countFrom(RecipeRegistry<?> registry, String prefix) {
        int count = 0;
        for (Object entry : registry.published().all()) {
            if (((minefantasy.mf2.api.recipe.RecipeEntry<?>) entry).getId().toString().startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }
}
