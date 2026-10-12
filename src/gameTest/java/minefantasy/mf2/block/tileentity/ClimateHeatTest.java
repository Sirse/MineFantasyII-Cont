package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;
import static minefantasy.mf2.gametest.Ticks.tick;

import java.util.Arrays;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.gametest.Overrides;

@GameTestHolder("minefantasy2")
public class ClimateHeatTest {

    @GameTest
    public static void repeatedWeatherChangesNeverPowerIdleStations(GameTestHelper helper) {
        Stations.begin(helper);
        float[] air = { 40F };
        try (Overrides overrides = new Overrides().air((world, x, y, z) -> air[0])) {
            reload(tx -> {
                tx.add(
                        MFRecipes.ALLOY,
                        id("alloy", "climate"),
                        new Alloy(new ItemStack(flour), 0, Arrays.asList(new ItemStack(seed))),
                        0);
                tx.add(
                        MFRecipes.BIG_FURNACE,
                        id("big_furnace", "climate"),
                        ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                        0);
            });
            TileEntityBigFurnace furnace = FurnaceIdleTest.builtFurnace(helper);
            TileEntityBigFurnace heater = helper.assertTileEntityPresent(TileEntityBigFurnace.class, 2, 1, 2);
            furnace.setInventorySlotContents(0, new ItemStack(seed));
            helper.setBlock(5, 1, 2, BlockListMF.forge_metal_active);
            helper.setBlock(5, 2, 2, BlockListMF.crucible);
            helper.setBlock(7, 1, 2, BlockListMF.forge);
            TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 5, 1, 2);
            TileEntityForge stone = helper.assertTileEntityPresent(TileEntityForge.class, 7, 1, 2);
            TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 5, 2, 2);
            crucible.setInventorySlotContents(0, new ItemStack(seed));
            for (float climate : new float[] { 40, 20, 60, -15, 40, 20 }) {
                air[0] = climate;
                for (int tick = 0; tick < 30; tick++) {
                    forge.updateEntity();
                    stone.updateEntity();
                    crucible.updateEntity();
                    heater.updateEntity();
                    furnace.updateEntity();
                    assertEquals(0, forge.getHeat());
                    assertEquals(0, stone.getHeat());
                    assertEquals(0F, crucible.progress, 0F);
                    assertEquals(0, furnace.progress);
                }
            }
            assertTrue(furnace.getStackInSlot(4) == null);
            assertTrue(crucible.getStackInSlot(9) == null);
            assertEquals(1, furnace.getStackInSlot(0).stackSize);
            assertEquals(1, crucible.getStackInSlot(0).stackSize);
            NBTTagCompound legacy = new NBTTagCompound();
            forge.writeToNBT(legacy);
            legacy.removeTag("SourceHeat");
            TileEntityForge loaded = new TileEntityForge();
            loaded.readFromNBT(legacy);
            helper.getWorld().setTileEntity(forge.xCoord, forge.yCoord, forge.zCoord, loaded);
            assertEquals("legacy air warmth gained work credit", 0, loaded.getHeat());
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void fixedFireHasTheSameSmeltingSpeedInEveryClimate(GameTestHelper helper) {
        Stations.begin(helper);
        float[] air = { -100F };
        try (Overrides overrides = new Overrides().air((world, x, y, z) -> air[0])) {
            reload(
                    tx -> tx.add(
                            MFRecipes.ALLOY,
                            id("alloy", "fire_climate"),
                            new Alloy(new ItemStack(flour), 0, Arrays.asList(new ItemStack(seed))),
                            0));
            helper.setBlock(2, 1, 2, Blocks.netherrack);
            helper.setBlock(2, 2, 2, Blocks.fire);
            helper.setBlock(2, 3, 2, BlockListMF.crucible);
            TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 2, 3, 2);
            crucible.setInventorySlotContents(0, new ItemStack(seed));
            float expected = -1;
            for (float climate : new float[] { -100, 20, 100, -100 }) {
                air[0] = climate;
                crucible.progress = 0;
                tick(crucible, 20);
                if (expected < 0) expected = crucible.progress;
                assertTrue(crucible.progress > 0);
                assertEquals("climate changed the fire's processing speed", expected, crucible.progress, .001F);
                assertEquals(250F, crucible.getWorkingHeat(), 0F);
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aCrucibleLightsOverFireAndGoesOutWithout(GameTestHelper helper) {
        helper.setBlock(2, 1, 2, Blocks.netherrack);
        helper.setBlock(2, 2, 2, Blocks.fire);
        helper.setBlock(2, 3, 2, BlockListMF.crucible);
        TestPos at = helper.absolute(2, 3, 2);
        TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 2, 3, 2);
        crucible.updateEntity();
        assertSame(
                "stayed cold over fire",
                BlockListMF.crucible_active,
                helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        helper.setBlock(2, 2, 2, Blocks.air);
        crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 2, 3, 2);
        crucible.updateEntity();
        assertSame("stayed lit without fire", BlockListMF.crucible, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        helper.succeed();
    }

    @GameTest
    public static void idleForgeStaysCleanButHiddenCoolingIsSaved(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        int x = 304040, y = 100, z = 304040;
        float[] air = { 40F };
        try (Overrides overrides = new Overrides().air((w, bx, by, bz) -> air[0])) {
            provider.loadChunk(x >> 4, z >> 4);
            world.setBlock(x, y, z, BlockListMF.forge_metal_active, 0, 3);
            TileEntityForge forge = (TileEntityForge) world.getTileEntity(x, y, z);
            forge.updateEntity();
            forge.fuel = 100;
            forge.fuelTemperature = 600F;
            forge.onUsedWithBellows(.04F);
            forge.fuel = 0;
            // Drain the bellows change before isolating the following cooling update.
            forge.updateEntity();
            world.getChunkFromBlockCoords(x, z).isModified = false;
            tick(forge, 3);
            assertEquals(40F, forge.temperature, 0F);
            assertEquals(10F, forge.getWorkingHeat(), .001F);
            assertTrue("hidden cooling was not saved", world.getChunkFromBlockCoords(x, z).isModified);
            forge = reloadForge(world, provider, x, y, z);
            assertEquals(10F, forge.getWorkingHeat(), .001F);
            tick(forge, 20);
            assertEquals(0F, forge.getWorkingHeat(), 0F);
            world.getChunkFromBlockCoords(x, z).isModified = false;
            tick(forge, 40);
            assertTrue("idle forge dirtied an unchanged chunk", !world.getChunkFromBlockCoords(x, z).isModified);
            air[0] = 60F;
            tick(forge, 10);
            assertTrue("air temperature change was not saved", world.getChunkFromBlockCoords(x, z).isModified);
        } finally {
            world.setBlockToAir(x, y, z);
        }
        helper.succeed();
    }

    @GameTest
    public static void sourceHeatSurvivesRealChunkReloadButAirNeverBecomesWork(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        // An isolated chunk: unloading a shared GameTest structure chunk would disturb other running tests.
        int x = 304008, y = 100, z = 304008;
        float[] air = { 40F };
        try (Overrides overrides = new Overrides().air((w, bx, by, bz) -> air[0])) {
            provider.loadChunk(x >> 4, z >> 4);
            world.setBlock(x, y, z, BlockListMF.forge_metal_active, 0, 3);
            TileEntityForge forge = (TileEntityForge) world.getTileEntity(x, y, z);
            forge.updateEntity();
            forge.fuel = 100;
            forge.fuelTemperature = 600;
            forge.onUsedWithBellows(1);
            forge.fuel = 0;
            float residue = forge.getWorkingHeat();
            assertTrue(residue > 0);
            world.getChunkFromBlockCoords(x, z).isModified = false;
            tick(forge, 10);
            assertTrue("cooling was not marked for saving", world.getChunkFromBlockCoords(x, z).isModified);
            residue = forge.getWorkingHeat();
            air[0] = 20;
            forge = reloadForge(world, provider, x, y, z);
            assertEquals("residual source heat changed on load", residue, forge.getWorkingHeat(), 0F);
            assertTrue(forge.getHeat() > 0);
            tick(forge, 300);
            assertEquals(0F, forge.getWorkingHeat(), 0F);
            air[0] = 40;
            tick(forge, 20);
            air[0] = 20;
            forge = reloadForge(world, provider, x, y, z);
            assertEquals("saved air warmth became useful heat", 0, forge.getHeat());
            tick(forge, 20);
            assertEquals(0, forge.getHeat());
        } finally {
            world.setBlockToAir(x, y, z);
        }
        helper.succeed();
    }

    @GameTest
    public static void blastContinuesAcrossFuelPortionsAndPersistsItsStop(GameTestHelper helper) {
        Stations.begin(helper);
        WorldServer world = helper.getWorld();
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        int x = 304024, y = 100, z = 304024;
        try (Overrides overrides = new Overrides()) {
            reload(
                    tx -> tx.add(
                            MFRecipes.BLAST_FURNACE,
                            id("blast_furnace", "fuel_cycles"),
                            ProcessRecipe.of(Input.of(ore), new ItemStack(bar)),
                            0));
            provider.loadChunk(x >> 4, z >> 4);
            world.setBlock(x, y - 1, z, BlockListMF.crucible);
            world.setBlock(x, y, z, BlockListMF.blast_heater);
            world.setBlock(x, y + 1, z, BlockListMF.blast_chamber);
            for (int dx : new int[] { -1, 1 })
                for (int dz : new int[] { -1, 1 }) world.setBlock(x + dx, y, z + dz, BlockListMF.firebricks);
            for (int[] side : new int[][] { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } })
                world.setBlock(x + side[0], y + 1, z + side[1], BlockListMF.firebricks);
            minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH heater = (minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH) world
                    .getTileEntity(x, y, z);
            minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC chamber = (minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC) world
                    .getTileEntity(x, y + 1, z);
            TileEntityCrucible crucible = (TileEntityCrucible) world.getTileEntity(x, y - 1, z);
            heater.updateBuild();
            chamber.updateBuild();
            assertTrue(heater.isBuilt && chamber.isBuilt);
            int burnTime = minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH
                    .getItemBurnTime(new ItemStack(Items.coal));
            assertTrue(burnTime > 0);
            // Each smelt necessarily crosses a fuel boundary; the normal burner and smoke paths still run.
            overrides.<Float>set(
                    () -> minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH.maxProgress,
                    v -> minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH.maxProgress = v,
                    burnTime + 20F);
            heater.setInventorySlotContents(0, new ItemStack(Items.coal, 3));
            chamber.setInventorySlotContents(0, new ItemStack(carbon));
            chamber.setInventorySlotContents(1, new ItemStack(ore, 2));
            int portions = 0;
            for (int tick = 0; tick < burnTime * 4; tick++) {
                int before = heater.getStackInSlot(0) == null ? 0 : heater.getStackInSlot(0).stackSize;
                float progress = heater.progress;
                heater.updateEntity();
                chamber.updateEntity();
                int after = heater.getStackInSlot(0) == null ? 0 : heater.getStackInSlot(0).stackSize;
                if (after < before) {
                    if (++portions > 1) assertTrue("progress reset between fuel portions", heater.progress >= progress);
                }
                if (crucible.getStackInSlot(9) != null && crucible.getStackInSlot(9).stackSize == 2) break;
            }
            assertTrue(portions >= 3);
            assertSame(bar, crucible.getStackInSlot(9).getItem());
            assertEquals(2, crucible.getStackInSlot(9).stackSize);
            assertTrue(chamber.getStackInSlot(1) == null);
            assertTrue(chamber.getStackInSlot(0) == null);
            assertEquals(2, chamber.tempUses);
            world.getChunkFromBlockCoords(x, z).isModified = false;
            tick(burnTime + 5, heater, chamber);
            assertEquals(0, heater.fuel);
            assertEquals(0F, heater.progress, 0F);
            assertTrue("stopped furnace was not marked for saving", world.getChunkFromBlockCoords(x, z).isModified);
            provider.unloadChunksIfNotNearSpawn(x >> 4, z >> 4);
            provider.unloadQueuedChunks();
            assertFalse(provider.chunkExists(x >> 4, z >> 4));
            provider.loadChunk(x >> 4, z >> 4);
            heater = (minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH) world.getTileEntity(x, y, z);
            chamber = (minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC) world
                    .getTileEntity(x, y + 1, z);
            crucible = (TileEntityCrucible) world.getTileEntity(x, y - 1, z);
            assertEquals(0, heater.fuel);
            assertEquals(0F, heater.progress, 0F);
            assertEquals(2, chamber.tempUses);
            assertEquals(2, crucible.getStackInSlot(9).stackSize);
        } finally {
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) world.setBlockToAir(x + dx, y + dy, z + dz);
            Stations.end();
        }
        helper.succeed();
    }

    private static TileEntityForge reloadForge(WorldServer world, ChunkProviderServer provider, int x, int y, int z) {
        provider.unloadChunksIfNotNearSpawn(x >> 4, z >> 4);
        provider.unloadQueuedChunks();
        assertFalse("the isolated chunk stayed loaded", provider.chunkExists(x >> 4, z >> 4));
        provider.loadChunk(x >> 4, z >> 4);
        return (TileEntityForge) world.getTileEntity(x, y, z);
    }
}
