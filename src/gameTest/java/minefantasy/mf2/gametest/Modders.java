package minefantasy.mf2.gametest;

import java.util.UUID;

import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.mojang.authlib.GameProfile;

/**
 * Names for the fake players of the tests: the mod's makers, as {@link minefantasy.mf2.MineFantasyII#isNameModder}
 * knows them. The game keeps a fake player per name, research and all, so the one who is taught research never stands
 * in for one who must lack it.
 */
public final class Modders {

    /** Is taught the research a test needs. */
    public static final String SCHOLAR = "Galactic_Hiker";
    /** Never learns research: the novice, and a smith with a weak tool. */
    public static final String NOVICE = "tim4200";
    /** Works the stations built in the world, and runs the commands as an operator. */
    public static final String SMITH = "Sirse";

    private Modders() {}

    /**
     * A fake player of its own, not the one the game keeps for the name: for a test that moves, mounts or empties its
     * player, which would otherwise carry over into the next test using that name.
     */
    public static FakePlayer fresh(GameTestHelper helper, String name) {
        return new FakePlayer(helper.getWorld(), new GameProfile(UUID.randomUUID(), name));
    }
}
