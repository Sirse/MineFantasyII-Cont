package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import cpw.mods.fml.common.Loader;
import minefantasy.mf2.api.archery.AmmoMechanicsMF;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.config.ConfigWeapon;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.item.weapon.ItemWeaponMF;
import xonin.backhand.api.core.BackhandUtils;

/**
 * The offhand through Backhand itself: the item sits in its real offhand slot and is used the way Backhand uses it, by
 * making that slot current for the right click only. What runs afterwards, while the item is held in use, sees the main
 * hand as held. Without Backhand these tests have nothing to check.
 * <p>
 * Swords are left out: Backhand never raises an ItemSword while the offhand holds anything, so neither hand blocks.
 */
@GameTestHolder("minefantasy2")
public class BackhandOffhandTest {

    private BackhandOffhandTest() {}

    private interface Check {

        void run();
    }

    /** Runs the check only with Backhand, with the stamina system off so a fresh player's empty bar does not decide. */
    private static void withBackhand(GameTestHelper helper, Check check) {
        if (Loader.isModLoaded("backhand")) {
            boolean stamina = StaminaBar.isSystemActive;
            StaminaBar.isSystemActive = false;
            try {
                check.run();
            } finally {
                StaminaBar.isSystemActive = stamina;
            }
        }
        helper.succeed();
    }

