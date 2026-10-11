package minefantasy.mf2.config;

public class ConfigIntegration extends ConfigurationBaseMF {

    public static final String CATEGORY_MODS = "Cross-mod Integration";
    public static boolean neiIntegration;
    public static boolean mtIntegration;
    public static boolean tcIntegration;
    public static boolean tcAspects;
    public static boolean tcInfusion;
    public static boolean etfuturumIntegration;
    public static boolean etfuturumBlastBlacklist;

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
        etfuturumIntegration = config.get(
                CATEGORY_MODS,
                "Et Futurum Integration",
                true,
                "Enable Et Futurum Requiem integration: composter and bee bridges, working stations hidden from spectators, and blast furnace guards. Spectator safety stays on regardless of this setting")
                .getBoolean();
        etfuturumBlastBlacklist = config.get(
                CATEGORY_MODS,
                "Et Futurum Blast Furnace Blacklist",
                true,
                "Keep MineFantasy raw ore, ore blocks and coal refining out of Et Futurum's blast furnace, so metals stay on the crucible and bloom")
                .getBoolean();
    }
}
