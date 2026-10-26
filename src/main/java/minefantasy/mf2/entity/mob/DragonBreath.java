package minefantasy.mf2.entity.mob;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.entity.EntityDragonBreath;

public abstract class DragonBreath {

    public static ArrayList<DragonBreath> projectiles = new ArrayList<DragonBreath>();

    public static int nextID = 0;
    public static DragonBreath fire, frost, poison, ash;
    public final String name;
    public int id = 0;
    protected Random random = new Random();
    public int circleTicks = 160, diveTicks = 100;
    public double circleRadius = 20.0D, circleHeight = 16.0D;

    public DragonBreath(String name) {
        this.name = name;
        register();
    }

    public static void init() {
        // Fire dives on its prey often; frost keeps high and away, slowing it with breath; poison flies low and close;
        // ash, blinding it, dives quick and short
        fire = new FireBreath("fire").hunts(120, 100, 18.0D, 14.0D);
        frost = new FrostBreath("frost").hunts(220, 80, 24.0D, 20.0D);
        poison = new PoisonBreath("poison").hunts(160, 100, 16.0D, 10.0D);
        ash = new AshBreath("ash").hunts(100, 60, 20.0D, 16.0D);
    }

    /**
     * How a dragon with this breath hunts in flight: ticks it circles its prey before diving, ticks a dive lasts at
     * most, and how wide and how high above the prey it circles.
     */
    public DragonBreath hunts(int circleTicks, int diveTicks, double circleRadius, double circleHeight) {
        this.circleTicks = circleTicks;
        this.diveTicks = diveTicks;
        this.circleRadius = circleRadius;
        this.circleHeight = circleHeight;
        return this;
    }

    public void register() {
        this.id = nextID;
        projectiles.add(this);
        ++nextID;
    }

    public abstract DamageSource getDamageSource(EntityDragonBreath breath, EntityLivingBase shooter);

    public boolean shouldExpand() {
        return true;
    }

    public int getLifeSpan() {
        return 30;
    }

    @SideOnly(Side.CLIENT)
    public abstract String getTexture(EntityDragonBreath instance);

    public void onHitEntity(Entity entityHit, EntityDragonBreath instance) {
        entityHit.attackEntityFrom(
                getDamageSource(instance, instance.shootingEntity),
                modifyDamage(entityHit, instance.getDamage()));
    }

    public float modifyDamage(Entity hit, float dam) {
        return dam;
    }

    public void hitBlock(World world, EntityDragonBreath instance, int x, int y, int z, boolean impact) {}
}
