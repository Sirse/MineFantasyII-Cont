package minefantasy.mf2.config;

import java.util.Locale;

import net.minecraftforge.common.config.Configuration;

public abstract class ConfigurationBaseMF {

    protected Configuration config;

    public void setConfig(Configuration configuration) {
        configuration.load();
        config = configuration;
        loadConfig();
        configuration.save();
    }

    protected abstract void loadConfig();

    /**
     * Removes an old category if the file has it. Forge files keep categories in lower case, as get() writes them,
     * while hasCategory and getCategory take the name as given.
     */
    protected void dropCategory(String category) {
        String name = category.toLowerCase(Locale.ENGLISH);
        if (config.hasCategory(name)) {
            config.removeCategory(config.getCategory(name));
        }
    }

    /** Removes an old key from a category if both are there. */
    protected void dropKey(String category, String key) {
        String name = category.toLowerCase(Locale.ENGLISH);
        if (config.hasCategory(name) && config.getCategory(name).containsKey(key)) {
            config.getCategory(name).remove(key);
        }
    }
}
