package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.mojang.authlib.GameProfile;

import minefantasy.mf2.api.archery.AmmoMechanicsMF;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.block.decor.BlockAmmoBox;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityAmmoBox;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * Provision boxes, ammunition boxes and crates, as the research book describes them: each takes its own kind of item,
 * holds several stacks by the tier of its wood, and keeps its contents when it is broken.
 */
@GameTestHolder("minefantasy2")
public class StorageBoxTest {

    private StorageBoxTest() {}

    private static TileEntityAmmoBox box(GameTestHelper helper, int x, net.minecraft.block.Block block, String wood) {
        helper.setBlock(x, 1, 1, block);
        TileEntityAmmoBox box = helper.assertTileEntityPresent(TileEntityAmmoBox.class, x, 1, 1);
        box.setMaterial(CustomMaterial.getMaterial(wood));
        return box;
    }

    private static FakePlayer holding(GameTestHelper helper, ItemStack held) {
        FakePlayer player = new FakePlayer(helper.getWorld(), new GameProfile(UUID.randomUUID(), Modders.SMITH));
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, held);
        return player;
    }

    @GameTest
    public static void eachBoxTakesItsOwnKind(GameTestHelper helper) {
        ItemStack bread = new ItemStack(Items.bread);
        ItemStack arrow = new ItemStack(CustomToolListMF.standard_arrow);
        ItemStack stone = new ItemStack(Blocks.stone);
        TileEntityAmmoBox food = box(helper, 1, BlockListMF.food_box_basic, "OakWood");
        TileEntityAmmoBox ammo = box(helper, 3, BlockListMF.ammo_box_basic, "OakWood");
        TileEntityAmmoBox crate = box(helper, 5, BlockListMF.crate_basic, "OakWood");
        assertTrue(food.canAcceptItem(bread));
        assertFalse("the provision box took arrows", food.canAcceptItem(arrow));
        assertTrue(ammo.canAcceptItem(arrow));
        assertFalse("the ammunition box took bread", ammo.canAcceptItem(bread));
        assertTrue(crate.canAcceptItem(stone) && crate.canAcceptItem(bread) && crate.canAcceptItem(arrow));
        assertFalse("a box went into a crate", crate.canAcceptItem(new ItemStack(BlockListMF.food_box_basic)));
        helper.succeed();
    }

    @GameTest
    public static void aBoxHoldsStacksByTheTierOfItsWood(GameTestHelper helper) {
        ItemStack bread = new ItemStack(Items.bread);
        int stack = bread.getMaxStackSize();
        String[] woods = { "ScrapWood", "OakWood", "RefinedWood", "EbonyWood" };
        for (int i = 0; i < woods.length; i++) {
            TileEntityAmmoBox box = box(helper, 1 + 2 * i, BlockListMF.food_box_basic, woods[i]);
            int tier = CustomMaterial.getMaterial(woods[i]).tier;
            assertEquals(woods[i] + " box", stack * (tier * 4 + 4), box.getMaxAmmo(bread));
            assertTrue(woods[i] + " box holds no more than a stack", box.getMaxAmmo(bread) > stack);
        }
        helper.succeed();
    }

    @GameTest
    public static void itemsGoInAndComeOutAStackAtATime(GameTestHelper helper) {
        TileEntityAmmoBox box = box(helper, 1, BlockListMF.food_box_basic, "OakWood");
        FakePlayer player = holding(helper, new ItemStack(Items.bread, 64));
        assertTrue(box.interact(player));
        assertNull("the bread stayed in hand", player.getHeldItem());
        player.inventory.setInventorySlotContents(0, new ItemStack(Items.bread, 64));
        assertTrue(box.interact(player));
        assertEquals(128, box.stock);

        FakePlayer empty = holding(helper, null);
        assertTrue(box.interact(empty));
        assertEquals("an empty hand took the wrong amount", 64, box.stock);
        ItemStack taken = empty.getHeldItem();
        assertNotNull("the stack left the box but not into the hand", taken);
        assertEquals(Items.bread, taken.getItem());
        assertEquals("the hand got the wrong amount", 64, taken.stackSize);
        assertEquals("bread made or lost", 128, box.stock + taken.stackSize);
        helper.succeed();
    }

    /**
     * Clicking an ammunition box with a bow or crossbow loads it: a stack at most, topping up ammunition of the same
     * kind, and only ammunition the weapon fires.
     */
    @GameTest
    public static void anAmmunitionBoxLoadsTheWeaponClickedOnIt(GameTestHelper helper) {
        TileEntityAmmoBox arrows = box(helper, 1, BlockListMF.ammo_box_basic, "OakWood");
        ItemStack arrow = new ItemStack(CustomToolListMF.standard_arrow);
        int stack = arrow.getMaxStackSize();
        arrows.setContentsValidated(arrow, stack * 2 + 5);

        ItemStack bow = new ItemStack(CustomToolListMF.standard_bow);
        assertTrue("the bow was not loaded", arrows.interact(holding(helper, bow)));
        ItemStack loaded = AmmoMechanicsMF.getAmmo(bow);
        assertNotNull(loaded);
        assertEquals(CustomToolListMF.standard_arrow, loaded.getItem());
        assertEquals("a bow takes one stack", stack, loaded.stackSize);
        assertEquals(stack + 5, arrows.stock);

        // A partly spent bow is topped up to a stack
        loaded.stackSize = stack - 3;
        AmmoMechanicsMF.setAmmo(bow, loaded);
        assertTrue(arrows.interact(holding(helper, bow)));
        assertEquals(stack, AmmoMechanicsMF.getAmmo(bow).stackSize);
        assertEquals(stack + 2, arrows.stock);

        // Arrows are not bolts: the crossbow stays empty and nothing leaves the box
        ItemStack crossbow = new ItemStack(ToolListMF.crossbow_custom);
        arrows.interact(holding(helper, crossbow));
        assertNull("a crossbow took arrows", AmmoMechanicsMF.getAmmo(crossbow));
        assertEquals(stack + 2, arrows.stock);

        TileEntityAmmoBox bolts = box(helper, 3, BlockListMF.ammo_box_basic, "OakWood");
        bolts.setContentsValidated(new ItemStack(CustomToolListMF.standard_bolt), 7);
        assertTrue("the crossbow was not loaded", bolts.interact(holding(helper, crossbow)));
        assertEquals(7, AmmoMechanicsMF.getAmmo(crossbow).stackSize);
        assertNull("the emptied box kept its bolts", bolts.ammo);
        helper.succeed();
    }

    @GameTest
    public static void aBrokenBoxKeepsItsContents(GameTestHelper helper) {
        TileEntityAmmoBox box = box(helper, 1, BlockListMF.food_box_basic, "EbonyWood");
        box.setContentsValidated(new ItemStack(Items.bread), 500);
        TestPos at = helper.absolute(1, 1, 1);
        helper.getWorld().setBlockToAir(at.x(), at.y(), at.z());
        List<?> drops = helper.getWorld().getEntitiesWithinAABB(
                EntityItem.class,
                AxisAlignedBB.getBoundingBox(at.x() - 1, at.y() - 1, at.z() - 1, at.x() + 2, at.y() + 2, at.z() + 2));
        assertEquals("the box did not drop once", 1, drops.size());
        ItemStack dropped = ((EntityItem) drops.get(0)).getEntityItem();
        assertEquals(500, dropped.getTagCompound().getInteger(BlockAmmoBox.NBT_Stock));

        // Placed again, the box holds what it held: its wood allows the whole amount
        helper.setBlock(3, 1, 1, BlockListMF.food_box_basic);
        TestPos again = helper.absolute(3, 1, 1);
        BlockListMF.food_box_basic
                .onBlockPlacedBy(helper.getWorld(), again.x(), again.y(), again.z(), holding(helper, null), dropped);
        TileEntityAmmoBox placed = helper.assertTileEntityPresent(TileEntityAmmoBox.class, 3, 1, 1);
        assertEquals(Items.bread, placed.ammo.getItem());
        assertEquals("the placed box lost contents", 500, placed.stock);
        helper.succeed();
    }
}
