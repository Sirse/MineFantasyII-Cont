package minefantasy.mf2.entity.mob;

import static minefantasy.mf2.gametest.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.entity.EntityDragonBreath;
import minefantasy.mf2.gametest.Difficulty;
import minefantasy.mf2.gametest.Players;

/** A dragon's hunt goes as it should: it bites, flies, breaks off and comes back without the faults it once had. */
@GameTestHolder("minefantasy2")
public class DragonHuntTest {

    private DragonHuntTest() {}

    /** Ground for the prey, and loaded chunks well around: a creature only moves with every chunk near it loaded. */
    private static void ground(GameTestHelper helper) {
        TestPos centre = helper.absolute(0, 0, 0);
        WorldServer world = (WorldServer) helper.getWorld();
        for (int cx = (centre.x() - 128) >> 4; cx <= (centre.x() + 128) >> 4; cx++) {
            for (int cz = (centre.z() - 128) >> 4; cz <= (centre.z() + 128) >> 4; cz++) {
                world.theChunkProviderServer.loadChunk(cx, cz);
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                helper.setBlock(x, 0, z, Blocks.stone);
            }
        }
    }

    /** Prey that outlasts the test: a dragon's bite or breath would end a plain zombie. */
    private static EntityZombie prey(GameTestHelper helper, double x, double y, double z) {
        EntityZombie prey = new EntityZombie(helper.getWorld());
        prey.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(100000.0D);
        prey.setHealth(100000.0F);
        prey.setPosition(x, y, z);
        helper.getWorld().spawnEntityInWorld(prey);
        return prey;
    }

    /** A dragon after the prey from its first tick: tests side by side have prey of their own a dragon might pick. */
    private static EntityDragon dragon(GameTestHelper helper, Entity prey, double x, double y, double z,
            boolean flying) {
        EntityDragon dragon = new EntityDragon(helper.getWorld());
        dragon.setTarget(prey);
        dragon.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        dragon.setDragon(0);
        dragon.setTerrestrial(!flying);
        helper.getWorld().spawnEntityInWorld(dragon);
        return dragon;
    }

    /**
     * A bite tosses the one bitten, not whatever the dragon is after. The bitten is kept from being knocked back by the
     * blow, which would overwrite the toss.
     */
    @GameTest
    public static void aBiteTossesOnlyTheOneBitten(GameTestHelper helper) {
        Difficulty.hold(helper);
        ground(helper);
        TestPos at = helper.absolute(0, 1, 0);
        EntityZombie hunted = prey(helper, at.x() + 0.5D, at.y(), at.z() + 16.5D);
        EntityZombie bitten = prey(helper, at.x() + 4.5D, at.y(), at.z() + 16.5D);
        EntityDragon dragon = dragon(helper, hunted, at.x() + 0.5D, at.y(), at.z() + 20.5D, false);
        // One bite in three tosses: thirty leave next to no chance of none
        boolean tossed = false;
        for (int i = 0; i < 30; i++) {
            hunted.motionY = bitten.motionY = 0.0D;
            bitten.hurtResistantTime = bitten.maxHurtResistantTime;
            dragon.attackEntity(bitten, 0.0F);
            assertTrue("a bite tossed the dragon's target instead of the one bitten", hunted.motionY == 0.0D);
            tossed |= bitten.motionY > 1.0D;
        }
        assertTrue("thirty bites never tossed the one bitten", tossed);
        helper.succeed();
    }

    /** Water across a dragon's course blocks it, as the course check means. */
    @GameTest
    public static void waterAcrossTheCourseBlocksIt(GameTestHelper helper) {
        Difficulty.hold(helper);
        ground(helper);
        WorldServer world = (WorldServer) helper.getWorld();
        // High above the test ground, clear of any test's blocks
        TestPos at = helper.absolute(0, 50, 0);
        EntityDragon dragon = dragon(helper, null, at.x() + 0.5D, at.y(), at.z() + 0.5D, true);
        dragon.waypointX = at.x() + 20.5D;
        dragon.waypointY = at.y();
        dragon.waypointZ = at.z() + 0.5D;
        assertTrue(
                "open air blocked the course",
                dragon.isCourseTraversable(dragon.waypointX, dragon.waypointY, dragon.waypointZ, 20.0D));
        // Set without updates and checked at once, before it can flow
        for (int y = -4; y <= 6; y++) {
            for (int z = -5; z <= 5; z++) {
                world.setBlock(at.x() + 10, at.y() + y, at.z() + z, Blocks.water, 0, 2);
            }
        }
        assertTrue(
                "water across the course did not block it",
                !dragon.isCourseTraversable(dragon.waypointX, dragon.waypointY, dragon.waypointZ, 20.0D));
        for (int y = -4; y <= 6; y++) {
            for (int z = -5; z <= 5; z++) {
                world.setBlock(at.x() + 10, at.y() + y, at.z() + z, Blocks.air, 0, 2);
            }
        }
        helper.succeed();
    }

