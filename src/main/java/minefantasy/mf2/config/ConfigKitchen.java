package minefantasy.mf2.config;

public class ConfigKitchen extends ConfigurationBaseMF {

    public static boolean enableBench = true;
    /**
     * Dirty progress accumulated by the bench blocks crafting until it is washed
     */
    public static float dirtyProgressMax = 100F;
    /**
     * Fraction of dirtyProgressMax removed per water container used on the bench
     */
    public static float washStrengthFraction = 0.5F;
    /**
     * How much the provisioning skill reduces mess per level: reduction = amount / skillModifier * (level / maxLevel)
     */
    public static int dirtyProgressSkillModifier = 2;

    @Override
    protected void loadConfig() {
        String basic = "Basic";
        enableBench = config
                .get(basic, "Enable Kitchen Bench", true, "Master switch for the kitchen bench block and recipes")
                .getBoolean(true);
        dirtyProgressMax = (float) config.get(
                basic,
                "Dirty Progress Max",
                100D,
                "Crafting stops when the bench reaches this much dirt until it is washed").getDouble(100D);
        washStrengthFraction = (float) config
                .get(basic, "Wash Strength Fraction", 0.5D, "Fraction of the dirt meter removed per water container")
                .getDouble(0.5D);
        dirtyProgressSkillModifier = config
                .get(basic, "Dirty Progress Skill Mod", 2, "How much the provisioning skill reduces mess per level")
                .getInt(2);
    }
}
