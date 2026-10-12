package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.Ticks.tick;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.heating.AmbientTemperature;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.item.heatable.ItemHeated;
import minefantasy.mf2.item.list.ComponentListMF;

@GameTestHolder("minefantasy2")
public class AmbientTemperatureTest {

    public static class SavingFurnace extends TileEntityBigFurnace {

        public int dirtyCalls;

        @Override
        public void markDirty() {
            dirtyCalls++;
            super.markDirty();
        }
    }

    @GameTest
    public static void furnaceCoolingSavesCleanChunksWithoutComparatorUpdates(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.furnace_heater);
        com.gtnewhorizons.horizonqa.api.TestPos at = helper.absolute(1, 1, 1);
        net.minecraft.tileentity.TileEntity original = helper.getWorld().getTileEntity(at.x(), at.y(), at.z());
        SavingFurnace furnace = new SavingFurnace();
        helper.getWorld().setTileEntity(at.x(), at.y(), at.z(), furnace);
        try (Overrides overrides = new Overrides().air((world, x, y, z) -> 20F)) {
            furnace.updateEntity();
            furnace.dirtyCalls = 0;
            furnace.heat = 80F;
            net.minecraft.world.chunk.Chunk chunk = helper.getWorld().getChunkFromBlockCoords(at.x(), at.z());
            chunk.isModified = false;
            furnace.updateEntity();
            assertTrue(furnace.heat < 80F);
            assertTrue("heat change was not saved", chunk.isModified);
            assertEquals("heat change issued comparator updates", 0, furnace.dirtyCalls);
        } finally {
            helper.getWorld().setTileEntity(at.x(), at.y(), at.z(), original);
        }
        helper.succeed();
    }

    @GameTest
    public static void coldStationsSettleAtAirAndKeepItAcrossSave(GameTestHelper helper) {
        try (Overrides overrides = new Overrides().air((world, x, y, z) -> -15F)) {
            helper.setBlock(1, 1, 1, BlockListMF.forge);
            TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 1, 1, 1);
            forge.updateEntity();
            assertEquals(-15F, forge.temperature, 0F);
            assertEquals("cold forge supplies heat", 0, forge.getHeat());
            forge.temperature = 85F;
            tick(forge, 60);
            assertEquals("forge did not cool to air", -15F, forge.temperature, 0F);
            NBTTagCompound saved = new NBTTagCompound();
            forge.writeToNBT(saved);
            TileEntityForge loaded = new TileEntityForge();
            loaded.readFromNBT(saved);
            assertEquals(-15F, loaded.temperature, 0F);
            helper.setBlock(2, 1, 1, BlockListMF.crucible);
            TileEntityCrucible crucible = helper.assertTileEntityPresent(TileEntityCrucible.class, 2, 1, 1);
            crucible.updateEntity();
            assertEquals(-15F, crucible.temperature, 0F);
            assertEquals(0F, crucible.progress, 0F);
        }
        helper.succeed();
    }

    @GameTest
    public static void ambientAirDoesNotMakeAColdPieceHot(GameTestHelper helper) {
        try (Overrides overrides = new Overrides().air((world, x, y, z) -> 40F)) {
            helper.setBlock(1, 1, 1, BlockListMF.forge);
            TileEntityForge forge = helper.assertTileEntityPresent(TileEntityForge.class, 1, 1, 1);
            ItemStack cold = new ItemStack(net.minecraft.init.Items.iron_ingot);
            forge.setInventorySlotContents(0, cold);
            tick(forge, 40);
            assertEquals(40F, forge.temperature, 0F);
            assertEquals(cold.getItem(), forge.getStackInSlot(0).getItem());
        }
        helper.succeed();
    }

    @GameTest
    public static void carriedPieceCoolsWithoutLosingItsTagsOrCount(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            if (helper.getWorld().getTotalWorldTime() % 20 != 0) return false;
            ItemStack hot = new ItemStack(ComponentListMF.hotItem, 3);
            hot.setTagCompound(new NBTTagCompound());
            hot.getTagCompound().setString("reviewMarker", "kept");
            ItemHeated.setTemp(hot, 200);
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            com.gtnewhorizons.horizonqa.api.TestPos at = helper.absolute(1, 1, 1);
            player.setPosition(at.x(), at.y(), at.z());
            int ambient = Math.round(AmbientTemperature.get(helper.getWorld(), at.x(), at.y(), at.z()));
            hot.getItem().onUpdate(hot, helper.getWorld(), player, 0, true);
            assertTrue(Heatable.getTemp(hot) < 200);
            assertTrue(Heatable.getTemp(hot) >= ambient);
            assertEquals(3, hot.stackSize);
            assertEquals("kept", hot.getTagCompound().getString("reviewMarker"));
            ItemHeated.setTemp(hot, ambient + 1);
            hot.getItem().onUpdate(hot, helper.getWorld(), player, 0, true);
            assertEquals(ambient, Heatable.getTemp(hot));
            assertFalse(((ItemHeated) hot.getItem()).isHot(hot));
            return true;
        });
    }
}
