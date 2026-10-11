package minefantasy.mf2.config;

import minefantasy.mf2.farming.FarmingHelper;

public class ConfigFarming extends ConfigurationBaseMF {

    public static final String CATEGORY_PENALTIES = "Penalties";

    @Override
    protected void loadConfig() {
        FarmingHelper.isEnabled = config
                .get("##Enable System##", "isEnabled", true, "This toggles the farming mechanics").getBoolean();

        FarmingHelper.hoeFailChanceCfg = config.getFloat(
                "Hoe Fail Modifier",
                CATEGORY_PENALTIES,
                1.0F,
                0F,
                100F,
                "Modifies the rate for hoes to fail making fields");
        FarmingHelper.farmBreakCfg = config.getFloat(
                "Harvest Ruin Modifier",
                CATEGORY_PENALTIES,
                1.0F,
                0F,
                100F,
                "Modifies the rate for ruining fields when harvesting (scythes increase this chance)");
    }

}
