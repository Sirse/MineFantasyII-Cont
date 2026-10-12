package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Blocks;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.refine.SmokeMechanics;

/** A fire is outside only with open sky over it and all around it: one roof block above a corner brings it in. */
@GameTestHolder("minefantasy2")
public class OutsideTest {

    private OutsideTest() {}

    @GameTest
    public static void oneRoofBlockAboveACornerMakesItInside(GameTestHelper helper) {
        TestPos at = helper.absolute(2, 1, 2);
        assertTrue("open sky", SmokeMechanics.isOutside(helper.getWorld(), at.x(), at.y(), at.z()));
        helper.setBlock(3, 4, 3, Blocks.stone);
        assertFalse("roofed corner", SmokeMechanics.isOutside(helper.getWorld(), at.x(), at.y(), at.z()));
        helper.succeed();
    }
}
