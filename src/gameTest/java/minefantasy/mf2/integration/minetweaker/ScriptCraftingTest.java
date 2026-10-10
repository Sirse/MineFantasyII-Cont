package minefantasy.mf2.integration.minetweaker;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.Stations;
import minefantasy.mf2.block.tileentity.TileEntityCrucible;
import minefantasy.mf2.block.tileentity.TileEntityQuern;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.gametest.TestItems;
import minefantasy.mf2.item.list.ComponentListMF;

/**
 * Script recipes as players meet them: the kinds of ingredient a script writes, and recipes a script built running on
 * the real stations, over ticks, paying what they ask.
 */
@GameTestHolder(value = "minefantasy2", requiredMods = "MineTweaker3")
public class ScriptCraftingTest {

    private ScriptCraftingTest() {}

    private static Input quernInput(String name, String ingredient) throws Exception {
        return Scripts
                .build(
                        MFRecipes.QUERN,
                        "crafttweaker:quern/" + name,
                        "mods.minefantasy.Quern.add(\"" + name + "\", <minecraft:redstone>, " + ingredient + ");")
                .getInput();
    }

    private static ItemStack damaged(ItemStack stack, int damage) {
        stack.setItemDamage(damage);
        return stack;
    }

    // region ingredient kinds

    @GameTest
    public static void anOreNameTakesWhatIsListedUnderIt(GameTestHelper helper) throws Exception {
        Input input = quernInput("ore_dust", "<ore:ingotIron>");
        assertTrue("an iron ingot is refused", input.matches(new ItemStack(Items.iron_ingot)));
        assertFalse("a gold ingot is taken", input.matches(new ItemStack(Items.gold_ingot)));
        helper.succeed();
    }

    @GameTest
    public static void alternativesTakeEitherItem(GameTestHelper helper) throws Exception {
        Input input = quernInput("either", "<minecraft:stick> | <minecraft:bone>");
        assertTrue(input.matches(new ItemStack(Items.stick)));
        assertTrue(input.matches(new ItemStack(Items.bone)));
        assertFalse("something else is taken", input.matches(new ItemStack(Blocks.dirt)));
        helper.succeed();
    }

    @GameTest
    public static void damageConditionsHold(GameTestHelper helper) throws Exception {
        Input any = quernInput("any_sword", "<minecraft:iron_sword>.anyDamage()");
        assertTrue("a new sword is refused", any.matches(new ItemStack(Items.iron_sword)));
        assertTrue("a worn sword is refused", any.matches(damaged(new ItemStack(Items.iron_sword), 40)));

        Input worn = quernInput("worn_sword", "<minecraft:iron_sword>.anyDamage().onlyDamaged()");
        assertFalse("a new sword passed onlyDamaged", worn.matches(new ItemStack(Items.iron_sword)));
        assertTrue("a worn sword is refused", worn.matches(damaged(new ItemStack(Items.iron_sword), 40)));

        // As in CraftTweaker itself: a damageable item matches its own damage only, so without anyDamage a damage
        // condition can never be met
        Input plainWorn = quernInput("plain_worn", "<minecraft:iron_sword>.onlyDamaged()");
        assertFalse("a new sword passed a plain onlyDamaged", plainWorn.matches(new ItemStack(Items.iron_sword)));
        assertFalse(
                "a plain onlyDamaged took a worn sword, unlike CraftTweaker",
                plainWorn.matches(damaged(new ItemStack(Items.iron_sword), 40)));

        Input wellWorn = quernInput("well_worn", "<minecraft:iron_sword>.anyDamage().onlyDamageAtLeast(100)");
        assertFalse("too little wear passed", wellWorn.matches(damaged(new ItemStack(Items.iron_sword), 40)));
        assertTrue(wellWorn.matches(damaged(new ItemStack(Items.iron_sword), 120)));
        helper.succeed();
    }

    // endregion

    // region on the stations

