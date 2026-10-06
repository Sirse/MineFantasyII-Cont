package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.mechanics.ProtectionFixtures.*;

import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.world.ExplosionEvent;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.entity.EntityFireBlast;
import minefantasy.mf2.entity.EntityMine;
import minefantasy.mf2.entity.Shockwave;

/**
 * Fire blasts, explosions, shockwaves and mines change blocks and hurt as whoever is behind them may: a player as
 * protection allows, a creature as mobGriefing allows, no one never.
 */
@GameTestHolder("minefantasy2")
public class BlastProtectionTest {

    private BlastProtectionTest() {}

    @GameTest
    public static void aBlastWithNoOneBehindItChangesNoBlock(GameTestHelper helper) {
        GameRules rules = helper.getWorld().getGameRules();
        String griefing = rules.getGameRuleStringValue("mobGriefing");
        rules.setOrCreateGameRule("mobGriefing", "true");
        try {
            helper.setBlock(1, 1, 1, Blocks.glass);
            TestPos at = helper.absolute(1, 1, 1);
            EntityFireBlast ownerless = new EntityFireBlast(helper.getWorld(), at.x(), at.y(), at.z(), 0, 0, 0);
            assertFalse("no one's rights to ask, even with mobGriefing", ownerless.mayChange(at.x(), at.y(), at.z()));
            assertFalse(ownerless.placeFire(at.x(), at.y() + 1, at.z()));

            EntityFireBlast shot = new EntityFireBlast(helper.getWorld(), unconnected(helper), 0, 0, 0);
            assertTrue("a player's blast where they may break", shot.mayChange(at.x(), at.y(), at.z()));
            NBTTagCompound saved = new NBTTagCompound();
            shot.writeEntityToNBT(saved);
            EntityFireBlast loaded = new EntityFireBlast(helper.getWorld());
            loaded.readEntityFromNBT(saved);
            assertFalse("a blast loaded from disk acts for no one", loaded.mayChange(at.x(), at.y(), at.z()));
        } finally {
            rules.setOrCreateGameRule("mobGriefing", griefing);
        }
        endOfTick(helper);
        helper.succeed();
    }

