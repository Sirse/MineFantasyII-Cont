package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.helpers.PlayerModeHelper;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.gametest.TestItems;
import minefantasy.mf2.hunger.HungerSystemMF;
import minefantasy.mf2.network.packet.PacketHandlerMF;
import minefantasy.mf2.network.packet.PacketMF;
import minefantasy.mf2.network.packet.RackCommand;

/**
 * Spectator mode, as Et Futurum Requiem adds it: an observer who may open inventories to look into, but no MineFantasy
 * action answers and no effect lands on the body. Et Futurum is absent from these runs, so the gametype treated as
 * spectator is forced for every test and cleared afterwards.
 */
@GameTestHolder("minefantasy2")
public class SpectatorModeTest {

    /** Stands in for Et Futurum's spectator gametype: any constant not otherwise in play. */
    private static final WorldSettings.GameType FAKE_SPECTATOR = WorldSettings.GameType.ADVENTURE;

    private SpectatorModeTest() {}

    private static EntityPlayerMP spectator(GameTestHelper helper) {
        EntityPlayerMP player = ProtectionFixtures.unconnected(helper);
        player.theItemInWorldManager.setGameType(FAKE_SPECTATOR);
        return player;
    }

    private static EntityPlayerMP survival(GameTestHelper helper) {
        EntityPlayerMP player = ProtectionFixtures.unconnected(helper);
        player.theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
        return player;
    }

    @GameTest
    public static void aSpectatorIsKnownByTheirGametype(GameTestHelper helper) {
        EntityPlayerMP adventurer = survival(helper);
        adventurer.theItemInWorldManager.setGameType(WorldSettings.GameType.ADVENTURE);
        assertFalse(
                "a spectator was recognised without Et Futurum or forcing",
                PlayerModeHelper.isSpectator(adventurer));

        try (Overrides overrides = new Overrides().run(
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(FAKE_SPECTATOR),
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(null))) {
            assertTrue("the spectator was not recognised", PlayerModeHelper.isSpectator(spectator(helper)));
            assertFalse("a survival player read as spectator", PlayerModeHelper.isSpectator(survival(helper)));
            FakePlayer fake = Modders.fresh(helper, "Spectator");
            fake.theItemInWorldManager.setGameType(FAKE_SPECTATOR);
            assertFalse("a fake player read as spectator", PlayerModeHelper.isSpectator(fake));
        }
        helper.succeed();
    }

    /** Every serverbound packet is an action request, and the gate drops them all for a spectator. */
    @GameTest
    public static void noServerboundPacketAnswersASpectator(GameTestHelper helper) {
        PacketHandlerMF handler = new PacketHandlerMF();
        RecordingPacket packet = new RecordingPacket();

        try (Overrides overrides = new Overrides().run(
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(FAKE_SPECTATOR),
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(null))) {
            handler.dispatchServerbound(packet, Unpooled.EMPTY_BUFFER, spectator(helper));
            assertTrue("a spectator's request was processed", packet.processed == 0);
            handler.dispatchServerbound(packet, Unpooled.EMPTY_BUFFER, survival(helper));
            assertTrue("a survival player's request was dropped", packet.processed == 1);
        }
        helper.succeed();
    }

    /** The real rack path: a spectator's rack command is dropped, the same command works for a player. */
    @GameTest
    public static void aSpectatorCannotWorkARackButAPlayerCan(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.rack_wood);
        TileEntityRack rack = helper.assertTileEntityPresent(TileEntityRack.class, 1, 1, 1);
        rack.setInventorySlotContents(0, new ItemStack(Items.iron_sword));
        TestPos at = helper.absolute(1, 1, 1);

        try (Overrides overrides = new Overrides().run(
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(FAKE_SPECTATOR),
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(null))) {
            EntityPlayerMP watcher = spectator(helper);
            watcher.setPosition(at.x() + 0.5D, at.y() + 1.0D, at.z() + 0.5D);
            sendRackCommand(rack, 0, watcher);
            assertTrue(
                    "the rack gave its item to a spectator",
                    rack.getStackInSlot(0) != null && rack.getStackInSlot(0).getItem() == Items.iron_sword);
            assertNull("a spectator picked up the item", watcher.getHeldItem());
        }

