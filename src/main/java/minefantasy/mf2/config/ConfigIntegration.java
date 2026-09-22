package minefantasy.mf2.config;

import java.util.Arrays;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class ConfigIntegration extends ConfigurationBaseMF {

    public static final String CATEGORY_MODS = "Cross-mod Integration";
    public static final String CATEGORY_BUKKIT = "Bukkit Integration";
    public static boolean neiIntegration;
    public static boolean mtIntegration;
    public static boolean tcIntegration;
    public static boolean tcAspects;
    public static boolean tcInfusion;
    public static String[] pluginsList = new String[0];

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

        if (FMLCommonHandler.instance().getSide() == Side.SERVER) {
            pluginsList = config.get(
                    CATEGORY_BUKKIT,
                    "Plugins List",
                    new String[] { "WorldGuard" },
                    "This registers listeners of Bukkit plugins (if avaliable). Here you are should list all protection-related plugins (e.g WorldGuard). You are can find correct plugin name in its plugin.yml (inside plugin's .jar file)")
                    .getStringList();
            Arrays.sort(pluginsList);
        }
    }
}
