package minefantasy.mf2.config;

import minefantasy.mf2.farming.FarmingHelper;

public class ConfigFarming extends ConfigurationBaseMF {

    public static final String CATEGORY_BONUS = "Bonuses";

    public static final String CATEGORY_PENALTIES = "Penalties";

    public static final String CATEGORY_MISC = "Other Features";

    @Override
    protected void loadConfig() {
        FarmingHelper.isEnabled = config
                .get("##Enable System##", "isEnabled", true, "This toggles the farming mechanics").getBoolean();

        FarmingHelper.hoeFailChanceCfg = (float) config
                .get(CATEGORY_PENALTIES, "Hoe Fail Modifier", 1.0F, "Modifies the rate for hoes to fail making fields")
                .getDouble();
        FarmingHelper.farmBreakCfg = (float) config
                .get(
                        CATEGORY_PENALTIES,
                        "Harvest Ruin Modifier",
                        1.0F,
                        "Modifies the rate for ruining fields when harvesting (scythes increase this chance)")
                .getDouble();
    }

}
