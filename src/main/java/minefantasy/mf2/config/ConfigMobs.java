package minefantasy.mf2.config;

import minefantasy.mf2.entity.mob.EntityDragon;

public class ConfigMobs extends ConfigurationBaseMF {

    public static final String BASIC = "1-1: Basic Entities";
    public static final String MOB_DRAGON = "2-4: DRAGON";
    public static final String MOB_MINOTAUR = "5-5: MINOTAUR";
    public static int youngdragonHP, dragonHP, diredragonHP, elderdragonHP, ancientdragonHP;
    public static int youngdragonMD, dragonMD, diredragonMD, elderdragonMD, ancientdragonMD;
    public static int youngdragonFD, dragonFD, diredragonFD, elderdragonFD, ancientdragonFD;
    public static int youngdragonFT, dragonFT, diredragonFT, elderdragonFT, ancientdragonFT;
    public static float dragonChance;
    public static int dragonInterval;
    public static boolean dragonKillNPC;
    public static boolean dragonGriefFire;
    public static boolean dragonGriefGeneral;
    public static boolean dragonMSG;
    public static int minotaurHP, minotaurMD, minotaurGD, minotaurBT, minotaurBD, minotaurDC, minotaurGC, minotaurGCB,
            minotaurTC;
    public static int guardminotaurHP, guardminotaurMD, guardminotaurGD, guardminotaurBT, guardminotaurBD,
            guardminotaurDC, guardminotaurGC, guardminotaurGCB, guardminotaurTC;
    public static int eliteminotaurHP, eliteminotaurMD, eliteminotaurGD, eliteminotaurBT, eliteminotaurBD,
            eliteminotaurDC, eliteminotaurGC, eliteminotaurGCB, eliteminotaurTC;
    public static int bossminotaurHP, bossminotaurMD, bossminotaurGD, bossminotaurBT, bossminotaurBD, bossminotaurDC,
            bossminotaurGC, bossminotaurGCB, bossminotaurTC;
    public static int lightminotaurAR, mediumminotaurAR, heavyminotaurAR, frostminotaurAR, dreadminotaurAR;
    public static int minotaurSpawnrate, minotaurSpawnrateNether;
    public static boolean minotaurBombs = true;

