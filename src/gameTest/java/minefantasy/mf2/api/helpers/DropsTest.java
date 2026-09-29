package minefantasy.mf2.api.helpers;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.gametest.Modders;

/** Items thrown into the world, and which way a player faces when placing something. */
@GameTestHolder("minefantasy2")
public class DropsTest {

    private DropsTest() {}

    @GameTest
    public static void aStillDropLiesWhereItIsPut(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 1, 1);
        EntityItem item = Drops.still(helper.getWorld(), at.x(), at.y(), at.z(), new ItemStack(Items.stick), 20);
        assertNotNull("nothing was thrown", item);
        assertTrue("it was not spawned", helper.getWorld().loadedEntityList.contains(item));
        assertEquals(0D, item.motionX, 0D);
        assertEquals(0D, item.motionY, 0D);
        assertEquals(0D, item.motionZ, 0D);
        assertEquals("the pickup delay", 20, item.delayBeforeCanPickup);
        item.setDead();
        helper.succeed();
    }

    @GameTest
    public static void nothingIsThrownForAnEmptyStack(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 1, 1);
        assertNull(Drops.fromBlock(helper.getWorld(), at.x(), at.y(), at.z(), null));
        assertNull(Drops.fromBlock(helper.getWorld(), at.x(), at.y(), at.z(), new ItemStack(Items.stick, 0)));
        helper.succeed();
    }

    @GameTest
    public static void aPlayerFacesTheQuarterTheyLookAt(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        float[] yaws = { 0F, 90F, 180F, 270F, -90F, 44F, 46F, 720F };
        int[] quarters = { 0, 1, 2, 3, 3, 0, 1, 0 };
        for (int i = 0; i < yaws.length; i++) {
            player.rotationYaw = yaws[i];
            assertEquals("yaw " + yaws[i], quarters[i], Heading.of(player));
            assertEquals("back towards yaw " + yaws[i], (quarters[i] + 2) & 3, Heading.towards(player));
        }
        helper.succeed();
    }

    @GameTest
    public static void aPlayerLooksAlongTheSteeperAxis(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.rotationYaw = 90F;
        player.rotationPitch = 0F;
        assertEquals(ForgeDirection.WEST, Heading.look(player));
        player.rotationPitch = 60F;
        assertEquals(ForgeDirection.DOWN, Heading.look(player));
        player.rotationPitch = -60F;
        assertEquals(ForgeDirection.UP, Heading.look(player));
        player.rotationYaw = 0F;
        player.rotationPitch = 30F;
        assertEquals(ForgeDirection.SOUTH, Heading.look(player));
        helper.succeed();
    }
}
