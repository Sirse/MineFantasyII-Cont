package minefantasy.mf2.config;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.Arrays;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.armour.CustomDamageRatioEntry;
import minefantasy.mf2.gametest.Overrides;

/** Custom item entries read from the config. */
@GameTestHolder("minefantasy2")
public class ConfigItemRegistryTest {

    private ConfigItemRegistryTest() {}

    /** A projectile line names an entity, not an item; bad lines before it are skipped, not fatal. */
    @GameTest
    public static void aProjectileLineSetsTheEntityRatio(GameTestHelper helper) {
        String[][] lists = { ConfigItemRegistry.armourListAC, ConfigItemRegistry.hoeList,
                ConfigItemRegistry.crafterList, ConfigItemRegistry.customDamagerList,
                ConfigItemRegistry.customDamagerEntityList };
        CustomDamageRatioEntry before = CustomDamageRatioEntry.entriesProj.get("Snowball");
        ConfigItemRegistry.armourListAC = new String[0];
        ConfigItemRegistry.hoeList = new String[0];
        // The same rules as scripts: MineFantasy's own tool and a NaN efficiency are refused
        ConfigItemRegistry.crafterList = new String[] { "minefantasy2:stone_hammer|2|1|knife",
                "minecraft:blaze_rod|NaN|1|hammer" };
        ConfigItemRegistry.customDamagerList = new String[0];
        ConfigItemRegistry.customDamagerEntityList = new String[] { "Snowball|0|0", "Snowball|x|0|1",
                "Snowball|0|0|1" };
        try {
            ConfigItemRegistry.readCustoms();
            assertNull(
                    minefantasy.mf2.api.crafting.CustomCrafterEntry.entries
                            .get(minefantasy.mf2.item.list.ToolListMF.hammerStone));
            assertNull(minefantasy.mf2.api.crafting.CustomCrafterEntry.entries.get(net.minecraft.init.Items.blaze_rod));
            // Cutting, piercing, blunt in the config; kept as cutting, blunt, piercing
            assertEquals(
                    Arrays.toString(new float[] { 0, 1, 0 }),
                    Arrays.toString(CustomDamageRatioEntry.getTraits("Snowball")));
        } finally {
            ConfigItemRegistry.armourListAC = lists[0];
            ConfigItemRegistry.hoeList = lists[1];
            ConfigItemRegistry.crafterList = lists[2];
            ConfigItemRegistry.customDamagerList = lists[3];
            ConfigItemRegistry.customDamagerEntityList = lists[4];
            if (before == null) {
                CustomDamageRatioEntry.entriesProj.remove("Snowball");
            } else {
                CustomDamageRatioEntry.entriesProj.put("Snowball", before);
            }
        }
        helper.succeed();
    }

    /** The handpick's fortune switch is read from the file, not from a Java system property. */
    @GameTest
    public static void theHandpickFortuneSwitchIsRead(GameTestHelper helper) throws Exception {
        try (Overrides overrides = new Overrides()
                .set(() -> ConfigTools.handpickFortune, v -> ConfigTools.handpickFortune = v, false)) {
            java.io.File file = java.io.File.createTempFile("mf-tools", ".cfg");
            try {
                net.minecraftforge.common.config.Configuration config = new net.minecraftforge.common.config.Configuration(
                        file);
                config.get(ConfigTools.CATEGORY_BONUS, "Fortune enchantment for handpick", false).set(true);
                config.save();
                new ConfigTools().setConfig(new net.minecraftforge.common.config.Configuration(file));
                assertTrue(ConfigTools.handpickFortune);
            } finally {
                file.delete();
            }
        }
        helper.succeed();
    }

    /** A value out of its range is brought into it, not used as written. */
    @GameTest
    public static void anOutOfRangeValueIsClamped(GameTestHelper helper) throws Exception {
        try (Overrides overrides = new Overrides().set(
                () -> minefantasy.mf2.api.heating.Quench.cracking,
                v -> minefantasy.mf2.api.heating.Quench.cracking = v,
                1F)) {
            java.io.File file = java.io.File.createTempFile("mf-crafting", ".cfg");
            try {
                net.minecraftforge.common.config.Configuration config = new net.minecraftforge.common.config.Configuration(
                        file);
                config.get(ConfigCrafting.CATEGORY_SMITHING, "Quench Crack Modifier", 1D).set(50D);
                config.save();
                new ConfigCrafting().setConfig(new net.minecraftforge.common.config.Configuration(file));
                assertEquals(10F, minefantasy.mf2.api.heating.Quench.cracking, 0F);
            } finally {
                file.delete();
            }
        }
        helper.succeed();
    }