    @GameTest
    public static void ownerlessBlastDoesNotGriefTntEvenWhenMobGriefingIsOn(GameTestHelper helper) {
        GameRules rules = helper.getWorld().getGameRules();
        String griefing = rules.getGameRuleStringValue("mobGriefing");
        rules.setOrCreateGameRule("mobGriefing", "true");
        try {
            helper.setBlock(2, 2, 2, Blocks.tnt);
            TestPos at = helper.absolute(2, 2, 2);
            EntityFireBlast blast = new EntityFireBlast(
                    helper.getWorld(),
                    at.x() + 0.5,
                    at.y() + 0.5,
                    at.z() + 0.5,
                    0,
                    0,
                    0);
            impact(
                    blast,
                    new MovingObjectPosition(
                            at.x(),
                            at.y(),
                            at.z(),
                            1,
                            Vec3.createVectorHelper(at.x() + 0.5, at.y() + 0.5, at.z() + 0.5)));
            assertEquals(
                    "ownerless TNT impact cannot destroy blocks",
                    Blocks.tnt,
                    helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        } finally {
            rules.setOrCreateGameRule("mobGriefing", griefing);
        }
        helper.succeed();
    }

    @GameTest
    public static void protectedBlocksAreRemovedFromAPlayersExplosion(GameTestHelper helper) {
        helper.setBlock(2, 2, 2, Blocks.stone);
        TestPos at = helper.absolute(2, 2, 2);
        EntityFireBlast blast = new EntityFireBlast(helper.getWorld(), unconnected(helper), 0, 0, 0);
        DenyAll deny = new DenyAll();
        MinecraftForge.EVENT_BUS.register(deny);
        try {
            helper.getWorld().newExplosion(blast, at.x() + 0.5D, at.y() + 0.5D, at.z() + 0.5D, 2.0F, false, true);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        assertEquals(
                "a denied explosion block remains",
                Blocks.stone,
                helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        helper.succeed();
    }

    /** Cancels every explosion before it starts, as a protection mod does. */
    public static final class CancelExplosions {

        @SubscribeEvent
        public void cancel(ExplosionEvent.Start event) {
            event.setCanceled(true);
        }
    }

    @GameTest
    public static void aCancelledExplosionBreaksAndBurnsNothing(GameTestHelper helper) {
        floor(helper);
        TestPos at = helper.absolute(2, 2, 2);
        CancelExplosions cancel = new CancelExplosions();
        MinecraftForge.EVENT_BUS.register(cancel);
        try {
            helper.getWorld().newExplosion(unconnected(helper), at.x() + 0.5D, at.y(), at.z() + 0.5D, 3F, true, true);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancel);
        }
        assertEquals("the floor stands", 25, count(helper, Blocks.stone));
        assertEquals("nothing burns", 0, count(helper, Blocks.fire));
        helper.succeed();
    }

    @GameTest
    public static void aBlastSetsNoFireWhereItsShooterMayNotBreak(GameTestHelper helper) {
        floor(helper);
        TestPos at = helper.absolute(2, 2, 2);
        EntityFireBlast blast = new EntityFireBlast(helper.getWorld(), unconnected(helper), 0, 0, 0);
        DenyAll deny = new DenyAll();
        MinecraftForge.EVENT_BUS.register(deny);
        try {
            for (int i = 0; i < 5; i++) {
                helper.getWorld().newExplosion(blast, at.x() + 0.5D, at.y(), at.z() + 0.5D, 3F, true, true);
            }
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        assertEquals("the floor stands", 25, count(helper, Blocks.stone));
        assertEquals("nothing burns", 0, count(helper, Blocks.fire));
        helper.succeed();
    }

    @GameTest
    public static void anotherExplosionIsLeftToItsOwnEvents(GameTestHelper helper) {
        floor(helper);
        TestPos at = helper.absolute(2, 2, 2);
        DenyAll deny = new DenyAll();
        MinecraftForge.EVENT_BUS.register(deny);
        try {
            // A player's own explosion raises no break event; its Start and Detonate events are for others to settle
            helper.getWorld().newExplosion(unconnected(helper), at.x() + 0.5D, at.y(), at.z() + 0.5D, 3F, false, true);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        assertTrue("MF took nothing out of it", count(helper, Blocks.stone) < 25);
        helper.succeed();
    }

    @GameTest
    public static void aCreatureChangesBlocksOnlyAsMobGriefingAllows(GameTestHelper helper) {
        GameRules rules = helper.getWorld().getGameRules();
        String griefing = rules.getGameRuleStringValue("mobGriefing");
        try {
            helper.setBlock(1, 1, 1, Blocks.stone);
            helper.setBlock(1, 2, 1, Blocks.air);
            TestPos fireAt = helper.absolute(1, 2, 1);
            EntityPig pig = new EntityPig(helper.getWorld());
            EntityFireBlast blast = new EntityFireBlast(helper.getWorld(), pig, 0, 0, 0);

            rules.setOrCreateGameRule("mobGriefing", "false");
            assertFalse("no griefing, no fire", blast.placeFire(fireAt.x(), fireAt.y(), fireAt.z()));
            assertFalse(
                    "no griefing, no breaking",
                    ProtectionHelper.canBreak(pig, helper.getWorld(), fireAt.x(), fireAt.y() - 1, fireAt.z()));
            assertEquals(Blocks.air, helper.getWorld().getBlock(fireAt.x(), fireAt.y(), fireAt.z()));

            rules.setOrCreateGameRule("mobGriefing", "true");
            assertTrue("griefing allows it", blast.placeFire(fireAt.x(), fireAt.y(), fireAt.z()));
            assertEquals(Blocks.fire, helper.getWorld().getBlock(fireAt.x(), fireAt.y(), fireAt.z()));
        } finally {
            rules.setOrCreateGameRule("mobGriefing", griefing);
        }
        helper.succeed();
    }

    @GameTest
    public static void aPlayersBlastSetsFireOnlyWhereTheyMayPlace(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.stone);
        helper.setBlock(1, 2, 1, Blocks.air);
        TestPos at = helper.absolute(1, 2, 1);
        EntityFireBlast blast = new EntityFireBlast(helper.getWorld(), unconnected(helper), 0, 0, 0);
        DenyPlacing deny = new DenyPlacing();
        MinecraftForge.EVENT_BUS.register(deny);
        try {
            assertFalse(blast.placeFire(at.x(), at.y(), at.z()));
            assertEquals("the place event was asked about the fire", Blocks.fire, deny.placed);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        assertEquals(Blocks.air, helper.getWorld().getBlock(at.x(), at.y(), at.z()));
        assertTrue("allowed, it burns", blast.placeFire(at.x(), at.y(), at.z()));
        helper.succeed();
    }

    /** Notes the source of the first attack on a living thing. */
    public static final class NoteAttack {

        public DamageSource source;

        @SubscribeEvent
        public void attacked(LivingAttackEvent event) {
            if (source == null) source = event.source;
        }
    }

    @GameTest
    public static void aMineHurtsAsWhoeverPlacedIt(GameTestHelper helper) {
        TestPos at = helper.absolute(2, 2, 2);
        EntityPlayerMP placer = unconnected(helper);
        EntityMine mine = new EntityMine(helper.getWorld(), placer);
        mine.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        EntityPig pig = new EntityPig(helper.getWorld());
        pig.setPosition(at.x() + 1.5D, at.y(), at.z() + 0.5D);
        helper.getWorld().spawnEntityInWorld(pig);
        NoteAttack note = new NoteAttack();
        MinecraftForge.EVENT_BUS.register(note);
        try {
            mine.explode();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(note);
            pig.setDead();
        }
        assertNotNull("the mine hurt the pig", note.source);
        assertEquals("mine", note.source.getDamageType());
        assertSame("the damage is the placer's, for protection to weigh", placer, note.source.getEntity());
        helper.succeed();
    }

    /** Cancels every attack on a living thing, as protection does where hurting is not allowed. */
    public static final class CancelAttacks {

        @SubscribeEvent
        public void cancel(LivingAttackEvent event) {
            event.setCanceled(true);
        }
    }

    private static Shockwave wave(GameTestHelper helper, TestPos at) {
        EntityPig stomper = new EntityPig(helper.getWorld());
        stomper.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        Shockwave wave = new Shockwave(
                "humanstomp",
                helper.getWorld(),
                stomper,
                at.x() + 0.5D,
                at.y(),
                at.z() + 0.5D,
                3F);
        wave.isGriefing = false;
        return wave;
    }

    private static EntityPig pigBeside(GameTestHelper helper, TestPos at) {
        EntityPig pig = new EntityPig(helper.getWorld());
        pig.setPosition(at.x() + 1.5D, at.y(), at.z() + 0.5D);
        helper.getWorld().spawnEntityInWorld(pig);
        return pig;
    }

    @GameTest
    public static void aRefusedAttackPushesNoOne(GameTestHelper helper) {
        TestPos at = helper.absolute(2, 2, 2);
        EntityPig pig = pigBeside(helper, at);
        EntityPlayerMP player = unconnected(helper);
        player.setPosition(at.x() - 0.5D, at.y(), at.z() + 0.5D);
        helper.getWorld().spawnEntityInWorld(player);
        float health = pig.getHealth();
        Shockwave wave = wave(helper, at);
        CancelAttacks cancel = new CancelAttacks();
        MinecraftForge.EVENT_BUS.register(cancel);
        try {
            wave.initiate();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancel);
            helper.getWorld().removePlayerEntityDangerously(player);
            pig.setDead();
        }
        assertEquals("the pig is not hurt", health, pig.getHealth());
        assertEquals("nor pushed", 0D, pig.motionX);
        assertEquals(0D, player.motionX);
        assertTrue("no player is to be thrown", wave.func_77277_b().isEmpty());
        helper.succeed();
    }

    @GameTest
    public static void anAllowedAttackHurtsAndPushes(GameTestHelper helper) {
        TestPos at = helper.absolute(2, 2, 2);
        EntityPig pig = pigBeside(helper, at);
        float health = pig.getHealth();
        try {
            wave(helper, at).initiate();
        } finally {
            pig.setDead();
        }
        assertTrue("the pig is hurt", pig.getHealth() < health);
        assertTrue("and pushed away", pig.motionX > 0D);
        helper.succeed();
    }
}
