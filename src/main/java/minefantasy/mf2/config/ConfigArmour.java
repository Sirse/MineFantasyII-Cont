package minefantasy.mf2.config;

import minefantasy.mf2.api.helpers.ArmourCalculator;
import minefantasy.mf2.api.helpers.TacticalManager;
import minefantasy.mf2.entity.EntityCogwork;

public class ConfigArmour extends ConfigurationBaseMF {

    public static final String CATEGORY_BONUS = "Bonuses";
    public static final String CATEGORY_DAMAGE_TYPES = "Damage Types";
    public static final String CATEGORY_PENALTIES = "Penalties";
    public static final String CATEGORY_COGWORK = "Cogwork Features";
    public static boolean resistArrow;
    public static boolean cogworkGrief;
    public static float cogworkFuelUnits;

    @Override
    protected void loadConfig() {
        resistArrow = config.get(
                CATEGORY_BONUS,
                "Deflect Arrows",
                true,
                "With this option; arrows have a chance to deflect off armour. It compares the arrow damage and armour rating. Chain and plate have higher rates. Arrows bounced off can hurt enemies")
                .getBoolean();
        TacticalManager.arrowDeflectChance = config.getFloat(
                "Deflect Arrows Modifier",
                CATEGORY_BONUS,
                1.0F,
                0F,
                100F,
                "This modifies the chance for arrows to deflect off armour: Increasing this increases the deflect chance, decreasing decreases the chance, 0 means it's impossible");

        TacticalManager.shouldSlow = config.get(
                CATEGORY_PENALTIES,
                "Armour Slow Movement",
                true,
                "With this: Heavier armours slow your movement down across both walking and sprinting. Speed is affected by design and material")
                .getBoolean();
        TacticalManager.minWeightSpeed = config.getFloat(
                "Min armour weigh speed",
                CATEGORY_PENALTIES,
                10F,
                0F,
                100F,
                "The minimal speed percent capable from weight (10 = 10%), MF armours don't go lower than 75% anyway");
        ArmourCalculator.slowRate = config.getFloat(
                "Slowdown Rate",
                CATEGORY_PENALTIES,
                1.0F,
                0F,
                100F,
                "A modifier for the rate armours slow down movement, increasing this slows you more, decreasing it slows less");

        dropCategory("Misc");
        String AADesc = "This feature Sets supported armours to calculate weapons differently: \n"
                + "Damage is divided into cutting and blunt, and MF armours will alter their ratings depending \n"
                + "Both weapons and ranged entities can be registered to deliver a certain ratio \n"
                + "Armours can't be registered, all coding needs to be hardwired into the code themselves \n"
                + "So this means mod armours can not support this, they need to have a system built-in to their own coding"
                + "But this ratio only effects armours that are told to. so it should have no effect on unsupported armours, being balanced as usual \n"
                + "Undefined weapon damages however have no effect either, but it will try and guess \n"
                + "This will also change axes and blunt weapons to match blade damage (since armour is vulnerable to blunt";
        ArmourCalculator.advancedDamageTypes = config
                .get(CATEGORY_DAMAGE_TYPES, "Advanced Armour Calculator", true, AADesc).getBoolean();

        cogworkGrief = config.get(
                CATEGORY_COGWORK,
                "Cogwork Grief",
                true,
                "Should cogwork armour cause environmental damage when impact landing").getBoolean();
        cogworkFuelUnits = config.getFloat(
                "Cogwork Fuel Modifier",
                CATEGORY_COGWORK,
                1F,
                0F,
                100F,
                "Modify the amount of fuel added to cogworks");
        EntityCogwork.health_modifier = config.getFloat(
                "Cogwork Durability Modifier",
                CATEGORY_COGWORK,
                1.0F,
                0F,
                100F,
                "Modify the relative durability of cogwork armour");
        EntityCogwork.rating_modifier = config.getFloat(
                "Cogwork Armour Modifier",
                CATEGORY_COGWORK,
                1.0F,
                0F,
                100F,
                "Modify the relative armour rating of cogwork armour");
        EntityCogwork.allowedBulk = config.getInt(
                "Worn Apparel Restrict",
                CATEGORY_COGWORK,
                0,
                -1,
                2,
                "-1: can't wear armour with cogwork, 0=Can enter cogwork with light armour, 1=can enter with medium, 2=can enter in any armour");
    }
}
