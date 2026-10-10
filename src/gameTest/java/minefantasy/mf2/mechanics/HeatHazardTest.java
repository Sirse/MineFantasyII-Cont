package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.mojang.authlib.GameProfile;

import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.TongsHelper;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.gametest.TestItems;
import minefantasy.mf2.item.list.ArmourListMF;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * What the research book promises about heat: a trough quenches without harm while open water may damage the piece, and
 * hot items burn a player who carries them without blacksmith attire.
 */
@GameTestHolder("minefantasy2")
public class HeatHazardTest {

    private HeatHazardTest() {}

    /** A hot stack carrying an undamaged iron sword. */
    private static ItemStack hotSword() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(Heatable.NBT_Item, new ItemStack(Items.iron_sword).writeToNBT(new NBTTagCompound()));
        tag.setInteger(Heatable.NBT_CurrentTemp, 500);
        tag.setInteger(Heatable.NBT_WorkableTemp, 100);
        tag.setInteger(Heatable.NBT_UnstableTemp, 1000);
        ItemStack hot = new ItemStack(TestItems.hot);
        hot.setTagCompound(tag);
        return hot;
    }

    private static float waterAt(GameTestHelper helper, int x, int y, int z) {
        TestPos pos = helper.absolute(x, y, z);
        return TongsHelper.getWaterSource(helper.getWorld(), pos.x(), pos.y(), pos.z());
    }

    @GameTest
    public static void emptyTongsLeaveCauldronWaterAndLoadedTongsQuenchOnce(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.cauldron);
        TestPos at = helper.absolute(1, 1, 1);
        helper.getWorld().setBlockMetadataWithNotify(at.x(), at.y(), at.z(), 3, 2);
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.setPositionAndRotation(at.x() + 0.5, at.y() + 2, at.z() + 0.5, 0F, 90F);
        ItemStack tongs = new ItemStack(ToolListMF.tongsStone);
        player.setCurrentItemOrArmor(0, tongs);
        tongs.getItem().onItemRightClick(tongs, helper.getWorld(), player);
        assertEquals("empty tongs took water", 3, helper.getWorld().getBlockMetadata(at.x(), at.y(), at.z()));
        assertTrue(TongsHelper.trySetHeldItem(tongs, hotSword()));
        tongs.getItem().onItemRightClick(tongs, helper.getWorld(), player);
        assertEquals("loaded tongs did not quench", 2, helper.getWorld().getBlockMetadata(at.x(), at.y(), at.z()));
        assertNull(TongsHelper.getHeldItem(tongs));
        tongs.getItem().onItemRightClick(tongs, helper.getWorld(), player);
        assertEquals("repeated use took water", 2, helper.getWorld().getBlockMetadata(at.x(), at.y(), at.z()));
        helper.succeed();
    }

    @GameTest
    public static void emptyTongsLeaveTankWater(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, BlockListMF.trough_wood);
        TileEntityTrough trough = helper.assertTileEntityPresent(TileEntityTrough.class, 1, 1, 1);
        trough.fill = 2;
        TestPos at = helper.absolute(1, 1, 1);
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.setPositionAndRotation(at.x() + 0.5, at.y() + 2, at.z() + 0.5, 0F, 90F);
        ItemStack tongs = new ItemStack(ToolListMF.tongsStone);
        player.setCurrentItemOrArmor(0, tongs);
        tongs.getItem().onItemRightClick(tongs, helper.getWorld(), player);
        assertEquals(2, trough.fill);
        assertTrue(TongsHelper.trySetHeldItem(tongs, hotSword()));
        tongs.getItem().onItemRightClick(tongs, helper.getWorld(), player);
        assertEquals("loaded tongs did not quench in the tank", 1, trough.fill);
        assertNull(TongsHelper.getHeldItem(tongs));
        helper.succeed();
    }

    @GameTest
    public static void aTroughQuenchesWithoutDamage(GameTestHelper helper) {
        boolean ruin = Heatable.HCCquenchRuin;
        try {
            Heatable.HCCquenchRuin = true;
            helper.setBlock(1, 1, 1, BlockListMF.trough_wood);
            TileEntityTrough trough = helper.assertTileEntityPresent(TileEntityTrough.class, 1, 1, 1);
            trough.fill = 2;
            float hazard = waterAt(helper, 1, 1, 1);
            assertEquals("a filled trough is harmless", 0F, hazard, 0F);
            assertEquals("the trough used no water", 1, trough.fill);
            ItemStack cooled = Heatable.getQuenchedItem(hotSword(), hazard);
            assertEquals(Items.iron_sword, cooled.getItem());
            assertEquals("the trough damaged the piece", 0, cooled.getItemDamage());

            trough.fill = 0;
            assertTrue("an empty trough still quenches", waterAt(helper, 1, 1, 1) < 0);
        } finally {
            Heatable.HCCquenchRuin = ruin;
        }
        helper.succeed();
    }

    @GameTest
    public static void openWaterDamagesThePieceUnlessTheRuleIsOff(GameTestHelper helper) {
        boolean ruin = Heatable.HCCquenchRuin;
        try {
            helper.setBlock(1, 1, 1, Blocks.water);
            float hazard = waterAt(helper, 1, 1, 1);
            assertTrue("water is no quenching source", hazard > 0);
            int max = Items.iron_sword.getMaxDamage();

            Heatable.HCCquenchRuin = true;
            assertEquals(
                    "water took the wrong share of durability",
                    (int) (max * hazard / 100F),
                    Heatable.getQuenchedItem(hotSword(), hazard).getItemDamage());
            Heatable.HCCquenchRuin = false;
            assertEquals(
                    "water damaged the piece with quench ruin off",
                    0,
                    Heatable.getQuenchedItem(hotSword(), hazard).getItemDamage());
        } finally {
            Heatable.HCCquenchRuin = ruin;
        }
        helper.succeed();
    }

    private static FakePlayer carrying(GameTestHelper helper, ItemStack item, boolean apron) {
        FakePlayer player = new FakePlayer(helper.getWorld(), new GameProfile(UUID.randomUUID(), Modders.SMITH));
        player.inventory.setInventorySlotContents(5, item);
        if (apron) {
            player.inventory.armorInventory[2] = new ItemStack(ArmourListMF.leatherapron);
        }
        return player;
    }

    @GameTest
    public static void hotItemsBurnAPlayerWithoutBlacksmithAttire(GameTestHelper helper) {
        boolean hardcore = ConfigHardcore.HCChotBurn;
        try {
            ConfigHardcore.HCChotBurn = false;
            FakePlayer bare = carrying(helper, hotSword(), false);
            assertTrue("a hot item did not burn", PlayerTickHandlerMF.burnWithHotItems(bare));
            assertTrue("the player is not on fire", bare.isBurning());

            assertFalse(
                    "attire did not protect",
                    PlayerTickHandlerMF.burnWithHotItems(carrying(helper, hotSword(), true)));
            assertFalse(
                    "a cold item burned",
                    PlayerTickHandlerMF.burnWithHotItems(carrying(helper, new ItemStack(Items.iron_sword), false)));

            ConfigHardcore.HCChotBurn = true;
            assertTrue(
                    "hardcore hot burns spared the attire",
                    PlayerTickHandlerMF.burnWithHotItems(carrying(helper, hotSword(), true)));
        } finally {
            ConfigHardcore.HCChotBurn = hardcore;
        }
        helper.succeed();
    }
}
