package minefantasy.mf2.entity.mob;

import static minefantasy.mf2.gametest.Assert.assertTrue;

import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.gametest.Difficulty;
import minefantasy.mf2.gametest.Players;

/**
 * A dragon takes its time to die: its body stays for the whole fall the client draws, and only then is gone, leaving
 * the experience a kill by a player earns.
 */
@GameTestHolder("minefantasy2")
public class DragonDeathTest {

    private DragonDeathTest() {}

    /**
     * Ground under the dragon, as a dead one falls, and loaded chunks well around it: a living dragon may take off, and
     * the game only moves a creature with every chunk within 32 blocks of it loaded.
     */
    private static void floor(GameTestHelper helper) {
        TestPos centre = helper.absolute(3, 0, 3);
        WorldServer world = (WorldServer) helper.getWorld();
        for (int cx = (centre.x() - 96) >> 4; cx <= (centre.x() + 96) >> 4; cx++) {
            for (int cz = (centre.z() - 96) >> 4; cz <= (centre.z() + 96) >> 4; cz++) {
                world.theChunkProviderServer.loadChunk(cx, cz);
            }
        }
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                helper.setBlock(x, 0, z, Blocks.stone);
            }
        }
    }

    /**
     * A player's hit counts toward the kill for a while; the longer fall must not shorten that. Hit, then slain by
     * something else while the hit still counts as vanilla judges it, the dragon still gives experience.
     */
    @GameTest(timeoutTicks = 100 + EntityDragon.DEATH_TICKS + 40)
    public static void aPlayersHitStillCountsAfterTheLongerFall(GameTestHelper helper) {
        // A peaceful world sends dragons away at once
        Difficulty.hold(helper);
        floor(helper);
        TestPos at = helper.absolute(3, 1, 3);
        EntityDragon dragon = new EntityDragon(helper.getWorld());
        dragon.setLocationAndAngles(at.x() + 0.5D, at.y(), at.z() + 0.5D, 0.0F, 0.0F);
        dragon.setDragon(0);
        helper.getWorld().spawnEntityInWorld(dragon);
        dragon.setTerrestrial(true);
        EntityPlayerMP slayer = Players.connected(helper, "Slayer", null);
        slayer.setPositionAndUpdate(at.x() + 0.5D, at.y(), at.z() + 3.5D);
        int[] ticks = { -1 };
        helper.succeedWhen(() -> {
            // Held where it stands while it lives: flown off over a neighbouring test's ground it stops being moved
            if (dragon.getHealth() > 0.0F) {
                dragon.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
                dragon.motionX = dragon.motionY = dragon.motionZ = 0.0D;
            }
            // A dragon may dodge a blow: the player strikes until one lands, and the count starts from it
            if (ticks[0] < 0) {
                if (dragon.hurtTime > 0) {
                    ticks[0] = 0;
                } else {
                    dragon.attackEntityFrom(DamageSource.causePlayerDamage(slayer), 1.0F);
                }
                return false;
            }
            // Sixty ticks on, vanilla still counts the hit for forty more: past the end of a vanilla death
            if (++ticks[0] == 60) dragon.attackEntityFrom(DamageSource.outOfWorld, Float.MAX_VALUE);
            if (!dragon.isDead) return false;
            AxisAlignedBB around = AxisAlignedBB.getBoundingBox(
                    dragon.posX - 8,
                    dragon.posY - 8,
                    dragon.posZ - 8,
                    dragon.posX + 8,
                    dragon.posY + 8,
                    dragon.posZ + 8);
            assertTrue(
                    "the player's hit earned no experience",
                    !helper.getWorld().getEntitiesWithinAABB(EntityXPOrb.class, around).isEmpty());
            return true;
        });
    }

    /** A dragon saved and loaded again between settling its reward and paying it still pays it. */
    @GameTest(timeoutTicks = 100 + EntityDragon.DEATH_TICKS + 40)
    public static void aDragonSavedMidDeathKeepsItsReward(GameTestHelper helper) {
        Difficulty.hold(helper);
        floor(helper);
        TestPos at = helper.absolute(3, 1, 3);
        EntityDragon dragon = new EntityDragon(helper.getWorld());
        dragon.setLocationAndAngles(at.x() + 0.5D, at.y(), at.z() + 0.5D, 0.0F, 0.0F);
        dragon.setDragon(0);
        dragon.setTerrestrial(true);
        helper.getWorld().spawnEntityInWorld(dragon);
        EntityPlayerMP slayer = Players.connected(helper, "Slayer", null);

        EntityDragon[] loaded = { null };
        helper.succeedWhen(() -> {
            if (loaded[0] == null) {
                if (dragon.getHealth() > 0.0F) {
                    dragon.attackEntityFrom(DamageSource.causePlayerDamage(slayer), Float.MAX_VALUE);
                    return false;
                }
                // Past the tick the reward is settled, before it is paid: unloaded and loaded again
                if (dragon.deathTime < 30) return false;
                NBTTagCompound saved = new NBTTagCompound();
                dragon.writeToNBT(saved);
                dragon.setDead();
                loaded[0] = new EntityDragon(helper.getWorld());
                loaded[0].readFromNBT(saved);
                helper.getWorld().spawnEntityInWorld(loaded[0]);
                return false;
            }
            if (!loaded[0].isDead) return false;
            AxisAlignedBB around = AxisAlignedBB.getBoundingBox(
                    loaded[0].posX - 8,
                    loaded[0].posY - 8,
                    loaded[0].posZ - 8,
                    loaded[0].posX + 8,
                    loaded[0].posY + 8,
                    loaded[0].posZ + 8);
            assertTrue(
                    "the dragon loaded mid-death lost its reward",
                    !helper.getWorld().getEntitiesWithinAABB(EntityXPOrb.class, around).isEmpty());
            return true;
        });
    }
}
