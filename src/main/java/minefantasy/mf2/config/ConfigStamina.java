package minefantasy.mf2.config;

import minefantasy.mf2.api.stamina.StaminaBar;

public class ConfigStamina extends ConfigurationBaseMF {

    public static String VALUES = "Value Modifiers";

    public static float exhaustDamage;
    public static float weaponModifier;
    public static float sprintModifier;
    public static int fullRegenSeconds;
    public static float weaponDrain;
    public static float bowModifier;
    public static float miningSpeed;

    public static String OPTION = "Options";

    public static boolean affectSpeed;
    public static boolean affectMining;

    public static String OTHER = "Misc";

    @Override
    protected void loadConfig() {
        StaminaBar.isSystemActive = config.get(
                "##Activate Stamina System##",
                "Is enabled",
                true,
                "This is the main switch for the stamina bar and all its features. Setting false disables the system entirely")
                .getBoolean();

        StaminaBar.decayModifierCfg = config
                .getFloat("Decay Modifier", VALUES, 1.0F, 0F, 100F, "This modifies the rate of decay");
        StaminaBar.configRegenModifier = config
                .getFloat("Regen Modifier", VALUES, 1.0F, 0F, 100F, "This modifies the rate of regen");
        StaminaBar.pauseModifier = config.getFloat(
                "Idle Modifier",
                VALUES,
                1.0F,
                0F,
                100F,
                "This modifies the time between decaying and regenning back");
        StaminaBar.configBulk = config.getFloat(
                "Bulk Regen Modifier",
                VALUES,
                1.0F,
                0F,
                100F,
                "This is the rate that worn apparel slows regen: Most items bring it down by 25%, plate is more scale/leather is less");
        StaminaBar.scaleDifficulty = config.get(
                VALUES,
                "Difficulty Scale Decay",
                false,
                "This makes difficulty change your decay rate across all fields: peaceful(-50%), easy(-25%), normal(base), hard(+25%)")
                .getBoolean();
        StaminaBar.defaultMax = config.getFloat(
                "Max stamina base",
                VALUES,
                100F,
                1F,
                10000F,
                "This is where your stamina starts on spawning, Note that when changed: it only applies on death or new worlds");
        weaponModifier = config.getFloat(
                "Weapon Modifier",
                VALUES,
                1.0F,
                0F,
                100F,
                "This modifies the amount using weapons influences stamina");
        sprintModifier = config
                .getFloat("Sprint Modifier", VALUES, 1.0F, 0F, 100F, "Modify how fast sprinting decays stamina");
        fullRegenSeconds = config.getInt(
                "Regen Time",
                VALUES,
                15,
                1,
                3600,
                "Base time(seconds) until the metre is refilled(this is the base, other variables will be modified)");
        weaponDrain = config.getFloat(
                "Exhausted Damage Modifier",
                VALUES,
                0.85F,
                0F,
                10F,
                "This is how being exhausted(empty stamina) influences your damage as a decimal. 0.85 = 85%. A value more than 1 increases the damage when out of stamina");
        bowModifier = config
                .getFloat("Bow Time", VALUES, 1.0F, 0F, 100F, "Modifies the rate drawing bows will drain stamina");
        StaminaBar.configArmourWeightModifier = config.getFloat(
                "Weight Modifier",
                VALUES,
                1.0F,
                0F,
                100F,
                "Modifies the amount weight of armour slows decay");
        miningSpeed = config
                .getFloat("Mining Modifier", VALUES, 1.0F, 0F, 100F, "Modifies the rate block breaking drains stamina");
        exhaustDamage = config.getFloat(
                "Exhausted Damage Taken",
                VALUES,
                1.5F,
                0F,
                10F,
                "How much damage you take when out of stamina (1.5 = +50% more damage)");

        affectSpeed = config.get(OPTION, "Affect Speed", true, "Slowness effect adds when out of stamina").getBoolean();
        affectMining = config.get(OPTION, "Affect Mining", true, "Breaking blocks drains stamina, adds mining fatigue")
                .getBoolean();

        StaminaBar.levelUp = config
                .get(OTHER, "Level Max Stamina", false, "This makes your experience bar increase max stamina")
                .getBoolean();
        StaminaBar.levelAmount = config.getFloat(
                "Level Up Amount",
                OTHER,
                5F,
                0F,
                1000F,
                "How much an experience level increases stamina max");
    }

}
