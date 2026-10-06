package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.mechanics.ProtectionFixtures.*;

import java.util.UUID;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.api.crafting.transformation.TransformationRecipes;
import minefantasy.mf2.gametest.Modders;

/**
 * Protection is asked for each block change a player makes: breaking, placing, replacing and interacting, through Forge
 * events, and refused changes cost and give nothing.
 */
@GameTestHolder("minefantasy2")
public class BlockPermissionTest {

    private BlockPermissionTest() {}

    @GameTest
    public static void aPlayerWithNoConnectionIsStillAskedAbout(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.stone);
        TestPos at = helper.absolute(1, 1, 1);
        EntityPlayerMP player = unconnected(helper);
        assertTrue(
                "allowed where nothing forbids it",
                ProtectionHelper.canBreak(player, helper.getWorld(), at.x(), at.y(), at.z()));
        DenyAll deny = new DenyAll();
        MinecraftForge.EVENT_BUS.register(deny);
        try {
            assertFalse(
                    "no connection is no permission",
                    ProtectionHelper.canBreak(player, helper.getWorld(), at.x(), at.y(), at.z()));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        endOfTick(helper);
        helper.succeed();
    }

    /** Counts the events one protection question raises, and denies interaction when told to. */
    public static final class CountEvents {

        public int breaks;
        public int interacts;
        public boolean cancelInteract;
        public boolean denyBlock;

        @SubscribeEvent
        public void broke(BlockEvent.BreakEvent event) {
            breaks++;
        }

        @SubscribeEvent
        public void interacted(PlayerInteractEvent event) {
            interacts++;
            if (cancelInteract) event.setCanceled(true);
            if (denyBlock) event.useBlock = Event.Result.DENY;
        }
    }

