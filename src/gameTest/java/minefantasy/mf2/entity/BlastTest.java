package minefantasy.mf2.entity;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import minefantasy.mf2.api.helpers.ArmourCalculator;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.gametest.Players;

/**
 * A bomb hurts what is near and is spent; a filling throws fragments that strike as projectiles, which armour stops,
 * burning for fire, and a wall stops them.
 */
@GameTestHolder("minefantasy2")
public class BlastTest {

    private BlastTest() {}

    /** Notes every attack on a living thing. */
    public static final class NoteAttacks {

        public final List<DamageSource> sources = new ArrayList<>();
        private float fragmentDamage;

        @SubscribeEvent
        public void attacked(LivingAttackEvent event) {
            sources.add(event.source);
            if (isFragment(event.source)) fragmentDamage += event.ammount;
        }

        private static boolean isFragment(DamageSource source) {
            return source.isProjectile() && !source.isExplosion();
        }

        DamageSource first(boolean fragment) {
            for (DamageSource source : sources) {
                if (fragment ? isFragment(source) : source.isExplosion()) return source;
            }
            throw new AssertionError(fragment ? "no fragment struck" : "the blast struck nothing");
        }

        /** Whether a fragment struck: a projectile that is not the blast. Otherwise, whether the blast did. */
        boolean any(boolean fragment) {
            for (DamageSource source : sources) {
                if (fragment ? isFragment(source) : source.isExplosion()) return true;
            }
            return false;
        }
    }

    /** A pig with the bomb inside it, so every fragment crosses it. */
    private static NoteAttacks blowUpInsideAPig(GameTestHelper helper, byte filling, EntityPig[] pigOut) {
        EntityPig pig = new EntityPig(helper.getWorld());
        pigOut[0] = pig;
        return blowUpInside(helper, pig, bomb(helper, filling, (byte) 0));
    }

    /** The bomb goes off inside the target, so every fragment crosses it. */
    private static NoteAttacks blowUpInside(GameTestHelper helper, EntityLivingBase target, EntityBomb bomb) {
        World world = helper.getWorld();
        TestPos at = helper.absolute(2, 2, 2);
        target.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        world.spawnEntityInWorld(target);
        bomb.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        NoteAttacks note = new NoteAttacks();
        try (Overrides overrides = new Overrides().listen(note)) {
            bomb.explode();
            assertTrue("the bomb was not spent", bomb.isDead);
        } finally {
            target.setDead();
        }
        return note;
    }

    private static EntityBomb bomb(GameTestHelper helper, byte filling, byte casing) {
        return new EntityBomb(helper.getWorld()).setType(filling, casing, (byte) 0, (byte) 0);
    }

    /** Health a golem, too tough to die of it, loses to this bomb. */
    private static float golemLoses(GameTestHelper helper, EntityBomb bomb) {
        EntityIronGolem golem = new EntityIronGolem(helper.getWorld());
        float health = golem.getHealth();
        blowUpInside(helper, golem, bomb);
        assertTrue("the golem died, so the loss does not count it all", golem.getHealth() > 0F);
        return health - golem.getHealth();
    }

    @GameTest
    public static void aFireBombHurtsBurnsAndStrikesWithFragments(GameTestHelper helper) {
        EntityPig[] pig = new EntityPig[1];
        NoteAttacks note = blowUpInsideAPig(helper, (byte) 2, pig);
        assertTrue("the blast did not hurt the pig", note.any(false));
        assertTrue("no fragment struck the pig as a projectile", note.any(true));
        assertTrue("the pig was not set alight", pig[0].isBurning());
        helper.succeed();
    }

    @GameTest
    public static void aBasicBombThrowsNoFragments(GameTestHelper helper) {
        NoteAttacks note = blowUpInsideAPig(helper, (byte) 0, new EntityPig[1]);
        assertTrue("the blast did not hurt the pig", note.any(false));
        assertFalse("a bomb without a filling threw fragments", note.any(true));
        helper.succeed();
    }

    @GameTest
    public static void aWallStopsAFragment(GameTestHelper helper) {
        World world = helper.getWorld();
        for (int y = 1; y < 4; y++) helper.setBlock(4, y, 2, Blocks.stone);
        TestPos at = helper.absolute(2, 2, 2);
        Vec3 start = Vec3.createVectorHelper(at.x() + 0.5D, at.y() + 0.5D, at.z() + 0.5D);
        Vec3 end = Fragments.end(world, start, Vec3.createVectorHelper(1D, 0D, 0D), 10D);
        assertEquals("the fragment did not stop at the wall", at.x() + 2D, end.xCoord, 1.0E-6D);
        helper.succeed();
    }

    @GameTest
    public static void bothSidesTraceTheSameFragments(GameTestHelper helper) {
        Vec3[] once = Fragments.directions(42L, 24, true);
        Vec3[] again = Fragments.directions(42L, 24, true);
        for (int i = 0; i < once.length; i++) {
            assertEquals(once[i].xCoord, again[i].xCoord, 0D);
            assertEquals(once[i].zCoord, again[i].zCoord, 0D);
            assertTrue("a mine threw a fragment into the ground", once[i].yCoord > 0D);
            assertEquals("not of unit length", 1D, once[i].lengthVector(), 1.0E-6D);
        }
        helper.succeed();
    }

