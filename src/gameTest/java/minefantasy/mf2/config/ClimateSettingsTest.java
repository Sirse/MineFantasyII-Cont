package minefantasy.mf2.config;

import static minefantasy.mf2.gametest.Assert.*;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("minefantasy2")
public class ClimateSettingsTest {

    @GameTest
    public static void migrationPersistsExistingChoicesAndPrefersClimate(GameTestHelper helper) throws Exception {
        File file = File.createTempFile("mf-climate-migration-", ".cfg");
        try {
            Configuration original = new Configuration(file);
            original.load();
            original.get("Smithing", "Use EnviroMine Climate", true).set(false);
            original.get("Smithing", "Temperate Ambient Celsius", 20D).set(31.5D);
            original.get("Smithing", "Use Serene Seasons Climate", true).set(true);
            original.get("Climate", "Use Serene Seasons Climate", true).set(false);
            original.save();
            Configuration migration = new Configuration(file);
            migration.load();
            ConfigCrafting.migrateClimateSettings(migration);
            migration.save();
            Configuration loaded = new Configuration(file);
            loaded.load();
            assertFalse(loaded.get("Climate", "Use EnviroMine Climate", true).getBoolean());
            assertEquals(31.5F, (float) loaded.get("Climate", "Temperate Ambient Celsius", 20D).getDouble(), 0F);
            assertFalse(loaded.get("Climate", "Use Serene Seasons Climate", true).getBoolean());
            assertFalse(loaded.getCategory("smithing").containsKey("Use EnviroMine Climate"));
            assertFalse(loaded.getCategory("smithing").containsKey("Temperate Ambient Celsius"));
            ConfigCrafting.migrateClimateSettings(loaded);
            assertFalse(loaded.get("Climate", "Use EnviroMine Climate", true).getBoolean());
        } finally {
            file.delete();
        }
        helper.succeed();
    }
}
