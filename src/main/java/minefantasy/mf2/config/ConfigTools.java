package minefantasy.mf2.config;

import minefantasy.mf2.item.tool.advanced.ItemLumberAxe;

public class ConfigTools extends ConfigurationBaseMF {

    public static final String CATEGORY_BONUS = "Bonuses";
    public static final String CATEGORY_PENALTIES = "Penalties";
    public static float handpickBonus;
    public static boolean handpickFortune;
    public static float hvyDropChance;

    @Override
    protected void loadConfig() {
        handpickBonus = config.getFloat(
                "Handpick double-drop chance",
                CATEGORY_BONUS,
                20F,
                0F,
                100F,
                "This is the percent value (0-100) handpicks double item drops");

        handpickFortune = config
                .get(CATEGORY_BONUS, "Fortune enchantment for handpick", false, "Be careful, may ruin balance!")
                .getBoolean();

        hvyDropChance = config.getFloat(
                "Heavy ruin-drop chance",
                CATEGORY_PENALTIES,
                25F,
                0F,
                100F,
                "This is the percent value (0-100) heavy picks/shovels and lumber axe do NOT drop blocks on break");
        ItemLumberAxe.fellsTrees = config
                .get(CATEGORY_BONUS, "Lumber Axe Fells Trees", true, "The lumber axe brings down a whole tree")
                .getBoolean();
        ItemLumberAxe.treeLimit = config.getInt(
                "Lumber Axe Tree Limit",
                CATEGORY_BONUS,
                32,
                1,
                1024,
                "The most logs a lumber axe fells at once");
    }
}
