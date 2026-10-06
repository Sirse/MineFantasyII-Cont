package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.event.world.BlockEvent;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import minefantasy.mf2.entity.EntityFireBlast;
import minefantasy.mf2.item.list.ComponentListMF;

/** Players, places, counts and protection stand-ins shared by the protection tests. */
final class ProtectionFixtures {

    private ProtectionFixtures() {}

    /** A real server player, not a fake one, with no connection, as automation and tests have. */
    static EntityPlayerMP unconnected(GameTestHelper helper) {
        WorldServer world = (WorldServer) helper.getWorld();
        EntityPlayerMP player = new EntityPlayerMP(
                MinecraftServer.getServer(),
                world,
                new GameProfile(UUID.randomUUID(), "mf2_unconnected"),
                new ItemInWorldManager(world));
        player.inventory.clearInventory(null, -1);
        return player;
    }

    static int rocksAround(GameTestHelper helper, TestPos at) {
        List<?> items = helper.getWorld().getEntitiesWithinAABB(
                EntityItem.class,
                AxisAlignedBB.getBoundingBox(at.x() - 2, at.y() - 2, at.z() - 2, at.x() + 3, at.y() + 3, at.z() + 3));
        int rocks = 0;
        for (Object item : items) {
            if (((EntityItem) item).getEntityItem().getItem() == ComponentListMF.sharp_rock) {
                rocks += ((EntityItem) item).getEntityItem().stackSize;
            }
        }
        return rocks;
    }

    static void endOfTick(GameTestHelper helper) {
        FMLCommonHandler.instance().bus()
                .post(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, helper.getWorld()));
    }

    static void impact(EntityFireBlast blast, MovingObjectPosition hit) {
        try {
            Method method = EntityFireBlast.class.getDeclaredMethod("onImpact", MovingObjectPosition.class);
            method.setAccessible(true);
            method.invoke(blast, hit);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("could not run fire blast impact path", exception);
        }
    }

    /** Denies every break, as a region protection mod would inside its region. */
    public static final class DenyAll {

        @SubscribeEvent
        public void deny(BlockEvent.BreakEvent event) {
            event.setCanceled(true);
        }
    }

    /** Denies placing but not breaking, as some protection mods do. */
    public static final class DenyPlacing {

        public Block placed;
        public Block worldBlockDuringEvent;

        @SubscribeEvent
        public void deny(BlockEvent.PlaceEvent event) {
            placed = event.block;
            worldBlockDuringEvent = event.world.getBlock(event.x, event.y, event.z);
            event.setCanceled(true);
        }
    }

    static int countItems(GameTestHelper helper, TestPos at, Item item) {
        List<?> entities = helper.getWorld().getEntitiesWithinAABB(
                EntityItem.class,
                AxisAlignedBB.getBoundingBox(at.x() - 2, at.y() - 2, at.z() - 2, at.x() + 3, at.y() + 3, at.z() + 3));
        int count = 0;
        for (Object entity : entities) {
            ItemStack stack = ((EntityItem) entity).getEntityItem();
            if (stack.getItem() == item) count += stack.stackSize;
        }
        return count;
    }

    static EntityPlayerMP named(GameTestHelper helper, String name) {
        WorldServer world = (WorldServer) helper.getWorld();
        return new EntityPlayerMP(
                MinecraftServer.getServer(),
                world,
                new GameProfile(UUID.randomUUID(), name),
                new ItemInWorldManager(world));
    }

    /** A floor of stone with air above, for an explosion to break and set alight. */
    static void floor(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            helper.setBlock(x, 1, z, Blocks.stone);
            helper.setBlock(x, 2, z, Blocks.air);
        }
    }

    static int count(GameTestHelper helper, Block block) {
        int found = 0;
        for (int x = 0; x < 5; x++) for (int y = 1; y < 4; y++) for (int z = 0; z < 5; z++) {
            TestPos at = helper.absolute(x, y, z);
            if (helper.getWorld().getBlock(at.x(), at.y(), at.z()) == block) found++;
        }
        return found;
    }

    /** A connected survival player beside the spot, to break blocks through vanilla's own path. */
    static EntityPlayerMP miner(GameTestHelper helper, TestPos at) {
        EntityPlayerMP player = unconnected(helper);
        player.setPosition(at.x() + 0.5D, at.y() + 0.5D, at.z() + 3.5D);
        new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), player);
        player.theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
        return player;
    }
}
