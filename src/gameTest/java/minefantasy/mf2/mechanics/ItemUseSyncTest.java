package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import io.netty.buffer.Unpooled;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.TileEntityRoast;
import minefantasy.mf2.block.tileentity.TileEntityTanningRack;
import minefantasy.mf2.farming.FarmingHelper;
import minefantasy.mf2.gametest.DenyPlacing;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.gametest.Watcher;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

/**
 * Tools used on a block through Forge's item use: the server and the player's client must agree, and protection that
 * refuses the change must leave the block and the payment.
 */
@GameTestHolder("minefantasy2")
public class ItemUseSyncTest {

    private ItemUseSyncTest() {}

    private static Watcher holding(GameTestHelper helper, TestPos at, ItemStack held) {
        Watcher watcher = new Watcher(helper, at.x() + 0.5D, at.y() + 1, at.z() + 2.5D);
        watcher.player.setCurrentItemOrArmor(0, held);
        // As logging in does, so the inventory reports its changes to the client
        watcher.player.addSelfToInternalCraftingInventory();
        return watcher;
    }

    /** As the server runs a right click on the top of a block. */
    private static boolean use(Watcher watcher, World world, TestPos at) {
        return ForgeHooks.onPlaceItemIntoWorld(
                watcher.player.getHeldItem(),
                watcher.player,
                world,
                at.x(),
                at.y(),
                at.z(),
                1,
                .5F,
                1F,
                .5F);
    }

    private static Block at(World world, TestPos pos) {
        return world.getBlock(pos.x(), pos.y(), pos.z());
    }

    private static Block shown(Watcher watcher, TestPos pos) {
        watcher.flush();
        return watcher.shown(pos.x(), pos.y(), pos.z());
    }

    private static int oil(Watcher watcher) {
        int count = 0;
        for (ItemStack stack : watcher.player.inventory.mainInventory) {
            if (stack != null && stack.getItem() == ComponentListMF.plant_oil) count += stack.stackSize;
        }
        return count;
    }

    private static Watcher painter(GameTestHelper helper, TestPos at) {
        Watcher watcher = holding(helper, at, new ItemStack(ToolListMF.paint_brush));
        watcher.player.inventory.setInventorySlotContents(1, new ItemStack(ComponentListMF.plant_oil, 2));
        ResearchLogic.forceUnlock(watcher.player, KnowledgeListMF.paint_brush);
        return watcher;
    }

