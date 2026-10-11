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
        dirtyProgressMax = config.getFloat(
                "Dirty Progress Max",
                basic,
                100F,
                1F,
                100000F,
                "Crafting stops when the bench reaches this much dirt until it is washed");
        washStrengthFraction = config.getFloat(
                "Wash Strength Fraction",
                basic,
                0.5F,
                0F,
                1F,
                "Fraction of the dirt meter removed per water container");
        dirtyProgressSkillModifier = config.getInt(
                "Dirty Progress Skill Mod",
                basic,
                2,
                0,
                100,
                "How much the provisioning skill reduces mess per level");
    }
}
