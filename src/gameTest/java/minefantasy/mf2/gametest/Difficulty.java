package minefantasy.mf2.gametest;

import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;

/**
 * Lets a test play on a world that is not peaceful, where monsters stay. Tests run side by side in one world, so the
 * difficulty is held while any of them needs it and given back only when the last one ends; a test restoring it on its
 * own would send a neighbour's monsters away.
 */
public final class Difficulty {

    private static int holders;
    private static EnumDifficulty before;

    private Difficulty() {}

    public static void hold(GameTestHelper helper) {
        World world = helper.getWorld();
        if (holders++ == 0) before = world.difficultySetting;
        world.difficultySetting = EnumDifficulty.EASY;
        helper.afterTest(() -> { if (--holders == 0) world.difficultySetting = before; });
    }
}
