package minefantasy.mf2.entity;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.config.ConfigWeapon;
import minefantasy.mf2.gametest.Overrides;
import minefantasy.mf2.gametest.Players;
import minefantasy.mf2.item.list.ToolListMF;

/**
 * Bombs and mines: what sets them off early and what does not, whom a mine springs for, whose blast it is after a
 * reload, and which way the blast shoves.
 */
@GameTestHolder("minefantasy2")
public class ExplosiveTest {

    private ExplosiveTest() {}

    /** An armed mine lying on a stone floor in the middle of the test area. */
    private static EntityMine armedMine(GameTestHelper helper, EntityLivingBase placer) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            helper.setBlock(x, 1, z, Blocks.stone);
            for (int y = 2; y < 5; y++) helper.setBlock(x, y, z, Blocks.air);
        }
        TestPos at = helper.absolute(2, 2, 2);
        EntityMine mine = placer == null ? new EntityMine(helper.getWorld())
                : new EntityMine(helper.getWorld(), placer);
        mine.setPosition(at.x() + 0.5D, at.y() + 0.125D, at.z() + 0.5D);
        mine.motionX = mine.motionY = mine.motionZ = 0;
        mine.fuse = 0;
        return mine;
    }

    private static boolean sprung(EntityMine mine) {
        return mine.fuse < 0;
    }

    private static EntityPig pigAt(GameTestHelper helper, double x, double z) {
        TestPos at = helper.absolute(0, 2, 0);
        EntityPig pig = new EntityPig(helper.getWorld());
        pig.setPosition(at.x() + x, at.y(), at.z() + z);
        helper.getWorld().spawnEntityInWorld(pig);
        return pig;
    }

    // region setting off

    @GameTest
    public static void aPlayersArrowOrBlowSetsOffAMine(GameTestHelper helper) {
        EntityPlayerMP archer = Players.unconnected(helper, "mf2_archer");
        EntityMine shot = armedMine(helper, null);
        EntityArrow arrow = new EntityArrow(helper.getWorld(), archer, 1F);
        assertTrue("the arrow passed through", shot.attackEntityFrom(DamageSource.causeArrowDamage(arrow, archer), 4F));
        assertTrue("a player's arrow did not set the mine off", sprung(shot));
        assertSame("the blast is the archer's, for protection to weigh", archer, shot.damageSource().getEntity());

        EntityMine struck = armedMine(helper, null);
        struck.attackEntityFrom(DamageSource.causePlayerDamage(archer), 1F);
        assertTrue("a player's blow did not set the mine off", sprung(struck));
        helper.succeed();
    }

    @GameTest
    public static void fireSetsOffAMine(GameTestHelper helper) {
        EntityMine mine = armedMine(helper, null);
        mine.setFire(5);
        assertTrue("catching fire did not set the mine off", sprung(mine));
        helper.succeed();
    }

    @GameTest
    public static void aCreatureOrAnotherBlastLeavesAMineBe(GameTestHelper helper) {
        EntitySkeleton skeleton = new EntitySkeleton(helper.getWorld());
        EntityMine mine = armedMine(helper, null);
        EntityArrow arrow = new EntityArrow(helper.getWorld(), skeleton, 1F);
        assertFalse(mine.attackEntityFrom(DamageSource.causeArrowDamage(arrow, skeleton), 4F));
        assertFalse(mine.attackEntityFrom(DamageSource.causeMobDamage(skeleton), 4F));
        assertFalse(mine.attackEntityFrom(DamageSource.setExplosionSource(null), 20F));
        assertFalse("a creature or a blast set the mine off", sprung(mine));
        helper.succeed();
    }

    @GameTest
    public static void aSetOffBombBurstsWithinAFewTicks(GameTestHelper helper) {
        EntityPlayerMP player = Players.unconnected(helper, "mf2_batter");
        EntityBomb bomb = new EntityBomb(helper.getWorld()).setType((byte) 0, (byte) 0, (byte) 1, (byte) 0);
        assertTrue(bomb.fuse > 3);
        bomb.attackEntityFrom(DamageSource.causePlayerDamage(player), 1F);
        assertTrue("striking the bomb left its fuse long", bomb.fuse <= 3);
        helper.succeed();
    }

    // endregion

    // region springing a mine

    @GameTest
    public static void aMineSpringsForACreatureButNotWhoeverSetIt(GameTestHelper helper) {
        EntityPig placer = pigAt(helper, 3.5D, 2.5D);
        EntityMine mine;
        try (Overrides overrides = owner(ConfigWeapon.MineOwnerTrigger.NEVER, 1F)) {
            mine = armedMine(helper, placer);
            mine.onUpdate();
            assertFalse("the mine sprang for whoever set it", sprung(mine));
        } finally {
            placer.setDead();
        }

        EntityPig pig = pigAt(helper, 3.5D, 2.5D);
        try {
            mine.onUpdate();
            assertTrue("the mine did not spring for a pig beside it", sprung(mine));
        } finally {
            pig.setDead();
        }
        helper.succeed();
    }

    private static Overrides owner(ConfigWeapon.MineOwnerTrigger mode, float chance) {
        return new Overrides()
                .set(() -> ConfigWeapon.mineOwnerTrigger, value -> ConfigWeapon.mineOwnerTrigger = value, mode)
                .set(() -> ConfigWeapon.mineOwnerChance, value -> ConfigWeapon.mineOwnerChance = value, chance);
    }

    @GameTest
    public static void aMineCanTreatItsOwnerAsAnyoneElse(GameTestHelper helper) {
        EntityPig placer = pigAt(helper, 3.5D, 2.5D);
        try (Overrides overrides = owner(ConfigWeapon.MineOwnerTrigger.ALWAYS, 0F)) {
            EntityMine mine = armedMine(helper, placer);
            mine.onUpdate();
            assertTrue("the mine spared its owner though set to treat them as anyone", sprung(mine));
        } finally {
            placer.setDead();
        }
        helper.succeed();
    }

    /** A forgetful owner is rolled for once as they come within reach, not at every look while they stay. */
    @GameTest
    public static void aForgetfulOwnerIsRolledForOncePerApproach(GameTestHelper helper) {
        EntityPig placer = pigAt(helper, 3.5D, 2.5D);
        try (Overrides overrides = owner(ConfigWeapon.MineOwnerTrigger.FORGETFUL, 0F)) {
            EntityMine mine = armedMine(helper, placer);
            mine.onUpdate();
            assertFalse("the mine sprang though the roll failed", sprung(mine));

            ConfigWeapon.mineOwnerChance = 1F;
            mine.onUpdate();
            assertFalse("the mine rolled again while its owner stayed", sprung(mine));

            TestPos far = helper.absolute(0, 2, 0);
            placer.setPosition(far.x() + 30D, far.y(), far.z());
            mine.onUpdate();
            placer.setPosition(far.x() + 3.5D, far.y(), far.z() + 2.5D);
            mine.onUpdate();
            assertTrue("the mine did not roll for its owner coming back", sprung(mine));
        } finally {
            placer.setDead();
        }
        helper.succeed();
    }

    // endregion

    // region disarming

    private static EntityPlayerMP sapper(GameTestHelper helper, ItemStack held) {
        EntityPlayerMP player = Players.unconnected(helper, "mf2_sapper");
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, held);
        player.setSneaking(true);
        return player;
    }

    private static int minesHeld(EntityPlayerMP player) {
        int mines = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && stack.getItem() == ToolListMF.mine_custom) mines += stack.stackSize;
        }
        return mines;
    }

    @GameTest
    public static void engineeringToolsDisarmAMineAndWearByAUse(GameTestHelper helper) {
        EntityMine mine = armedMine(helper, null);
        EntityPlayerMP player = sapper(helper, new ItemStack(ToolListMF.engin_anvil_tools));
        assertTrue("the tools did not take the mine", mine.interactFirst(player));
        assertTrue("the mine stayed armed", mine.isDead);
        assertEquals("the mine did not come back", 1, minesHeld(player));
        assertEquals("the tools did not wear", 1, player.getHeldItem().getItemDamage());
        helper.succeed();
    }

    @GameTest
    public static void aMineStaysWithoutToolsOrOnceSprung(GameTestHelper helper) {
        EntityMine bare = armedMine(helper, null);
        EntityPlayerMP emptyHanded = sapper(helper, null);
        assertFalse(bare.interactFirst(emptyHanded));
        assertFalse("a bare hand disarmed the mine", bare.isDead);
        assertEquals(0, minesHeld(emptyHanded));

        EntityPlayerMP standing = sapper(helper, new ItemStack(ToolListMF.engin_anvil_tools));
        standing.setSneaking(false);
        assertFalse("disarmed without crouching", bare.interactFirst(standing));

        EntityMine sprung = armedMine(helper, null);
        sprung.setOff(null);
        EntityPlayerMP late = sapper(helper, new ItemStack(ToolListMF.engin_anvil_tools));
        assertFalse("a sprung mine was disarmed", sprung.interactFirst(late));
        assertEquals(0, minesHeld(late));
        helper.succeed();
    }

    // endregion

    // region blasts nearby

    /** A blast sets off a crude casing nearby, too poorly made to hold, and leaves a better one be. */
    @GameTest
    public static void aBlastSetsOffOnlyACrudeCasingNearby(GameTestHelper helper) {
        EntityMine crude = armedMine(helper, null).setType((byte) 0, (byte) -1, (byte) 0, (byte) 0);
        EntityMine ceramic = armedMine(helper, null).setType((byte) 0, (byte) 0, (byte) 0, (byte) 0);
        TestPos at = helper.absolute(2, 2, 2);
        crude.setPosition(at.x() + 1.5D, at.y() + 0.125D, at.z() + 0.5D);
        ceramic.setPosition(at.x() - 0.5D, at.y() + 0.125D, at.z() + 0.5D);
        crude.fuse = ceramic.fuse = 0;
        World world = helper.getWorld();
        world.spawnEntityInWorld(crude);
        world.spawnEntityInWorld(ceramic);
        EntityBomb bomb = new EntityBomb(world).setType((byte) 0, (byte) 0, (byte) 0, (byte) 0);
        bomb.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        try {
            bomb.explode();
            assertTrue("a crude casing held through a blast beside it", sprung(crude));
            assertFalse("a ceramic casing went off with the blast", sprung(ceramic));
        } finally {
            crude.setDead();
            ceramic.setDead();
        }
        helper.succeed();
    }

    // endregion

    // region springing a mine continued

    @GameTest
    public static void aWallKeepsAMineFromSpringing(GameTestHelper helper) {
        EntityMine mine = armedMine(helper, null);
        for (int z = 0; z < 5; z++) for (int y = 2; y < 5; y++) helper.setBlock(3, y, z, Blocks.stone);
        EntityPig pig = pigAt(helper, 4.5D, 2.5D);
        try {
            mine.onUpdate();
            assertFalse("the mine sprang for a pig behind a wall", sprung(mine));
        } finally {
            pig.setDead();
        }
        helper.succeed();
    }

    @GameTest
    public static void aLandedMineLiesStillUntilItsGroundGoes(GameTestHelper helper) {
        EntityMine mine = armedMine(helper, null);
        mine.fuse = 40;
        mine.motionY = -0.2D;
        for (int tick = 1; tick <= 5; tick++) {
            mine.ticksExisted = tick;
            mine.onUpdate();
        }
        assertTrue("the mine did not land on the floor", mine.isLanded());
        double y = mine.posY;
        mine.ticksExisted = 6;
        mine.onUpdate();
        assertEquals("a landed mine moved", y, mine.posY, 0D);

        helper.setBlock(2, 1, 2, Blocks.air);
        mine.ticksExisted = 10;
        mine.onUpdate();
        assertFalse("the mine stayed landed on nothing", mine.isLanded());
        mine.ticksExisted = 11;
        mine.onUpdate();
        assertTrue("the mine did not fall", mine.posY < y);
        helper.succeed();
    }

    // endregion

    // region blast

    /** Notes the source of the first attack on a living thing. */
    public static final class NoteAttack {

        public DamageSource source;

        @SubscribeEvent
        public void attacked(LivingAttackEvent event) {
            if (source == null) source = event.source;
        }
    }

    @GameTest
    public static void aReloadedBombStillHurtsAsWhoeverThrewIt(GameTestHelper helper) {
        World world = helper.getWorld();
        EntityPlayerMP thrower = Players.unconnected(helper, "mf2_grenadier");
        TestPos at = helper.absolute(2, 2, 2);
        NBTTagCompound saved = new NBTTagCompound();
        new EntityBomb(world, thrower).setType((byte) 0, (byte) 0, (byte) 0, (byte) 0).writeToNBT(saved);
        EntityBomb bomb = new EntityBomb(world);
        bomb.readFromNBT(saved);
        bomb.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);

        EntityPig pig = pigAt(helper, 3.5D, 2.5D);
        NoteAttack note = new NoteAttack();
        try (Overrides overrides = new Overrides().listen(note)
                .run(() -> world.playerEntities.add(thrower), () -> world.playerEntities.remove(thrower))) {
            bomb.explode();
        } finally {
            pig.setDead();
        }
        assertNotNull("the bomb hurt the pig", note.source);
        assertSame("the reloaded bomb's damage is no longer the thrower's", thrower, note.source.getEntity());
        helper.succeed();
    }

    /** The blast shoves away from the bomb, not from whoever threw it, and no harder than a plain hit. */
    @GameTest
    public static void theBlastShovesAwayFromTheBomb(GameTestHelper helper) {
        World world = helper.getWorld();
        EntityPlayerMP thrower = Players.unconnected(helper, "mf2_shover");
        TestPos at = helper.absolute(2, 2, 2);
        EntityBomb bomb = new EntityBomb(world, thrower).setType((byte) 0, (byte) 0, (byte) 0, (byte) 0);
        bomb.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        // The thrower stands beyond the pig, so a push away from the thrower goes the other way
        thrower.setPosition(at.x() + 4.5D, at.y(), at.z() + 0.5D);
        EntityPig pig = pigAt(helper, 3.5D, 2.5D);
        try {
            bomb.explode();
            assertTrue("the pig was not shoved away from the bomb", pig.motionX > 0D);
            assertTrue("the shove was harder than a plain hit", pig.motionX <= Blast.PUSH);
        } finally {
            pig.setDead();
        }
        helper.succeed();
    }

    // endregion
}
