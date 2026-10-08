package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.archery.AmmoMechanicsMF;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.TacticalManager;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.gadget.ItemClimbingPick;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * An item used from the offhand (Backhand) is in use without being the held item. Code that runs while it is held in
 * use must ask for the item in use: here the item is put in use from a slot other than the current one, as Backhand
 * does, with something else in the main hand.
 */
@GameTestHolder("minefantasy2")
public class OffhandUseTest {

    private static final int OFFHAND = 5;

    private OffhandUseTest() {}

    /** A player facing along +z with a stick in the main hand and the given item in another slot. */
    private static FakePlayer holding(GameTestHelper helper, ItemStack offhand, double z) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        TestPos at = helper.absolute(3, 1, 3);
        player.setPositionAndRotation(at.x() + 0.5, at.y(), at.z() + z, 0F, 0F);
        player.rotationYawHead = 0F;
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, new ItemStack(Items.stick));
        player.inventory.setInventorySlotContents(OFFHAND, offhand);
        return player;
    }

    private interface Check {

        void run();
    }

    private static void withoutStamina(Check check) {
        boolean stamina = StaminaBar.isSystemActive;
        StaminaBar.isSystemActive = false;
        try {
            check.run();
        } finally {
            StaminaBar.isSystemActive = stamina;
        }
    }

    @GameTest
    public static void aSwordBlockingInTheOffhandParries(GameTestHelper helper) {
        withoutStamina(() -> {
            ItemStack sword = CustomToolHelper.construct(CustomToolListMF.standard_sword, "iron", "oakwood");
            FakePlayer player = holding(helper, sword, 0.5);
            player.inventory.setInventorySlotContents(0, new ItemStack(Items.iron_pickaxe));
            player.setItemInUse(sword, 72000);
            assertTrue("the offhand sword does not block", player.isBlocking());
            assertSame(sword, TacticalManager.parryingWeapon(player));

            EntityZombie zombie = new EntityZombie(helper.getWorld());
            TestPos front = helper.absolute(3, 1, 5);
            zombie.setPosition(front.x() + 0.5, front.y(), front.z() + 0.5);
            float through = Parrying.parry(player, zombie, DamageSource.causeMobDamage(zombie), 4F, true);
            assertTrue("the blow was not parried with the offhand sword: " + through, through < 4F);
        });
        helper.succeed();
    }

    @GameTest
    public static void anOffhandClimbingPickMarksItselfNotTheHeldItem(GameTestHelper helper) {
        withoutStamina(() -> {
            helper.setBlock(3, 2, 4, Blocks.stone);
            ItemStack pick = new ItemStack(ToolListMF.climbing_pick_basic);
            FakePlayer player = holding(helper, pick, 0.9);
            ItemStack held = player.getHeldItem();

            pick.getItem().onItemRightClick(pick, player.worldObj, player);

            assertFalse("the held item was marked by the pick", held.hasTagCompound());
            assertTrue(
                    "the pick did not grip the wall",
                    pick.hasTagCompound() && pick.getTagCompound().hasKey("MF_HeldPosX"));
        });
        helper.succeed();
    }

    @GameTest
    public static void aWornOutOffhandPickDoesNotDestroyTheHeldItem(GameTestHelper helper) {
        ItemStack pick = new ItemStack(ToolListMF.climbing_pick_basic);
        pick.setItemDamage(pick.getMaxDamage());
        FakePlayer player = holding(helper, pick, 0.5);
        player.setItemInUse(pick, 72000);

        ((ItemClimbingPick) pick.getItem()).onUsingTick(pick, player, 71990);

        assertNotNull("the held item was destroyed for the worn-out pick", player.getHeldItem());
        assertFalse("climbing went on with a worn-out pick", player.isUsingItem());
        helper.succeed();
    }

    @GameTest
    public static void aFirearmBreakingInTheOffhandLeavesTheHeldItem(GameTestHelper helper) {
        ItemStack bow = new ItemStack(CustomToolListMF.standard_bow);
        bow.setItemDamage(bow.getMaxDamage());
        FakePlayer player = holding(helper, bow, 0.5);

        AmmoMechanicsMF.damageContainer(bow, player, 1);

        assertNotNull("the held item was lost for the broken offhand bow", player.getHeldItem());
        assertNull("the broken bow stayed in its slot", player.inventory.getStackInSlot(OFFHAND));
        helper.succeed();
    }

    @GameTest
    public static void drawingAnOffhandBowTires(GameTestHelper helper) {
        FakePlayer player = holding(helper, new ItemStack(Items.bow), 0.5);
        float idle = StaminaMechanics.getConstantDecay(player);
        player.setItemInUse(player.inventory.getStackInSlot(OFFHAND), 72000);
        assertTrue("drawing a bow in the offhand did not tire", StaminaMechanics.getConstantDecay(player) > idle);
        helper.succeed();
    }
}