        EntityPlayerMP worker = survival(helper);
        worker.setPosition(at.x() + 0.5D, at.y() + 1.0D, at.z() + 0.5D);
        worker.getEntityData().setLong("MF2_LastRackCmd", worker.worldObj.getTotalWorldTime() - 1000L);
        sendRackCommand(rack, 0, worker);
        assertNull("the rack kept its item for the player", rack.getStackInSlot(0));
        assertTrue(
                "the player did not take the item",
                worker.getHeldItem() != null && worker.getHeldItem().getItem() == Items.iron_sword);
        helper.succeed();
    }

    private static void sendRackCommand(TileEntityRack rack, int slot, EntityPlayerMP player) {
        ByteBuf payload = Unpooled.buffer();
        payload.writeInt(rack.xCoord);
        payload.writeInt(rack.yCoord);
        payload.writeInt(rack.zCoord);
        payload.writeInt(slot);
        new PacketHandlerMF().dispatchServerbound(new RackCommand(), payload, player);
    }

    /** Hot items in the inventory burn a smith, but neither a spectator nor a creative player. */
    @GameTest
    public static void hotItemsBurnASmithButNotASpectator(GameTestHelper helper) {
        try (Overrides overrides = new Overrides().run(
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(FAKE_SPECTATOR),
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(null))) {
            EntityPlayerMP watcher = spectator(helper);
            watcher.inventory.mainInventory[0] = hotStack();
            assertFalse("a spectator burned on hot items", PlayerTickHandlerMF.burnWithHotItems(watcher));
            assertTrue("a spectator caught fire", fire(watcher) == 0);

            EntityPlayerMP creator = survival(helper);
            creator.capabilities.isCreativeMode = true;
            creator.inventory.mainInventory[0] = hotStack();
            assertFalse("a creative player burned on hot items", PlayerTickHandlerMF.burnWithHotItems(creator));
            assertTrue("a creative player caught fire", fire(creator) == 0);

            EntityPlayerMP worker = survival(helper);
            worker.inventory.mainInventory[0] = hotStack();
            assertTrue("a survival smith did not burn", PlayerTickHandlerMF.burnWithHotItems(worker));
            assertTrue("no burning was left on the smith", fire(worker) > 0);
        }
        helper.succeed();
    }

    @GameTest
    public static void aSpectatorKeepsTheirStaminaAndHunger(GameTestHelper helper) {
        try (Overrides overrides = new Overrides().run(
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(FAKE_SPECTATOR),
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(null))) {
            assertTrue("stamina did not affect a survival player", StaminaBar.doesAffectEntity(survival(helper)));
            assertFalse("stamina affected a spectator", StaminaBar.doesAffectEntity(spectator(helper)));

            EntityPlayerMP worker = survival(helper);
            feedTo(worker, 16);
            HungerSystemMF.setSaturation(worker, 3F);
            tickHunger(helper, worker, TickEvent.Phase.START);
            worker.getFoodStats().addStats(-1, 0F);
            tickHunger(helper, worker, TickEvent.Phase.END);
            assertTrue("hunger was not slowed for a survival player", worker.getFoodStats().getFoodLevel() == 16);
            assertEquals(
                    "saturation was not spent for a survival player",
                    2F,
                    HungerSystemMF.getSaturation(worker),
                    0F);

            EntityPlayerMP watcher = spectator(helper);
            feedTo(watcher, 16);
            HungerSystemMF.setSaturation(watcher, 3F);
            tickHunger(helper, watcher, TickEvent.Phase.START);
            watcher.getFoodStats().addStats(-1, 0F);
            tickHunger(helper, watcher, TickEvent.Phase.END);
            assertTrue("hunger changed for a spectator", watcher.getFoodStats().getFoodLevel() == 15);
            assertEquals("saturation was spent for a spectator", 3F, HungerSystemMF.getSaturation(watcher), 0F);
        }
        helper.succeed();
    }

    private static void tickHunger(GameTestHelper helper, EntityPlayer player, TickEvent.Phase phase) {
        new HungerSystemMF().onPlayerTick(new TickEvent.PlayerTickEvent(phase, player));
    }

    /** Sets the vanilla food level, as 1.7.10 has no setter: empties it, then adds the share asked for. */
    private static void feedTo(EntityPlayer player, int food) {
        player.getFoodStats().addStats(-20, 0F);
        player.getFoodStats().addStats(food, 0F);
    }

    @GameTest
    public static void aSpectatorIsNotScaldedByAFlare(GameTestHelper helper) {
        try (Overrides overrides = new Overrides().run(
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(FAKE_SPECTATOR),
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(null))) {
            EntityPlayerMP watcher = spectator(helper);
            QuenchFlare.flare(helper.getWorld(), 0, 0, 0, watcher);
            assertFalse("a spectator caught fire from a flare", watcher.isBurning());
            assertEquals("a spectator was scalded", watcher.getMaxHealth(), watcher.getHealth(), 0F);
        }
        helper.succeed();
    }

    /** A trough answers no spectator: neither its salt nor its water moves, and the held bucket stays whole. */
    @GameTest
    public static void aSpectatorCannotUseATrough(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.trough_wood);
        TileEntityTrough trough = helper.assertTileEntityPresent(TileEntityTrough.class, 1, 1, 1);
        trough.fill(ForgeDirection.UNKNOWN, new FluidStack(FluidRegistry.WATER, 1000), true);

        try (Overrides overrides = new Overrides().run(
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(FAKE_SPECTATOR),
                () -> PlayerModeHelper.setSpectatorGameTypeForTest(null))) {
            EntityPlayerMP watcher = spectator(helper);
            ItemStack bucket = new ItemStack(Items.bucket);
            assertFalse("the trough answered a spectator", trough.interact(watcher, bucket));
            assertTrue(
                    "the bucket was taken from the spectator",
                    bucket.getItem() == Items.bucket && bucket.stackSize == 1);
        }
        helper.succeed();
    }

    /** A piece hot enough to burn: above 100 in the heated item's own temperature tag. */
    private static ItemStack hotStack() {
        ItemStack hot = new ItemStack(TestItems.hot);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(Heatable.NBT_CurrentTemp, 500);
        hot.setTagCompound(tag);
        return hot;
    }

    /** The ticks the entity still burns. */
    private static int fire(EntityPlayer player) {
        return ReflectionHelper.getPrivateValue(Entity.class, player, "fire", "field_70151_c");
    }

    /** Counts the requests that got through the gate. */
    private static final class RecordingPacket extends PacketMF {

        int processed;

        @Override
        public void process(ByteBuf packet, EntityPlayer player) {
            processed++;
        }

        @Override
        public String getChannel() {
            return "MF2_Test_Channel";
        }

        @Override
        public void write(ByteBuf packet) {}
    }
}