    /** Fragments land after the blast, and the blast's moment of invulnerability does not swallow them. */
    @GameTest
    public static void fragmentsHurtOnTopOfTheBlast(GameTestHelper helper) {
        float plain = golemLoses(helper, bomb(helper, (byte) 0, (byte) -1));
        float withFragments = golemLoses(helper, bomb(helper, (byte) 1, (byte) -1));
        assertTrue(
                "fragments added nothing to the blast: " + plain + " against " + withFragments,
                withFragments > plain * 2);
        helper.succeed();
    }

    /** The blast is blunt, as every explosion; fragments also pierce, as the old shrapnel did. */
    @GameTest
    public static void fragmentsStrikeAsBluntAndPiercing(GameTestHelper helper) {
        NoteAttacks shrapnel = blowUpInsideAPig(helper, (byte) 1, new EntityPig[1]);
        assertEquals("[0.0, 1.0, 0.0]", Arrays.toString(ArmourCalculator.getRatioForSource(shrapnel.first(false))));
        assertEquals("[0.0, 1.0, 1.0]", Arrays.toString(ArmourCalculator.getRatioForSource(shrapnel.first(true))));
        helper.succeed();
    }

    @GameTest
    public static void fragmentsGrowWithTheCasing(GameTestHelper helper) {
        NoteAttacks crude = blowUpInside(
                helper,
                new EntityIronGolem(helper.getWorld()),
                bomb(helper, (byte) 1, (byte) -1));
        NoteAttacks ceramic = blowUpInside(
                helper,
                new EntityIronGolem(helper.getWorld()),
                bomb(helper, (byte) 1, (byte) 0));
        assertTrue("no fragment struck", crude.fragmentDamage > 0F);
        assertEquals("a ceramic casing doubles a crude one", crude.fragmentDamage * 2F, ceramic.fragmentDamage, 0.01F);
        helper.succeed();
    }

    /** Fragments add no knockback of their own: a pig beside the bomb is shoved away from it only. */
    @GameTest
    public static void fragmentsDoNotShoveTowardTheThrower(GameTestHelper helper) {
        World world = helper.getWorld();
        TestPos at = helper.absolute(2, 2, 2);
        EntityPlayerMP thrower = Players.unconnected(helper, "mf2_fragmenter");
        // West of the pig, so a shove away from the thrower pushes the pig the wrong way
        thrower.setPosition(at.x() - 3.5D, at.y(), at.z() + 0.5D);
        EntityBomb bomb = new EntityBomb(world, thrower).setType((byte) 1, (byte) 0, (byte) 0, (byte) 0);
        // Inside the pig's east side, so every fragment strikes it and the blast pushes it west
        bomb.setPosition(at.x() + 0.8D, at.y(), at.z() + 0.5D);
        EntityPig pig = new EntityPig(world);
        pig.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        world.spawnEntityInWorld(pig);
        NoteAttacks note = new NoteAttacks();
        try (Overrides overrides = new Overrides().listen(note)) {
            bomb.explode();
            assertTrue("no fragment struck the pig", note.any(true));
            assertTrue("the pig was shoved toward the bomb's side", pig.motionX < 0D);
            assertTrue("fragments added their own shove", pig.motionX >= -Blast.PUSH);
        } finally {
            pig.setDead();
        }
        helper.succeed();
    }

    /** Fragments thrown from the ground fly up and clear of it, for a real world as for the directions alone. */
    @GameTest
    public static void aMinesFragmentsFlyUp(GameTestHelper helper) {
        World world = helper.getWorld();
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(x, 1, z, Blocks.stone);
        TestPos at = helper.absolute(2, 2, 2);
        Vec3 centre = Vec3.createVectorHelper(at.x() + 0.5D, at.y() + 0.1D, at.z() + 0.5D);
        for (Vec3 direction : Fragments.directions(world.rand.nextLong(), 64, true)) {
            Vec3 end = Fragments.end(world, centre, direction, 4D);
            assertTrue("a fragment went into the ground", end.yCoord > centre.yCoord);
        }
        helper.succeed();
    }

    /** Blindness on a player the blast hurt, or null. */
    private static PotionEffect stunAt(GameTestHelper helper, double away, String name) {
        World world = helper.getWorld();
        TestPos at = helper.absolute(2, 2, 2);
        EntityPlayerMP player = Players.connected(helper, name, null);
        player.setPosition(at.x() + 0.5D + away, at.y(), at.z() + 0.5D);
        world.spawnEntityInWorld(player);
        // A player just joined is untouchable for a few seconds
        ReflectionHelper.setPrivateValue(EntityPlayerMP.class, player, 0, "field_147101_bU");
        try {
            EntityBomb bomb = bomb(helper, (byte) 0, (byte) 0);
            bomb.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
            bomb.explode();
            return player.getActivePotionEffect(Potion.blindness);
        } finally {
            world.removeEntity(player);
        }
    }

    /** A blast stuns a player it hurts, the closer the longer, and nobody past its radius. */
    @GameTest
    public static void aBlastStunsAPlayerItHurts(GameTestHelper helper) {
        PotionEffect near = stunAt(helper, 0.5D, "mf2_stunned_near");
        PotionEffect edge = stunAt(helper, 3.5D, "mf2_stunned_edge");
        PotionEffect far = stunAt(helper, 12D, "mf2_stunned_far");
        assertNotNull("a blast beside the player did not stun", near);
        assertNotNull("a blast that hurt at its edge did not stun", edge);
        assertTrue("a closer blast stunned no longer", near.getDuration() > edge.getDuration());
        assertNull("a blast stunned past its radius", far);
        helper.succeed();
    }
}
