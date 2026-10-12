package minefantasy.mf2.entity;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.config.ConfigArmour;
import minefantasy.mf2.gametest.DenyPlacing;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.gametest.Watcher;

/** A ridden cogwork treads grass into dirt; its rider's client must see what the server kept. */
@GameTestHolder("minefantasy2")
public class CogworkTreadTest {

    private CogworkTreadTest() {}

    /** A suit whose every roll comes up: each tread while moving steps on the block under it. */
    private static final class Steady extends EntityCogwork {

        Steady(World world, double x, double y, double z) {
            super(world, x, y, z);
            rand = new Random() {

                @Override
                public int nextInt(int bound) {
                    return 0;
                }
            };
        }
    }

    /** One step forward, which treads on the block under the suit. */
    private static void walk(EntityCogwork cog) {
        cog.motionX = 0.1D;
        CogworkTrampling.tread(cog);
    }

    private static EntityCogwork ridden(World world, TestPos ground, Watcher rider) {
        EntityCogwork cog = new Steady(world, ground.x() + 0.5D, ground.y() + 1, ground.z() + 0.5D);
        cog.riddenByEntity = rider.player;
        rider.player.ridingEntity = cog;
        return cog;
    }

    @GameTest
    public static void treadingShowsTheDirt(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.grass);
        TestPos ground = helper.absolute(1, 1, 1);
        World world = helper.getWorld();
        try (Overrides overrides = new Overrides()
                .set(() -> ConfigArmour.cogworkGrief, v -> ConfigArmour.cogworkGrief = v, true);
                Watcher rider = new Watcher(helper, ground.x() + 0.5D, ground.y() + 2, ground.z() + 0.5D)) {
            walk(ridden(world, ground, rider));
            assertEquals(Blocks.dirt, world.getBlock(ground.x(), ground.y(), ground.z()));
            rider.flush();
            assertEquals("the rider sees dirt", Blocks.dirt, rider.shown(ground.x(), ground.y(), ground.z()));
        }
        helper.succeed();
    }

    @GameTest
    public static void aProtectedTreadShowsTheGrassBack(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.grass);
        TestPos ground = helper.absolute(1, 1, 1);
        World world = helper.getWorld();
        DenyPlacing deny = new DenyPlacing();
        try (Overrides overrides = new Overrides()
                .set(() -> ConfigArmour.cogworkGrief, v -> ConfigArmour.cogworkGrief = v, true).listen(deny);
                Watcher rider = new Watcher(helper, ground.x() + 0.5D, ground.y() + 2, ground.z() + 0.5D)) {
            walk(ridden(world, ground, rider));
            assertEquals("the place event was not asked", 1, deny.refused);
            assertEquals(Blocks.grass, world.getBlock(ground.x(), ground.y(), ground.z()));
            rider.flush();
            assertEquals("the rider sees grass", Blocks.grass, rider.shown(ground.x(), ground.y(), ground.z()));
        }
        helper.succeed();
    }
}
