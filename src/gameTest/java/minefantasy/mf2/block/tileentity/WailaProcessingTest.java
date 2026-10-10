package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.impl.ModuleRegistrar;
import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeMetadata;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.integration.waila.WailaData;
import minefantasy.mf2.integration.waila.WailaProvider;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.ToolListMF;

/** Drives the server's Waila callbacks on real processing blocks; looking must never perform work. */
@GameTestHolder("minefantasy2")
public class WailaProcessingTest {

    private WailaProcessingTest() {}

    private static FakePlayer near(GameTestHelper helper, TileEntity tile) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.setPosition(tile.xCoord + 0.5, tile.yCoord + 1, tile.zCoord + 0.5);
        return player;
    }

    private static NBTTagCompound saved(TileEntity tile) {
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        return tag;
    }

    private static NBTTagCompound inspect(TileEntity tile, FakePlayer player) {
        NBTTagCompound before = saved(tile);
        tile.getWorldObj().getChunkFromBlockCoords(tile.xCoord, tile.zCoord).isModified = false;
        NBTTagCompound data = null;
        for (int repeat = 0; repeat < 3; repeat++) {
            data = new WailaProvider().getNBTData(
                    player,
                    tile,
                    new NBTTagCompound(),
                    tile.getWorldObj(),
                    tile.xCoord,
                    tile.yCoord,
                    tile.zCoord).getCompoundTag(WailaData.KEY);
        }
        assertEquals("tooltip changed saved state", before, saved(tile));
        assertFalse(
                "tooltip dirtied a clean chunk",
                tile.getWorldObj().getChunkFromBlockCoords(tile.xCoord, tile.zCoord).isModified);
        return data;
    }

    @GameTest
    public static void processingProvidersAreRegisteredOnTheServer(GameTestHelper helper) {
        for (TileEntity tile : new TileEntity[] { new TileEntityBigFurnace(), new TileEntityBlastFC(),
                new TileEntityBlastFH(), new TileEntityBloomery(), new TileEntityRoast(), new TileEntityQuern(),
                new TileEntityTanningRack() }) {
            boolean found = false;
            for (java.util.List<IWailaDataProvider> providers : ModuleRegistrar.instance().getNBTProviders(tile)
                    .values()) {
                for (IWailaDataProvider provider : providers) found |= provider instanceof WailaProvider;
            }
            assertTrue("missing provider for " + tile.getClass().getSimpleName(), found);
        }
        helper.succeed();
    }

    @GameTest
    public static void bigFurnaceShowsFourResultsAndOnlyStopsWhenEveryOutputIsBlocked(GameTestHelper helper)
            throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.BIG_FURNACE,
                            id("big_furnace", "waila"),
                            ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                            1000));
            helper.setBlock(2, 2, 2, BlockListMF.furnace_stone);
            TileEntityBigFurnace furnace = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 2, 2);
            furnace.built = true;
            furnace.heat = 200;
            furnace.progress = 40000;
            for (int slot = 0; slot < 4; slot++) furnace.setInventorySlotContents(slot, new ItemStack(seed));
            FakePlayer player = near(helper, furnace);
            NBTTagCompound data = inspect(furnace, player);
            assertEquals(4, data.getTagList("Results", 10).tagCount());
            assertFalse(data.hasKey("Reason"));
            assertEquals(40000F, data.getFloat("Progress"), 0F);
            for (int slot = 4; slot < 8; slot++) furnace.setInventorySlotContents(slot, new ItemStack(junk));
            assertEquals("output_full", inspect(furnace, player).getString("Reason"));
            furnace.setInventorySlotContents(7, null);
            assertFalse("one valid smelt was treated as blocked", inspect(furnace, player).hasKey("Reason"));
            furnace.heat = 0;
            assertEquals("no_heat", inspect(furnace, player).getString("Reason"));
            furnace.built = false;
            assertEquals("not_built", inspect(furnace, player).getString("Reason"));
            helper.setBlock(2, 1, 2, BlockListMF.furnace_heater);
            TileEntityBigFurnace heater = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 1, 2);
            heater.built = true;
            heater.fuel = 200;
            assertEquals(10F, inspect(heater, player).getFloat("FuelSeconds"), 0F);
            reload(
                    tx -> tx.add(
                            MFRecipes.BIG_FURNACE,
                            id("big_furnace", "waila"),
                            ProcessRecipe.of(Input.of(seed), new ItemStack(bar)),
                            1000));
            furnace.built = true;
            data = inspect(furnace, player);
            assertEquals(4, data.getTagList("Results", 10).tagCount());
            assertSame(
                    "the HUD kept the old recipe result",
                    bar,
                    ItemStack.loadItemStackFromNBT(data.getTagList("Results", 10).getCompoundTagAt(0)).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void blastShaftInspectionKeepsCarbonChargesAndReportsSmoke(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.BLAST_FURNACE,
                            id("blast_furnace", "waila"),
                            ProcessRecipe.of(Input.of(seed), new ItemStack(bar)),
                            1000));
            helper.setBlock(2, 1, 2, BlockListMF.blast_heater);
            helper.setBlock(2, 2, 2, BlockListMF.blast_chamber);
            TileEntityBlastFH heater = helper.assertTileEntityPresent(TileEntityBlastFH.class, 2, 1, 2);
            TileEntityBlastFC shaft = helper.assertTileEntityPresent(TileEntityBlastFC.class, 2, 2, 2);
            heater.isBuilt = shaft.isBuilt = true;
            heater.fuel = 200;
            heater.maxFuel = 400;
            heater.progress = 500;
            shaft.setInventorySlotContents(1, new ItemStack(seed));
            FakePlayer player = near(helper, shaft);
            assertEquals("carbon", inspect(shaft, player).getString("Reason"));
            shaft.setInventorySlotContents(0, new ItemStack(carbon));
            assertFalse(inspect(shaft, player).hasKey("Reason"));
            shaft.setInventorySlotContents(0, null);
            shaft.tempUses = 3;
            NBTTagCompound data = inspect(shaft, player);
            assertSame(bar, ItemStack.loadItemStackFromNBT(data.getCompoundTag("Item")).getItem());
            assertEquals(500F, data.getFloat("Progress"), 0F);
            assertEquals(3, shaft.tempUses);
            heater.setSmokeValue(heater.getMaxSmokeStorage());
            assertEquals("smoke", inspect(shaft, player).getString("Reason"));
            assertEquals("smoke", inspect(heater, player).getString("Reason"));
            heater.setSmokeValue(0);
            heater.fuel = 0;
            assertEquals("no_heat", inspect(shaft, player).getString("Reason"));
            heater.isBuilt = false;
            assertEquals("not_built", inspect(shaft, player).getString("Reason"));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void quernTooltipUsesTheSamePotAndOutputChecksAsGrinding(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.QUERN,
                            id("quern", "waila"),
                            ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                            1000));
            helper.setBlock(1, 1, 1, BlockListMF.quern);
            TileEntityQuern quern = helper.assertTileEntityPresent(TileEntityQuern.class, 1, 1, 1);
            quern.setInventorySlotContents(0, new ItemStack(seed));
            FakePlayer player = near(helper, quern);
            assertEquals("pot", inspect(quern, player).getString("Reason"));
            quern.setInventorySlotContents(1, new ItemStack(ComponentListMF.clay_pot));
            assertFalse(inspect(quern, player).hasKey("Reason"));
            quern.setInventorySlotContents(2, new ItemStack(junk));
            assertEquals("output_full", inspect(quern, player).getString("Reason"));
            quern.setInventorySlotContents(2, null);
            assertTrue(quern.onRevolutionComplete());
            assertSame(flour, quern.getStackInSlot(2).getItem());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void tanningTooltipChecksToolsWithoutRestartingAChangedRecipe(GameTestHelper helper)
            throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.TANNING,
                            id("tanning", "waila"),
                            ProcessRecipe.of(
                                    Input.of(seed),
                                    new ItemStack(flour),
                                    RecipeMetadata.builder().put(MFRecipeKeys.TIME, 100F)
                                            .put(MFRecipeKeys.TOOL, "hammer").put(MFRecipeKeys.TOOL_TIER, 0).build()),
                            1000));
            helper.setBlock(1, 1, 1, BlockListMF.tanner);
            TileEntityTanningRack rack = helper.assertTileEntityPresent(TileEntityTanningRack.class, 1, 1, 1);
            rack.setInventorySlotContents(0, new ItemStack(seed));
            rack.updateRecipe();
            rack.progress = 40;
            FakePlayer player = near(helper, rack);
            assertEquals("tool", inspect(rack, player).getString("Reason"));
            player.setCurrentItemOrArmor(0, new ItemStack(ToolListMF.hammerStone));
            assertFalse(inspect(rack, player).hasKey("Reason"));
            reload(tx -> {});
            NBTTagCompound data = inspect(rack, player);
            assertEquals("checking", data.getString("Reason"));
            assertFalse(data.hasKey("Item"));
            assertEquals(40F, rack.progress, 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void bloomeryInspectionDoesNotLightExtinguishOrPayTheProject(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.BLOOMERY,
                            id("bloomery", "waila"),
                            BloomRecipe.of(Input.of(seed), new ItemStack(bar)),
                            1000));
            helper.setBlock(1, 1, 1, BlockListMF.bloomery);
            TileEntityBloomery bloomery = helper.assertTileEntityPresent(TileEntityBloomery.class, 1, 1, 1);
            bloomery.setInventorySlotContents(0, new ItemStack(seed, 4));
            bloomery.setInventorySlotContents(1, new ItemStack(carbon));
            FakePlayer player = near(helper, bloomery);
            assertEquals("unlit", inspect(bloomery, player).getString("Reason"));
            assertFalse(bloomery.isActive);
            assertTrue(bloomery.light(player));
            bloomery.progress = 30;
            assertFalse(inspect(bloomery, player).hasKey("Reason"));
            bloomery.smeltItem();
            assertTrue(inspect(bloomery, player).getBoolean("Ready"));
            assertEquals(4, bloomery.getStackInSlot(2).stackSize);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void spitAndOvenTooltipsRespectTheirRecipesAndDoNotCookTheFood(GameTestHelper helper)
            throws Exception {
        Stations.begin(helper);
        try {
            reload(tx -> {
                CookRecipe.builder(Input.of(seed), new ItemStack(flour)).temperature(50, 75).time(100)
                        .burnt(new ItemStack(junk)).canBurn(true).build().addTo(tx, id("cooking", "waila_spit"), 1000);
                tx.add(
                        MFRecipes.COOKING,
                        id("cooking", "waila_oven"),
                        CookRecipe.builder(Input.of(seed), new ItemStack(bar)).temperature(50, 1000).time(100).oven()
                                .canBurn(false).build(),
                        1000);
            });
            helper.setBlock(1, 1, 1, BlockListMF.firepit);
            TileEntityFirepit fire = helper.assertTileEntityPresent(TileEntityFirepit.class, 1, 1, 1);
            fire.fuel = 600;
            fire.setLit(true);
            helper.setBlock(1, 2, 1, BlockListMF.roast);
            TileEntityRoast spit = helper.assertTileEntityPresent(TileEntityRoast.class, 1, 2, 1);
            spit.setInventorySlotContents(0, new ItemStack(seed));
            spit.updateRecipe();
            spit.progress = 20;
            FakePlayer player = near(helper, spit);
            NBTTagCompound data = inspect(spit, player);
            assertSame(flour, ItemStack.loadItemStackFromNBT(data.getCompoundTag("Item")).getItem());
            assertEquals("overheat", data.getString("Reason"));
            fire.setLit(false);
            assertEquals("temperature", inspect(spit, player).getString("Reason"));
            spit.setInventorySlotContents(0, new ItemStack(flour));
            spit.updateRecipe();
            assertTrue(inspect(spit, player).getBoolean("Ready"));
            assertEquals("burning_food", inspect(spit, player).getString("Reason"));
            helper.setBlock(2, 2, 1, BlockListMF.oven_stone);
            TileEntityRoast oven = helper.assertTileEntityPresent(TileEntityRoast.class, 2, 2, 1);
            oven.setInventorySlotContents(0, new ItemStack(seed));
            oven.updateRecipe();
            data = inspect(oven, player);
            assertSame(bar, ItemStack.loadItemStackFromNBT(data.getCompoundTag("Item")).getItem());
            reload(tx -> {});
            assertEquals("checking", inspect(oven, player).getString("Reason"));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