    /** The new balance options reach the code that uses them. */
    @GameTest
    public static void balanceOptionsAreRead(GameTestHelper helper) throws Exception {
        try (Overrides overrides = new Overrides()
                .keep(
                        () -> minefantasy.mf2.api.rpg.RPGElements.levelUpModifier,
                        v -> minefantasy.mf2.api.rpg.RPGElements.levelUpModifier = v)
                .keep(
                        () -> minefantasy.mf2.api.rpg.RPGElements.levelSpeedModifier,
                        v -> minefantasy.mf2.api.rpg.RPGElements.levelSpeedModifier = v)
                .keep(
                        () -> minefantasy.mf2.block.tileentity.TileEntityForge.fuelUse,
                        v -> minefantasy.mf2.block.tileentity.TileEntityForge.fuelUse = v)
                .keep(
                        () -> minefantasy.mf2.api.heating.Quench.CRACK_WEAR,
                        v -> minefantasy.mf2.api.heating.Quench.CRACK_WEAR = v)) {
            java.io.File hardcore = java.io.File.createTempFile("mf-hardcore", ".cfg");
            java.io.File crafting = java.io.File.createTempFile("mf-crafting", ".cfg");
            try {
                net.minecraftforge.common.config.Configuration config = new net.minecraftforge.common.config.Configuration(
                        hardcore);
                config.get(ConfigHardcore.CATEGORY_SKILLS, "Level Cost Rise", 1.5D).set(0D);
                config.get(ConfigHardcore.CATEGORY_SKILLS, "XP Gain Modifier", 1D).set(3D);
                config.save();
                config = new net.minecraftforge.common.config.Configuration(crafting);
                config.get(ConfigCrafting.CATEGORY_REFINING, "Forge Fuel Use Modifier", 1D).set(2D);
                config.get(ConfigCrafting.CATEGORY_SMITHING, "Quench Crack Wear", 0.25D).set(0.5D);
                config.save();
                new ConfigHardcore().setConfig(new net.minecraftforge.common.config.Configuration(hardcore));
                new ConfigCrafting().setConfig(new net.minecraftforge.common.config.Configuration(crafting));
                // With no rise every level costs the same
                minefantasy.mf2.api.rpg.Skill skill = minefantasy.mf2.api.rpg.SkillList.artisanry;
                assertEquals(skill.getLvlXP(1, null), skill.getLvlXP(30, null));
                assertEquals(3F, minefantasy.mf2.api.rpg.RPGElements.levelSpeedModifier, 0F);
                assertEquals(2F, minefantasy.mf2.block.tileentity.TileEntityForge.fuelUse, 0F);
                assertEquals(0.5F, minefantasy.mf2.api.heating.Quench.CRACK_WEAR, 0F);
            } finally {
                hardcore.delete();
                crafting.delete();
            }
        }
        helper.succeed();
    }

    /**
     * Road speed off: walking on a road gives no speed, a level gives that level, and transformations stay off by
     * default.
     */
    @GameTest
    public static void roadSpeedAndTransformationsFollowTheConfig(GameTestHelper helper) throws Exception {
        try (Overrides overrides = new Overrides().keep(
                () -> minefantasy.mf2.block.decor.BlockRoad.speedLevel,
                v -> minefantasy.mf2.block.decor.BlockRoad.speedLevel = v)) {
            java.io.File file = java.io.File.createTempFile("mf-crafting", ".cfg");
            try {
                net.minecraftforge.common.config.Configuration config = new net.minecraftforge.common.config.Configuration(
                        file);
                config.get(ConfigCrafting.CATEGORY_BLOCKS, "Road Speed Level", 1).set(0);
                config.save();
                new ConfigCrafting().setConfig(new net.minecraftforge.common.config.Configuration(file));
                assertFalse("transformations are on by default", ConfigCrafting.enableTransformations);
                net.minecraft.entity.monster.EntityZombie walker = new net.minecraft.entity.monster.EntityZombie(
                        helper.getWorld());
                minefantasy.mf2.block.list.BlockListMF.road.onEntityWalking(helper.getWorld(), 0, 0, 0, walker);
                assertNull(walker.getActivePotionEffect(net.minecraft.potion.Potion.moveSpeed));
                minefantasy.mf2.block.decor.BlockRoad.speedLevel = 2;
                minefantasy.mf2.block.list.BlockListMF.road.onEntityWalking(helper.getWorld(), 0, 0, 0, walker);
                assertEquals(1, walker.getActivePotionEffect(net.minecraft.potion.Potion.moveSpeed).getAmplifier());
            } finally {
                file.delete();
            }
        }
        helper.succeed();
    }

    /** Old categories and keys go, though Forge wrote them in lower case, and no mixed-case copy is left behind. */
    @GameTest
    public static void oldEntriesAreRemovedWhateverTheirCase(GameTestHelper helper) throws Exception {
        java.io.File armour = java.io.File.createTempFile("mf-armour", ".cfg");
        java.io.File hardcore = java.io.File.createTempFile("mf-hardcore", ".cfg");
        try {
            net.minecraftforge.common.config.Configuration config = new net.minecraftforge.common.config.Configuration(
                    armour);
            config.get("Misc", "Use config indrect damage", true);
            config.save();
            config = new net.minecraftforge.common.config.Configuration(hardcore);
            config.get(ConfigHardcore.CATEGORY_CRAFTING, "Hot burns", true);
            config.save();
            new ConfigArmour().setConfig(new net.minecraftforge.common.config.Configuration(armour));
            new ConfigHardcore().setConfig(new net.minecraftforge.common.config.Configuration(hardcore));
            net.minecraftforge.common.config.Configuration read = new net.minecraftforge.common.config.Configuration(
                    armour);
            read.load();
            assertFalse("misc", read.hasCategory("misc"));
            read = new net.minecraftforge.common.config.Configuration(hardcore);
            read.load();
            assertFalse(read.getCategory("1: hardcore crafting").containsKey("Hot burns"));
            assertFalse("a mixed-case copy", read.hasCategory(ConfigHardcore.CATEGORY_CRAFTING));
        } finally {
            armour.delete();
            hardcore.delete();
        }
        helper.succeed();
    }
}
