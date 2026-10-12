package minefantasy.mf2.entity;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.passive.EntityPig;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

/** A bomb hurts what is near, a fire filling sets it alight and throws burning shrapnel, and the bomb is spent. */
@GameTestHolder("minefantasy2")
public class BlastTest {

    private BlastTest() {}

    @GameTest
    public static void aFireBombHurtsBurnsAndThrowsShrapnel(GameTestHelper helper) {
        World world = helper.getWorld();
        TestPos at = helper.absolute(2, 2, 2);
        EntityBomb bomb = new EntityBomb(world).setType((byte) 2, (byte) 0, (byte) 0, (byte) 0);
        bomb.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        EntityPig pig = new EntityPig(world);
        pig.setPosition(at.x() + 1.5D, at.y(), at.z() + 0.5D);
        world.spawnEntityInWorld(pig);
        AxisAlignedBB around = AxisAlignedBB
                .getBoundingBox(at.x() - 4, at.y() - 4, at.z() - 4, at.x() + 5, at.y() + 5, at.z() + 5);
        try {
            float health = pig.getHealth();
            bomb.explode();
            assertTrue("the pig was not hurt", pig.getHealth() < health);
            assertTrue("the pig was not set alight", pig.isBurning());
            assertTrue("the bomb was not spent", bomb.isDead);
            assertEquals("shrapnel", 16, world.getEntitiesWithinAABB(EntityShrapnel.class, around).size());
        } finally {
            pig.setDead();
            for (Object shrapnel : world.getEntitiesWithinAABB(EntityShrapnel.class, around)) {
                ((EntityShrapnel) shrapnel).setDead();
            }
        }
        helper.succeed();
    }
}
