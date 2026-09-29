package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import java.lang.reflect.Field;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityAmmoBox;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.network.packet.StationStatePacket;

/**
 * What the blocks without a window show players: the state a block describes, applied to a fresh client copy, gives the
 * copy what the block has. Moments, such as a press starting, are checked on the receiving end only, since they go
 * straight to the players watching.
 */
@GameTestHolder("minefantasy2")
public class ShownStateTest {

    private ShownStateTest() {}

    /** Feeds the block's description to a client copy, as a player coming near gets it. */
    private static <T extends StationStatePacket.Shown> T seen(TileEntity block, T copy) throws Exception {
        S35PacketUpdateTileEntity packet = (S35PacketUpdateTileEntity) block.getDescriptionPacket();
        assertNotNull("the block sends no description", packet);
        for (Field field : S35PacketUpdateTileEntity.class.getDeclaredFields()) {
            if (field.getType() == NBTTagCompound.class) {
                field.setAccessible(true);
                copy.show((NBTTagCompound) field.get(packet));
                return copy;
            }
        }
        throw new IllegalStateException("the packet has no data field");
    }

    @GameTest
    public static void aFirepitShowsItsFuel(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.firepit);
        TileEntityFirepit pit = helper.assertTileEntityPresent(TileEntityFirepit.class, 1, 1, 1);
        assertTrue("sticks were not taken as fuel", pit.addFuel(new ItemStack(Items.stick)));
        assertEquals(pit.fuel, seen(pit, new TileEntityFirepit()).fuel);
        helper.succeed();
    }

    @GameTest
    public static void aTroughShowsItsWoodAndWater(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.trough_wood);
        TileEntityTrough trough = helper.assertTileEntityPresent(TileEntityTrough.class, 1, 1, 1);
        trough.trySetMaterial("IronbarkWood");
        trough.fill = 40;
        TileEntityTrough copy = seen(trough, new TileEntityTrough());
        assertEquals("ironbarkwood", copy.getMaterialName());
        assertEquals("the water level", 40, copy.fill);
        helper.succeed();
    }

    @GameTest
    public static void aRackShowsWhatHangsOnIt(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.rack_wood);
        TileEntityRack rack = helper.assertTileEntityPresent(TileEntityRack.class, 1, 1, 1);
        rack.setInventorySlotContents(2, new ItemStack(Items.iron_sword));
        TileEntityRack copy = seen(rack, new TileEntityRack());
        assertNull(copy.getStackInSlot(0));
        assertNotNull("the sword is not shown", copy.getStackInSlot(2));
        assertEquals(Items.iron_sword, copy.getStackInSlot(2).getItem());
        helper.succeed();
    }

    @GameTest
    public static void anAmmoBoxShowsItsAmmoAndLid(GameTestHelper helper) throws Exception {
        helper.setBlock(1, 1, 1, BlockListMF.ammo_box_basic);
        TileEntityAmmoBox box = helper.assertTileEntityPresent(TileEntityAmmoBox.class, 1, 1, 1);
        assertTrue(box.setContentsValidated(new ItemStack(ToolListMF.exploding_arrow), 7));
        box.angle = 12;
        // The client copy sits in the world like the real one: its size comes from the block
        TileEntityAmmoBox client = new TileEntityAmmoBox();
        client.setWorldObj(box.getWorldObj());
        client.xCoord = box.xCoord;
        client.yCoord = box.yCoord;
        client.zCoord = box.zCoord;
        TileEntityAmmoBox copy = seen(box, client);
        assertNotNull("the ammo is not shown", copy.ammo);
        assertEquals(ToolListMF.exploding_arrow, copy.ammo.getItem());
        assertEquals("the stock", 7, copy.stock);
        assertEquals("the lid", 12, copy.angle);
        helper.succeed();
    }

    @GameTest
    public static void aFurnaceShowsItsDoor(GameTestHelper helper) throws Exception {
        TileEntityBigFurnace furnace = new TileEntityBigFurnace();
        furnace.doorAngle = 17;
        assertEquals(17, seen(furnace, new TileEntityBigFurnace()).doorAngle);
        helper.succeed();
    }

    /** A pressed bellows and a bomb press start their swing from the moment they are sent, never below rest. */
    @GameTest
    public static void pressesStartFromTheMomentTheyGet(GameTestHelper helper) {
        NBTTagCompound moment = new NBTTagCompound();
        moment.setFloat("Press", 50F);
        TileEntityBellows bellows = new TileEntityBellows();
        bellows.show(moment);
        assertEquals(50, bellows.press);
        TileEntityBombPress press = new TileEntityBombPress();
        moment.setFloat("Press", 1F);
        press.show(moment);
        assertEquals(1F, press.animation, 0F);
        moment.setFloat("Press", -3F);
        bellows.show(moment);
        press.show(moment);
        assertEquals("the bellows went below rest", 0, bellows.press);
        assertEquals("the press went below rest", 0F, press.animation, 0F);
        helper.succeed();
    }
}
