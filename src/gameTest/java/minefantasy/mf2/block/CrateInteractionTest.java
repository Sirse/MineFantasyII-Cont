package minefantasy.mf2.block;

import static minefantasy.mf2.gametest.Assert.*;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityAmmoBox;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.CustomToolListMF;

@GameTestHolder("minefantasy2")
public class CrateInteractionTest {

    @GameTest
    public static void rightClickNeverFallsThroughToBlockPlacement(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
        helper.setBlock(2, 2, 2, BlockListMF.crate_basic);
        TileEntityAmmoBox box = helper.assertTileEntityPresent(TileEntityAmmoBox.class, 2, 2, 2);
        player.setPosition(box.xCoord + .5, box.yCoord + 1, box.zCoord + .5);
        player.setCurrentItemOrArmor(0, new ItemStack(Blocks.cobblestone, 5));
        click(player, box);
        assertEquals(5, box.stock);
        assertTrue(player.getHeldItem() == null);
        click(player, box);
        assertEquals(0, box.stock);
        assertEquals(5, player.getHeldItem().stackSize);
        click(player, box);
        assertEquals(5, box.stock);
        player.setCurrentItemOrArmor(0, new ItemStack(Blocks.dirt, 3));
        click(player, box);
        assertEquals(5, box.stock);
        assertEquals(3, player.getHeldItem().stackSize);
        box.setContentsValidated(new ItemStack(Blocks.cobblestone), box.getMaxAmmo(new ItemStack(Blocks.cobblestone)));
        int full = box.stock;
        player.setCurrentItemOrArmor(0, new ItemStack(Blocks.cobblestone, 3));
        click(player, box);
        assertEquals(full, box.stock);
        assertEquals(3, player.getHeldItem().stackSize);
        player.setCurrentItemOrArmor(0, new ItemStack(BlockListMF.crate_basic));
        click(player, box);
        assertEquals(full, box.stock);
        assertEquals(1, player.getHeldItem().stackSize);
        helper.succeed();
    }

    @GameTest
    public static void foodAndAmmoBoxesConsumeAcceptedAndRejectedClicks(GameTestHelper helper) {
        Block[] blocks = { BlockListMF.food_box_basic, BlockListMF.ammo_box_basic };
        ItemStack[] accepted = { new ItemStack(Items.bread, 4), CustomToolListMF.standard_bolt.construct("Iron") };
        for (int i = 0; i < blocks.length; i++) {
            FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
            helper.setBlock(2 + i, 2, 2, blocks[i]);
            TileEntityAmmoBox box = helper.assertTileEntityPresent(TileEntityAmmoBox.class, 2 + i, 2, 2);
            player.setCurrentItemOrArmor(0, accepted[i].copy());
            click(player, box);
            assertEquals(accepted[i].stackSize, box.stock);
            assertTrue(player.getHeldItem() == null);
            player.setCurrentItemOrArmor(0, new ItemStack(Blocks.cobblestone, 3));
            click(player, box);
            assertEquals(accepted[i].stackSize, box.stock);
            assertEquals(3, player.getHeldItem().stackSize);
        }
        helper.succeed();
    }

    @GameTest
    public static void sneakingPlacesBesideTheCrateWithoutDepositing(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
        helper.setBlock(2, 2, 2, BlockListMF.crate_basic);
        TileEntityAmmoBox box = helper.assertTileEntityPresent(TileEntityAmmoBox.class, 2, 2, 2);
        player.setSneaking(true);
        player.setCurrentItemOrArmor(0, new ItemStack(Blocks.cobblestone, 3));
        assertTrue(
                player.theItemInWorldManager.activateBlockOrUseItem(
                        player,
                        box.getWorldObj(),
                        player.getHeldItem(),
                        box.xCoord,
                        box.yCoord,
                        box.zCoord,
                        1,
                        .5F,
                        .5F,
                        .5F));
        assertSame(Blocks.cobblestone, box.getWorldObj().getBlock(box.xCoord, box.yCoord + 1, box.zCoord));
        assertEquals(0, box.stock);
        assertEquals(2, player.getHeldItem().stackSize);
        helper.succeed();
    }

