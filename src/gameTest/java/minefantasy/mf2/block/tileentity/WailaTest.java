package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.block.tileentity.WailaFixtures.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;
import static minefantasy.mf2.gametest.Ticks.tick;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.impl.ModuleRegistrar;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.fluid.FluidsMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.integration.waila.WailaData;
import minefantasy.mf2.integration.waila.WailaProvider;
import minefantasy.mf2.item.list.ToolListMF;

/** Real Waila server callbacks: looking at a station must not act on it or expose a distant inventory. */
@GameTestHolder("minefantasy2")
public class WailaTest {

    private WailaTest() {}

    @GameTest
    public static void heatHudReadsFuelAtTheRealBurnRate(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            helper.setBlock(1, 1, 1, BlockListMF.forge_active);
            TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 1, 1, 1);
            forge.fuel = 100;
            forge.temperature = 100;
            forge.fuelTemperature = 1000;
            FakePlayer player = near(helper, forge.xCoord, forge.yCoord, forge.zCoord);
            NBTTagCompound before = saved(forge);
            NBTTagCompound data = inspect(forge, player);
            assertTrue(data.getBoolean("Burning"));
            assertEquals(25F, data.getFloat("FuelSeconds"), 0.01F);
            assertEquals(100F, data.getFloat("Temperature"), 0F);
            assertEquals(before, saved(forge));
            tick(forge, 10);
            assertEquals(
                    "the HUD disagrees with ten real fuel ticks",
                    24.5F,
                    inspect(forge, player).getFloat("FuelSeconds"),
                    0.01F);
            helper.setBlock(2, 1, 1, BlockListMF.firepit);
            TileEntityFirepit fire = helper.assertTileEntityPresent(TileEntityFirepit.class, 2, 1, 1);
            fire.setLit(true);
            fire.fuel = 100;
            assertEquals(5F, inspect(fire, player).getFloat("FuelSeconds"), 0F);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aDisabledWailaCategoryAddsNoTooltipOrAdvancedPrompt(GameTestHelper helper) {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound data = new NBTTagCompound();
        data.setString("Kind", "craft");
        data.setString("Tool", "hammer");
        data.setInteger("ToolTier", 2);
        data.setInteger("BenchTier", 1);
        tag.setTag(WailaData.KEY, data);
        IWailaDataAccessor accessor = (IWailaDataAccessor) Proxy.newProxyInstance(
                WailaTest.class.getClassLoader(),
                new Class<?>[] { IWailaDataAccessor.class },
                (proxy, method, args) -> method.getName().equals("getNBTData") ? tag : null);
        IWailaConfigHandler config = (IWailaConfigHandler) Proxy.newProxyInstance(
                WailaTest.class.getClassLoader(),
                new Class<?>[] { IWailaConfigHandler.class },
                (proxy, method, args) -> false);
        WailaProvider provider = new WailaProvider();
        List<String> tip = new ArrayList<>();
        tip.add("Existing tooltip");
        assertSame(tip, provider.getWailaBody(null, tip, accessor, config));
        assertFalse(provider.hasWailaAdvancedBody(null, accessor, config));
        provider.getWailaAdvancedBody(null, tip, accessor, config);
        assertEquals(1, tip.size());
        IWailaConfigHandler enabled = (IWailaConfigHandler) Proxy.newProxyInstance(
                WailaTest.class.getClassLoader(),
                new Class<?>[] { IWailaConfigHandler.class },
                (proxy, method, args) -> true);
        assertTrue(provider.hasWailaAdvancedBody(null, accessor, enabled));
        provider.getWailaAdvancedBody(null, tip, accessor, enabled);
        assertEquals(3, tip.size());
        helper.succeed();
    }

