package minefantasy.mf2.item.weapon;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("minefantasy2")
public class AxeCombatTest {

    @GameTest
    public static void bothAxesPreserveChargeJumpAndLungeEffects(GameTestHelper helper) {
        boolean waraxe = false, battleaxe = false;
        for (Object value : Item.itemRegistry) {
            if (!(value instanceof ItemWaraxeMF) && !(value instanceof ItemBattleaxeMF)) continue;
            if (value instanceof ItemWaraxeMF && waraxe || value instanceof ItemBattleaxeMF && battleaxe) continue;
            waraxe |= value instanceof ItemWaraxeMF;
            battleaxe |= value instanceof ItemBattleaxeMF;
            ItemWeaponMF axe = (ItemWeaponMF) value;
            EntityZombie user = new EntityZombie(helper.getWorld());
            EntityZombie target = new EntityZombie(helper.getWorld());
            user.setCurrentItemOrArmor(0, new ItemStack(axe));
            user.onGround = target.onGround = true;
            ItemStack stack = user.getHeldItem();
            assertEquals(10F, axe.modifyDamage(stack, user, target, 10F, true), 0F);
            user.setSprinting(true);
            assertEquals(12.5F, axe.modifyDamage(stack, user, target, 10F, false), 0F);
            assertFalse(target.isPotionActive(Potion.moveSlowdown));
            assertEquals("inactive grounded branch must stay inactive", 0, user.hurtResistantTime);
            assertEquals(12.5F, axe.modifyDamage(stack, user, target, 10F, true), 0F);
            assertEquals(200, target.getActivePotionEffect(Potion.moveSlowdown).getDuration());
            // Repeating a hit must not refresh an existing slow effect.
            target.addPotionEffect(new net.minecraft.potion.PotionEffect(Potion.moveSlowdown.id, 123, 4));
            axe.modifyDamage(stack, user, target, 10F, true);
            assertEquals(123, target.getActivePotionEffect(Potion.moveSlowdown).getDuration());
            target.removePotionEffect(Potion.moveSlowdown.id);
            user.setSprinting(false);
            user.fallDistance = 2F;
            assertEquals(12.5F, axe.modifyDamage(stack, user, target, 10F, true), 0F);
            assertFalse(target.isPotionActive(Potion.moveSlowdown));
            user.setSprinting(true);
            float bonus = Math.min(2F, axe.getMeleeDamage(stack)) / 2F;
            assertEquals((10F + bonus) * 1.25F, axe.modifyDamage(stack, user, target, 10F, true), 0F);
            assertEquals(2F - bonus, user.fallDistance, 0F);
            assertEquals(400, target.getActivePotionEffect(Potion.moveSlowdown).getDuration());
        }
        assertTrue("waraxe not exercised", waraxe);
        assertTrue("battleaxe not exercised", battleaxe);
        helper.succeed();
    }
}
