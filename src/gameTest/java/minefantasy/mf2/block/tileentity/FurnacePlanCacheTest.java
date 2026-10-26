package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;
import static minefantasy.mf2.gametest.Ticks.tick;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.gametest.Templates;

/**
 * The furnace keeps the plan it looked up for each slot between ticks; a changed slot or a recipe reload is seen at
 * once, not when the cache runs out.
 */
@GameTestHolder("minefantasy2")
public class FurnacePlanCacheTest {

    private FurnacePlanCacheTest() {}

    @GameTest(template = Templates.BIG_FURNACE)
    public static void aChangedSlotIsPlannedAtOnce(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        reload(
                tx -> tx.add(
                        MFRecipes.BIG_FURNACE,
                        id("big_furnace", "seed"),
                        ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                        0));
        TileEntityBigFurnace furnace = Templates.furnace(helper);
        tick(furnace, 1);
        assertTrue("the furnace was not built", furnace.built);

        furnace.setInventorySlotContents(0, new ItemStack(seed));
        assertNotNull("the seed had no plan", furnace.planFor(0, 4));
        furnace.setInventorySlotContents(0, new ItemStack(Blocks.dirt));
        assertNull("dirt kept the seed's plan", furnace.planFor(0, 4));
        furnace.setInventorySlotContents(0, new ItemStack(seed));
        assertNotNull("the seed lost its plan", furnace.planFor(0, 4));
        helper.succeed();
    }

    @GameTest(template = Templates.BIG_FURNACE)
    public static void aReloadIsPlannedAtOnce(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        reload(tx -> {});
        TileEntityBigFurnace furnace = Templates.furnace(helper);
        tick(furnace, 1);
        furnace.setInventorySlotContents(0, new ItemStack(Blocks.dirt));
        assertNull("dirt smelted with no recipe", furnace.planFor(0, 4));

        reload(
                tx -> tx.add(
                        MFRecipes.BIG_FURNACE,
                        id("big_furnace", "dirt"),
                        ProcessRecipe.of(Input.of(new ItemStack(Blocks.dirt)), new ItemStack(flour)),
                        0));
        assertNotNull("the reloaded recipe was not seen", furnace.planFor(0, 4));
        helper.succeed();
    }
}
