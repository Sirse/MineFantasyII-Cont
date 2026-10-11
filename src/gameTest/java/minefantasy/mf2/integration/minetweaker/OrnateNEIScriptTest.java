package minefantasy.mf2.integration.minetweaker;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.exotic.SpecialForging.SpecialCraft;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.integration.nei.AnvilSpecialRecipes;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.styles.OrnateStyle;

/** The registry selection used by NEI's R lookup, catalogue and Ornate Items usage, without client rendering. */
@GameTestHolder(value = "minefantasy2", requiredMods = "MineTweaker3")
public class OrnateNEIScriptTest {

    private OrnateNEIScriptTest() {}

    private static AnvilSpecialRecipes.Result ornate(ItemStack base) {
        for (AnvilSpecialRecipes.Result result : AnvilSpecialRecipes.results(base)) {
            if ("ornate".equals(result.design)) {
                return result;
            }
        }
        throw new AssertionError("No Ornate display result for " + base);
    }

    @GameTest
    public static void everyNativeOrnateTransformationIsFoundWithoutAnItemDesignInterface(GameTestHelper helper)
            throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(0, Scripts.run("").size());
            int count = 0;
            for (RecipeEntry<SpecialCraft> entry : MFRecipes.SPECIAL_FORGING.published().all()) {
                SpecialCraft craft = entry.getRecipe();
                if (!"ornate".equals(craft.getDesign())) {
                    continue;
                }
                ItemStack base = new ItemStack(craft.getBase());
                ItemStack wanted = new ItemStack(craft.getOutput());
                CustomMaterial.addMaterial(wanted, CustomToolHelper.slot_main, "Steel");
                AnvilSpecialRecipes.Result result = ornate(base);
                assertEquals(craft.getOutput(), result.getOutput().getItem());
                assertTrue("R lookup missed " + craft.getOutput(), result.matches(wanted));
                count++;
            }
            assertTrue("Ornate registry unexpectedly empty", count > 30);
            assertTrue(
                    ornate(new ItemStack(CustomToolListMF.standard_pick))
                            .matches(new ItemStack(OrnateStyle.ornate_pick)));
            assertTrue(
                    ornate(new ItemStack(CustomToolListMF.standard_bow))
                            .matches(new ItemStack(OrnateStyle.ornate_bow)));
        });
    }

    @GameTest
    public static void fixedMaterialsAndMetadataStillRestrictTheDisplayAndStacksAreCopied(GameTestHelper helper)
            throws Exception {
        Scripts.around(helper, () -> {
            ItemStack base = new ItemStack(CustomToolListMF.standard_pick, 2, 0);
            CustomMaterial.addMaterial(base, CustomToolHelper.slot_main, "Steel");
            CustomMaterial.addMaterial(base, CustomToolHelper.slot_haft, "OakWood");
            String before = base.getTagCompound().toString();
            AnvilSpecialRecipes.Result result = ornate(base);
            ItemStack wanted = new ItemStack(OrnateStyle.ornate_pick);
            CustomMaterial.addMaterial(wanted, CustomToolHelper.slot_main, "Steel");
            CustomMaterial.addMaterial(wanted, CustomToolHelper.slot_haft, "OakWood");
            assertTrue(result.matches(wanted));
            wanted.setItemDamage(1);
            assertFalse("different output metadata matched", result.matches(wanted));
            wanted.setItemDamage(0);
            CustomMaterial.addMaterial(wanted, CustomToolHelper.slot_main, "Bronze");
            assertFalse("fixed steel recipe shown for bronze", result.matches(wanted));
            CustomMaterial.addMaterial(wanted, CustomToolHelper.slot_main, "Steel");
            CustomMaterial.addMaterial(wanted, CustomToolHelper.slot_haft, "BirchWood");
            assertFalse("fixed oak recipe shown for birch", result.matches(wanted));
            assertFalse(result.matches(new ItemStack(Items.diamond)));
            assertFalse(result.matches(null));
            ItemStack shown = result.getOutput();
            assertEquals(2, shown.stackSize);
            shown.getTagCompound().setString("display_test", "changed");
            shown.stackSize = 50;
            assertEquals(2, result.getOutput().stackSize);
            assertFalse(result.getOutput().getTagCompound().hasKey("display_test"));
            assertEquals(before, base.getTagCompound().toString());
        });
    }

    @GameTest
    public static void scriptReplacementRemovalAndReloadImmediatelyChangeTheNEILookup(GameTestHelper helper)
            throws Exception {
        Scripts.around(helper, () -> {
            ItemStack base = new ItemStack(CustomToolListMF.standard_pick);
            String bracket = Scripts.bracket(base);
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.SpecialForging.set(\"ornate\", " + bracket + ", <minecraft:diamond>);")
                            .size());
            assertTrue(ornate(base).matches(new ItemStack(Items.diamond)));
            assertFalse(ornate(base).matches(new ItemStack(OrnateStyle.ornate_pick)));
            assertEquals(0, Scripts.run("mods.minefantasy.SpecialForging.remove(\"ornate\", " + bracket + ");").size());
            assertFalse(AnvilSpecialRecipes.results(base).stream().anyMatch(result -> "ornate".equals(result.design)));
            assertEquals(0, Scripts.run("").size());
            assertTrue(ornate(base).matches(new ItemStack(OrnateStyle.ornate_pick)));
            // A script output need not expose ISpecialDesign either.
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.SpecialForging.set(\"ornate\", <minecraft:stick>, <minecraft:emerald>);")
                            .size());
            assertTrue(ornate(new ItemStack(Items.stick)).matches(new ItemStack(Items.emerald)));
        });
    }
}
