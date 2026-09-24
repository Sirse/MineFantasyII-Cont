package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.lang.reflect.Field;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeMetadata;

/**
 * What a player sees on a station's HUD comes from the description packet the server sends when the player starts
 * watching: it must carry the station's progress, requirements and result. Reading the packet back is client code the
 * dedicated server does not have, so the tests check what the server puts in it.
 */
@GameTestHolder("minefantasy2")
public class HudSyncTest {

    private HudSyncTest() {}

    /** The data of the station's description packet. */
    private static NBTTagCompound sent(TileEntity station) throws Exception {
        S35PacketUpdateTileEntity packet = (S35PacketUpdateTileEntity) station.getDescriptionPacket();
        assertNotNull("the station sends no description", packet);
        for (Field field : S35PacketUpdateTileEntity.class.getDeclaredFields()) {
            if (field.getType() == NBTTagCompound.class) {
                field.setAccessible(true);
                return (NBTTagCompound) field.get(packet);
            }
        }
        throw new IllegalStateException("the packet has no data field");
    }

    private static String knownResearch() {
        return InformationList.nameMap.keySet().iterator().next();
    }

    /** One junk into three bars: hammer tier 2, station tier 1, time 7, a real research. */
    private static GridRecipe gridRecipe(GridRecipe.Grid grid) {
        return GridRecipe.shaped(grid, 1, 1, new Object[] { new ItemStack(junk) }, null, new ItemStack(bar, 3))
                .tool("hammer", 2).stationTier(1).time(7).research(knownResearch()).build();
    }

    private static void assertProject(NBTTagCompound hud, float progress) {
        assertEquals("progress", progress, hud.getFloat("Progress"), 0F);
        assertEquals("work time", 7F, hud.getFloat("ProgressMax"), 0F);
        assertEquals("tool", "hammer", hud.getString("ToolNeeded"));
        assertEquals("research", knownResearch(), hud.getString("Research"));
        assertTrue("no result is sent", hud.hasKey("Result"));
        ItemStack result = ItemStack.loadItemStackFromNBT(hud.getCompoundTag("Result"));
        assertEquals(bar, result.getItem());
        assertEquals(3, result.stackSize);
    }

    @GameTest
    public static void anvilSendsItsProject(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(tx -> tx.add(MFRecipes.ANVIL, id("anvil", "hud"), gridRecipe(GridRecipe.Grid.ANVIL), 0));
            TileEntityAnvilMF anvil = place(new TileEntityAnvilMF());
            anvil.setInventorySlotContents(0, new ItemStack(junk));
            anvil.updateCraftingData();
            anvil.progress = 3;
            NBTTagCompound hud = sent(anvil);
            assertProject(hud, 3);
            assertEquals("hammer tier", 2, hud.getInteger("HammerTier"));
            assertEquals("anvil tier", 1, hud.getInteger("AnvilTier"));
            // The hit marks are dealt on the anvil's first tick; the packet carries whatever the server holds
            assertEquals("left hit mark", anvil.leftHit, hud.getFloat("LeftHit"), 0F);
            assertEquals("right hit mark", anvil.rightHit, hud.getFloat("RightHit"), 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void carpenterSendsItsProject(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(tx -> tx.add(MFRecipes.CARPENTER, id("carpenter", "hud"), gridRecipe(GridRecipe.Grid.BENCH), 0));
            TileEntityCarpenterMF bench = place(new TileEntityCarpenterMF());
            bench.setInventorySlotContents(0, new ItemStack(junk));
            bench.updateCraftingData();
            bench.progress = 3;
            NBTTagCompound hud = sent(bench);
            assertProject(hud, 3);
            assertEquals("tool tier", 2, hud.getInteger("HammerTier"));
            assertEquals("bench tier", 1, hud.getInteger("CarpenterTier"));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void kitchenSendsItsProjectAndDirt(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(tx -> tx.add(MFRecipes.KITCHEN, id("kitchen", "hud"), gridRecipe(GridRecipe.Grid.BENCH), 0));
            TileEntityKitchenBench bench = place(new TileEntityKitchenBench());
            bench.setInventorySlotContents(0, new ItemStack(junk));
            bench.updateCraftingData();
            bench.progress = 3;
            bench.dirtyProgress = 20;
            NBTTagCompound hud = sent(bench);
            assertProject(hud, 3);
            assertEquals("dirt", 20F, hud.getFloat("DirtyProgress"), 0F);
            assertEquals("dirt limit", bench.getDirtyMax(), hud.getFloat("DirtyMax"), 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void idleBenchSendsNoResult(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityCarpenterMF bench = place(new TileEntityCarpenterMF());
            bench.updateCraftingData();
            NBTTagCompound hud = sent(bench);
            assertFalse("an empty bench shows a result", hud.hasKey("Result"));
            assertEquals(0F, hud.getFloat("Progress"), 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void tanningRackSendsItsHideAndWork(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.TANNING,
                            id("tanning", "hud"),
                            ProcessRecipe.of(
                                    Input.of(seed),
                                    new ItemStack(flour),
                                    RecipeMetadata.builder().put(MFRecipeKeys.TIME, 12F)
                                            .put(MFRecipeKeys.TOOL, "shears").build()),
                            0));
            TileEntityTanningRack rack = place(new TileEntityTanningRack());
            rack.setInventorySlotContents(0, new ItemStack(seed));
            rack.updateRecipe();
            rack.progress = 4;
            // The rack sends its whole save: the client reads the same fields a load does
            TileEntityTanningRack copy = place(new TileEntityTanningRack());
            copy.readFromNBT(sent(rack));
            assertEquals(4F, copy.progress, 0F);
            assertEquals(12F, copy.maxProgress, 0F);
            assertEquals("shears", copy.toolType);
            assertEquals(seed, copy.getStackInSlot(0).getItem());
            assertEquals("what the hide becomes is shown", flour, copy.getStackInSlot(1).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void spitSendsItsFoodAndProgress(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.COOKING,
                            id("cooking", "hud"),
                            CookRecipe.of(Input.of(seed), new ItemStack(flour), null, 50, 500, 30, 10, false, false),
                            0));
            TileEntityRoast spit = place(new TileEntityRoast());
            spit.setInventorySlotContents(0, new ItemStack(seed));
            spit.updateRecipe();
            spit.progress = 11;
            TileEntityRoast copy = place(new TileEntityRoast());
            copy.readFromNBT(sent(spit));
            assertEquals(11F, copy.progress, 0F);
            assertEquals(seed, copy.getStackInSlot(0).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void bloomerySendsItsFireAndBloom(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.BLOOMERY,
                            id("bloomery", "hud"),
                            BloomRecipe.of(Input.of(ore), new ItemStack(bar)),
                            0));
            TileEntityBloomery bloomery = place(new TileEntityBloomery());
            bloomery.setInventorySlotContents(0, new ItemStack(ore));
            bloomery.setInventorySlotContents(1, new ItemStack(carbon));
            assertTrue(bloomery.light(null));
            assertTrue("the fire is not sent", sent(bloomery).getBoolean("isActive"));
            assertFalse(sent(bloomery).getBoolean("hasBloom"));

            TileEntityBloomery done = place(new TileEntityBloomery());
            done.setInventorySlotContents(2, new ItemStack(bar));
            assertTrue("the bloom is not sent", sent(done).getBoolean("hasBloom"));
            assertFalse(sent(done).getBoolean("isActive"));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
