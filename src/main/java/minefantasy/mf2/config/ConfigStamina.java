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
                "This is the main switch for the stamina bar and all it's features. Setting false disables the system entirely")
                .getBoolean();

        StaminaBar.decayModifierCfg = (float) config
                .get(VALUES, "Decay Modifier", 1.0F, "This modifies the rate of decay").getDouble();
        StaminaBar.configRegenModifier = (float) config
                .get(VALUES, "Regen Modifier", 1.0F, "This modifies the rate of regen").getDouble();
        StaminaBar.pauseModifier = (float) config
                .get(VALUES, "Idle Modifier", 1.0F, "This modifies the time between decaying and regenning back")
                .getDouble();
        StaminaBar.configBulk = (float) config.get(
                VALUES,
                "Bulk Regen Modifier",
                1.0F,
                "This is the rate that worn apparel slows regen: Most items bring it down by 25%, plate is more scale/leather is less")
                .getDouble();
        StaminaBar.scaleDifficulty = config.get(
                VALUES,
                "Difficulty Scale Decay",
                false,
                "This makes difficulty change your decay rate across all fields: peaceful(-50%), easy(-25%), normal(base), hard(+25%)")
                .getBoolean();
        StaminaBar.defaultMax = (float) config.get(
                VALUES,
                "Max stamina base",
                100F,
                "This is where your stamina starts on spawning, Note that when changed: it only applies on death or new worlds")
                .getDouble();
        // StaminaBar.restrictSystem = config.get("Restrict use to
        // players", "Will restrict", false, "This restricts the stamina system only to
        // players").getBoolean();

        weaponModifier = (float) config
                .get(VALUES, "Weapon Modifier", 1.0F, "This modifies the amount using weapons influences stamina")
                .getDouble();
        sprintModifier = (float) config.get(VALUES, "Sprint Modifier", 1.0F, "Modify how fast sprinting decays stamina")
                .getDouble();
        fullRegenSeconds = config.get(
                VALUES,
                "Regen Time",
                15,
                "Base time(seconds) until the metre is refilled(this is the base, other variables will be modified)")
                .getInt();
        weaponDrain = (float) config.get(
                VALUES,
                "Exhausted Damage Modifier",
                0.85F,
                "This is how being exhausted(empty stamina) influences your damage as a decimal. 0.85 = 85%. A value more than 1 increases the damage when out of stamina")
                .getDouble();
        bowModifier = (float) config.get(VALUES, "Bow Time", 1.0F, "Modifies the rate drawing bows will drain stamina")
                .getDouble();
        StaminaBar.configArmourWeightModifier = (float) config
                .get(VALUES, "Weight Modifier", 1.0F, "Modifies the amount weight of armour slows decay").getDouble();
        miningSpeed = (float) config
                .get(VALUES, "Mining Modifier", 1.0F, "Modifies the rate block breaking drains stamina").getDouble();
        exhaustDamage = (float) config.get(
                VALUES,
                "Exhausted Damage Modifier",
                1.5F,
                "How much damage you take when out of stamina (1.5 = +50% more damage)").getDouble();

        affectSpeed = config.get(OPTION, "Affect Speed", true, "Slowness effect adds when out of stamina").getBoolean();
        affectMining = config.get(OPTION, "Affect Mining", true, "Breaking blocks drains stamina, adds mining fatigue")
                .getBoolean();

        StaminaBar.levelUp = config
                .get(OTHER, "Level Max Stamina", false, "This makes your experience bar increase max stamina")
                .getBoolean();
        StaminaBar.levelAmount = (float) config
                .get(OTHER, "Level Up Amount", 5F, "How much an experience level increases stamina max").getDouble();
    }

}
