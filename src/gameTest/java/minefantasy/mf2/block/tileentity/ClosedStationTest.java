package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.function.BiFunction;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.container.ContainerAnvilMF;
import minefantasy.mf2.container.ContainerBigFurnace;
import minefantasy.mf2.container.ContainerBlastChamber;
import minefantasy.mf2.container.ContainerBlastHeater;
import minefantasy.mf2.container.ContainerBloomery;
import minefantasy.mf2.container.ContainerBombBench;
import minefantasy.mf2.container.ContainerCarpenterMF;
import minefantasy.mf2.container.ContainerCrossbowBench;
import minefantasy.mf2.container.ContainerCrucible;
import minefantasy.mf2.container.ContainerForge;
import minefantasy.mf2.container.ContainerKitchenBench;
import minefantasy.mf2.container.ContainerQuern;
import minefantasy.mf2.container.ContainerResearch;
import minefantasy.mf2.gametest.Modders;

/**
 * A station the player can no longer use while its window is open: broken, left behind by a teleport, or in another
 * dimension. The window closes on the player's next tick, but a click can arrive before that and must take nothing: a
 * broken station has already dropped its contents, so the items would exist twice.
 */
@GameTestHolder("minefantasy2")
public class ClosedStationTest {

    private ClosedStationTest() {}

    /** How the player comes to be no longer at the station while its window is still open. */
    private enum Leave {

        BROKEN,
        TELEPORTED,
        OTHER_DIMENSION;

        void apply(GameTestHelper helper, TestPos at, FakePlayer player) {
            switch (this) {
                case BROKEN:
                    helper.getWorld().setBlockToAir(at.x(), at.y(), at.z());
                    break;
                case TELEPORTED:
                    player.setPosition(at.x() + 100.5, at.y() + 1, at.z() + 0.5);
                    break;
                case OTHER_DIMENSION:
                    // The same coordinates in the Nether: only the world tells the station apart
                    player.worldObj = MinecraftServer.getServer().worldServerForDimension(-1);
                    player.dimension = -1;
                    break;
            }
        }
    }

    private static <T extends TileEntity & IInventory> void breakWithWindowOpen(GameTestHelper helper, Block block,
            Class<T> type, BiFunction<FakePlayer, T, Container> open) {
        for (Leave leave : Leave.values()) {
            String what = type.getSimpleName() + " " + leave;
            helper.setBlock(1, 1, 1, block);
            T station = helper.assertTileEntityPresent(type, 1, 1, 1);
            FakePlayer player = Modders.fresh(helper, Modders.SMITH);
            player.inventory.clearInventory(null, -1);
            TestPos at = helper.absolute(1, 1, 1);
            player.setPosition(at.x() + 0.5, at.y() + 1, at.z() + 0.5);
            Container window = open.apply(player, station);
            int slot = -1;
            for (int i = 0; i < window.inventorySlots.size(); i++) {
                Slot s = (Slot) window.inventorySlots.get(i);
                if (s.inventory == station && s.getSlotIndex() == 0) slot = i;
            }
            assertTrue(what + ": no slot 0", slot >= 0);
            station.setInventorySlotContents(0, new ItemStack(Items.coal, 8));
            assertTrue(what + ": refused a player beside it", window.canInteractWith(player));

            leave.apply(helper, at, player);
            assertFalse(what + ": stayed usable", window.canInteractWith(player));
            window.slotClick(slot, 0, 0, player);
            window.slotClick(slot, 0, 1, player);
            assertNull(what + ": the cursor took from it", player.inventory.getItemStack());
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                assertNull(what + ": the inventory took from it", player.inventory.getStackInSlot(i));
            }
            helper.getWorld().setBlockToAir(at.x(), at.y(), at.z());
        }
    }

    @GameTest
    public static void anvil(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.anvilStone,
                TileEntityAnvilMF.class,
                (p, t) -> new ContainerAnvilMF(p.inventory, t));
        helper.succeed();
    }

    @GameTest
    public static void carpenter(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.carpenter,
                TileEntityCarpenterMF.class,
                (p, t) -> new ContainerCarpenterMF(p.inventory, t));
        helper.succeed();
    }

    @GameTest
    public static void kitchenBench(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.kitchenBench,
                TileEntityKitchenBench.class,
                (p, t) -> new ContainerKitchenBench(p.inventory, t));
        helper.succeed();
    }

    @GameTest
    public static void quern(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.quern,
                TileEntityQuern.class,
                (p, t) -> new ContainerQuern(p.inventory, t));
        helper.succeed();
    }

    @GameTest
    public static void crucible(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.crucible,
                TileEntityCrucible.class,
                (p, t) -> new ContainerCrucible(p.inventory, t));
        helper.succeed();
    }

    @GameTest
    public static void bloomery(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.bloomery,
                TileEntityBloomery.class,
                (p, t) -> new ContainerBloomery(p.inventory, t));
        helper.succeed();
    }

    @GameTest
    public static void forge(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.forge,
                TileEntityForge.class,
                (p, t) -> new ContainerForge(p.inventory, t));
        helper.succeed();
    }

    @GameTest
    public static void researchTable(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.research,
                TileEntityResearch.class,
                (p, t) -> new ContainerResearch(p.inventory, t));
        helper.succeed();
    }

    @GameTest
    public static void bombBench(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.bombBench,
                TileEntityBombBench.class,
                (p, t) -> new ContainerBombBench(p.inventory, t));
        helper.succeed();
    }

    @GameTest
    public static void crossbowBench(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.crossbowBench,
                TileEntityCrossbowBench.class,
                (p, t) -> new ContainerCrossbowBench(p.inventory, t));
        helper.succeed();
    }

    @GameTest
    public static void bigFurnace(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.furnace_stone,
                TileEntityBigFurnace.class,
                (p, t) -> new ContainerBigFurnace(p, t));
        helper.succeed();
    }

    @GameTest
    public static void blastHeater(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.blast_heater,
                TileEntityBlastFH.class,
                (p, t) -> new ContainerBlastHeater(p.inventory, t));
        helper.succeed();
    }

    @GameTest
    public static void blastChamber(GameTestHelper helper) {
        breakWithWindowOpen(
                helper,
                BlockListMF.blast_chamber,
                TileEntityBlastFC.class,
                (p, t) -> new ContainerBlastChamber(p.inventory, t));
        helper.succeed();
    }
}