    private static IWailaDataAccessor accessor(NBTTagCompound data) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(WailaData.KEY, data);
        return (IWailaDataAccessor) Proxy.newProxyInstance(
                WailaTest.class.getClassLoader(),
                new Class<?>[] { IWailaDataAccessor.class },
                (proxy, method, args) -> method.getName().equals("getNBTData") ? tag : null);
    }

    private static IWailaConfigHandler enabledConfig() {
        return (IWailaConfigHandler) Proxy.newProxyInstance(
                WailaTest.class.getClassLoader(),
                new Class<?>[] { IWailaConfigHandler.class },
                (proxy, method, args) -> true);
    }

    private static List<String> tooltip(NBTTagCompound data, boolean advanced) {
        WailaProvider provider = new WailaProvider();
        IWailaDataAccessor accessor = accessor(data);
        List<String> tip = new ArrayList<>();
        tip.add("Other provider");
        provider.getWailaBody(null, tip, accessor, enabledConfig());
        if (advanced) provider.getWailaAdvancedBody(null, tip, accessor, enabledConfig());
        return tip;
    }

    @GameTest
    public static void rackContentsReplaceOccupancyOnlyInTheAdvancedHud(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            minefantasy.mf2.block.tileentity.decor.TileEntityRack rack = place(
                    new minefantasy.mf2.block.tileentity.decor.TileEntityRack());
            FakePlayer player = near(helper, rack.xCoord, rack.yCoord, rack.zCoord);
            WailaProvider provider = new WailaProvider();
            NBTTagCompound empty = inspect(rack, player);
            assertFalse(provider.hasWailaAdvancedBody(null, accessor(empty), enabledConfig()));
            for (int i = 0; i < 4; i++) rack.setInventorySlotContents(i, new ItemStack(ToolListMF.hammerStone));
            NBTTagCompound before = new NBTTagCompound();
            rack.writeToNBT(before);
            NBTTagCompound data = inspect(rack, player);
            assertTrue(provider.hasWailaAdvancedBody(null, accessor(data), enabledConfig()));
            List<String> compact = tooltip(data, false);
            assertEquals(2, compact.size());
            assertFalse(
                    compact.toString().contains(
                            ToolListMF.hammerStone.getItemStackDisplayName(new ItemStack(ToolListMF.hammerStone))));
            List<String> expanded = tooltip(data, true);
            assertEquals(5, expanded.size());
            assertEquals("Other provider", expanded.get(0));
            assertFalse(expanded.contains(compact.get(1)));
            assertEquals(4, data.getTagList("Contents", 10).tagCount());
            NBTTagCompound after = new NBTTagCompound();
            rack.writeToNBT(after);
            assertEquals(before, after);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void advancedToolRequirementsDoNotRepeatTheRefusal(GameTestHelper helper) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("Kind", "craft");
        data.setString("Tool", "knife");
        data.setInteger("ToolTier", 2);
        data.setString("Reason", "tool");
        net.minecraft.nbt.NBTTagList args = new net.minecraft.nbt.NBTTagList();
        args.appendTag(new net.minecraft.nbt.NBTTagString("knife"));
        args.appendTag(new net.minecraft.nbt.NBTTagString("nothing"));
        data.setTag("Args", args);
        List<String> compact = tooltip(data, false);
        List<String> expanded = tooltip(data, true);
        assertEquals(2, compact.size());
        assertEquals(2, expanded.size()); // one enriched requirement, no Tool: knife or phantom bench tier
        assertTrue(expanded.get(1).contains("2"));
        assertFalse(expanded.get(1).equals(compact.get(1)));
        data.removeTag("Reason");
        data.removeTag("Args");
        assertEquals(1, tooltip(data, false).size());
        assertEquals(2, tooltip(data, true).size());
        helper.succeed();
    }

    @GameTest
    public static void heatDetailsKeepProgressAndProblemsWithoutDuplicatingBaseRows(GameTestHelper helper) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("Kind", "heat");
        data.setBoolean("Built", false);
        data.setString("Reason", "not_built");
        data.setInteger("Smoke", 20);
        data.setInteger("MaxSmoke", 100);
        data.setFloat("FuelSeconds", 90);
        data.setBoolean("ShowProgress", true);
        data.setFloat("Progress", 30);
        data.setFloat("MaxProgress", 100);
        List<String> compact = tooltip(data, false);
        List<String> expanded = tooltip(data, true);
        assertEquals(3, compact.size()); // foreign row, progress, failure; no duplicate 'not built'
        assertEquals(5, expanded.size()); // fuel and smoke added
        assertEquals("Other provider", expanded.get(0));
        assertTrue(expanded.contains(compact.get(1)));
        assertTrue(expanded.contains(compact.get(2)));
        assertEquals(expanded.size(), new java.util.HashSet<String>(expanded).size());
        helper.succeed();
    }

    @GameTest
    public static void theServerReceivedTheWailaRegistration(GameTestHelper helper) {
        TileEntityKitchenBench bench = new TileEntityKitchenBench();
        boolean found = false;
        for (List<IWailaDataProvider> providers : ModuleRegistrar.instance().getNBTProviders(bench).values()) {
            for (IWailaDataProvider provider : providers) {
                if (provider instanceof WailaProvider) found = true;
            }
        }
        assertTrue("MineFantasy's Waila callback did not register on the dedicated server", found);
        helper.succeed();
    }

    @GameTest
    public static void wailaDoesNotRevealAnUnresearchedResult(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            InformationBase locked = null;
            for (InformationBase base : InformationList.nameMap.values()) {
                if (!base.isPreUnlocked() && base.parentInfo == null) {
                    locked = base;
                    break;
                }
            }
            assertNotNull(locked);
            final String research = locked.getUnlocalisedName();
            reload(
                    tx -> tx.add(
                            MFRecipes.KITCHEN,
                            id("kitchen", "hidden_waila"),
                            GridRecipe.shapeless(
                                    GridRecipe.Grid.BENCH,
                                    new Object[] { new ItemStack(seed) },
                                    null,
                                    new ItemStack(bar)).tool("hands", 0).time(100).research(research).build(),
                            0));
            TileEntityKitchenBench bench = place(new TileEntityKitchenBench());
            bench.setInventorySlotContents(0, new ItemStack(seed));
            bench.updateCraftingData();
            FakePlayer novice = helper.spawnFakePlayer(Modders.NOVICE);
            novice.setPosition(bench.xCoord + 0.5, bench.yCoord + 1, bench.zCoord + 0.5);
            NBTTagCompound data = inspect(bench, novice);
            assertTrue(data.getBoolean("Unknown"));
            assertFalse(data.hasKey("Item"));
            assertFalse(data.hasKey("Tool"));
            assertFalse(data.hasKey("Args"));
            assertEquals("unknown_research", data.getString("Reason"));
            FakePlayer smith = near(helper, bench.xCoord, bench.yCoord, bench.zCoord);
            ResearchLogic.forceUnlock(smith, locked);
            assertTrue("an informed player saw no result", inspect(bench, smith).hasKey("Item"));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void lookingAtEachBenchReadsRequirementsWithoutResettingWork(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(tx -> {
                tx.add(
                        MFRecipes.ANVIL,
                        id("anvil", "waila"),
                        GridRecipe.shapeless(
                                GridRecipe.Grid.ANVIL,
                                new Object[] { new ItemStack(seed) },
                                null,
                                new ItemStack(bar)).tool("hammer", 0).time(100).build(),
                        0);
                tx.add(
                        MFRecipes.CARPENTER,
                        id("carpenter", "waila"),
                        GridRecipe.shapeless(
                                GridRecipe.Grid.BENCH,
                                new Object[] { new ItemStack(seed) },
                                null,
                                new ItemStack(bar)).tool("hammer", 0).time(100).build(),
                        0);
                tx.add(
                        MFRecipes.KITCHEN,
                        id("kitchen", "waila"),
                        GridRecipe.shapeless(
                                GridRecipe.Grid.BENCH,
                                new Object[] { new ItemStack(seed) },
                                null,
                                new ItemStack(bar)).tool("hands", 0).time(100).build(),
                        0);
            });
            for (TileEntityStation station : new TileEntityStation[] { new TileEntityAnvilMF(),
                    new TileEntityCarpenterMF(), new TileEntityKitchenBench() }) {
                place(station);
                world().setTileEntity(station.xCoord, station.yCoord, station.zCoord, station);
                station.setInventorySlotContents(0, new ItemStack(seed));
                ((GridProject.Bench) station).updateCraftingData();
                setField(station, "progress", 40F);
                FakePlayer player = near(helper, station.xCoord, station.yCoord, station.zCoord);
                NBTTagCompound before = saved(station);
                world().getChunkFromBlockCoords(station.xCoord, station.zCoord).isModified = false;
                NBTTagCompound data = inspect(station, player);
                assertEquals("craft", data.getString("Kind"));
                assertEquals(40F, data.getFloat("Progress"), 0F);
                assertSame(bar, ItemStack.loadItemStackFromNBT(data.getCompoundTag("Item")).getItem());
                if (!(station instanceof TileEntityKitchenBench)) assertEquals("tool", data.getString("Reason"));
                player.setCurrentItemOrArmor(0, new ItemStack(ToolListMF.hammerStone));
                assertFalse("valid tool was refused", inspect(station, player).hasKey("Reason"));
                assertEquals("inspection changed saved state", before, saved(station));
                assertFalse(
                        "inspection dirtied the chunk",
                        world().getChunkFromBlockCoords(station.xCoord, station.zCoord).isModified);
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void dirtyKitchenReportsDirtAndBlockedOutput(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.KITCHEN,
                            id("kitchen", "waila"),
                            GridRecipe.shapeless(
                                    GridRecipe.Grid.BENCH,
                                    new Object[] { new ItemStack(seed) },
                                    null,
                                    new ItemStack(bar)).tool("hands", 0).time(100).build(),
                            0));
            TileEntityKitchenBench bench = place(new TileEntityKitchenBench());
            bench.setInventorySlotContents(0, new ItemStack(seed));
            bench.updateCraftingData();
            FakePlayer player = near(helper, bench.xCoord, bench.yCoord, bench.zCoord);
            bench.dirtyProgress = bench.getDirtyMax();
            assertEquals("dirty", inspect(bench, player).getString("Reason"));
            bench.dirtyProgress = 0;
            bench.setInventorySlotContents(bench.getSizeInventory() - 5, new ItemStack(junk));
            assertEquals("output_full", inspect(bench, player).getString("Reason"));
            bench.setInventorySlotContents(bench.getSizeInventory() - 5, null);
            assertFalse(inspect(bench, player).hasKey("Reason"));
            NBTTagCompound before = saved(bench);
            reload(tx -> {});
            NBTTagCompound stale = inspect(bench, player);
            assertEquals("checking", stale.getString("Reason"));
            assertFalse(stale.hasKey("Item"));
            assertEquals("inspection refreshed a removed recipe", before, saved(bench));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void crucibleInspectionWaitsForRecipeReloadWithoutActing(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            reload(
                    tx -> tx.add(
                            MFRecipes.ALLOY,
                            id("alloy", "waila"),
                            new Alloy(new ItemStack(bar), 0, Arrays.asList(new ItemStack(seed))),
                            0));
            helper.setBlock(1, 1, 1, BlockListMF.crucible);
            TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 1, 1, 1);
            crucible.setInventorySlotContents(0, new ItemStack(seed));
            crucible.progress = 40;
            helper.setBlock(1, 0, 1, net.minecraft.init.Blocks.lava);
            crucible.temperature = 100;
            FakePlayer player = near(helper, crucible.xCoord, crucible.yCoord, crucible.zCoord);
            assertFalse(inspect(crucible, player).hasKey("Reason"));
            helper.setBlock(1, 0, 1, net.minecraft.init.Blocks.air);
            crucible.temperature = 0;
            assertEquals("no_heat", inspect(crucible, player).getString("Reason"));
            helper.setBlock(1, 0, 1, net.minecraft.init.Blocks.lava);
            crucible.temperature = 100;
            reload(tx -> {});
            NBTTagCompound before = saved(crucible);
            NBTTagCompound data = inspect(crucible, player);
            assertEquals("checking", data.getString("Reason"));
            assertFalse("removed alloy was displayed", data.hasKey("Item"));
            assertEquals("looking up a removed alloy changed progress", before, saved(crucible));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void wailaReadsTroughAmountsWithoutQuenchingOrDraining(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.trough_wood);
        TileEntityTrough trough = helper.assertTileEntityPresent(TileEntityTrough.class, 1, 1, 1);
        trough.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidRegistry.WATER, 100), true);
        FakePlayer player = near(helper, trough.xCoord, trough.yCoord, trough.zCoord);
        NBTTagCompound before = new NBTTagCompound();
        trough.writeToNBT(before);
        inspectTrough(trough, player, "water", 100, true);
        NBTTagCompound after = new NBTTagCompound();
        trough.writeToNBT(after);
        assertEquals(before, after);
        trough.drain(ForgeDirection.UNKNOWN, 1000, true);
        trough.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidsMF.saltWater, 10), true);
        inspectTrough(trough, player, "saltwater", 10, false);
        trough.drain(ForgeDirection.UNKNOWN, 1000, true);
        trough.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidsMF.seedOil, 100), true);
        inspectTrough(trough, player, "seedoil", 100, true);
        helper.succeed();
    }

    private static void inspectTrough(TileEntityTrough trough, FakePlayer player, String fluid, int amount,
            boolean quench) {
        NBTTagCompound data = new WailaProvider().getNBTData(
                player,
                trough,
                new NBTTagCompound(),
                trough.getWorldObj(),
                trough.xCoord,
                trough.yCoord,
                trough.zCoord).getCompoundTag(WailaData.KEY);
        assertEquals(fluid, data.getString("Fluid"));
        assertEquals(amount, data.getInteger("Amount"));
        assertEquals(quench, data.getBoolean("Quench"));
    }

    @GameTest
    public static void wailaRejectsDistantAndMismatchedTargets(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            TileEntityKitchenBench bench = place(new TileEntityKitchenBench());
            FakePlayer player = near(helper, bench.xCoord, bench.yCoord, bench.zCoord);
            assertEquals("craft", inspect(bench, player).getString("Kind"));
            player.setPosition(bench.xCoord + 20, bench.yCoord, bench.zCoord);
            assertTrue(inspect(bench, player).hasNoTags());
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag(WailaData.KEY, new NBTTagCompound());
            new WailaProvider().getNBTData(player, bench, tag, world(), bench.xCoord + 1, bench.yCoord, bench.zCoord);
            assertFalse(tag.hasKey(WailaData.KEY));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
