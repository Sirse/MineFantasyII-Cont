package minefantasy.mf2.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.entity.EntityCogwork;
import minefantasy.mf2.entity.EntityDragonBreath;
import minefantasy.mf2.mechanics.ElementalWear;

public class PoisonBreath extends DragonBreath {

    public PoisonBreath(String name) {
        super(name);
    }

    @Override
    public DamageSource getDamageSource(EntityDragonBreath breath, EntityLivingBase shooter) {
        return shooter == null ? new DamageSource("acidblastBase")
                : (new EntityDamageSourceIndirect("acidblast", breath, shooter));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getTexture(EntityDragonBreath instance) {
        return "textures/projectile/dragonbreath_poison";
    }

    public void onHitEntity(Entity target, EntityDragonBreath instance) {
        super.onHitEntity(target, instance);
        if (target instanceof EntityLivingBase) {
            float dam = instance.getDamage();
            ((EntityLivingBase) target).addPotionEffect(new PotionEffect(Potion.poison.id, 40, (int) dam));
            ((EntityLivingBase) target).addPotionEffect(new PotionEffect(Potion.confusion.id, 50, 10));

            // Now and then the acid eats into the armour and what is held
            if (ElementalWear.acid && instance.rand.nextInt(10) == 0) {
                for (int slot = 0; slot <= 4; slot++) {
                    ElementalWear.wear((EntityLivingBase) target, slot, (int) dam);
                }
            }
        }
    }

    @Override
    public float modifyDamage(Entity hit, float dam) {
        if (hit instanceof EntityCogwork) {
            return dam * 2;
        }
        return dam;
    }

    @Override
    public void hitBlock(World world, EntityDragonBreath instance, int x, int y, int z, boolean impact) {}
}
