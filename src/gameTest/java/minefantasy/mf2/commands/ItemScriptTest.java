package minefantasy.mf2.commands;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.item.heatable.ItemHeated;
import minefantasy.mf2.item.list.ComponentListMF;

/** What /mf zs and /mf recipes write: only what a recipe means. */
@GameTestHolder("minefantasy2")
public class ItemScriptTest {

    private ItemScriptTest() {}

    @GameTest
    public static void materialsGoThroughTheHelpers(GameTestHelper helper) {
        ItemStack bars = ComponentListMF.bar("Steel", 3);
        assertEquals("mods.minefantasy.MF.stack(<minefantasy2:custom_bar>, \"steel\") * 3", ItemScript.output(bars));
        assertEquals("mods.minefantasy.MF.input(<minefantasy2:custom_bar>, \"steel\") * 3", ItemScript.input(bars));
        assertTrue("materials were called a dropped tag", ItemScript.droppedTags(bars).isEmpty());
        helper.succeed();
    }

    /** Heat and wear are what an item picks up in use, never conditions. */
    @GameTest
    public static void heatAndWearAreLeftOut(GameTestHelper helper) {
        ItemStack hot = ItemHeated.createHotItem(ComponentListMF.bar("Steel", 1));
        assertEquals("mods.minefantasy.MF.stack(<minefantasy2:custom_bar>, \"steel\")", ItemScript.output(hot));
        assertTrue("heat was kept: " + ItemScript.droppedTags(hot), ItemScript.droppedTags(hot).isEmpty());

        ItemStack worn = new ItemStack(Items.iron_sword);
        worn.setItemDamage(40);
        assertEquals("<minecraft:iron_sword>", ItemScript.output(worn));
        assertEquals("<minecraft:iron_sword>.anyDamage()", ItemScript.input(worn));

        ItemStack named = new ItemStack(Items.stick);
        named.setStackDisplayName("Wand");
        assertEquals("<minecraft:stick>", ItemScript.output(named));
        assertTrue(ItemScript.droppedTags(named).contains("display"));
        helper.succeed();
    }

    /** "any" names no material, so it is never written as one. */
    @GameTest
    public static void anyIsNeverExported(GameTestHelper helper) {
        ItemStack bar = new ItemStack(ComponentListMF.bar);
        CustomMaterial.addMaterial(bar, CustomToolHelper.slot_main, "any");
        assertEquals("<minefantasy2:custom_bar>", ItemScript.output(bar));
        assertFalse(ItemScript.input(bar).contains("any"));
        helper.succeed();
    }

    @GameTest
    public static void aRecipeExportsItsRemovalLine(GameTestHelper helper) {
        assertEquals(
                "mods.minefantasy.Anvil.remove(\"minefantasy2:anvil/minecraft.bucket.0\");",
                ItemScript.removeLine(RecipeId.parse("minefantasy2:anvil/minecraft.bucket.0")));
        assertNull("salvage has no removal by id", ItemScript.removeLine(RecipeId.parse("minefantasy2:salvage/x")));
        helper.succeed();
    }
}