    @Override
    protected void loadConfig() {
        youngdragonHP = config.getInt("2Aa: Health", MOB_DRAGON, 60, 1, 100000, "Young Dragon Stats");
        youngdragonMD = config.getInt("2Ab: Bite dmg", MOB_DRAGON, 4, 0, 10000, "");
        youngdragonFD = config.getInt("2Ab: Fire dmg", MOB_DRAGON, 2, 0, 10000, "");
        youngdragonFT = config.getInt("2Ac: Fire time", MOB_DRAGON, 10, 0, 10000, "");

        dragonHP = config.getInt("2Ba: Health", MOB_DRAGON, 100, 1, 100000, "Adult Dragon Stats");
        dragonMD = config.getInt("2Bb: Bite dmg", MOB_DRAGON, 7, 0, 10000, "");
        dragonFD = config.getInt("2Bb: Fire dmg", MOB_DRAGON, 5, 0, 10000, "");
        dragonFT = config.getInt("2Bc: Fire time", MOB_DRAGON, 40, 0, 10000, "");

        diredragonHP = config.getInt("2Ca: Health", MOB_DRAGON, 200, 1, 100000, "Dire Dragon Stats");
        diredragonMD = config.getInt("2Cb: Bite dmg", MOB_DRAGON, 8, 0, 10000, "");
        diredragonFD = config.getInt("2Cb: Fire dmg", MOB_DRAGON, 8, 0, 10000, "");
        diredragonFT = config.getInt("2Cc: Fire time", MOB_DRAGON, 40, 0, 10000, "");

        elderdragonHP = config.getInt("2Da: Health", MOB_DRAGON, 500, 1, 100000, "Elder Dragon Stats");
        elderdragonMD = config.getInt("2Db: Bite dmg", MOB_DRAGON, 14, 0, 10000, "");
        elderdragonFD = config.getInt("2Db: Fire dmg", MOB_DRAGON, 10, 0, 10000, "");
        elderdragonFT = config.getInt("2Dc: Fire time", MOB_DRAGON, 50, 0, 10000, "");

        ancientdragonHP = config.getInt("2Ea: Health", MOB_DRAGON, 1000, 1, 100000, "Ancient Dragon Stats");
        ancientdragonMD = config.getInt("2Eb: Bite dmg", MOB_DRAGON, 20, 0, 10000, "");
        ancientdragonFD = config.getInt("2Eb: Fire dmg", MOB_DRAGON, 10, 0, 10000, "");
        ancientdragonFT = config.getInt("2Ec: Fire time", MOB_DRAGON, 100, 0, 10000, "");

        dragonInterval = config.getInt(
                "3A: Dragon Spawn Interval",
                MOB_DRAGON,
                12000,
                1,
                10000000,
                "How many ticks between visits (12000 means 4 times a day), there is a chance for a dragon each time");
        dragonChance = config.getFloat(
                "3B: Spawn Chance",
                MOB_DRAGON,
                5F,
                0F,
                100F,
                "A Percent (0-100) chance that a dragon spawns at set times");

        dragonKillNPC = config.get(
                MOB_DRAGON,
                "4A: Kill NPC Grief",
                true,
                "Should dragons kill NPCs (including villages as well as animals/mobs)... Not as determined though")
                .getBoolean();
        dragonGriefFire = config.get(MOB_DRAGON, "4B: Fire Grief", true, "Should fire breath start fires").getBoolean();
        dragonGriefGeneral = config.get(
                MOB_DRAGON,
                "4C: General Block Grief",
                true,
                "Should blocks be frozen by frost breath, melted by fire, or glass shatter with fire and stomping")
                .getBoolean();
        dragonMSG = config
                .get(MOB_DRAGON, "4D: Spawn Message", true, "Will players get a message when dragons enter/leave")
                .getBoolean();
        EntityDragon.interestTimeSeconds = config.getInt(
                "4E: Dragon Interest Time",
                MOB_DRAGON,
                90,
                1,
                100000,
                "How many seconds until a dragon leaves (2x as long if wounded) ");
        EntityDragon.heartChance = config.getFloat(
                "4F: Heart Drop chance modifier",
                MOB_DRAGON,
                1F,
                0F,
                100F,
                "Modify chance of getting a heart");

        minotaurSpawnrate = config.getInt("5Aa: Overworld Spawnrate", MOB_MINOTAUR, 5, 0, 1000, "");
        minotaurSpawnrateNether = config.getInt("5Ab: Nether Spawnrate", MOB_MINOTAUR, 25, 0, 1000, "");
        minotaurBombs = config
                .get(MOB_MINOTAUR, "5Ac: Throw Bombs", true, "Minotaurs that carry bombs throw them at their target")
                .getBoolean();

        minotaurHP = config.getInt("5Ba: Health", MOB_MINOTAUR, 30, 1, 100000, "Lesser Minotaur");
        minotaurMD = config.getInt("5Bb: Pound dmg", MOB_MINOTAUR, 5, 0, 10000, "");
        minotaurGD = config.getInt("5Bc: Gore dmg", MOB_MINOTAUR, 5, 0, 10000, "");
        minotaurBD = config.getInt("5Bd: Beserk dmg", MOB_MINOTAUR, 7, 0, 10000, "");
        minotaurBT = config.getInt("5Be: Beserk threshold (% health)", MOB_MINOTAUR, 25, 0, 100, "");
        minotaurDC = config.getInt("5Bf: Disarm chance when mob power attacks", MOB_MINOTAUR, 10, 0, 100, "");
        minotaurGC = config.getInt("5Bg: Grab Chance", MOB_MINOTAUR, 5, 0, 100, "");
        minotaurGCB = config.getInt("5Bh: Grab Chance (Beserk)", MOB_MINOTAUR, 10, 0, 100, "");
        minotaurTC = config.getInt("5Bi: Throw Chance", MOB_MINOTAUR, 20, 0, 100, "");

        guardminotaurHP = config.getInt("5Ca: Health", MOB_MINOTAUR, 30, 1, 100000, "Minotaur Warrior");
        guardminotaurMD = config.getInt("5Cb: Pound dmg", MOB_MINOTAUR, 6, 0, 10000, "");
        guardminotaurGD = config.getInt("5Cc: Gore dmg", MOB_MINOTAUR, 7, 0, 10000, "");
        guardminotaurBD = config.getInt("5Cd: Beserk dmg", MOB_MINOTAUR, 8, 0, 10000, "");
        guardminotaurBT = config.getInt("5Ce: Beserk threshold (% health)", MOB_MINOTAUR, 35, 0, 100, "");
        guardminotaurDC = config.getInt("5Cf: Disarm chance when mob power attacks", MOB_MINOTAUR, 10, 0, 100, "");
        guardminotaurGC = config.getInt("5Cg: Grab Chance", MOB_MINOTAUR, 5, 0, 100, "");
        guardminotaurGCB = config.getInt("5Ch: Grab Chance (Beserk)", MOB_MINOTAUR, 10, 0, 100, "");
        guardminotaurTC = config.getInt("5Ci: Throw Chance", MOB_MINOTAUR, 20, 0, 100, "");
        lightminotaurAR = config.getInt("5Cj: Armour rating", MOB_MINOTAUR, 100, 0, 10000, "");

        eliteminotaurHP = config.getInt("5Da: Health", MOB_MINOTAUR, 50, 1, 100000, "Elite Minotaur");
        eliteminotaurMD = config.getInt("5Db: Pound dmg", MOB_MINOTAUR, 6, 0, 10000, "");
        eliteminotaurGD = config.getInt("5Dc: Gore dmg", MOB_MINOTAUR, 6, 0, 10000, "");
        eliteminotaurBD = config.getInt("5Dd: Beserk dmg", MOB_MINOTAUR, 8, 0, 10000, "");
        eliteminotaurBT = config.getInt("5De: Beserk threshold (% health)", MOB_MINOTAUR, 40, 0, 100, "");
        eliteminotaurDC = config.getInt("5Df: Disarm chance when mob power attacks", MOB_MINOTAUR, 20, 0, 100, "");
        eliteminotaurGC = config.getInt("5Dg: Grab Chance", MOB_MINOTAUR, 10, 0, 100, "");
        eliteminotaurGCB = config.getInt("5Dh: Grab Chance (Beserk)", MOB_MINOTAUR, 20, 0, 100, "");
        eliteminotaurTC = config.getInt("5Di: Throw Chance", MOB_MINOTAUR, 20, 0, 100, "");
        mediumminotaurAR = config.getInt("5Dj: Armour rating", MOB_MINOTAUR, 400, 0, 10000, "");

        bossminotaurHP = config.getInt("5Ea: Health", MOB_MINOTAUR, 60, 1, 100000, "Minotaur Warlord");
        bossminotaurMD = config.getInt("5Eb: Pound dmg", MOB_MINOTAUR, 8, 0, 10000, "");
        bossminotaurGD = config.getInt("5Ec: Gore dmg", MOB_MINOTAUR, 10, 0, 10000, "");
        bossminotaurBD = config.getInt("5Ed: Beserk dmg", MOB_MINOTAUR, 10, 0, 10000, "");
        bossminotaurBT = config.getInt("5Ee: Beserk threshold (% health)", MOB_MINOTAUR, 40, 0, 100, "");
        bossminotaurDC = config.getInt("5Ef: Disarm chance when mob power attacks", MOB_MINOTAUR, 40, 0, 100, "");
        bossminotaurGC = config.getInt("5Eg: Grab Chance", MOB_MINOTAUR, 20, 0, 100, "");
        bossminotaurGCB = config.getInt("5Eh: Grab Chance (Beserk)", MOB_MINOTAUR, 40, 0, 100, "");
        bossminotaurTC = config.getInt("5Ei: Throw Chance", MOB_MINOTAUR, 20, 0, 100, "");
        heavyminotaurAR = config.getInt("5Ej: Armour rating", MOB_MINOTAUR, 600, 0, 10000, "");

        frostminotaurAR = config.getInt("5Fa: Frost armour bonus", MOB_MINOTAUR, 50, 0, 10000, "Species Bonus");
        dreadminotaurAR = config.getInt("5Fb: Dread armour bonus", MOB_MINOTAUR, 100, 0, 10000, "");

    }

}
