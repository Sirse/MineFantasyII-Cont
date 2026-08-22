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

    @Override
    protected void loadConfig() {
        youngdragonHP = config.get(MOB_DRAGON, "2Aa: Health", 60, "Young Dragon Stats").getInt();
        youngdragonMD = config.get(MOB_DRAGON, "2Ab: Bite dmg", 4).getInt();
        youngdragonFD = config.get(MOB_DRAGON, "2Ab: Fire dmg", 2).getInt();
        youngdragonFT = config.get(MOB_DRAGON, "2Ac: Fire time", 10).getInt();

        dragonHP = config.get(MOB_DRAGON, "2Ba: Health", 100, "Adult Dragon Stats").getInt();
        dragonMD = config.get(MOB_DRAGON, "2Bb: Bite dmg", 7).getInt();
        dragonFD = config.get(MOB_DRAGON, "2Bb: Fire dmg", 5).getInt();
        dragonFT = config.get(MOB_DRAGON, "2Bc: Fire time", 40).getInt();

        diredragonHP = config.get(MOB_DRAGON, "2Ca: Health", 200, "Dire Dragon Stats").getInt();
        diredragonMD = config.get(MOB_DRAGON, "2Cb: Bite dmg", 8).getInt();
        diredragonFD = config.get(MOB_DRAGON, "2Cb: Fire dmg", 8).getInt();
        diredragonFT = config.get(MOB_DRAGON, "2Cc: Fire time", 40).getInt();

        elderdragonHP = config.get(MOB_DRAGON, "2Da: Health", 500, "Elder Dragon Stats").getInt();
        elderdragonMD = config.get(MOB_DRAGON, "2Db: Bite dmg", 14).getInt();
        elderdragonFD = config.get(MOB_DRAGON, "2Db: Fire dmg", 10).getInt();
        elderdragonFT = config.get(MOB_DRAGON, "2Dc: Fire time", 50).getInt();

        ancientdragonHP = config.get(MOB_DRAGON, "2Ea: Health", 1000, "Ancient Dragon Stats").getInt();
        ancientdragonMD = config.get(MOB_DRAGON, "2Eb: Bite dmg", 20).getInt();
        ancientdragonFD = config.get(MOB_DRAGON, "2Eb: Fire dmg", 10).getInt();
        ancientdragonFT = config.get(MOB_DRAGON, "2Ec: Fire time", 100).getInt();

        dragonInterval = config.get(
                MOB_DRAGON,
                "3A: Dragon Spawn Interval",
                12000,
                "How many ticks between visits (12000 means 4 times a day), there is a chance for a dragon each time")
                .getInt();
        dragonChance = (float) config
                .get(MOB_DRAGON, "3B: Spawn Chance", 5F, "A Percent (0-100) chance that a dragon spawns at set times")
                .getDouble();

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
        EntityDragon.interestTimeSeconds = config.get(
                MOB_DRAGON,
                "4E: Dragon Interest Time",
                90,
                "How many seconds until a dragon leaves (2x as long if wounded) ").getInt();
        EntityDragon.heartChance = (float) config
                .get(MOB_DRAGON, "4F: Heart Drop chance modifier", 1F, "Modify chance of getting a heart").getDouble();

        minotaurSpawnrate = config.get(MOB_MINOTAUR, "5Aa: Overworld Spawnrate", 5).getInt();
        minotaurSpawnrateNether = config.get(MOB_MINOTAUR, "5Ab: Nether Spawnrate", 25).getInt();

        minotaurHP = config.get(MOB_MINOTAUR, "5Ba: Health", 30, "Lesser Minotaur").getInt();
        minotaurMD = config.get(MOB_MINOTAUR, "5Bb: Pound dmg", 5).getInt();
        minotaurGD = config.get(MOB_MINOTAUR, "5Bc: Gore dmg", 5).getInt();
        minotaurBD = config.get(MOB_MINOTAUR, "5Bd: Beserk dmg", 7).getInt();
        minotaurBT = config.get(MOB_MINOTAUR, "5Be: Beserk threshold (% health)", 25).getInt();
        minotaurDC = config.get(MOB_MINOTAUR, "5Bf: Disarm chance when mob power attacks", 10).getInt();
        minotaurGC = config.get(MOB_MINOTAUR, "5Bg: Grab Chance", 5).getInt();
        minotaurGCB = config.get(MOB_MINOTAUR, "5Bh: Grab Chance (Beserk)", 10).getInt();
        minotaurTC = config.get(MOB_MINOTAUR, "5Bi: Throw Chance", 20).getInt();

        guardminotaurHP = config.get(MOB_MINOTAUR, "5Ca: Health", 30, "Minotaur Warrior").getInt();
        guardminotaurMD = config.get(MOB_MINOTAUR, "5Cb: Pound dmg", 6).getInt();
        guardminotaurGD = config.get(MOB_MINOTAUR, "5Cc: Gore dmg", 7).getInt();
        guardminotaurBD = config.get(MOB_MINOTAUR, "5Cd: Beserk dmg", 8).getInt();
        guardminotaurBT = config.get(MOB_MINOTAUR, "5Ce: Beserk threshold (% health)", 35).getInt();
        guardminotaurDC = config.get(MOB_MINOTAUR, "5Cf: Disarm chance when mob power attacks", 10).getInt();
        guardminotaurGC = config.get(MOB_MINOTAUR, "5Cg: Grab Chance", 5).getInt();
        guardminotaurGCB = config.get(MOB_MINOTAUR, "5Ch: Grab Chance (Beserk)", 10).getInt();
        guardminotaurTC = config.get(MOB_MINOTAUR, "5Ci: Throw Chance", 20).getInt();
        lightminotaurAR = config.get(MOB_MINOTAUR, "5Cj: Armour rating", 100).getInt();

        eliteminotaurHP = config.get(MOB_MINOTAUR, "5Da: Health", 50, "Elite Minotaur").getInt();
        eliteminotaurMD = config.get(MOB_MINOTAUR, "5Db: Pound dmg", 6).getInt();
        eliteminotaurGD = config.get(MOB_MINOTAUR, "5Dc: Gore dmg", 6).getInt();
        eliteminotaurBD = config.get(MOB_MINOTAUR, "5Dd: Beserk dmg", 8).getInt();
        eliteminotaurBT = config.get(MOB_MINOTAUR, "5De: Beserk threshold (% health)", 40).getInt();
        eliteminotaurDC = config.get(MOB_MINOTAUR, "5Df: Disarm chance when mob power attacks", 20).getInt();
        eliteminotaurGC = config.get(MOB_MINOTAUR, "5Dg: Grab Chance", 10).getInt();
        eliteminotaurGCB = config.get(MOB_MINOTAUR, "5Dh: Grab Chance (Beserk)", 20).getInt();
        eliteminotaurTC = config.get(MOB_MINOTAUR, "5Di: Throw Chance", 20).getInt();
        mediumminotaurAR = config.get(MOB_MINOTAUR, "5Dj: Armour rating", 400).getInt();

        bossminotaurHP = config.get(MOB_MINOTAUR, "5Ea: Health", 60, "Minotaur Warlord").getInt();
        bossminotaurMD = config.get(MOB_MINOTAUR, "5Eb: Pound dmg", 8).getInt();
        bossminotaurGD = config.get(MOB_MINOTAUR, "5Ec: Gore dmg", 10).getInt();
        bossminotaurBD = config.get(MOB_MINOTAUR, "5Ed: Beserk dmg", 10).getInt();
        bossminotaurBT = config.get(MOB_MINOTAUR, "5Ee: Beserk threshold (% health)", 40).getInt();
        bossminotaurDC = config.get(MOB_MINOTAUR, "5Ef: Disarm chance when mob power attacks", 40).getInt();
        bossminotaurGC = config.get(MOB_MINOTAUR, "5Eg: Grab Chance", 20).getInt();
        bossminotaurGCB = config.get(MOB_MINOTAUR, "5Eh: Grab Chance (Beserk)", 40).getInt();
        bossminotaurTC = config.get(MOB_MINOTAUR, "5Ei: Throw Chance", 20).getInt();
        heavyminotaurAR = config.get(MOB_MINOTAUR, "5Ej: Armour rating", 600).getInt();

        frostminotaurAR = config.get(MOB_MINOTAUR, "5Fa: Frost armour bonus", 50, "Species Bonus").getInt();
        dreadminotaurAR = config.get(MOB_MINOTAUR, "5Fb: Dread armour bonus", 100).getInt();

    }

}