    @GameTest
    public static void creativeDepositsKeepTheHeldStack(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
        player.theItemInWorldManager.setGameType(WorldSettings.GameType.CREATIVE);
        helper.setBlock(2, 2, 2, BlockListMF.crate_basic);
        TileEntityAmmoBox box = helper.assertTileEntityPresent(TileEntityAmmoBox.class, 2, 2, 2);
        player.setCurrentItemOrArmor(0, new ItemStack(Blocks.cobblestone, 3));
        click(player, box);
        assertEquals(3, box.stock);
        assertEquals(3, player.getHeldItem().stackSize);
        click(player, box);
        assertEquals(6, box.stock);
        assertEquals(3, player.getHeldItem().stackSize);
        helper.succeed();
    }

    @GameTest
    public static void clientActivationConsumesClicksWithoutMutatingInventory(GameTestHelper helper) throws Exception {
        FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
        Block[] blocks = { BlockListMF.crate_basic, BlockListMF.food_box_basic, BlockListMF.ammo_box_basic };
        Field remote = World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        World world = helper.getWorld();
        for (int i = 0; i < blocks.length; i++) {
            helper.setBlock(2 + i, 2, 2, blocks[i]);
            TileEntityAmmoBox box = helper.assertTileEntityPresent(TileEntityAmmoBox.class, 2 + i, 2, 2);
            player.setCurrentItemOrArmor(0, new ItemStack(Blocks.cobblestone, 3));
            NBTTagCompound before = new NBTTagCompound();
            box.writeToNBT(before);
            // Exercise the client branch synchronously; restore the world's flag before any test tick runs.
            boolean previous = world.isRemote;
            try {
                remote.setBoolean(world, true);
                assertTrue(
                        blocks[i]
                                .onBlockActivated(world, box.xCoord, box.yCoord, box.zCoord, player, 1, .5F, .5F, .5F));
            } finally {
                remote.setBoolean(world, previous);
            }
            NBTTagCompound after = new NBTTagCompound();
            box.writeToNBT(after);
            assertEquals(before, after);
            assertEquals(3, player.getHeldItem().stackSize);
        }
        helper.succeed();
    }

    @GameTest
    public static void rackConsumesBlockClicksWhileTroughAllowsPlacement(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.NOVICE);
        helper.setBlock(2, 2, 3, Blocks.stone);
        helper.setBlock(2, 2, 2, BlockListMF.rack_wood);
        com.gtnewhorizons.horizonqa.api.TestPos rack = helper.absolute(2, 2, 2);
        helper.getWorld().setBlockMetadataWithNotify(rack.x(), rack.y(), rack.z(), 2, 2);
        helper.setBlock(3, 2, 2, BlockListMF.trough_wood);
        for (int i = 0; i < 2; i++) {
            int x = helper.absolute(2 + i, 2, 2).x();
            int y = helper.absolute(2 + i, 2, 2).y();
            int z = helper.absolute(2 + i, 2, 2).z();
            player.setCurrentItemOrArmor(0, new ItemStack(Blocks.cobblestone, 3));
            assertTrue(
                    player.theItemInWorldManager.activateBlockOrUseItem(
                            player,
                            helper.getWorld(),
                            player.getHeldItem(),
                            x,
                            y,
                            z,
                            1,
                            .5F,
                            .5F,
                            .5F));
            if (i == 0) {
                assertTrue(helper.getWorld().isAirBlock(x, y + 1, z));
                assertEquals(3, player.getHeldItem().stackSize);
            } else {
                assertSame(Blocks.cobblestone, helper.getWorld().getBlock(x, y + 1, z));
                assertEquals(2, player.getHeldItem().stackSize);
            }
        }
        helper.succeed();
    }

    private static void click(FakePlayer player, TileEntityAmmoBox box) {
        assertTrue(
                player.theItemInWorldManager.activateBlockOrUseItem(
                        player,
                        box.getWorldObj(),
                        player.getHeldItem(),
                        box.xCoord,
                        box.yCoord,
                        box.zCoord,
                        1,
                        .5F,
                        .5F,
                        .5F));
        assertTrue(box.getWorldObj().isAirBlock(box.xCoord, box.yCoord + 1, box.zCoord));
    }
}