    /** A player facing along +z, with the main hand item held and the offhand item in Backhand's slot. */
    private static FakePlayer wielding(GameTestHelper helper, ItemStack main, ItemStack offhand, double z) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        player.inventory.clearInventory(null, -1);
        TestPos at = helper.absolute(3, 1, 3);
        player.setPositionAndRotation(at.x() + 0.5, at.y(), at.z() + z, 0F, 0F);
        player.rotationYawHead = 0F;
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, main);
        BackhandUtils.setPlayerOffhandItem(player, offhand);
        return player;
    }

    /** Right clicks with the offhand item as Backhand does, and returns the main hand to the current slot. */
    private static void useOffhand(FakePlayer player) {
        BackhandUtils.useOffhandItem(player, false, () -> {
            ItemStack held = player.getHeldItem();
            held.getItem().onItemRightClick(held, player.worldObj, player);
        });
    }

    @GameTest
    public static void aBowDrawnInTheOffhandTires(GameTestHelper helper) {
        withBackhand(helper, () -> {
            ItemStack bow = new ItemStack(Items.bow);
            FakePlayer player = wielding(helper, new ItemStack(Items.stick), bow, 0.5);
            player.inventory.setInventorySlotContents(9, new ItemStack(Items.arrow, 16));
            float idle = StaminaMechanics.getConstantDecay(player);
            useOffhand(player);

            assertTrue("the offhand bow was not drawn", player.isUsingItem());
            assertTrue("drawing the offhand bow did not tire", StaminaMechanics.getConstantDecay(player) > idle);
        });
    }

    @GameTest
    public static void aFirearmBreakingInTheOffhandLeavesTheHeldItem(GameTestHelper helper) {
        withBackhand(helper, () -> {
            ItemStack stick = new ItemStack(Items.stick);
            ItemStack bow = new ItemStack(CustomToolListMF.standard_bow);
            bow.setItemDamage(bow.getMaxDamage());
            FakePlayer player = wielding(helper, stick, bow, 0.5);

            // Fired on release, which Backhand does not run with the offhand as the current slot
            AmmoMechanicsMF.damageContainer(bow, player, 1);

            assertSame("the held item was lost for the broken offhand bow", stick, player.getHeldItem());
            assertNull("the broken bow stayed in the offhand", BackhandUtils.getOffhandItem(player));
        });
    }

    @GameTest
    public static void aHeavyWeaponInTheOffhandWeighsOnStamina(GameTestHelper helper) {
        withBackhand(helper, () -> {
            FakePlayer player = wielding(helper, new ItemStack(Items.stick), null, 0.5);
            float light = StaminaBar.getBaseDecayModifier(player, false, true);
            BackhandUtils.setPlayerOffhandItem(
                    player,
                    CustomToolHelper.construct(CustomToolListMF.standard_greatsword, "iron", "oakwood"));

            assertTrue(
                    "a greatsword in the offhand weighs nothing",
                    StaminaBar.getBaseDecayModifier(player, false, true) > light);
        });
    }

    /** Strikes a zombie with the held weapon, the stamina system on, and returns the stamina it cost. */
    private static float swing(FakePlayer player) {
        StaminaBar.isSystemActive = true;
        try {
            float before = StaminaBar.getStaminaValue(player);
            ItemStack held = player.getHeldItem();
            held.getItem().onLeftClickEntity(held, player, new EntityZombie(player.worldObj));
            return before - StaminaBar.getStaminaValue(player);
        } finally {
            StaminaBar.isSystemActive = false;
        }
    }

    @GameTest
    public static void aTwoHandedSwingAtTheAirKeepsBalance(GameTestHelper helper) {
        withBackhand(helper, () -> {
            ItemStack greatsword = CustomToolHelper.construct(CustomToolListMF.standard_greatsword, "iron", "oakwood");
            FakePlayer player = wielding(helper, greatsword, new ItemStack(Items.stick), 0.5);
            float pitch = player.rotationPitch;
            player.swingItem();
            assertEquals("a swing at the air threw the player off balance", pitch, player.rotationPitch, 0.001F);
        });
    }

    @GameTest
    public static void aTwoHandedSwingWithAFullOffhandLosesBalance(GameTestHelper helper) {
        withBackhand(helper, () -> {
            ItemStack greatsword = CustomToolHelper.construct(CustomToolListMF.standard_greatsword, "iron", "oakwood");
            FakePlayer player = wielding(helper, greatsword, null, 0.5);
            assertEquals("an empty offhand cost stamina", 0F, swing(player), 0.001F);

            BackhandUtils.setPlayerOffhandItem(player, new ItemStack(Items.stick));
            float pitch = player.rotationPitch;
            assertTrue("a full offhand cost no stamina", swing(player) > 0F);
            // Off balance tilts the view at once or, with the new balance system, through entity data
            assertTrue(
                    "the player kept their balance",
                    player.rotationPitch != pitch || player.getEntityData().getFloat("MF_Balance_Pitch") != 0F);
        });
    }

    @GameTest
    public static void withoutWeaponBalanceAFullOffhandOnlyTires(GameTestHelper helper) {
        withBackhand(helper, () -> {
            boolean balance = ConfigWeapon.useBalance;
            ConfigWeapon.useBalance = false;
            try {
                ItemStack greatsword = CustomToolHelper
                        .construct(CustomToolListMF.standard_greatsword, "iron", "oakwood");
                FakePlayer player = wielding(helper, greatsword, new ItemStack(Items.stick), 0.5);
                float pitch = player.rotationPitch;
                assertTrue("a full offhand cost no stamina", swing(player) > 0F);
                assertEquals("the view was thrown off with balance turned off", pitch, player.rotationPitch, 0.001F);
                assertEquals(0F, player.getEntityData().getFloat("MF_Balance_Pitch"), 0.001F);
            } finally {
                ConfigWeapon.useBalance = balance;
            }
        });
    }

    @GameTest
    public static void aKatanaSwingsWithAFullOffhand(GameTestHelper helper) {
        withBackhand(helper, () -> {
            ItemStack katana = CustomToolHelper.construct(CustomToolListMF.standard_katana, "iron", "oakwood");
            FakePlayer player = wielding(helper, katana, new ItemStack(Items.stick), 0.5);
            assertEquals("the katana was penalised for the offhand", 0F, swing(player), 0.001F);
        });
    }

    private static ItemStack weapon(ItemWeaponMF item) {
        return CustomToolHelper.construct(item, "iron", "oakwood");
    }

    @GameTest
    public static void dualWieldingDodgesWithoutABlock(GameTestHelper helper) {
        withBackhand(helper, () -> {
            FakePlayer pair = wielding(
                    helper,
                    weapon(CustomToolListMF.standard_sword),
                    weapon(CustomToolListMF.standard_dagger),
                    0.5);
            assertTrue("two swords could not dodge", Dodging.canDodge(pair));

            FakePlayer heavy = wielding(
                    helper,
                    weapon(CustomToolListMF.standard_greatsword),
                    weapon(CustomToolListMF.standard_sword),
                    0.5);
            assertFalse("a greatsword and a sword dodged", Dodging.canDodge(heavy));

            FakePlayer torch = wielding(
                    helper,
                    weapon(CustomToolListMF.standard_sword),
                    new ItemStack(Items.stick),
                    0.5);
            assertFalse("a sword and a stick dodged without a block", Dodging.canDodge(torch));
        });
    }

    @GameTest
    public static void anOffhandClimbingPickGripsWithoutMarkingTheHeldItem(GameTestHelper helper) {
        withBackhand(helper, () -> {
            helper.setBlock(3, 2, 4, Blocks.stone);
            ItemStack stick = new ItemStack(Items.stick);
            ItemStack pick = new ItemStack(ToolListMF.climbing_pick_basic);
            FakePlayer player = wielding(helper, stick, pick, 0.9);
            useOffhand(player);

            assertTrue("the offhand pick did not grip the wall", player.isUsingItem());
            assertSame("the main hand is not held again", stick, player.getHeldItem());
            pick.getItem().onUsingTick(pick, player, 71990);
            assertTrue("climbing stopped while the pick holds the wall", player.isUsingItem());
            assertFalse("the held item was marked by the pick", stick.hasTagCompound());
        });
    }
}
