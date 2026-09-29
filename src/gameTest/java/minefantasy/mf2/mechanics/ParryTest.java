package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.TacticalManager;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.gametest.Modders;
import minefantasy.mf2.item.list.CustomToolListMF;

/**
 * Parrying, as the book tells it: a blocking player turns aside a blow from in front, not from behind; a player who has
 * learned autoparry does it without blocking; and after a parry the next waits for its cooldown.
 */
@GameTestHolder("minefantasy2")
public class ParryTest {

    private ParryTest() {}

    /** A player with a sword, facing along +z. */
    private static FakePlayer fencer(GameTestHelper helper, boolean blocking) {
        FakePlayer player = Modders.fresh(helper, Modders.SMITH);
        TestPos at = helper.absolute(3, 1, 3);
        player.setPositionAndRotation(at.x() + 0.5, at.y(), at.z() + 0.5, 0F, 0F);
        player.rotationYawHead = 0F;
        ItemStack sword = CustomToolHelper.construct(CustomToolListMF.standard_sword, "iron", "oakwood");
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, sword);
        if (blocking) {
            player.setItemInUse(sword, 72000);
        }
        return player;
    }

    /** A zombie two blocks along z from the player: in front for +2, behind for -2. */
    private static EntityZombie zombie(GameTestHelper helper, int dz) {
        EntityZombie zombie = new EntityZombie(helper.getWorld());
        TestPos at = helper.absolute(3, 1, 3 + dz);
        zombie.setPosition(at.x() + 0.5, at.y(), at.z() + 0.5);
        return zombie;
    }

    private static boolean parries(FakePlayer player, EntityZombie zombie) {
        return TacticalManager.canParry(DamageSource.causeMobDamage(zombie), player, zombie, player.getHeldItem());
    }

    private interface Check {

        void run();
    }

    /** With the stamina system off, so a fresh player's empty stamina does not decide it. */
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
    public static void aBlockTurnsAsideABlowFromInFrontOnly(GameTestHelper helper) {
        withoutStamina(() -> {
            FakePlayer player = fencer(helper, true);
            assertTrue("the fake player does not block", player.isBlocking());
            assertTrue("a blow from in front was not parried", parries(player, zombie(helper, 2)));
            assertFalse("a blow from behind was parried", parries(player, zombie(helper, -2)));
        });
        helper.succeed();
    }

    @GameTest
    public static void withoutABlockOnlyAutoparryParries(GameTestHelper helper) {
        withoutStamina(() -> {
            FakePlayer player = fencer(helper, false);
            assertFalse("a player not blocking parried", parries(player, zombie(helper, 2)));
            ResearchLogic.forceUnlock(player, ResearchLogic.getResearch("autoparry"));
            assertTrue("autoparry did not parry", parries(player, zombie(helper, 2)));
        });
        helper.succeed();
    }

    @GameTest
    public static void aParryWaitsForItsCooldown(GameTestHelper helper) {
        withoutStamina(() -> {
            FakePlayer player = fencer(helper, true);
            Parrying.setParryCooldown(player, 20);
            assertFalse("a parry came during the cooldown", parries(player, zombie(helper, 2)));
            Parrying.setParryCooldown(player, 0);
            assertTrue(parries(player, zombie(helper, 2)));
        });
        helper.succeed();
    }
}
