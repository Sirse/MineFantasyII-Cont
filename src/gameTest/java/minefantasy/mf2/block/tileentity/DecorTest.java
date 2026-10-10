package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.block.tileentity.Stations.*;
import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.block.decor.BlockTrough;
import minefantasy.mf2.block.decor.BlockWoodDecor;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityAmmoBox;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.block.tileentity.decor.TileEntityWoodDecor;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * Blocks with a tile of their own, placed and broken as a player would: they turn to face the player, wooden ones keep
 * their wood and contents through the item, and nothing of them stays behind.
 */
@GameTestHolder("minefantasy2")
public class DecorTest {

    private DecorTest() {}

    /** Places the block at (1, 1, 1) as a player facing the given quarter turn would, with the given item. */
    private static void place(GameTestHelper helper, Block block, int quarter, ItemStack item) {
        helper.setBlock(1, 1, 1, block);
        TestPos at = helper.absolute(1, 1, 1);
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.rotationYaw = quarter * 90F;
        block.onBlockPlacedBy(helper.getWorld(), at.x(), at.y(), at.z(), player, item);
    }

    private static void breakIt(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 1, 1);
        helper.getWorld().setBlockToAir(at.x(), at.y(), at.z());
    }

    private static <T extends net.minecraft.tileentity.TileEntity> T tile(GameTestHelper helper, Class<T> type) {
        return helper.assertTileEntityPresent(type, 1, 1, 1);
    }

    private static ItemStack wooden(Block block, String wood) {
        return ((BlockWoodDecor<?>) block).construct(wood);
    }

    private static ItemStack onlyDrop(Block block) {
        List<ItemStack> drops = droppedStacks(Item.getItemFromBlock(block));
        assertEquals("the block did not drop once", 1, drops.size());
        return drops.get(0);
    }

    private static String woodOf(ItemStack item) {
        CustomMaterial material = CustomMaterial.getMaterialFor(item, CustomToolHelper.slot_main);
        return material == null ? null : material.name.toLowerCase();
    }

    @GameTest
    public static void blocksTurnToFaceWhoPlacesThem(GameTestHelper helper) {
        Block[] blocks = { BlockListMF.ammo_box_basic, BlockListMF.trough_wood, BlockListMF.bellows,
                BlockListMF.bombPress, BlockListMF.components };
        TestPos at = helper.absolute(1, 1, 1);
        for (Block block : blocks) {
            for (int quarter = 0; quarter < 4; quarter++) {
                place(helper, block, quarter, new ItemStack(block));
                assertEquals(
                        block.getUnlocalizedName() + " facing " + quarter,
                        quarter,
                        helper.getWorld().getBlockMetadata(at.x(), at.y(), at.z()));
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void aWoodenBlockKeepsItsWood(GameTestHelper helper) {
        Stations.begin(helper);
        try {
            for (Block block : new Block[] { BlockListMF.trough_wood, BlockListMF.rack_wood,
                    BlockListMF.ammo_box_basic }) {
                place(helper, block, 0, wooden(block, "IronbarkWood"));
                assertEquals(
                        block.getUnlocalizedName() + " took the wrong wood",
                        "ironbarkwood",
                        tile(helper, TileEntityWoodDecor.class).getMaterialName());
                breakIt(helper);
                ItemStack drop = onlyDrop(block);
                assertEquals(block.getUnlocalizedName() + " lost its wood", "ironbarkwood", woodOf(drop));
                Stations.end();
                Stations.begin(helper);
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void pickingWoodenBlocksKeepsTheirMaterial(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 1, 1);
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        for (Block block : new Block[] { BlockListMF.crate_basic, BlockListMF.trough_wood, BlockListMF.rack_wood }) {
            for (String wood : new String[] { "RefinedWood", "IronbarkWood" }) {
                place(helper, block, 0, wooden(block, wood));
                ItemStack picked = block.getPickBlock(null, helper.getWorld(), at.x(), at.y(), at.z(), player);
                assertEquals("pick lost material", wood.toLowerCase(), woodOf(picked));
                assertEquals(
                        "pick changed the block",
                        wood.toLowerCase(),
                        tile(helper, TileEntityWoodDecor.class).getMaterialName());
                place(helper, block, 0, picked);
                assertEquals(
                        "picked block lost material on placement",
                        wood.toLowerCase(),
                        tile(helper, TileEntityWoodDecor.class).getMaterialName());
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void anAmmoBoxKeepsItsAmmoThroughTheItem(GameTestHelper helper) {
        Stations.begin(helper);
        try {
            place(helper, BlockListMF.ammo_box_basic, 0, wooden(BlockListMF.ammo_box_basic, "OakWood"));
            TileEntityAmmoBox box = tile(helper, TileEntityAmmoBox.class);
            assertTrue(
                    "the box turned the arrows away",
                    box.setContentsValidated(new ItemStack(ToolListMF.exploding_arrow), 5));
            breakIt(helper);
            ItemStack drop = onlyDrop(BlockListMF.ammo_box_basic);
            place(helper, BlockListMF.ammo_box_basic, 0, drop);
            TileEntityAmmoBox again = tile(helper, TileEntityAmmoBox.class);
            assertNotNull("the ammo was lost", again.ammo);
            assertEquals(ToolListMF.exploding_arrow, again.ammo.getItem());
            assertEquals("the stock changed", 5, again.stock);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    /** A trough's water is kept within what its wood holds, whatever the item claims. */
    @GameTest
    public static void aTroughHoldsNoMoreThanItsWoodAllows(GameTestHelper helper) {
        int[] held = new int[2];
        String[] woods = { "ScrapWood", "IronbarkWood" };
        for (int i = 0; i < woods.length; i++) {
            ItemStack item = wooden(BlockListMF.trough_wood, woods[i]);
            item.getTagCompound().setInteger(BlockTrough.NBT_fill, 100000);
            place(helper, BlockListMF.trough_wood, 0, item);
            TileEntityTrough trough = tile(helper, TileEntityTrough.class);
            assertEquals(woods[i] + " trough", trough.getCapacity(), trough.fill);
            held[i] = trough.fill;
        }
        assertTrue("an ironbark trough holds no more than a scrap one", held[1] > held[0]);
        helper.succeed();
    }

    @GameTest
    public static void aBrokenComponentLeavesNoTileAndDropsItsStack(GameTestHelper helper) {
        Stations.begin(helper);
        try {
            helper.setBlock(1, 1, 1, BlockListMF.components);
            TileEntityComponent component = tile(helper, TileEntityComponent.class);
            component.setItem(new ItemStack(Items.iron_ingot), "bar", "bar", 16, 3);
            breakIt(helper);
            TestPos at = helper.absolute(1, 1, 1);
            assertNull("the tile stayed", helper.getWorld().getTileEntity(at.x(), at.y(), at.z()));
            assertEquals("the stack was not dropped", 3, dropped(Items.iron_ingot));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
