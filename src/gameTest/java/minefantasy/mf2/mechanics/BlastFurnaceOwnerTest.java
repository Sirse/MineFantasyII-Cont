package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.mechanics.ProtectionFixtures.*;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.refining.BlockBFH;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;

/**
 * The blast furnace acts for the player who built it, known by UUID, online or through a stand-in; with no owner known
 * it changes nothing around it.
 */
@GameTestHolder("minefantasy2")
public class BlastFurnaceOwnerTest {

    private BlastFurnaceOwnerTest() {}

    @GameTest
    public static void aFurnaceSetsFireAsItsOwnerMayPlace(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.blast_heater);
        TestPos at = helper.absolute(1, 1, 1);
        TileEntityBlastFH heater = (TileEntityBlastFH) helper.getWorld().getTileEntity(at.x(), at.y(), at.z());
        assertNull("no owner known", heater.owner());
        assertFalse("no owner, no fire", heater.setFire(at.x() + 1, at.y(), at.z()));

        EntityPlayerMP builder = unconnected(helper);
        heater.setOwner(builder);
        NBTTagCompound saved = new NBTTagCompound();
        heater.writeToNBT(saved);
        heater.readFromNBT(saved);
        assertNotNull("an absent owner acts through a stand-in", heater.owner());
        assertEquals("the stand-in is the owner", builder.getUniqueID(), heater.owner().getUniqueID());
        assertSame("stand-in belongs to the furnace world", helper.getWorld(), heater.owner().worldObj);
        NBTTagCompound corruptOwner = (NBTTagCompound) saved.copy();
        corruptOwner.setString("OwnerId", "not-a-uuid");
        heater.readFromNBT(corruptOwner);
        assertNull("a corrupt UUID does not fall back to a name", heater.owner());
        NBTTagCompound legacyOwner = (NBTTagCompound) saved.copy();
        legacyOwner.removeTag("OwnerId");
        heater.readFromNBT(legacyOwner);
        assertNull("legacy offline owners are not guessed by name", heater.owner());
        heater.readFromNBT(saved);
        helper.setBlock(3, 1, 1, BlockListMF.blast_heater);
        TestPos secondAt = helper.absolute(3, 1, 1);
        TileEntityBlastFH secondHeater = (TileEntityBlastFH) helper.getWorld()
                .getTileEntity(secondAt.x(), secondAt.y(), secondAt.z());
        secondHeater.setOwner(builder);
        NBTTagCompound secondSaved = new NBTTagCompound();
        secondHeater.writeToNBT(secondSaved);
        secondHeater.readFromNBT(secondSaved);
        assertSame("same-world owner proxies are shared by UUID", heater.owner(), secondHeater.owner());
        assertSame("second stand-in belongs to its furnace world", helper.getWorld(), secondHeater.owner().worldObj);
        // Fire needs something to stand on, as beside a furnace built on the ground
        helper.getWorld().setBlock(at.x() + 1, at.y() - 1, at.z(), Blocks.stone);
        assertTrue(heater.setFire(at.x() + 1, at.y(), at.z()));
        assertEquals(
                "allowed furnace fire commits",
                Blocks.fire,
                helper.getWorld().getBlock(at.x() + 1, at.y(), at.z()));

        DenyPlacing deny = new DenyPlacing();
        MinecraftForge.EVENT_BUS.register(deny);
        try {
            helper.getWorld().setBlock(at.x(), at.y() - 1, at.z() + 1, Blocks.stone);
            assertFalse("not where the owner may not place", heater.setFire(at.x(), at.y(), at.z() + 1));
            assertEquals("the place event was asked", Blocks.fire, deny.placed);
            assertEquals(
                    "cancelled furnace fire restores air",
                    Blocks.air,
                    helper.getWorld().getBlock(at.x(), at.y(), at.z() + 1));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        endOfTick(helper);
        helper.succeed();
    }

