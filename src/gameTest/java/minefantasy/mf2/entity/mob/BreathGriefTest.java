package minefantasy.mf2.entity.mob;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.config.ConfigMobs;
import minefantasy.mf2.entity.EntityDragonBreath;
import minefantasy.mf2.gametest.Overrides;

/** Dragon breath changes blocks as its dragon may: mobGriefing decides, and a breath with no dragon changes none. */
@GameTestHolder("minefantasy2")
public class BreathGriefTest {

    private BreathGriefTest() {}

    static Overrides griefing(World world, boolean allowed) {
        String before = world.getGameRules().getGameRuleStringValue("mobGriefing");
        return new Overrides()
                .run(
                        () -> world.getGameRules().setOrCreateGameRule("mobGriefing", String.valueOf(allowed)),
                        () -> world.getGameRules().setOrCreateGameRule("mobGriefing", before))
                .set(() -> ConfigMobs.dragonGriefGeneral, v -> ConfigMobs.dragonGriefGeneral = v, true)
                .set(() -> ConfigMobs.dragonGriefFire, v -> ConfigMobs.dragonGriefFire = v, true);
    }

    private static Block hit(GameTestHelper helper, DragonBreath breath, boolean dragon, Block block, int meta) {
        World world = helper.getWorld();
        helper.setBlock(1, 1, 1, block, meta);
        helper.setBlock(1, 2, 1, Blocks.air);
        TestPos at = helper.absolute(1, 1, 1);
        EntityDragonBreath shot = dragon ? new EntityDragonBreath(world, new EntityPig(world), 0, 0, 1, 0F)
                : new EntityDragonBreath(world);
        breath.hitBlock(world, shot, at.x(), at.y(), at.z(), true);
        return world.getBlock(at.x(), at.y(), at.z());
    }

    private static Block above(GameTestHelper helper) {
        TestPos at = helper.absolute(1, 2, 1);
        return helper.getWorld().getBlock(at.x(), at.y(), at.z());
    }

    @GameTest
    public static void frostFreezesWaterAndSetsLava(GameTestHelper helper) {
        try (Overrides overrides = griefing(helper.getWorld(), true)) {
            assertEquals("still water freezes", Blocks.ice, hit(helper, DragonBreath.frost, true, Blocks.water, 0));
            assertEquals(Blocks.ice, hit(helper, DragonBreath.frost, true, Blocks.flowing_water, 1));
            assertEquals(Blocks.obsidian, hit(helper, DragonBreath.frost, true, Blocks.lava, 0));
            assertEquals(Blocks.cobblestone, hit(helper, DragonBreath.frost, true, Blocks.flowing_lava, 1));
            assertEquals(Blocks.stone, hit(helper, DragonBreath.frost, true, Blocks.stone, 0));
            assertEquals("snow settles on stone", Blocks.snow_layer, above(helper));
        }
        helper.succeed();
    }

    @GameTest
    public static void fireMeltsIceAndBreaksGlass(GameTestHelper helper) {
        try (Overrides overrides = griefing(helper.getWorld(), true)) {
            assertEquals(Blocks.water, hit(helper, DragonBreath.fire, true, Blocks.ice, 0));
            assertEquals(Blocks.air, hit(helper, DragonBreath.fire, true, Blocks.glass, 0));
        }
        helper.succeed();
    }

    @GameTest
    public static void noGriefingOrNoDragonChangesNothing(GameTestHelper helper) {
        try (Overrides overrides = griefing(helper.getWorld(), false)) {
            assertEquals(Blocks.water, hit(helper, DragonBreath.frost, true, Blocks.water, 0));
            assertEquals(Blocks.air, above(helper));
            assertEquals(Blocks.ice, hit(helper, DragonBreath.fire, true, Blocks.ice, 0));
            assertEquals(Blocks.glass, hit(helper, DragonBreath.fire, true, Blocks.glass, 0));
        }
        try (Overrides overrides = griefing(helper.getWorld(), true)) {
            assertEquals(
                    "a breath with no dragon",
                    Blocks.water,
                    hit(helper, DragonBreath.frost, false, Blocks.water, 0));
            assertEquals(Blocks.glass, hit(helper, DragonBreath.fire, false, Blocks.glass, 0));
        }
        helper.succeed();
    }
}
