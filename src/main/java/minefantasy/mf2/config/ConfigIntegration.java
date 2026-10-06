package minefantasy.mf2.config;

public class ConfigIntegration extends ConfigurationBaseMF {

    public static final String CATEGORY_MODS = "Cross-mod Integration";
    public static boolean neiIntegration;
    public static boolean mtIntegration;
    public static boolean tcIntegration;
    public static boolean tcAspects;
    public static boolean tcInfusion;

    @Override
    protected void loadConfig() {
        neiIntegration = config.get(CATEGORY_MODS, "NEI Integration", true, "Enable Not Enough Items integration")
                .getBoolean();
        mtIntegration = config
                .get(CATEGORY_MODS, "MT Integration", true, "Enable MineTweaker (CraftTweaker) integration")
                .getBoolean();
        tcIntegration = config.get(CATEGORY_MODS, "Thaumcraft Integration", true, "Enable Thaumcraft integration")
                .getBoolean();
        tcAspects = config
                .get(CATEGORY_MODS, "Thaumcraft Aspects", true, "Register aspects for unambiguous MF resources")
                .getBoolean();
        tcInfusion = config.get(
                CATEGORY_MODS,
                "Thaumcraft Infusion",
                false,
                "Enable the shared research and infusion recipes (unfinished)").getBoolean();
    }
}
