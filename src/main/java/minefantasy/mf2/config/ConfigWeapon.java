package minefantasy.mf2.config;

import minefantasy.mf2.api.helpers.ArmourCalculator;

public class ConfigWeapon extends ConfigurationBaseMF {

    public static final String CATEGORY_PENALTIES = "Penalties";
    public static final String CATEGORY_BONUS = "Bonuses";
    public static final String CATEGORY_ARCHERY = "Archery";
    public static boolean stickArrows;
    public static boolean dynamicArrows;
    public static boolean useBalance;
    public static boolean breakArrowsGround;
    public static float arrowBreakMod;
    public static boolean xpTrain;

    @Override
    protected void loadConfig() {
        useBalance = config.get(
                CATEGORY_PENALTIES,
                "Heavy Weapon Balance",
                true,
                "This causes heavy weapons to throw the camera off angle. This is recommended as it gives a downside to avoid overpowering heavy weapons. With this active: spamming hits is difficult")
                .getBoolean();
        breakArrowsGround = config
                .get(CATEGORY_PENALTIES, "Arrow Ground Break", true, "Arrows can break when hitting the ground")
                .getBoolean();
        arrowBreakMod = config.getFloat(
                "Arrow Break Chance modifier",
                CATEGORY_PENALTIES,
                1.0F,
                0F,
                100F,
                "This modifies the rate that arrows break when hitting blocks, 1.0=normal higher numbers increases the break chance");

        stickArrows = config
                .get(
                        CATEGORY_ARCHERY,
                        "Save Arrows",
                        true,
                        "With this active; arrows fired into enemies save, so they can be dropped on death")
                .getBoolean();
        dynamicArrows = config.get(
                CATEGORY_ARCHERY,
                "Dynamic arrow sync",
                true,
                "This is for whatever hosts the world (singleplayer, or servers); it increases the rate arrows sync their data for smoother rendering for players. This however increases packet traffic. If you have a lot of players on a server or a lot of arrows, disable this to help clean it up")
                .getBoolean();
        ArmourCalculator.useConfigIndirectDmg = config.get(
                CATEGORY_ARCHERY,
                "Use config indirect damage",
                true,
                "Allows config for indirect projectiles, disable if you get problems when getting hit by modded projectiles")
                .getBoolean();

        xpTrain = config
                .get(
                        CATEGORY_BONUS,
                        "Training weapon xp",
                        true,
                        "This option allows training weapons to increase your xp level slowly when hitting with them")
                .getBoolean();
    }

}
