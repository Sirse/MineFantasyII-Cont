package minefantasy.mf2.item.archery;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import minefantasy.mf2.api.archery.IArrowHandler;
import minefantasy.mf2.api.archery.ISpecialBow;
import minefantasy.mf2.entity.EntityArrowMF;

public class ArrowFireFlint implements IArrowHandler {

    @Override
    public boolean onFireArrow(World world, ItemStack arrow, ItemStack bow, EntityPlayer user, float charge,
            boolean infinite) {
        if (arrow == null || arrow.getItem() != Items.arrow) {
            return false;
        }
        float firepower = charge;

        if (firepower < 0.1D) {
            return false;
        }
        if (firepower > 1.0F) {
            firepower = 1.0F;
        }

        EntityArrowMF entArrow = new EntityArrowMF(world, user, firepower * 2.0F);
        entArrow.setArrow(new ItemStack(Items.arrow));

        int powerLvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, bow);
        if (powerLvl > 0) {
            entArrow.setPower(1.0F + 0.25F * powerLvl);
        }

        int var10 = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, bow);

        if (var10 > 0) {
            entArrow.setKnockbackStrength(var10);
        }

        if (EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, bow) > 0) {
            entArrow.setFire(100);
        }

        if (infinite) {
            entArrow.canBePickedUp = 2;
        }

        if (bow != null && bow.getItem() instanceof ISpecialBow) {
            entArrow = (EntityArrowMF) ((ISpecialBow) bow.getItem()).modifyArrow(bow, entArrow);
        }
        if (!world.isRemote) {
            world.spawnEntityInWorld(entArrow);
        }

        return true;
    }
}