    /** A quern grinding by a script's material input: the wrong metal stays, the right one pays exactly two. */
    @GameTest(timeoutTicks = 200)
    public static void aQuernGrindsAScriptMaterialInput(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        ProcessRecipe recipe = Scripts.build(
                MFRecipes.QUERN,
                "crafttweaker:quern/steel_dust",
                "mods.minefantasy.Quern.add(\"steel_dust\", <minecraft:redstone>,"
                        + " mods.minefantasy.MF.input(<minefantasy2:custom_bar>, \"steel\") * 2);");
        Stations.reload(tx -> tx.add(MFRecipes.QUERN, RecipeId.of("crafttweaker", "quern/steel_dust_t"), recipe, 0));
        assertNull("a copper bar found a grind", MFRecipes.find(MFRecipes.QUERN, ComponentListMF.bar("Copper", 4)));

        helper.setBlock(1, 1, 1, BlockListMF.quern);
        TileEntityQuern quern = helper.assertTileEntityPresent(TileEntityQuern.class, 1, 1, 1);
        quern.setInventorySlotContents(0, ComponentListMF.bar("Steel", 3));
        quern.setInventorySlotContents(1, new ItemStack(ComponentListMF.clay_pot, 2));
        quern.onUse(helper.spawnFakePlayer(Modders.SMITH));
        Stations.keepUntilFinished();
        helper.succeedWhen(() -> {
            ItemStack out = quern.getStackInSlot(2);
            if (out == null) {
                return false;
            }
            assertEquals(Items.redstone, out.getItem());
            assertEquals("the grind did not pay exactly two bars", 1, quern.getStackInSlot(0).stackSize);
            return true;
        });
    }

    /** A recipe a reload takes away mid-grind leaves the input whole and makes nothing. */
    @GameTest(timeoutTicks = 200)
    public static void aReloadMidGrindMakesNothingAndKeepsTheInput(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        ProcessRecipe recipe = Scripts.build(
                MFRecipes.QUERN,
                "crafttweaker:quern/gone_dust",
                "mods.minefantasy.Quern.add(\"gone_dust\", <minecraft:redstone>, <minecraft:bone>);");
        Stations.reload(tx -> tx.add(MFRecipes.QUERN, RecipeId.of("crafttweaker", "quern/gone_dust_t"), recipe, 0));
        helper.setBlock(1, 1, 1, BlockListMF.quern);
        TileEntityQuern quern = helper.assertTileEntityPresent(TileEntityQuern.class, 1, 1, 1);
        quern.setInventorySlotContents(0, new ItemStack(Items.bone, 2));
        quern.setInventorySlotContents(1, new ItemStack(ComponentListMF.clay_pot, 2));
        quern.onUse(helper.spawnFakePlayer(Modders.SMITH));
        // The script line is gone before the turn completes
        Stations.reload(helper, tx -> {});
        Stations.keepUntilFinished();
        helper.onEachTick("the gone recipe ground", () -> {
            assertNull("something was made", quern.getStackInSlot(2));
            assertEquals("the input was taken", 2, quern.getStackInSlot(0).stackSize);
        });
        helper.succeedAtTimeout();
    }

    /** A crucible alloying a script's recipe, with coal standing in for the script's carbon. */
    @GameTest(timeoutTicks = 600)
    public static void aCrucibleAlloysAScriptRecipeWithCoalForCarbon(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        Alloy alloy = Scripts.build(
                MFRecipes.ALLOY,
                "crafttweaker:alloy/carbon_steel",
                "mods.minefantasy.Crucible.add(\"carbon_steel\", <minecraft:diamond>, 0, 1,"
                        + " [<minefantasy2tests:ore>, mods.minefantasy.MF.carbon()]);");
        Stations.reload(tx -> tx.add(MFRecipes.ALLOY, RecipeId.of("crafttweaker", "alloy/carbon_steel_t"), alloy, 0));
        helper.setBlock(1, 0, 1, Blocks.stone);
        for (int[] side : new int[][] { { 0, 1 }, { 2, 1 }, { 1, 0 }, { 1, 2 } }) {
            helper.setBlock(side[0], 1, side[1], Blocks.stone);
        }
        helper.setBlock(1, 1, 1, Blocks.lava);
        helper.setBlock(1, 2, 1, BlockListMF.crucible);
        TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 2, 1);
        crucible.setInventorySlotContents(0, new ItemStack(TestItems.ore));
        crucible.setInventorySlotContents(1, new ItemStack(Items.coal));
        Stations.keepUntilFinished();
        int output = crucible.getSizeInventory() - 1;
        helper.succeedWhen(() -> {
            ItemStack made = crucible.getStackInSlot(output);
            if (made == null) {
                return false;
            }
            assertEquals(Items.diamond, made.getItem());
            assertNull("the ore was not paid", crucible.getStackInSlot(0));
            assertNull("the coal was not paid", crucible.getStackInSlot(1));
            return true;
        });
    }

    // endregion
}
