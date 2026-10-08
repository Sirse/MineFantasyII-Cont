package minefantasy.mf2.container;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import cpw.mods.fml.common.Loader;
import minefantasy.mf2.api.archery.AmmoMechanicsMF;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.CustomToolListMF;
import mods.battlegear2.api.quiver.IArrowContainer2;
import mods.battlegear2.utils.BattlegearConfig;

/**
 * A MineFantasy bow fires what is loaded into it. Battlegear fires any ItemBow from a quiver the player carries; which
 * one shoots decides whether loading the bow still means anything.
 */
@GameTestHolder("minefantasy2")
public class QuiverBowTest {

    private QuiverBowTest() {}

    @GameTest
    public static void aLoadedBowFiresItsOwnArrowWithAQuiverCarried(GameTestHelper helper) {
        if (Loader.isModLoaded("battlegear2")) {
            ItemStack bow = new ItemStack(CustomToolListMF.standard_bow);
            AmmoMechanicsMF.setAmmo(bow, new ItemStack(CustomToolListMF.standard_arrow, 5));
            ItemStack quiver = new ItemStack(BattlegearConfig.quiver);
            ((IArrowContainer2) quiver.getItem()).setStackInSlot(quiver, 0, new ItemStack(Items.arrow, 10));

            FakePlayer archer = Modders.fresh(helper, Modders.SMITH);
            archer.inventory.clearInventory(null, -1);
            archer.inventory.currentItem = 0;
            archer.inventory.setInventorySlotContents(0, bow);
            archer.inventory.setInventorySlotContents(9, quiver);
            archer.setItemInUse(bow, bow.getMaxItemUseDuration());

            bow.getItem().onPlayerStoppedUsing(bow, archer.worldObj, archer, bow.getMaxItemUseDuration() - 30);

            ItemStack left = ((IArrowContainer2) quiver.getItem()).getStackInSlot(quiver, 0);
            assertEquals("the quiver shot instead of the loaded bow", 10, left == null ? 0 : left.stackSize);
            ItemStack loaded = AmmoMechanicsMF.getAmmo(bow);
            assertEquals("the loaded arrow was not shot", 4, loaded == null ? 0 : loaded.stackSize);
        }
        helper.succeed();
    }

    /** Other bows are left alone: a vanilla bow shoots an arrow from the inventory and uses it up. */
    @GameTest
    public static void aVanillaBowShootsAndSpendsAnArrow(GameTestHelper helper) {
        ItemStack bow = new ItemStack(Items.bow);
        FakePlayer archer = Modders.fresh(helper, Modders.SMITH);
        archer.inventory.clearInventory(null, -1);
        archer.inventory.currentItem = 0;
        archer.inventory.setInventorySlotContents(0, bow);
        archer.inventory.setInventorySlotContents(9, new ItemStack(Items.arrow, 5));
        archer.setItemInUse(bow, bow.getMaxItemUseDuration());

        bow.getItem().onPlayerStoppedUsing(bow, archer.worldObj, archer, bow.getMaxItemUseDuration() - 30);

        ItemStack left = archer.inventory.getStackInSlot(9);
        assertEquals("the vanilla bow did not shoot", 4, left == null ? 0 : left.stackSize);
        helper.succeed();
    }
}
