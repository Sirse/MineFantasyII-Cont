package minefantasy.mf2.config;

public class ConfigExperiment extends ConfigurationBaseMF {

    public static final String CATEGORY_DEBUG = "Debug";
    public static boolean debug;

    @Override
    protected void loadConfig() {
        // Moved to Armours and Weapons, or folded into the debug switch
        for (String old : new String[] { "###Enable Debug Mode###", "Unfinished Features", "Archery Features",
                "Armour Features" }) {
            dropCategory(old);
        }
        debug = config.get(
                CATEGORY_DEBUG,
                "Debug Mode",
                false,
                "Verbose logs (parrying included), material details in tooltips, and test generation on flat worlds")
                .getBoolean();
    }

}