    /** Gives a young dragon the breed with this breath. */
    private static void breed(EntityDragon dragon, DragonBreath breath) {
        for (int id = 0; id < 4; id++) {
            dragon.setBreed(id);
            if (dragon.getType().rangedAttack == breath) return;
        }
        throw new IllegalStateException("no young dragon breathes " + breath.name);
    }

    /**
     * A wounded dragon breaks off after its bite, and the tick goes on without the prey it let go: the rest of its hunt
     * once threw on the missing prey.
     */
    @GameTest(timeoutTicks = 100)
    public static void aWoundedDragonBreaksOffAfterItsBite(GameTestHelper helper) {
        Difficulty.hold(helper);
        ground(helper);
        TestPos at = helper.absolute(0, 1, 0);
        EntityZombie prey = prey(helper, at.x() + 0.5D, at.y(), at.z() + 2.5D);
        EntityDragon dragon = dragon(helper, prey, at.x() + 0.5D, at.y(), at.z() + 0.5D, false);
        // Helmed, so it does not burn by day
        prey.setCurrentItemOrArmor(4, new ItemStack(Items.iron_helmet));
        // Below a quarter of its health, where it breaks off for 50 ticks after every bite
        dragon.setHealth(dragon.getMaxHealth() / 5.0F);
        helper.succeedWhen(() -> {
            prey.setPosition(at.x() + 0.5D, at.y(), at.z() + 2.5D);
            int time = dragon.getDisengageTime();
            if (time > 0 && time <= 50) return !dragon.isDead;
            // Any other break-off, a wound taken meanwhile, is not the one under test
            if (time > 0) dragon.setDisengageTime(0);
            dragon.setTarget(prey);
            return false;
        });
    }

