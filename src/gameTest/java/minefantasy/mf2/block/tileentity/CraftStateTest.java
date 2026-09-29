package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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

    /** A block whose state is one number; it keeps what it sends instead of sending it. */
    private static final class Counter extends TileEntityShown {

        float value;
        final List<NBTTagCompound> sends = new ArrayList<>();

        @Override
        protected NBTTagCompound describe() {
            NBTTagCompound state = new NBTTagCompound();
            state.setFloat("Value", value);
            return state;
        }

        @Override
        protected void send(NBTTagCompound tag) {
            sends.add((NBTTagCompound) tag.copy());
        }
    }

    @GameTest
    public static void anUnchangedStateIsSentOnce(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            Counter counter = place(new Counter());
            counter.value = 1F;
            counter.sendState(false);
            counter.sendState(false);
            assertEquals("an unchanged state was sent again", 1, counter.sends.size());
            assertEquals(1F, counter.sends.get(0).getFloat("Value"), 0F);
            counter.value = 2F;
            counter.sendState(false);
            assertEquals("a changed state was not sent", 2, counter.sends.size());
            assertEquals(2F, counter.sends.get(1).getFloat("Value"), 0F);
            counter.sendState(true);
            assertEquals("a forced send was skipped", 3, counter.sends.size());
            NBTTagCompound moment = new NBTTagCompound();
            moment.setBoolean("Moment", true);
            counter.sendMoment(moment);
            assertEquals("the moment was not sent", 4, counter.sends.size());
            assertTrue(counter.sends.get(3).getBoolean("Moment"));
            counter.sendState(false);
            assertEquals("a moment made the state due again", 4, counter.sends.size());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion

    // region stations

    /** Whether the block's shown state differs from what its watchers last got. */
    private static boolean unsent(TileEntityShown shown) throws Exception {
        Field sent = TileEntityShown.class.getDeclaredField("sent");
        sent.setAccessible(true);
        return !shown.describe().equals(sent.get(shown));
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
            assertFalse("the spit did not send its food", unsent(spit));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    // endregion
}
