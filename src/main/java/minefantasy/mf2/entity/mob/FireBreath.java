package minefantasy.mf2.entity.mob;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.config.ConfigMobs;
import minefantasy.mf2.entity.EntityDragonBreath;
import minefantasy.mf2.mechanics.ProtectionHelper;

public class FireBreath extends DragonBreath {

    public FireBreath(String name) {
        super(name);
    }

    @Override
    public DamageSource getDamageSource(EntityDragonBreath breath, EntityLivingBase shooter) {
        return shooter == null ? new DamageSource("fireblastBase").setFireDamage()
                : (new EntityDamageSourceIndirect("fireblast", breath, shooter)).setFireDamage();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getTexture(EntityDragonBreath instance) {
        return "textures/projectile/dragonbreath_fire";
    }

    public void onHitEntity(Entity target, EntityDragonBreath instance) {
        super.onHitEntity(target, instance);
        if (target instanceof EntityLivingBase) {
            target.setFire(5);
        }
    }

    @Override
    public float modifyDamage(Entity hit, float dam) {
        return dam;
    }

    @Override
    public void hitBlock(World world, EntityDragonBreath instance, int x, int y, int z, boolean impact) {
        if (world.isRemote || !impact && instance.rand.nextInt(20) != 0 || !world.isAirBlock(x, y + 1, z)) return;
        // The dragon behind the breath changes blocks as a creature may: mobGriefing decides
        Entity dragon = instance.shootingEntity;
        Block hit = world.getBlock(x, y, z);
        if (ConfigMobs.dragonGriefFire && hit.isSideSolid(world, x, y, z, ForgeDirection.UP)
                && hit.isFlammable(world, x, y + 1, z, ForgeDirection.UP)) {
            ProtectionHelper.placeBlock(dragon, world, x, y + 1, z, Blocks.fire, 0);
        }
        if (!ConfigMobs.dragonGriefGeneral) return;
        if (hit == Blocks.ice) {
            ProtectionHelper.replaceBlock(dragon, world, x, y, z, Blocks.water, 0);
        } else if (hit.getMaterial() == Material.glass && ProtectionHelper.breakBlock(dragon, world, x, y, z)) {
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "dig.glass", 1.0F, 1.0F);
        }
    }
}
