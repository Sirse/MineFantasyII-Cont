package minefantasy.mf2.integration.minetweaker;

import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.armour.ArmourDesign;
import minefantasy.mf2.api.armour.CustomDamageRatioEntry;
import minefantasy.mf2.api.farming.CustomHoeEntry;
import minefantasy.mf2.api.helpers.ArmourCalculator;
import minefantasy.mf2.api.helpers.ToolHelper;

/**
 * {@code mods.minefantasy.ItemTraits}: traits a script gives other items are read where MineFantasy reads them, a
 * reload puts back what was there, and bad values are refused.
 */
@GameTestHolder(value = "minefantasy2", requiredMods = "MineTweaker3")
public class ItemTraitsScriptTest {

    private ItemTraitsScriptTest() {}

    private static final ItemStack HELMET = new ItemStack(Items.leather_helmet);
    private static final ItemStack ROD = new ItemStack(Items.blaze_rod);

    @GameTest
    public static void armourTakesADesignAndAReloadPutsTheOldOneBack(GameTestHelper helper) throws Exception {
        float leather = ArmourCalculator.getPieceWeight(HELMET, 0);
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run("mods.minefantasy.ItemTraits.armour(<minecraft:leather_helmet>, \"plate\");").size());
            assertEquals(
                    ArmourDesign.PLATE.getWeight() * ArmourCalculator.sizes[0],
                    ArmourCalculator.getPieceWeight(HELMET, 0),
                    0.001F);
            assertEquals(ArmourDesign.PLATE.getGroup(), ArmourCalculator.getArmourClass(HELMET));
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.ItemTraits.wornArmour(<minecraft:leather_helmet>, 2.5, 1, \"light\");")
                            .size());
            assertEquals(2.5F, ArmourCalculator.getPieceWeight(HELMET, 0), 0.001F);
            assertEquals("light", ArmourCalculator.getArmourClass(HELMET));
        });
        assertEquals(
                "the leather weight was not put back",
                leather,
                ArmourCalculator.getPieceWeight(HELMET, 0),
                0.001F);
    }

    @GameTest
    public static void aScriptMakesAToolAWeaponAndAHoe(GameTestHelper helper) throws Exception {
        Scripts.around(helper, () -> {
            assertEquals(
                    0,
                    Scripts.run(
                            "mods.minefantasy.ItemTraits.crafter(<minecraft:blaze_rod>, \"hammer\", 3.5, 2);\n"
                                    + "mods.minefantasy.ItemTraits.damage(<minecraft:blaze_rod>, 1, 2, 3);\n"
                                    + "mods.minefantasy.ItemTraits.hoe(<minecraft:blaze_rod>, 9);\n"
                                    + "mods.minefantasy.ItemTraits.projectileDamage(\"Snowball\", 0, 0, 1);")
                            .size());
            assertEquals("hammer", ToolHelper.getCrafterTool(ROD));
            assertEquals(2, ToolHelper.getCrafterTier(ROD));
            assertEquals(3.5F, ToolHelper.getCrafterEfficiency(ROD), 0F);
            // Cutting, blunt, piercing as the damage code reads it
            assertEquals(
                    java.util.Arrays.toString(new float[] { 1, 3, 2 }),
                    java.util.Arrays.toString(ArmourCalculator.getRatioForWeapon(ROD)));
            assertEquals(9F, CustomHoeEntry.getEntryEfficiency(ROD, 6F), 0F);
            assertEquals(
                    java.util.Arrays.toString(new float[] { 0, 1, 0 }),
                    java.util.Arrays.toString(CustomDamageRatioEntry.getTraits("Snowball")));
            assertEquals(0, Scripts.run("mods.minefantasy.ItemTraits.clear(<minecraft:blaze_rod>);").size());
            assertEquals("nothing", ToolHelper.getCrafterTool(ROD));
            assertNull(CustomDamageRatioEntry.getTraits(Items.blaze_rod));
        });
        assertEquals("nothing", ToolHelper.getCrafterTool(ROD));
        assertNull(CustomDamageRatioEntry.getTraits(Items.blaze_rod));
        assertEquals(6F, CustomHoeEntry.getEntryEfficiency(ROD, 6F), 0F);
        assertEquals(
                java.util.Arrays.toString(new float[] { 1, 1, 1 }),
                java.util.Arrays.toString(CustomDamageRatioEntry.getTraits("Snowball")));
    }

    @GameTest
    public static void badValuesAreRefused(GameTestHelper helper) throws Exception {
        float leather = ArmourCalculator.getPieceWeight(HELMET, 0);
        Scripts.around(helper, () -> {
            String[] bad = { "mods.minefantasy.ItemTraits.armour(<minecraft:leather_helmet>, \"glass\");",
                    "mods.minefantasy.ItemTraits.armour(<minecraft:leather_helmet>, \"plate\", -1);",
                    "mods.minefantasy.ItemTraits.armour(<minecraft:leather_helmet>, \"plate\", 1, \"huge\");",
                    "mods.minefantasy.ItemTraits.wornArmour(<minecraft:blaze_rod>, 2, 1, \"light\");",
                    "mods.minefantasy.ItemTraits.damage(<minecraft:blaze_rod>, 0, 0, 0);",
                    "mods.minefantasy.ItemTraits.damage(<minecraft:blaze_rod>, -1, 2, 0);",
                    "mods.minefantasy.ItemTraits.crafter(<minecraft:blaze_rod>, \"nothing\", 2, 1);",
                    "mods.minefantasy.ItemTraits.crafter(<minecraft:blaze_rod>, \"hammer\", 0, 1);",
                    "mods.minefantasy.ItemTraits.hoe(<minecraft:blaze_rod>, -2);",
                    "mods.minefantasy.ItemTraits.crafter(<minefantasy2:stone_hammer>, \"knife\", 2, 1);" };
            for (String line : bad) {
                assertFalse("accepted: " + line, Scripts.run(line).isEmpty());
            }
            assertEquals(leather, ArmourCalculator.getPieceWeight(HELMET, 0), 0.001F);
            assertEquals("nothing", ToolHelper.getCrafterTool(ROD));
            assertNull(CustomDamageRatioEntry.getTraits(Items.blaze_rod));
            assertEquals(6F, CustomHoeEntry.getEntryEfficiency(ROD, 6F), 0F);
        });
    }
}
