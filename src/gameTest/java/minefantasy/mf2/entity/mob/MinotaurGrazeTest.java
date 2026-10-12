package minefantasy.mf2.entity.mob;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.gametest.Overrides;

/** A minotaur eats ripe crops only while mobGriefing allows, as a sheep eats grass. */
@GameTestHolder("minefantasy2")
public class MinotaurGrazeTest {

    private MinotaurGrazeTest() {}

    /** A minotaur that never rolls to search around: each forage eats what it stands in. */
    private static final class Hungry extends EntityMinotaur {

        Hungry(World world) {
            super(world);
            rand = new Random() {

                @Override
                public int nextInt(int bound) {
                    return bound - 1;
                }
            };
        }
    }

    private static boolean grazes(GameTestHelper helper, boolean griefing) {
        World world = helper.getWorld();
        helper.setBlock(1, 0, 1, Blocks.farmland);
        helper.setBlock(1, 1, 1, Blocks.wheat, 7);
        TestPos crop = helper.absolute(1, 1, 1);
        EntityMinotaur minotaur = new Hungry(world);
        minotaur.setPosition(crop.x() + 0.5D, crop.y(), crop.z() + 0.5D);
        String before = world.getGameRules().getGameRuleStringValue("mobGriefing");
        try (Overrides overrides = new Overrides().run(
                () -> world.getGameRules().setOrCreateGameRule("mobGriefing", String.valueOf(griefing)),
                () -> world.getGameRules().setOrCreateGameRule("mobGriefing", before))) {
            minotaur.forage();
        }
        return world.getBlock(crop.x(), crop.y(), crop.z()) != Blocks.wheat;
    }

    @GameTest
    public static void aMinotaurEatsCropsOnlyWhileGriefingIsAllowed(GameTestHelper helper) {
        assertFalse("ate with mobGriefing off", grazes(helper, false));
        assertTrue("did not eat with mobGriefing on", grazes(helper, true));
        helper.succeed();
    }
}
