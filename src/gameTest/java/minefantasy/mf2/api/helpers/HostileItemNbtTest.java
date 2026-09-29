package minefantasy.mf2.api.helpers;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.archery.AmmoMechanicsMF;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.block.decor.BlockAmmoBox;
import minefantasy.mf2.block.decor.BlockTrough;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.decor.TileEntityAmmoBox;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.food.FoodListMF;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.CustomToolListMF;

/**
 * Items whose tag the server did not write: from the creative menu, a command, another mod's hole or an old save. The
 * mod reads counts, stats and stored items out of such tags; out-of-range values must not crash the server, poison
 * health with NaN, or hand out more than a legitimate item could hold.
 */
@GameTestHolder("minefantasy2")
public class HostileItemNbtTest {

    private HostileItemNbtTest() {}

    private static final int[] COUNTS = { Integer.MIN_VALUE, -1, 0, 127, 32767, Integer.MAX_VALUE };

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        TestPos at = helper.absolute(1, 1, 1);
        player.setPositionAndRotation(at.x() + 0.5, at.y(), at.z() + 0.5, 0F, 0F);
        return player;
    }

    @GameTest
    public static void aTroughTakesNoMoreThanItHoldsFromItsItem(GameTestHelper helper) {
        FakePlayer player = player(helper);
        for (int i = 0; i < COUNTS.length; i++) {
            ItemStack item = new ItemStack(BlockListMF.trough_wood);
            item.setTagCompound(new NBTTagCompound());
            item.getTagCompound().setInteger(BlockTrough.NBT_fill, COUNTS[i]);
            helper.setBlock(1 + i, 1, 3, BlockListMF.trough_wood);
            TestPos at = helper.absolute(1 + i, 1, 3);
            BlockListMF.trough_wood.onBlockPlacedBy(helper.getWorld(), at.x(), at.y(), at.z(), player, item);
            TileEntityTrough trough = helper.assertTileEntityPresent(TileEntityTrough.class, 1 + i, 1, 3);
            assertTrue(
                    "a trough placed with " + COUNTS[i] + " holds " + trough.fill,
                    trough.fill >= 0 && trough.fill <= trough.getCapacity());
        }
        helper.succeed();
    }

    @GameTest
    public static void aLoadedCountOutsideAStackIsNotTrusted(GameTestHelper helper) {
        int stack = new ItemStack(CustomToolListMF.standard_arrow).getMaxStackSize();
        for (int count : COUNTS) {
            ItemStack bow = new ItemStack(CustomToolListMF.standard_bow);
            ItemStack forged = new ItemStack(CustomToolListMF.standard_arrow, 1);
            forged.stackSize = count;
            AmmoMechanicsMF.setAmmo(bow, forged);
            ItemStack loaded = AmmoMechanicsMF.getAmmo(bow);
            // A stack's count is saved as a byte
            if ((byte) count <= 0) {
                assertNull("a bow loaded with " + count + " shows ammunition", loaded);
            } else {
                assertNotNull(loaded);
                assertTrue(
                        "a bow loaded with " + count + " shows " + loaded.stackSize,
                        loaded.stackSize > 0 && loaded.stackSize <= stack);
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void anAmmunitionBoxRefusesABoxAndAnOversizedStock(GameTestHelper helper) {
        FakePlayer player = player(helper);
        ItemStack inner = new ItemStack(BlockListMF.ammo_box_basic);
        ItemStack[] contents = { new ItemStack(CustomToolListMF.standard_arrow), inner };
        for (int i = 0; i < contents.length; i++) {
            for (int j = 0; j < COUNTS.length; j++) {
                ItemStack item = new ItemStack(BlockListMF.ammo_box_basic);
                item.setTagCompound(new NBTTagCompound());
                item.getTagCompound().setTag(BlockAmmoBox.NBT_Ammo, contents[i].writeToNBT(new NBTTagCompound()));
                item.getTagCompound().setInteger(BlockAmmoBox.NBT_Stock, COUNTS[j]);
                helper.setBlock(1 + j, 1, 1 + 2 * i, BlockListMF.ammo_box_basic);
                TestPos at = helper.absolute(1 + j, 1, 1 + 2 * i);
                BlockListMF.ammo_box_basic.onBlockPlacedBy(helper.getWorld(), at.x(), at.y(), at.z(), player, item);
                TileEntityAmmoBox box = helper.assertTileEntityPresent(TileEntityAmmoBox.class, 1 + j, 1, 1 + 2 * i);
                String what = contents[i].getDisplayName() + " x" + COUNTS[j];
                if (box.ammo == null) {
                    assertEquals(what + " left a count in an empty box", 0, box.stock);
                } else {
                    assertTrue(what + " went into the box", box.canAcceptItem(box.ammo));
                    assertTrue(what + " holds " + box.stock, box.stock >= 0 && box.stock <= box.getMaxAmmo(box.ammo));
                }
            }
        }
        helper.succeed();
    }

    @GameTest
    public static void eatenFoodReturnsNoEmptyOrNegativeLeftover(GameTestHelper helper) {
        FakePlayer player = player(helper);
        for (int count : new int[] { Integer.MIN_VALUE, -5, 0 }) {
            ItemStack stew = new ItemStack(FoodListMF.stew);
            ItemStack leftover = new ItemStack(net.minecraft.init.Items.bowl);
            leftover.stackSize = count;
            stew.setTagCompound(new NBTTagCompound());
            stew.getTagCompound().setTag("MF_Food_leftover", leftover.writeToNBT(new NBTTagCompound()));
            ItemStack after = stew.getItem().onEaten(stew, helper.getWorld(), player);
            assertTrue(
                    "a leftover of " + count + " came back as " + after,
                    after == null
                            || after.stackSize >= 0 && (after.stackSize > 0 || after.getItem() == FoodListMF.stew));
        }
        helper.succeed();
    }

    /** A weapon whose stats are not numbers; a hit must not make the target's health one either. */
    @GameTest
    public static void statsThatAreNotNumbersDoNotPoisonHealth(GameTestHelper helper) {
        float[] values = { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -0F };
        String[] keys = { ItemQuality.QUALITY_KEY, ToolHelper.sharpnessLevelNBT };
        int z = 3;
        for (String key : keys) {
            for (float value : values) {
                ItemStack sword = CustomToolHelper.construct(CustomToolListMF.standard_sword, "steel", "oakwood");
                sword.getTagCompound().setFloat(key, value);
                FakePlayer player = player(helper);
                player.setCurrentItemOrArmor(0, sword);
                EntityZombie zombie = new EntityZombie(helper.getWorld());
                TestPos at = helper.absolute(1, 1, z++);
                zombie.setPosition(at.x() + 0.5, at.y(), at.z() + 0.5);
                helper.getWorld().spawnEntityInWorld(zombie);
                player.attackTargetEntityWithCurrentItem(zombie);
                assertFalse(key + "=" + value + " made the health NaN", Float.isNaN(zombie.getHealth()));
                zombie.setDead();
            }
        }
        helper.succeed();
    }

    /** A hot piece holds the item it was made from; a crafted chain of them hundreds deep must still show a name. */
    @GameTest
    public static void aDeeplyNestedHotPieceStillHasAName(GameTestHelper helper) {
        ItemStack piece = new ItemStack(ComponentListMF.plate);
        for (int depth = 0; depth < 256; depth++) {
            ItemStack hot = new ItemStack(ComponentListMF.hotItem);
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setTag(Heatable.NBT_Item, piece.writeToNBT(new NBTTagCompound()));
            nbt.setInteger(Heatable.NBT_CurrentTemp, Integer.MAX_VALUE);
            hot.setTagCompound(nbt);
            piece = hot;
        }
        assertNotNull(piece.getDisplayName());
        helper.succeed();
    }
}
