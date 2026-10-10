package minefantasy.mf2.integration.minetweaker;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraftforge.fluids.FluidRegistry;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.heating.QuenchMedium;
import minefantasy.mf2.api.heating.QuenchResponse;

/** {@code mods.minefantasy.Quench}: fluids and metals a script sets, put back by a reload, bad values refused. */
@GameTestHolder(value = "minefantasy2", requiredMods = "MineTweaker3")
public class QuenchScriptTest {

    private QuenchScriptTest() {}

    @GameTest
    public static void aScriptSetsAFluidAndAReloadPutsItBack(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(0, Scripts.run("mods.minefantasy.Quench.fluid(\"lava\", \"brine\");").size());
            assertSame(QuenchMedium.BRINE, QuenchMedium.of(FluidRegistry.LAVA));
            assertEquals(0, Scripts.run("mods.minefantasy.Quench.fluid(\"water\", \"none\");").size());
            assertNull("lava still quenches after a reload", QuenchMedium.of(FluidRegistry.LAVA));
            assertNull("water was not taken out", QuenchMedium.of(FluidRegistry.WATER));
        });
        assertSame("water was not put back", QuenchMedium.WATER, QuenchMedium.of(FluidRegistry.WATER));
    }

    @GameTest
    public static void aScriptSetsAMetalAndAReloadPutsItBack(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(0, Scripts.run("mods.minefantasy.Quench.metal(\"Bronze\", 1, 2, 3, 4, 5, 6);").size());
            QuenchResponse bronze = QuenchResponse.of("Bronze");
            assertEquals(3F, bronze.bonus(QuenchMedium.WATER), 0F);
            assertEquals(6F, bronze.crack(QuenchMedium.BRINE), 0F);
            assertEquals(0, Scripts.run("mods.minefantasy.Quench.justCools(\"Steel\");").size());
            assertFalse("steel still hardens", QuenchResponse.of("Steel").matters());
        });
        assertFalse("bronze kept the script's response", QuenchResponse.of("Bronze").matters());
        assertEquals("steel was not put back", 15F, QuenchResponse.of("Steel").bonus(QuenchMedium.WATER), 0F);
    }

    @GameTest
    public static void badValuesAreRefused(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertFalse(Scripts.run("mods.minefantasy.Quench.fluid(\"water\", \"acid\");").isEmpty());
            assertSame(QuenchMedium.WATER, QuenchMedium.of(FluidRegistry.WATER));
            assertFalse(Scripts.run("mods.minefantasy.Quench.metal(\"Steel\", 1, 120, 3, 4, 5, 6);").isEmpty());
            assertFalse(Scripts.run("mods.minefantasy.Quench.metal(\"Steel\", 1, -1, 3, 4, 5, 6);").isEmpty());
            assertEquals("a refused line changed steel", 15F, QuenchResponse.of("Steel").bonus(QuenchMedium.WATER), 0F);
        });
    }
}