    @GameTest
    public static void anOwnerIsKnownByUuidNotByName(GameTestHelper helper) {
        WorldServer world = (WorldServer) helper.getWorld();
        EntityPlayerMP first = named(helper, "mf2_same_name");
        EntityPlayerMP second = named(helper, "mf2_same_name");
        ActionOwner firstOwner = new ActionOwner();
        firstOwner.set(first);
        ActionOwner secondOwner = new ActionOwner();
        secondOwner.set(second);
        assertEquals(first.getUniqueID(), firstOwner.resolve(world).getUniqueID());
        assertEquals(second.getUniqueID(), secondOwner.resolve(world).getUniqueID());
        assertTrue("one name, two players, two stand-ins", firstOwner.resolve(world) != secondOwner.resolve(world));

        // Online, the owner acts as themselves; the other one stays a stand-in
        world.playerEntities.add(first);
        try {
            assertSame("an online owner acts as themselves", first, firstOwner.resolve(world));
            assertTrue("the namesake is not taken for them", secondOwner.resolve(world) instanceof FakePlayer);
        } finally {
            world.playerEntities.remove(first);
        }

        // A stand-in gets no rights of its own: protection still says no
        DenyAll deny = new DenyAll();
        MinecraftForge.EVENT_BUS.register(deny);
        try {
            helper.setBlock(1, 1, 1, Blocks.stone);
            TestPos at = helper.absolute(1, 1, 1);
            assertFalse(ProtectionHelper.canBreak(firstOwner.resolve(world), world, at.x(), at.y(), at.z()));
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        assertFalse("a stand-in is no operator", firstOwner.resolve(world).canCommandSenderUseCommand(2, "gamemode"));
        helper.succeed();
    }

    @GameTest
    public static void anOwnersStandInBelongsToEachWorldAndGoesWithIt(GameTestHelper helper) {
        WorldServer here = (WorldServer) helper.getWorld();
        WorldServer nether = MinecraftServer.getServer().worldServerForDimension(-1);
        assertNotSame(here, nether);
        ActionOwner owner = new ActionOwner();
        owner.set(named(helper, "mf2_two_worlds"));
        EntityPlayer inHere = owner.resolve(here);
        EntityPlayer inNether = owner.resolve(nether);
        assertSame(here, inHere.worldObj);
        assertSame(nether, inNether.worldObj);
        assertSame("one stand-in per world", inHere, owner.resolve(here));
        ActionOwner.unload(nether);
        assertTrue("an unloaded world's stand-in is dropped", inNether != owner.resolve(nether));
        assertSame("other worlds keep theirs", inHere, owner.resolve(here));
        ActionOwner.unload(nether);
        helper.succeed();
    }

    @GameTest
    public static void aHeaterKeepsItsOwnerWhenItLightsUp(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.blast_heater);
        TestPos at = helper.absolute(1, 1, 1);
        TileEntityBlastFH heater = (TileEntityBlastFH) helper.getWorld().getTileEntity(at.x(), at.y(), at.z());
        EntityPlayerMP builder = unconnected(helper);
        heater.setOwner(builder);
        BlockBFH.updateFurnaceBlockState(true, helper.getWorld(), at.x(), at.y(), at.z());
        TileEntityBlastFH lit = (TileEntityBlastFH) helper.getWorld().getTileEntity(at.x(), at.y(), at.z());
        assertEquals(BlockListMF.blast_heater_active, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        assertEquals("the owner stays", builder.getUniqueID(), lit.owner().getUniqueID());
        helper.succeed();
    }

    /** A heater with no owner known, standing on the floor in the middle of it. */
    private static TileEntityBlastFH ownerlessHeater(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(2, 2, 2, BlockListMF.blast_heater);
        TestPos at = helper.absolute(2, 2, 2);
        return (TileEntityBlastFH) helper.getWorld().getTileEntity(at.x(), at.y(), at.z());
    }

    @GameTest
    public static void aChokedFurnaceWithNoOwnerBreaksAndBurnsNothing(GameTestHelper helper) {
        TileEntityBlastFH heater = ownerlessHeater(helper);
        assertNull(heater.owner());
        for (int i = 0; i < 3; i++) {
            heater.explodeFromSmoke();
        }
        assertEquals("the floor stands", 25, count(helper, Blocks.stone));
        assertEquals("nothing burns", 0, count(helper, Blocks.fire));
        helper.succeed();
    }

    @GameTest
    public static void aChokedFurnaceBlastsAsItsOwnerMay(GameTestHelper helper) {
        TileEntityBlastFH heater = ownerlessHeater(helper);
        heater.setOwner(unconnected(helper));
        heater.explodeFromSmoke();
        assertTrue("with an owner allowed, the blast breaks the floor", count(helper, Blocks.stone) < 25);
        helper.succeed();
    }
}
