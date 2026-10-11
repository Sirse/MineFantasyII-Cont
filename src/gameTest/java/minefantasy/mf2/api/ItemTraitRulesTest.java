package minefantasy.mf2.api;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.item.ItemArmor;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.armour.ArmourDesign;
import minefantasy.mf2.api.armour.CustomArmourEntry;
import minefantasy.mf2.api.helpers.ItemTraitRules;

/** Armour of a mod's extra slot, past the four worn ones, is refused or shared out as a quarter, never a crash. */
@GameTestHolder("minefantasy2")
public class ItemTraitRulesTest {

    private ItemTraitRulesTest() {}

    @GameTest
    public static void armourInAnExtraSlotDoesNotCrash(GameTestHelper helper) {
        // ItemArmor reads its material by slot when made, so the extra slot is set afterwards, as such mods end up
        ItemArmor extra = new ItemArmor(ItemArmor.ArmorMaterial.IRON, 0, 0);
        cpw.mods.fml.relauncher.ReflectionHelper
                .setPrivateValue(ItemArmor.class, extra, 4, "armorType", "field_77881_a");
        CustomArmourEntry entry = ItemTraitRules.armour(extra, "plate", 1F, null);
        assertEquals(ArmourDesign.PLATE.getBulk() * 0.25F, entry.bulkiness, 0.001F);
        try {
            ItemTraitRules.wornArmour(extra, 2F, 1F, "light");
            fail("an extra slot was taken as worn");
        } catch (IllegalArgumentException expected) {
            // refused with a reason the config and scripts report
        }
        CustomArmourEntry.registerItem(extra, ArmourDesign.PLATE);
        CustomArmourEntry.entries.remove(extra);
        helper.succeed();
    }
}