    @GameTest
    public static void eachQuestionRaisesOneEventAndHonoursItsAnswer(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.stone);
        TestPos at = helper.absolute(1, 1, 1);
        EntityPlayerMP player = unconnected(helper);
        CountEvents count = new CountEvents();
        MinecraftForge.EVENT_BUS.register(count);
        try {
            assertTrue(ProtectionHelper.canBreak(player, helper.getWorld(), at.x(), at.y(), at.z()));
            assertEquals("one break event per question", 1, count.breaks);
            assertTrue(ProtectionHelper.canInteract(player, helper.getWorld(), at.x(), at.y(), at.z()));
            assertEquals("one interact event per question", 1, count.interacts);
            count.cancelInteract = true;
            assertFalse(
                    "a cancelled interaction",
                    ProtectionHelper.canInteract(player, helper.getWorld(), at.x(), at.y(), at.z()));
            count.cancelInteract = false;
            count.denyBlock = true;
            assertFalse(
                    "useBlock denied",
                    ProtectionHelper.canInteract(player, helper.getWorld(), at.x(), at.y(), at.z()));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(count);
        }
        endOfTick(helper);
        helper.succeed();
    }

    @GameTest
    public static void aPlayerNotOnTheServerSideIsAllowedNothing(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.stone);
        TestPos at = helper.absolute(1, 1, 1);
        EntityPlayer player = new EntityPlayer(helper.getWorld(), new GameProfile(UUID.randomUUID(), "mf2_plain")) {

            @Override
            public void addChatMessage(IChatComponent message) {}

            @Override
            public boolean canCommandSenderUseCommand(int level, String command) {
                return false;
            }

            @Override
            public ChunkCoordinates getPlayerCoordinates() {
                return new ChunkCoordinates(0, 0, 0);
            }
        };
        assertFalse(ProtectionHelper.canBreak(player, helper.getWorld(), at.x(), at.y(), at.z()));
        assertFalse(ProtectionHelper.breakBlock(player, helper.getWorld(), at.x(), at.y(), at.z()));
        assertEquals("the stone stays", Blocks.stone, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        endOfTick(helper);
        helper.succeed();
    }

    public static final class ReenterPlacement {

        public boolean attempted;
        public boolean nestedResult;

        @SubscribeEvent
        public void place(BlockEvent.PlaceEvent event) {
            if (attempted) return;
            attempted = true;
            nestedResult = ProtectionHelper
                    .placeBlock(event.player, event.world, event.x, event.y, event.z, Blocks.tnt, 0);
        }
    }

    @GameTest
    public static void placingIsAskedApartFromBreaking(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.stone);
        TestPos ground = helper.absolute(1, 1, 1);
        TestPos at = helper.absolute(1, 2, 1);
        EntityPlayerMP player = unconnected(helper);
        assertTrue(ProtectionHelper.placeBlock(player, helper.getWorld(), at.x(), at.y(), at.z(), Blocks.fire, 0));
        assertEquals("allowed placement stands", Blocks.fire, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        helper.setBlock(1, 2, 1, Blocks.air);
        EntityItem nearbyDrop = new EntityItem(
                helper.getWorld(),
                at.x() + 0.25D,
                at.y() + 0.25D,
                at.z() + 0.25D,
                new ItemStack(Items.diamond));
        assertTrue("an item lies beside it", helper.getWorld().spawnEntityInWorld(nearbyDrop));
        DenyPlacing deny = new DenyPlacing();
        MinecraftForge.EVENT_BUS.register(deny);
        try {
            assertTrue(
                    "breaking is still allowed",
                    ProtectionHelper.canBreak(player, helper.getWorld(), ground.x(), ground.y(), ground.z()));
            assertFalse(ProtectionHelper.placeBlock(player, helper.getWorld(), at.x(), at.y(), at.z(), Blocks.fire, 0));
            assertEquals("event saw the intended block", Blocks.fire, deny.placed);
            assertEquals("the world showed it during the event", Blocks.fire, deny.worldBlockDuringEvent);
            assertEquals("a refused fire is put out", Blocks.air, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
            assertFalse("the item beside it stays", nearbyDrop.isDead);
            assertFalse(
                    "a replacement needs both",
                    ProtectionHelper.replaceBlock(
                            player,
                            helper.getWorld(),
                            ground.x(),
                            ground.y(),
                            ground.z(),
                            Blocks.dirt,
                            0));
            assertEquals(
                    "the stone stays",
                    Blocks.stone,
                    helper.getWorld().getBlock(ground.x(), ground.y(), ground.z()));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        endOfTick(helper);
        helper.succeed();
    }

    @GameTest
    public static void aBlockWithAnInventoryIsNeverReplaced(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.chest);
        TestPos at = helper.absolute(1, 1, 1);
        TileEntityChest chest = (TileEntityChest) helper.getWorld().getTileEntity(at.x(), at.y(), at.z());
        chest.setInventorySlotContents(0, new ItemStack(Items.diamond, 3));
        EntityPlayerMP player = unconnected(helper);
        assertFalse(ProtectionHelper.replaceBlock(player, helper.getWorld(), at.x(), at.y(), at.z(), Blocks.stone, 0));
        assertEquals(Blocks.chest, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        TileEntityChest after = (TileEntityChest) helper.getWorld().getTileEntity(at.x(), at.y(), at.z());
        assertEquals("the inventory stays", 3, after.getStackInSlot(0).stackSize);
        assertEquals("nothing dropped", 0, countItems(helper, at, Items.diamond));
        endOfTick(helper);
        helper.succeed();
    }

    @GameTest
    public static void placementEventCannotRepeatItsOwnCoordinate(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.stone);
        TestPos at = helper.absolute(1, 2, 1);
        EntityPlayerMP player = unconnected(helper);
        ReenterPlacement reenter = new ReenterPlacement();
        MinecraftForge.EVENT_BUS.register(reenter);
        try {
            assertTrue(
                    "outer placement commits",
                    ProtectionHelper.placeBlock(player, helper.getWorld(), at.x(), at.y(), at.z(), Blocks.fire, 0));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(reenter);
        }
        assertTrue("placement listener ran", reenter.attempted);
        assertFalse("a placement inside the event is refused", reenter.nestedResult);
        assertEquals(
                "only the requested outer result committed",
                Blocks.fire,
                helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        helper.succeed();
    }

    @GameTest
    public static void deniedTransformationPaysAndRewardsOnlyAfterAllowedCommit(GameTestHelper helper) {
        TransformationRecipes.addRecipe(
                Blocks.sponge,
                -1,
                Blocks.glowstone,
                0,
                false,
                "pickaxe",
                0,
                1,
                new ItemStack(Items.redstone),
                new ItemStack(Items.diamond),
                "dig.stone",
                null,
                0,
                null,
                1);
        helper.setBlock(1, 1, 1, Blocks.sponge);
        TestPos at = helper.absolute(1, 1, 1);
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        ItemStack pick = new ItemStack(Items.iron_pickaxe);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, pick);
        player.inventory.setInventorySlotContents(1, new ItemStack(Items.redstone, 1));
        TransformationHandler handler = new TransformationHandler();
        DenyPlacing deny = new DenyPlacing();
        MinecraftForge.EVENT_BUS.register(deny);
        try {
            handler.onPlayerInteract(
                    new PlayerInteractEvent(
                            player,
                            PlayerInteractEvent.Action.LEFT_CLICK_BLOCK,
                            at.x(),
                            at.y(),
                            at.z(),
                            1,
                            helper.getWorld()));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        assertEquals("denied result leaves source", Blocks.sponge, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        assertEquals("denied result does not consume payment", 1, player.inventory.getStackInSlot(1).stackSize);
        assertEquals("denied result does not damage the tool", 0, pick.getItemDamage());
        assertEquals("denied result gives no recipe output", 0, countItems(helper, at, Items.diamond));

        handler.onPlayerInteract(
                new PlayerInteractEvent(
                        player,
                        PlayerInteractEvent.Action.LEFT_CLICK_BLOCK,
                        at.x(),
                        at.y(),
                        at.z(),
                        1,
                        helper.getWorld()));
        assertEquals(
                "allowed result is committed once",
                Blocks.glowstone,
                helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        assertNull("allowed result consumes its payment", player.inventory.getStackInSlot(1));
        assertEquals("allowed result gives one recipe output", 1, countItems(helper, at, Items.diamond));
        assertEquals("allowed result damages tool once", 1, pick.getItemDamage());
        helper.succeed();
    }

    /** Swaps the held tool while the place event is decided, as another mod might. */
    public static final class SwapHeld {

        @SubscribeEvent
        public void swap(BlockEvent.PlaceEvent event) {
            event.player.inventory
                    .setInventorySlotContents(event.player.inventory.currentItem, new ItemStack(Items.iron_pickaxe));
        }
    }

    @GameTest
    public static void aToolSwappedDuringTheEventTransformsNothing(GameTestHelper helper) {
        TransformationRecipes.addRecipe(
                Blocks.melon_block,
                -1,
                Blocks.pumpkin,
                0,
                false,
                "pickaxe",
                0,
                1,
                new ItemStack(Items.redstone),
                null,
                "dig.stone",
                null,
                0,
                null,
                1);
        helper.setBlock(1, 1, 1, Blocks.melon_block);
        TestPos at = helper.absolute(1, 1, 1);
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        ItemStack pick = new ItemStack(Items.iron_pickaxe);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, pick);
        player.inventory.setInventorySlotContents(1, new ItemStack(Items.redstone, 1));
        SwapHeld swap = new SwapHeld();
        MinecraftForge.EVENT_BUS.register(swap);
        try {
            new TransformationHandler().onPlayerInteract(
                    new PlayerInteractEvent(
                            player,
                            PlayerInteractEvent.Action.LEFT_CLICK_BLOCK,
                            at.x(),
                            at.y(),
                            at.z(),
                            1,
                            helper.getWorld()));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(swap);
        }
        assertEquals("the melon stays", Blocks.melon_block, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        assertEquals("no payment taken", 1, player.inventory.getStackInSlot(1).stackSize);
        assertEquals("the first tool is not worn", 0, pick.getItemDamage());
        helper.succeed();
    }
}
