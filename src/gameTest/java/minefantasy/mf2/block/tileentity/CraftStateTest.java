package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeMetadata;

/**
 * The shared work state of timed stations: progress carries on only under the started plan, and watchers are sent the
 * state only when it changed.
 */
@GameTestHolder("minefantasy2")
public class CraftStateTest {

    private CraftStateTest() {}

    private static CraftPlan plan(String path, ItemStack output) {
        return CraftPlan.builder(RecipeId.of("minefantasy2", "test/" + path), 1, 0).output(output).build();
    }

    // region component

    @GameTest
    public static void theSamePlanCarriesOnAndAnotherStartsOver(GameTestHelper helper) throws Exception {
        CraftState state = new CraftState();
        assertFalse("nothing was started", state.follow(plan("a", new ItemStack(bar))));
        assertTrue(state.follow(plan("a", new ItemStack(bar))));
        assertFalse("another result is another plan", state.follow(plan("a", new ItemStack(bar, 2))));
        assertFalse(state.follow(null));
        assertFalse("no plan leaves nothing running", state.isRunning());
        helper.succeed();
    }

    @GameTest
    public static void anUnchangedStateIsSentOnce(GameTestHelper helper) throws Exception {
        CraftState state = new CraftState();
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setFloat("Progress", 1F);
        assertTrue(state.changed(nbt));
        assertFalse(state.changed((NBTTagCompound) nbt.copy()));
        nbt.setFloat("Progress", 2F);
        assertTrue("the kept copy followed the station's tag", state.changed(nbt));
        helper.succeed();
    }

    // endregion

    // region stations

    /** Whether the station's shown state differs from what its watchers last got. */
    private static boolean unsent(Object station) throws Exception {
        CraftState craft = (CraftState) get(station, "craft");
        return craft.changed((NBTTagCompound) call(station, "describe", new Class<?>[0]));
    }

    @GameTest
    public static void anvilRecheckingAnUnchangedGridSendsNothing(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.ANVIL,
                            id("anvil", "unchanged"),
                            GridRecipe.shaped(
                                    GridRecipe.Grid.ANVIL,
                                    1,
                                    1,
                                    new Object[] { new ItemStack(junk) },
                                    null,
                                    new ItemStack(bar)).tool("hammer", 0).time(5).build(),
                            0));
            TileEntityAnvilMF anvil = place(new TileEntityAnvilMF());
            anvil.setInventorySlotContents(0, new ItemStack(junk));
            anvil.updateCraftingData();
            anvil.updateCraftingData();
            assertFalse("the anvil did not send its project", unsent(anvil));
            anvil.progress = 2;
            anvil.updateCraftingData();
            assertEquals("the recheck dropped the progress", 2F, anvil.progress, 0F);
            assertFalse("the progress was not sent", unsent(anvil));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    private static void tanning(String path, ItemStack input) {
        reload(
                tx -> tx.add(
                        MFRecipes.TANNING,
                        id("tanning", path),
                        ProcessRecipe.of(
                                Input.of(input.getItem()),
                                new ItemStack(flour),
                                RecipeMetadata.builder().put(MFRecipeKeys.TIME, 50F).build()),
                        0));
    }

    @GameTest
    public static void rackKeepsTheProgressOfTheSameHide(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            tanning("same_hide", new ItemStack(seed));
            TileEntityTanningRack rack = place(new TileEntityTanningRack());
            rack.setInventorySlotContents(0, new ItemStack(seed));
            rack.updateRecipe();
            rack.progress = 7;
            rack.updateRecipe();
            assertEquals("a recheck of the same hide restarted it", 7F, rack.progress, 0F);
            rack.setInventorySlotContents(0, null);
            rack.updateRecipe();
            assertEquals("taking the hide off kept its progress", 0F, rack.progress, 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void spitSendsOnlyWhatChanged(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.COOKING,
                            id("cooking", "sent_once"),
                            CookRecipe.builder(Input.of(seed), new ItemStack(flour)).temperature(50, 500).time(30)
                                    .burnTime(10).canBurn(false).build(),
                            0));
            TileEntityRoast spit = place(new TileEntityRoast());
            spit.setInventorySlotContents(0, new ItemStack(seed));
            spit.updateRecipe();
            spit.progress = 4;
            spit.updateRecipe();
            assertEquals("a recheck of the same food restarted it", 4F, spit.progress, 0F);
            CraftState craft = (CraftState) get(spit, "craft");
            NBTTagCompound shown = new NBTTagCompound();
            spit.writeToNBT(shown);
            assertFalse("the spit did not send its food", craft.changed(shown));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion
}
