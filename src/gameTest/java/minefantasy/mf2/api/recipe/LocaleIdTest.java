package minefantasy.mf2.api.recipe;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.Locale;

import net.minecraft.init.Items;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.exotic.SpecialForging;
import minefantasy.mf2.api.material.CustomMaterial;

/**
 * Ids and material names do not depend on the machine's language: in Turkish a capital I lowers to a dotless one, which
 * once changed recipe ids, lost their running work and hid them from scripts.
 */
@GameTestHolder("minefantasy2")
public class LocaleIdTest {

    private LocaleIdTest() {}

    @GameTest
    public static void idsAndMaterialsReadTheSameInTurkish(GameTestHelper helper) {
        Locale before = Locale.getDefault();
        String id = SpecialForging.idFor("DIRE", Items.iron_sword).toString();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals(
                    "the id changed with the language",
                    id,
                    SpecialForging.idFor("DIRE", Items.iron_sword).toString());
            assertNotNull("iron was not found by its capital name", CustomMaterial.getMaterial("IRON"));
        } finally {
            Locale.setDefault(before);
        }
        helper.succeed();
    }
}
