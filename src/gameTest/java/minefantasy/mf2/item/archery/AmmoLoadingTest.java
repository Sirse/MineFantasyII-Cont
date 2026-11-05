package minefantasy.mf2.item.archery;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.archery.AmmoMechanicsMF;
import minefantasy.mf2.api.crafting.engineer.ICrossbowPart;
import minefantasy.mf2.entity.EntityArrowMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.gadget.ItemCrossbow;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * Ammunition is neither made nor changed by loading and shooting: an arrow a bow of Infinity keeps on the bow is not
 * one to pick up as well, and a crossbow loads only bolts of the make already loaded.
 */
@GameTestHolder("minefantasy2")
public class AmmoLoadingTest {

    private AmmoLoadingTest() {}

    /** Shoots a loaded MineFantasy bow in survival; returns the arrow it fired. */
    private static EntityArrowMF shoot(GameTestHelper helper, ItemStack bow) {
        ItemStack arrows = CustomToolListMF.standard_arrow.construct("Iron");
        arrows.stackSize = 5;
        AmmoMechanicsMF.setAmmo(bow, arrows);
        FakePlayer archer = Modders.fresh(helper, Modders.SMITH);
        archer.inventory.clearInventory(null, -1);
        archer.inventory.currentItem = 0;
        archer.inventory.setInventorySlotContents(0, bow);
        archer.setItemInUse(bow, bow.getMaxItemUseDuration());
        // The arrow fired is the one that was not there before
        java.util.Set<Object> before = new java.util.HashSet<>(helper.getWorld().loadedEntityList);
        bow.getItem().onPlayerStoppedUsing(bow, archer.worldObj, archer, bow.getMaxItemUseDuration() - 30);
        for (Object entity : helper.getWorld().loadedEntityList) {
            if (entity instanceof EntityArrowMF && !before.contains(entity)) return (EntityArrowMF) entity;
        }
        return null;
    }

    @GameTest
    public static void aBowOfInfinityKeepsItsArrowAndLeavesNoneToPickUp(GameTestHelper helper) {
        ItemStack bow = new ItemStack(CustomToolListMF.standard_bow);
        bow.addEnchantment(Enchantment.infinity, 1);
        EntityArrowMF arrow = shoot(helper, bow);
        assertNotNull("the bow did not shoot", arrow);
        assertEquals("a bow of Infinity spent its arrow", 5, AmmoMechanicsMF.getAmmo(bow).stackSize);
        assertTrue("the kept arrow can be picked up as well", arrow.canBePickedUp != 1);
        arrow.setDead();
        helper.succeed();
    }

    @GameTest
    public static void aPlainBowSpendsItsArrowWhichCanBePickedUp(GameTestHelper helper) {
        ItemStack bow = new ItemStack(CustomToolListMF.standard_bow);
        EntityArrowMF arrow = shoot(helper, bow);
        assertNotNull("the bow did not shoot", arrow);
        assertEquals("the arrow was not spent", 4, AmmoMechanicsMF.getAmmo(bow).stackSize);
        assertEquals("the spent arrow cannot be picked up", 1, arrow.canBePickedUp);
        arrow.setDead();
        helper.succeed();
    }

    private static ItemStack bolts(String material, int count) {
        ItemStack bolts = CustomToolListMF.standard_bolt.construct(material);
        bolts.stackSize = count;
        return bolts;
    }

    /** Reloads a crossbow with a magazine once, from a stock of bolts; returns what is loaded after. */
    private static ItemStack reload(GameTestHelper helper, ItemStack loaded, ItemStack stock) {
        ItemCrossbow crossbow = ToolListMF.crossbow_custom;
        ItemStack bow = crossbow.constructCrossbow(
                (ICrossbowPart) ComponentListMF.crossbow_stock_wood,
                (ICrossbowPart) ComponentListMF.cross_arms_basic,
                (ICrossbowPart) ComponentListMF.cross_ammo);
        AmmoMechanicsMF.putAmmoOnFirearm(bow, loaded);
        AmmoMechanicsMF.setAmmo(bow, stock);
        ItemCrossbow.setUseAction(bow, "reload");
        FakePlayer archer = Modders.fresh(helper, Modders.SMITH);
        archer.inventory.clearInventory(null, -1);
        archer.inventory.currentItem = 0;
        archer.inventory.setInventorySlotContents(0, bow);
        crossbow.onEaten(bow, archer.worldObj, archer);
        return AmmoMechanicsMF.getArrowOnBow(bow);
    }

    @GameTest
    public static void aCrossbowLoadsOnlyBoltsOfTheMakeLoaded(GameTestHelper helper) {
        ItemStack loaded = reload(helper, CustomToolListMF.standard_bolt.construct("Steel"), bolts("Iron", 4));
        assertEquals("an iron bolt joined the steel one", 1, loaded.stackSize);
        assertEquals(
                "the loaded bolt changed its make",
                CustomToolListMF.standard_bolt.construct("Steel").getTagCompound(),
                loaded.getTagCompound());
        helper.succeed();
    }

    @GameTest
    public static void aCrossbowLoadsMoreBoltsOfTheSameMake(GameTestHelper helper) {
        ItemStack loaded = reload(helper, CustomToolListMF.standard_bolt.construct("Iron"), bolts("Iron", 4));
        assertEquals("a second iron bolt was not loaded", 2, loaded.stackSize);
        helper.succeed();
    }
}
