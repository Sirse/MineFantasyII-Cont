package minefantasy.mf2.network.packet;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.Map;
import java.util.Random;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;
import minefantasy.mf2.entity.EntityCogwork;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * Packets as a modified client would send them: truncated, garbage, or naming things the player cannot see or reach.
 * Each refusal is paired with the honest packet doing its job, so a handler that does nothing at all would not pass.
 */
@GameTestHolder("minefantasy2")
public class PacketAbuseTest {

    private PacketAbuseTest() {}

    private static FakePlayer player(GameTestHelper helper, int x, int y, int z) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        later(player);
        player.inventory.clearInventory(null, -1);
        TestPos at = helper.absolute(x, y, z);
        player.setPositionAndRotation(at.x() + 0.5, at.y(), at.z() + 0.5, 0F, 0F);
        return player;
    }

    private static ByteBuf ints(int... values) {
        ByteBuf buf = Unpooled.buffer();
        for (int value : values) buf.writeInt(value);
        return buf;
    }

    /** Rate limit keys of the serverbound handlers. */
    private static final String[] COOLDOWNS = { "MF2_LastRackCmd", "MF2_LastCogworkCtrl", "MF2_LastExtReachAtk",
            "MF2_LastDodgeCmd", "MF2_LastResearchReq" };

    /**
     * As if the player's last packets were long ago. Set rather than removed: a test world may not have ticked yet, and
     * a missing key reads as tick 0, which is then still within the cooldown.
     */
    private static void later(FakePlayer player) {
        for (String key : COOLDOWNS) {
            player.getEntityData().setLong(key, player.worldObj.getTotalWorldTime() - 1000);
        }
    }

    // region garbage

    @GameTest
    public static void everyHandlerSurvivesGarbageFromAClient(GameTestHelper helper) {
        FakePlayer player = player(helper, 1, 1, 1);
        Random random = new Random(42);
        for (Map.Entry<String, PacketMF> entry : MineFantasyII.packetHandler.packetList.entrySet()) {
            ByteBuf[] payloads = { Unpooled.buffer(), Unpooled.wrappedBuffer(new byte[] { 1, 2, 3 }),
                    ints(Integer.MIN_VALUE, -1, Integer.MAX_VALUE, 255, 4, 0, 0, 0),
                    ints(-1, -1, -1, -1, -1, -1, -1, -1) };
            byte[] noise = new byte[64];
            for (int round = 0; round < 8; round++) {
                random.nextBytes(noise);
                // Each payload on its own: throttled, it would stop before the checks it is meant to reach
                later(player);
                check(entry.getKey(), entry.getValue(), Unpooled.wrappedBuffer(noise.clone()), player);
            }
            for (ByteBuf payload : payloads) {
                later(player);
                check(entry.getKey(), entry.getValue(), payload, player);
            }
        }
        helper.succeed();
    }

    private static void check(String channel, PacketMF handler, ByteBuf payload, FakePlayer player) {
        try {
            handler.process(payload, player);
        } catch (Throwable t) {
            fail(channel + " threw " + t + " on a malformed payload");
        }
    }

    // endregion

    // region rack

    @GameTest
    public static void aRackCommandActsOnlyOnARackWithinReach(GameTestHelper helper) {
        helper.setBlock(1, 1, 2, BlockListMF.rack_wood);
        helper.setBlock(3, 1, 2, Blocks.chest);
        TileEntityRack rack = helper.assertTileEntityPresent(TileEntityRack.class, 1, 1, 2);
        rack.setInventorySlotContents(0, new ItemStack(Items.iron_sword));
        TestPos at = helper.absolute(1, 1, 2);
        TestPos chest = helper.absolute(3, 1, 2);
        RackCommand handler = new RackCommand();
        FakePlayer player = player(helper, 1, 1, 1);

        handler.process(ints(at.x(), at.y(), at.z(), 4), player);
        later(player);
        handler.process(ints(at.x(), at.y(), at.z(), -1), player);
        later(player);
        handler.process(ints(chest.x(), chest.y(), chest.z(), 0), player);
        later(player);
        int farX = at.x() + 1_000_000, farZ = at.z() + 1_000_000;
        handler.process(ints(farX, at.y(), farZ, 0), player);
        later(player);
        assertFalse(
                "a packet loaded a far chunk",
                helper.getWorld().getChunkProvider().chunkExists(farX >> 4, farZ >> 4));
        assertNull("a refused packet took the sword", player.getHeldItem());

        // From across the room it is refused, from beside the rack it works
        player.setPosition(at.x() + 0.5, at.y(), at.z() + 20.5);
        handler.process(ints(at.x(), at.y(), at.z(), 0), player);
        later(player);
        assertNull("the sword was taken from 20 blocks away", player.getHeldItem());
        player.setPosition(at.x() + 0.5, at.y(), at.z() - 0.5);
        handler.process(ints(at.x(), at.y(), at.z(), 0), player);
        assertNotNull("the honest packet did nothing", player.getHeldItem());

        // A second packet in the same tick is throttled: the sword is not hung back
        handler.process(ints(at.x(), at.y(), at.z(), 0), player);
        assertNotNull("a packet in the same tick was not throttled", player.getHeldItem());
        assertNull(rack.getStackInSlot(0));
        helper.succeed();
    }

    // endregion

    // region extended reach

    private static EntityZombie zombie(GameTestHelper helper, int x, int y, int z) {
        EntityZombie zombie = new EntityZombie(helper.getWorld());
        TestPos at = helper.absolute(x, y, z);
        zombie.setPosition(at.x() + 0.5, at.y(), at.z() + 0.5);
        helper.getWorld().spawnEntityInWorld(zombie);
        return zombie;
    }

    @GameTest
    public static void aReachAttackLandsOnlyWithinTheWeaponsReachAndSight(GameTestHelper helper) {
        ExtendedReachPacket handler = new ExtendedReachPacket();
        FakePlayer player = player(helper, 1, 1, 1);
        player.setCurrentItemOrArmor(0, new ItemStack(ToolListMF.spearStone));

        EntityZombie far = zombie(helper, 1, 1, 11);
        handler.process(ints(far.getEntityId()), player);
        later(player);
        assertEquals("a zombie 10 blocks away was hit", far.getMaxHealth(), far.getHealth(), 0F);

        helper.setBlock(1, 1, 3, Blocks.stone);
        helper.setBlock(1, 2, 3, Blocks.stone);
        EntityZombie walled = zombie(helper, 1, 1, 5);
        handler.process(ints(walled.getEntityId()), player);
        later(player);
        assertEquals("a zombie behind a wall was hit", walled.getMaxHealth(), walled.getHealth(), 0F);
        helper.setBlock(1, 1, 3, Blocks.air);
        helper.setBlock(1, 2, 3, Blocks.air);

        player.setCurrentItemOrArmor(0, new ItemStack(Items.stick));
        handler.process(ints(walled.getEntityId()), player);
        later(player);
        assertEquals("a stick reached like a spear", walled.getMaxHealth(), walled.getHealth(), 0F);

        player.setCurrentItemOrArmor(0, new ItemStack(ToolListMF.spearStone));
        handler.process(ints(walled.getEntityId()), player);
        assertTrue("the honest reach attack missed", walled.getHealth() < walled.getMaxHealth());
        helper.succeed();
    }

    // endregion

    // region cogwork

    private static ByteBuf control(EntityCogwork suit, float forward) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(suit.getEntityId());
        buf.writeFloat(forward);
        buf.writeFloat(0F);
        buf.writeBoolean(false);
        return buf;
    }

    @GameTest
    public static void onlyTheRiderSteersACogwork(GameTestHelper helper) {
        CogworkControlPacket handler = new CogworkControlPacket();
        TestPos at = helper.absolute(3, 1, 3);
        EntityCogwork suit = new EntityCogwork(helper.getWorld(), at.x() + 0.5, at.y(), at.z() + 0.5);
        helper.getWorld().spawnEntityInWorld(suit);
        FakePlayer player = player(helper, 3, 1, 1);

        handler.process(control(suit, 1F), player);
        later(player);
        assertEquals("a bystander steered the suit", 0F, suit.getMoveForward(), 0F);

        // mountEntity needs a network connection a fake player does not have
        player.ridingEntity = suit;
        suit.riddenByEntity = player;
        handler.process(control(suit, Float.NaN), player);
        later(player);
        assertEquals("NaN reached the suit", 0F, suit.getMoveForward(), 0F);
        handler.process(control(suit, 50F), player);
        later(player);
        assertEquals("the rider's input was not clamped", 1F, suit.getMoveForward(), 0F);
        helper.succeed();
    }

    // endregion
}
