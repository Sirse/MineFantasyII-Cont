package minefantasy.mf2.entity;

import static minefantasy.mf2.gametest.Assert.*;

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

    /** Treads until the block under the suit was asked about, as walking does over some ticks. */
    private static void walk(EntityCogwork cog, World world, TestPos ground, DenyPlacing deny) {
        for (int step = 0; step < 500; step++) {
            cog.motionX = 0.1D;
            CogworkTrampling.tread(cog);
            if (world.getBlock(ground.x(), ground.y(), ground.z()) != Blocks.grass || deny != null && deny.refused > 0)
                return;
        }
        throw new AssertionError("the cogwork never trod on the block");
    }

    private static EntityCogwork ridden(World world, TestPos ground, Watcher rider) {
        EntityCogwork cog = new EntityCogwork(world, ground.x() + 0.5D, ground.y() + 1, ground.z() + 0.5D);
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
            walk(ridden(world, ground, rider), world, ground, null);
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
            walk(ridden(world, ground, rider), world, ground, deny);
            assertEquals(Blocks.grass, world.getBlock(ground.x(), ground.y(), ground.z()));
            rider.flush();
            assertEquals("the rider sees grass", Blocks.grass, rider.shown(ground.x(), ground.y(), ground.z()));
        }
        helper.succeed();
    }
}