    @GameTest
    public static void paintingShowsThePaintedBlockAndPaysOnce(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.nailed_planks);
        TestPos plank = helper.absolute(1, 1, 1);
        World world = helper.getWorld();
        try (Watcher watcher = painter(helper, plank)) {
            use(watcher, world, plank);
            assertFalse("Forge's capture is left as it was", world.captureBlockSnapshots);
            assertEquals(BlockListMF.refined_planks, at(world, plank));
            assertEquals("one oil is paid", 1, oil(watcher));
            assertEquals("the brush wore", 1, watcher.player.getHeldItem().getItemDamage());
            assertEquals("the client sees the paint", BlockListMF.refined_planks, shown(watcher, plank));
        }
        helper.succeed();
    }

    /**
     * How many the client was last told a hotbar slot holds, or -1 when it was never told. The player's own inventory
     * numbers the hotbar from 36.
     */
    private static int shownIn(Watcher watcher, int hotbar) {
        int shown = -1;
        for (Packet packet : watcher.sent) {
            if (!(packet instanceof S2FPacketSetSlot)) continue;
            PacketBuffer data = new PacketBuffer(Unpooled.buffer());
            try {
                packet.writePacketData(data);
                data.readByte();
                int slot = data.readShort();
                ItemStack stack = data.readItemStackFromBuffer();
                if (slot == 36 + hotbar) shown = stack == null ? 0 : stack.stackSize;
            } catch (IOException exception) {
                throw new AssertionError("could not read a slot", exception);
            }
        }
        return shown;
    }

    /** A right click on the top of a block as the client sends it; the packet's own constructor is client only. */
    private static C08PacketPlayerBlockPlacement click(TestPos at, ItemStack held) {
        PacketBuffer data = new PacketBuffer(Unpooled.buffer());
        C08PacketPlayerBlockPlacement packet = new C08PacketPlayerBlockPlacement();
        try {
            data.writeInt(at.x());
            data.writeByte(at.y());
            data.writeInt(at.z());
            data.writeByte(1);
            data.writeItemStackToBuffer(held);
            data.writeByte(8);
            data.writeByte(16);
            data.writeByte(8);
            packet.readPacketData(data);
        } catch (IOException exception) {
            throw new AssertionError("could not write a click", exception);
        }
        return packet;
    }

    /** A right click on the top of a block, through the server's handling of the client's packet. */
    private static void rightClick(Watcher watcher, TestPos at) {
        watcher.sent.clear();
        watcher.player.playerNetServerHandler.processPlayerBlockPlacement(click(at, watcher.player.getHeldItem()));
    }

    /** Taking food off a spit or hide off a rack into a slot other than the held one shows it to the client at once. */
    @GameTest
    public static void takingOffAStationShowsTheItemTaken(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.roast);
        helper.setBlock(3, 1, 1, BlockListMF.tanner);
        World world = helper.getWorld();
        TestPos spit = helper.absolute(1, 1, 1);
        TestPos rack = helper.absolute(3, 1, 1);
        ((TileEntityRoast) world.getTileEntity(spit.x(), spit.y(), spit.z()))
                .setInventorySlotContents(0, new ItemStack(Items.beef));
        ((TileEntityTanningRack) world.getTileEntity(rack.x(), rack.y(), rack.z()))
                .setInventorySlotContents(0, new ItemStack(Items.leather));
        try (Watcher watcher = holding(helper, spit, new ItemStack(Items.stick))) {
            rightClick(watcher, spit);
            assertEquals("the client was not shown the food taken", 1, shownIn(watcher, 1));
            watcher.player.inventory.setInventorySlotContents(1, null);
            rightClick(watcher, rack);
            assertEquals("the client was not shown the hide taken", 1, shownIn(watcher, 1));
        }
        helper.succeed();
    }

    /** Painting through the client's real click: the oil paid from another slot reaches the client at once (#83). */
    @GameTest
    public static void paintingShowsThePaidOil(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.nailed_planks);
        TestPos plank = helper.absolute(1, 1, 1);
        World world = helper.getWorld();
        try (Watcher watcher = painter(helper, plank)) {
            rightClick(watcher, plank);
            assertEquals(BlockListMF.refined_planks, at(world, plank));
            assertEquals("one oil is paid", 1, oil(watcher));
            assertEquals("the client was not told of the oil paid", 1, shownIn(watcher, 1));
        }
        helper.succeed();
    }

    @GameTest
    public static void refusedPaintingKeepsTheBlockAndTheOil(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.nailed_planks);
        TestPos plank = helper.absolute(1, 1, 1);
        World world = helper.getWorld();
        DenyPlacing deny = new DenyPlacing();
        try (Overrides overrides = new Overrides().listen(deny); Watcher watcher = painter(helper, plank)) {
            use(watcher, world, plank);
            assertEquals("protection was asked", BlockListMF.refined_planks, deny.placed);
            assertEquals(BlockListMF.nailed_planks, at(world, plank));
            assertEquals("no oil is paid", 2, oil(watcher));
            assertEquals("the brush did not wear", 0, watcher.player.getHeldItem().getItemDamage());
            assertEquals("the client is shown it back", BlockListMF.nailed_planks, shown(watcher, plank));
        }
        helper.succeed();
    }

    @GameTest
    public static void aMattockRoadIsShownAndRefusedWithoutWear(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.grass);
        helper.setBlock(3, 1, 1, Blocks.grass);
        TestPos road = helper.absolute(1, 1, 1);
        TestPos kept = helper.absolute(3, 1, 1);
        World world = helper.getWorld();
        try (Watcher watcher = holding(helper, road, CustomToolListMF.standard_mattock.construct("Iron", "OakWood"))) {
            assertTrue(use(watcher, world, road));
            assertEquals(BlockListMF.road, at(world, road));
            assertEquals("the client sees the road", BlockListMF.road, shown(watcher, road));

            int wear = watcher.player.getHeldItem().getItemDamage();
            DenyPlacing deny = new DenyPlacing();
            try (Overrides overrides = new Overrides().listen(deny)) {
                assertFalse(use(watcher, world, kept));
            }
            assertEquals(Blocks.grass, at(world, kept));
            assertEquals(
                    "the refused road did not wear the mattock",
                    wear,
                    watcher.player.getHeldItem().getItemDamage());
            Block shown = shown(watcher, kept);
            assertTrue("the client sees grass", shown == null || shown == Blocks.grass);
        }
        helper.succeed();
    }

    @GameTest
    public static void aRefusedLighterStartsNoFire(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.stone);
        TestPos stone = helper.absolute(1, 1, 1);
        TestPos above = helper.absolute(1, 2, 1);
        World world = helper.getWorld();
        DenyPlacing deny = new DenyPlacing();
        try (Overrides overrides = new Overrides().listen(deny);
                Watcher watcher = holding(helper, stone, new ItemStack(ToolListMF.tinderbox))) {
            for (int strike = 0; strike < 20; strike++) use(watcher, world, stone);
            assertEquals("protection was asked", Blocks.fire, deny.placed);
            assertEquals(Blocks.air, at(world, above));
            Block shown = shown(watcher, above);
            assertTrue("the client sees no fire", shown == null || shown == Blocks.air);
        }
        helper.succeed();
    }

    @GameTest
    public static void aFailedHoeLeavesTheGrassForTheClientToo(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.grass);
        helper.setBlock(3, 1, 1, Blocks.grass);
        TestPos failed = helper.absolute(1, 1, 1);
        TestPos tilled = helper.absolute(3, 1, 1);
        World world = helper.getWorld();
        try (Overrides overrides = new Overrides();
                Watcher watcher = holding(helper, failed, new ItemStack(Items.iron_hoe))) {
            overrides.set(() -> FarmingHelper.isEnabled, v -> FarmingHelper.isEnabled = v, true)
                    .set(() -> FarmingHelper.hoeFailChanceCfg, v -> FarmingHelper.hoeFailChanceCfg = v, 1000F);
            assertFalse("the hoe fails", use(watcher, world, failed));
            assertEquals(Blocks.grass, at(world, failed));
            assertEquals("a failed hoe does not wear", 0, watcher.player.getHeldItem().getItemDamage());
            Block shown = shown(watcher, failed);
            assertTrue("the client sees grass", shown == null || shown == Blocks.grass);

            FarmingHelper.isEnabled = false;
            assertTrue("the hoe tills", use(watcher, world, tilled));
            assertEquals(Blocks.farmland, at(world, tilled));
            assertEquals("the client sees farmland", Blocks.farmland, shown(watcher, tilled));
        }
        helper.succeed();
    }
}