    /** A dragon hunting its prey still rises over what blocks its way, rather than its hunt steering it back. */
    @GameTest
    public static void aHuntingDragonRisesOverAWall(GameTestHelper helper) {
        Difficulty.hold(helper);
        ground(helper);
        WorldServer world = (WorldServer) helper.getWorld();
        // High above the test ground, clear of any test's blocks: a ring of stone round the dragon, open above and
        // below
        TestPos at = helper.absolute(0, 90, 0);
        EntityZombie prey = prey(helper, at.x() + 0.5D, at.y() - 14.0D, at.z() + 0.5D);
        EntityDragon dragon = dragon(helper, prey, at.x() + 0.5D, at.y(), at.z() + 0.5D, true);
        breed(dragon, DragonBreath.fire);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 4) continue;
                for (int dy = -1; dy <= 3; dy++) {
                    world.setBlock(at.x() + dx, at.y() + dy, at.z() + dz, Blocks.stone, 0, 2);
                }
            }
        }
        helper.afterTest(() -> {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    for (int dy = -1; dy <= 3; dy++) {
                        world.setBlock(at.x() + dx, at.y() + dy, at.z() + dz, Blocks.air, 0, 2);
                    }
                }
            }
        });
        // Circling at its height, the next point round the circle lies beyond the ring
        dragon.waypointX = at.x() + 18.5D;
        dragon.waypointY = at.y();
        dragon.waypointZ = at.z() + 0.5D;
        dragon.courseChangeCooldown = 0;
        dragon.updateEntityActionState();
        assertTrue("the dragon lost its prey", dragon.getTarget() == prey);
        assertTrue(
                "the hunt steered the dragon back from rising, its waypoint " + (dragon.waypointY - dragon.posY)
                        + " above it",
                dragon.waypointY > dragon.posY + EntityDragon.CLIMB / 2.0D);
        helper.succeed();
    }

    /**
     * A dragon called over a player hunts that player first, though another stands nearer when it comes down. The
     * dragon is held where it came, so the other stays the nearer.
     */
    @GameTest(timeoutTicks = 200)
    public static void aCalledDragonHuntsThePlayerItWasCalledTo(GameTestHelper helper) {
        Difficulty.hold(helper);
        ground(helper);
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(2, y, 0, Blocks.stone);
        }
        TestPos at = helper.absolute(0, 1, 0);
        EntityPlayerMP called = Players.connected(helper, "Called", null);
        called.setPositionAndUpdate(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        EntityPlayerMP nearer = Players.connected(helper, "Nearer", null);
        nearer.setPositionAndUpdate(at.x() + 2.5D, at.y() + 3.0D, at.z() + 0.5D);
        // A step since the last call, which the call waits for
        called.distanceWalkedOnStepModified = called.getEntityData().getInteger("MF_LastStep") + 1;
        Set<Object> before = new HashSet<>(helper.getWorld().loadedEntityList);
        minefantasy.mf2.mechanics.PlayerTickHandlerMF.spawnDragon(called, 3);
        EntityDragon dragon = null;
        for (Object entity : helper.getWorld().loadedEntityList) {
            if (entity instanceof EntityDragon && !before.contains(entity)) dragon = (EntityDragon) entity;
        }
        assertTrue("no dragon came", dragon != null);
        EntityDragon came = dragon;
        double x = came.posX, y = came.posY, z = came.posZ;
        helper.succeedWhen(() -> {
            came.setPosition(x, y, z);
            came.motionX = came.motionY = came.motionZ = 0.0D;
            called.setPositionAndUpdate(at.x() + 0.5D, at.y(), at.z() + 0.5D);
            nearer.setPositionAndUpdate(at.x() + 2.5D, at.y() + 3.0D, at.z() + 0.5D);
            if (came.getTarget() == null) return false;
            assertTrue(
                    "the called dragon went for " + came.getTarget().getCommandSenderName(),
                    came.getTarget() == called);
            return true;
        });
    }

    /**
     * A breath never goes out past where the dragon's head can turn: prey that leaps behind it as it breathes is not
     * breathed at back over its shoulder. The dragon is held where it stands; its body may turn after the prey.
     */
    @GameTest(timeoutTicks = 300)
    public static void aBreathStaysWithinTheHeadsReach(GameTestHelper helper) {
        Difficulty.hold(helper);
        ground(helper);
        TestPos at = helper.absolute(0, 1, 0);
        EntityZombie prey = prey(helper, at.x() + 0.5D, at.y() + 8.0D, at.z() + 16.5D);
        EntityDragon dragon = dragon(helper, prey, at.x() + 0.5D, at.y(), at.z() + 0.5D, false);
        Set<Object> seen = new HashSet<>();
        int[] breaths = { 0 };
        helper.succeedWhen(() -> {
            dragon.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
            dragon.motionX = dragon.motionY = dragon.motionZ = 0.0D;
            dragon.setTarget(prey);
            for (Object entity : helper.getWorld().loadedEntityList) {
                if (!(entity instanceof EntityDragonBreath) || !seen.add(entity)) continue;
                EntityDragonBreath breath = (EntityDragonBreath) entity;
                if (breath.shootingEntity != dragon) continue;
                float off = MathHelper.wrapAngleTo180_float(
                        EntityDragon.heading(breath.accelerationX, breath.accelerationZ) - dragon.renderYawOffset);
                // The breath's own spread on top of the head's reach
                assertTrue(
                        "a breath went out " + off + " degrees from the body",
                        Math.abs(off) < EntityDragon.HEAD_ARC + 15.0F);
                breaths[0]++;
            }
            // Once it breathes, the prey leaps behind it
            prey.setPosition(at.x() + 0.5D, at.y() + 8.0D, at.z() + (breaths[0] > 0 ? -15.5D : 16.5D));
            prey.motionX = prey.motionY = prey.motionZ = 0.0D;
            return breaths[0] >= 40;
        });
    }
}
