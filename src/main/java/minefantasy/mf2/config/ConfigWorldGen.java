package minefantasy.mf2.config;

public class ConfigWorldGen extends ConfigurationBaseMF {

    public static String copper = "1A: [Ore Gen] Copper Ore";
    public static float copperRarity;
    public static int copperFrequencyMin;
    public static int copperFrequencyMax;
    public static int copperLayerMin;
    public static int copperLayerMax;
    public static int copperSize;

    public static String tin = "1B: [Ore Gen] Tin Ore";
    public static float tinRarity;
    public static int tinFrequencyMin;
    public static int tinFrequencyMax;
    public static int tinLayerMin;
    public static int tinLayerMax;
    public static int tinSize;

    public static String silver = "1C: [Ore Gen] Silver Ore";
    public static float silverRarity;
    public static int silverFrequencyMin;
    public static int silverFrequencyMax;
    public static int silverLayerMin;
    public static int silverLayerMax;
    public static int silverSize;

    public static String wolframite = "1D: [Ore Gen] Wolframite Ore";
    public static float wolframiteRarity;
    public static int wolframiteFrequencyMin;
    public static int wolframiteFrequencyMax;
    public static int wolframiteLayerMin;
    public static int wolframiteLayerMax;
    public static int wolframiteSize;

    public static String mythic = "1E: [Ore Gen] Mythic Ore";
    public static float mythicRarity;
    public static int mythicFrequencyMin;
    public static int mythicFrequencyMax;
    public static int mythicLayerMin;
    public static int mythicLayerMax;
    public static int mythicSize;

    public static String kaolinite = "2A: [Mineral Gen] Kaolinite Deposit";
    public static float kaoliniteRarity;
    public static int kaoliniteFrequencyMin;
    public static int kaoliniteFrequencyMax;
    public static int kaoliniteLayerMin;
    public static int kaoliniteLayerMax;
    public static int kaoliniteSize;

    public static String clay = "2B: [Mineral Gen] Clay Deposit";
    public static float clayRarity;
    public static int clayFrequencyMin;
    public static int clayFrequencyMax;
    public static int clayLayerMin;
    public static int clayLayerMax;
    public static int claySize;

    public static String nitre = "2C: [Mineral Gen] Nitre Deposit";
    public static float nitreRarity;
    public static int nitreFrequencyMin;
    public static int nitreFrequencyMax;
    public static int nitreLayerMin;
    public static int nitreLayerMax;
    public static int nitreSize;

    public static String sulfur = "2D: [Mineral Gen] Sulfur Deposit";
    public static float sulfurRarity;
    public static int sulfurFrequencyMin;
    public static int sulfurFrequencyMax;
    public static int sulfurLayerMin;
    public static int sulfurLayerMax;
    public static int sulfurSize;

    public static String borax = "2E: [Mineral Gen] Borax Deposit";
    public static float boraxRarity;
    public static int boraxFrequencyMin;
    public static int boraxFrequencyMax;
    public static int boraxLayerMin;
    public static int boraxLayerMax;
    public static int boraxSize;

    public static String coal = "2F: [Mineral Gen] Rich Coal Deposit";
    public static float coalRarity;
    public static int coalFrequencyMin;
    public static int coalFrequencyMax;
    public static int coalLayerMin;
    public static int coalLayerMax;
    public static int coalSize;

    public static String limestone = "3A: [Rock Gen] Limestone";
    public static float limestoneRarity;
    public static int limestoneFrequencyMax;
    public static int limestoneFrequencyMin;
    public static int limestoneLayerMax;
    public static int limestoneLayerMin;
    public static int limestoneSize;

    public static String berry = "4A: [Plant Gen] Berry Bush";
    public static float berryRarity;
    public static float berryMinTemp;
    public static float berryMaxTemp;
    public static float berryMinRain;
    public static float berryMaxRain;

    public static String yew = "5A: [Tree Gen] Yew";
    public static float yewRarity;
    public static float yewMinTemp;
    public static float yewMaxTemp;
    public static float yewMinRain;
    public static float yewMaxRain;

    public static String ironbark = "5B: [Tree Gen] Ironbark";
    public static float ironbarkRarity;
    public static float ironbarkMinTemp;
    public static float ironbarkMaxTemp;
    public static float ironbarkMinRain;
    public static float ironbarkMaxRain;

    public static String ebony = "5C: [Tree Gen] Ebony";
    public static float ebonyRarity;
    public static float ebonyMinTemp;
    public static float ebonyMaxTemp;
    public static float ebonyMinRain;
    public static float ebonyMaxRain;

    public static String structure = "6A: [Structure Gen] General";
    public static int structureTickRate;
    public static String ancientForge = "6B: [Structure Gen] Ancient Forge";
    public static float MFChance;
    public static String ancientAlter = "6C: [Structure Gen] Ancient Alter";
    public static float MAChance;
    public static String dwarvenSH = "6D: [Structure Gen] Dwarven Stronghold";
    public static float DSChance;
    public static int DSGrid, DSLength, DSDeviations;

    @Override
    protected void loadConfig() {
        copperRarity = config.getFloat(
                "Copper Rarity",
                copper,
                1.0F,
                0F,
                1F,
                "The chance for copper to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any copper");
        copperFrequencyMin = config.getInt(
                "Copper Frequency Min",
                copper,
                8,
                0,
                256,
                "Copper will try spawn between this and max veins per chunk");
        copperFrequencyMax = config.getInt(
                "Copper Frequency Max",
                copper,
                8,
                0,
                256,
                "Copper will try spawn between min and this veins per chunk");
        copperLayerMin = config.getInt("Copper Layer Min", copper, 48, 0, 255, "Copper veins spawn above this layer");
        copperLayerMax = config.getInt("Copper Layer Max", copper, 96, 0, 255, "Copper veins spawn below this layer");
        copperSize = config.getInt("Copper Size", copper, 8, 0, 512, "How many blocks consist of the vein");

        tinRarity = config.getFloat(
                "Tin Rarity",
                tin,
                1.0F,
                0F,
                1F,
                "The chance for tin to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any tin");
        tinFrequencyMin = config
                .getInt("Tin Frequency Min", tin, 8, 0, 256, "Tin will try spawn between this and max veins per chunk");
        tinFrequencyMax = config
                .getInt("Tin Frequency Max", tin, 8, 0, 256, "Tin will try spawn between min and this veins per chunk");
        tinLayerMin = config.getInt("Tin Layer Min", tin, 48, 0, 255, "Tin veins spawn above this layer");
        tinLayerMax = config.getInt("Tin Layer Max", tin, 96, 0, 255, "Tin veins spawn below this layer");
        tinSize = config.getInt("Tin Size", tin, 5, 0, 512, "How many blocks consist of the vein");

        silverRarity = config.getFloat(
                "Silver Rarity",
                silver,
                1.0F,
                0F,
                1F,
                "The chance for silver to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any ");
        silverFrequencyMin = config.getInt(
                "Silver Frequency Min",
                silver,
                3,
                0,
                256,
                "Silver will try spawn between this and max veins per chunk");
        silverFrequencyMax = config.getInt(
                "Silver Frequency Max",
                silver,
                4,
                0,
                256,
                "Silver will try spawn between min and this veins per chunk");
        silverLayerMin = config.getInt("Silver Layer Min", silver, 0, 0, 255, "Silver veins spawn above this layer");
        silverLayerMax = config.getInt("Silver Layer Max", silver, 32, 0, 255, "Silver veins spawn below this layer");
        silverSize = config.getInt("Silver Size", silver, 8, 0, 512, "How many blocks consist of the vein");

        wolframiteRarity = config.getFloat(
                "Wolframite Rarity",
                wolframite,
                1.0F,
                0F,
                1F,
                "The chance for wolframite to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any ");
        wolframiteFrequencyMin = config.getInt(
                "Wolframite Frequency Min",
                wolframite,
                1,
                0,
                256,
                "Wolframite will try spawn between this and max veins per chunk");
        wolframiteFrequencyMax = config.getInt(
                "Wolframite Frequency Max",
                wolframite,
                1,
                0,
                256,
                "Wolframite will try spawn between min and this veins per chunk");
        wolframiteLayerMin = config
                .getInt("Wolframite Layer Min", wolframite, 0, 0, 255, "Wolframite veins spawn above this layer");
        wolframiteLayerMax = config
                .getInt("Wolframite Layer Max", wolframite, 16, 0, 255, "Wolframite veins spawn below this layer");
        wolframiteSize = config.getInt("Wolframite Size", wolframite, 7, 0, 512, "How many blocks consist of the vein");

        mythicRarity = config.getFloat(
                "Mythic Rarity",
                mythic,
                0.05F,
                0F,
                1F,
                "The chance for mythic to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any mythic");
        mythicFrequencyMin = config.getInt(
                "Mythic Frequency Min",
                mythic,
                2,
                0,
                256,
                "Mythic will try spawn between this and max veins per chunk");
        mythicFrequencyMax = config.getInt(
                "Mythic Frequency Max",
                mythic,
                5,
                0,
                256,
                "Mythic will try spawn between min and this veins per chunk");
        mythicLayerMin = config.getInt("Mythic Layer Min", mythic, 4, 0, 255, "Mythic veins spawn above this layer");
        mythicLayerMax = config.getInt("Mythic Layer Max", mythic, 6, 0, 255, "Mythic veins spawn below this layer");
        mythicSize = config.getInt("Mythic Size", mythic, 8, 0, 512, "How many blocks consist of the vein");

        kaoliniteRarity = config.getFloat(
                "Kaolinite Rarity",
                kaolinite,
                0.25F,
                0F,
                1F,
                "The chance for kaolinite to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any kaolinite");
        kaoliniteFrequencyMin = config.getInt(
                "Kaolinite Frequency Min",
                kaolinite,
                1,
                0,
                256,
                "Kaolinite will try spawn between this and max deposits per chunk");
        kaoliniteFrequencyMax = config.getInt(
                "Kaolinite Frequency Max",
                kaolinite,
                1,
                0,
                256,
                "Kaolinite will try spawn between min and this deposits per chunk");
        kaoliniteLayerMin = config
                .getInt("Kaolinite Layer Min", kaolinite, 48, 0, 255, "Kaolinite deposits spawn above this layer");
        kaoliniteLayerMax = config
                .getInt("Kaolinite Layer Max", kaolinite, 72, 0, 255, "Kaolinite deposits spawn below this layer");
        kaoliniteSize = config
                .getInt("Kaolinite Size", kaolinite, 16, 0, 512, "How many blocks consist of the deposit");

        // Coal: 20 deposits of 16: Up to 320 coal value per chunk
        // Rich Coal: 10 deposits of 8: up to 80 coal value
        coalRarity = config.getFloat(
                "Rich Coal Rarity",
                coal,
                1F,
                0F,
                1F,
                "The chance for rich coal to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any rich coal deposits");
        coalFrequencyMin = config.getInt(
                "Rich Coal Frequency Min",
                coal,
                5,
                0,
                256,
                "Rich Coal will try spawn between this and max deposits per chunk");
        coalFrequencyMax = config.getInt(
                "Rich Coal Frequency Max",
                coal,
                5,
                0,
                256,
                "Rich Coal will try spawn between min and this deposits per chunk");
        coalLayerMin = config
                .getInt("Rich Coal Layer Min", coal, 0, 0, 255, "Rich Coal deposits spawn above this layer");
        coalLayerMax = config
                .getInt("Rich Coal Layer Max", coal, 64, 0, 255, "Rich Coal deposits spawn below this layer");
        coalSize = config.getInt("Rich Coal Size", coal, 8, 0, 512, "How many blocks consist of the deposit");

        clayRarity = config.getFloat(
                "Clay Rarity",
                clay,
                0.15F,
                0F,
                1F,
                "The chance for clay to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any clay");
        clayFrequencyMin = config.getInt(
                "Clay Frequency Min",
                clay,
                1,
                0,
                256,
                "Clay will try spawn between this and max deposits per chunk");
        clayFrequencyMax = config.getInt(
                "Clay Frequency Max",
                clay,
                1,
                0,
                256,
                "Clay will try spawn between min and this deposits per chunk");
        clayLayerMin = config.getInt("Clay Layer Min", clay, 60, 0, 255, "Clay deposits spawn above this layer");
        clayLayerMax = config.getInt("Clay Layer Max", clay, 68, 0, 255, "Clay deposits spawn below this layer");
        claySize = config.getInt("Clay Size", clay, 32, 0, 512, "How many blocks consist of the deposit");

        nitreRarity = config.getFloat(
                "Nitre Rarity",
                nitre,
                1.0F,
                0F,
                1F,
                "The chance for nitre to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any nitre");
        nitreFrequencyMin = config.getInt(
                "Nitre Frequency Min",
                nitre,
                2,
                0,
                256,
                "Nitre will try spawn between this and max deposits per chunk");
        nitreFrequencyMax = config.getInt(
                "Nitre Frequency Max",
                nitre,
                5,
                0,
                256,
                "Nitre will try spawn between min and this deposits per chunk");
        nitreLayerMin = config.getInt("Nitre Layer Min", nitre, 16, 0, 255, "Nitre deposits spawn above this layer");
        nitreLayerMax = config.getInt("Nitre Layer Max", nitre, 64, 0, 255, "Nitre deposits spawn below this layer");
        nitreSize = config.getInt("Nitre Size", nitre, 8, 0, 512, "How many blocks consist of the deposit");

        sulfurRarity = config.getFloat(
                "Sulfur Rarity",
                sulfur,
                1.0F,
                0F,
                1F,
                "The chance for sulfur to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any sulfur");
        sulfurFrequencyMin = config.getInt(
                "Sulfur Frequency Min",
                sulfur,
                6,
                0,
                256,
                "Sulfur will try spawn between this and max deposits per chunk");
        sulfurFrequencyMax = config.getInt(
                "Sulfur Frequency Max",
                sulfur,
                12,
                0,
                256,
                "Sulfur will try spawn between min and this deposits per chunk");
        sulfurLayerMin = config.getInt("Sulfur Layer Min", sulfur, 0, 0, 255, "Sulfur deposits spawn above this layer");
        sulfurLayerMax = config
                .getInt("Sulfur Layer Max", sulfur, 16, 0, 255, "Sulfur deposits spawn below this layer");
        sulfurSize = config.getInt("Sulfur Size", sulfur, 4, 0, 512, "How many blocks consist of the deposit");

        boraxRarity = config.getFloat(
                "Borax Rarity",
                borax,
                0.1F,
                0F,
                1F,
                "The chance for borax to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any borax");
        boraxFrequencyMin = config.getInt(
                "Borax Frequency Min",
                borax,
                5,
                0,
                256,
                "Borax will try spawn between this and max deposits per chunk");
        boraxFrequencyMax = config.getInt(
                "Borax Frequency Max",
                borax,
                10,
                0,
                256,
                "Borax will try spawn between min and this deposits per chunk");
        boraxLayerMin = config.getInt("Borax Layer Min", borax, 48, 0, 255, "Borax deposits spawn above this layer");
        boraxLayerMax = config.getInt("Borax Layer Max", borax, 96, 0, 255, "Borax deposits spawn below this layer");
        boraxSize = config.getInt("Borax Size", borax, 8, 0, 512, "How many blocks consist of the deposit");

        limestoneRarity = config.getFloat(
                "Limestone Rarity",
                limestone,
                2.5E-2F,
                0F,
                1F,
                "The chance for limestone to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any limestone");
        limestoneFrequencyMin = config.getInt(
                "Limestone Frequency Min",
                limestone,
                1,
                0,
                256,
                "Limestone will try spawn between this and max deposits per chunk");
        limestoneFrequencyMax = config.getInt(
                "Limestone Frequency Max",
                limestone,
                1,
                0,
                256,
                "Limestone will try spawn between min and this deposits per chunk");
        limestoneLayerMin = config
                .getInt("Limestone Layer Min", limestone, 48, 0, 255, "Limestone deposits spawn above this layer");
        limestoneLayerMax = config
                .getInt("Limestone Layer Max", limestone, 96, 0, 255, "Limestone deposits spawn below this layer");
        limestoneSize = config
                .getInt("Limestone Size", limestone, 128, 0, 512, "How many blocks consist of the deposit");

        // Trees and Plants use SN values to make them more compact (since their pretty
        // rare)
        berryRarity = config.getFloat(
                "Berry Bush Rarity",
                berry,
                3.0E-2F,
                0F,
                1F,
                "The chance for berry bushes to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any berries");
        yewRarity = config.getFloat(
                "Yew Tree Rarity",
                yew,
                1.0E-3F,
                0F,
                1F,
                "The chance for yew trees to spawn in a chunk. (0=never, 1.0=always), this means many chunks may not have any trees");
        ironbarkRarity = config.getFloat(
                "Ironbark Tree Rarity",
                ironbark,
                1.5E-3F,
                0F,
                1F,
                "The chance for ironbark trees to spawn in a chunk. (0=never, 1.0=always), this means many chunks may not have any trees");
        ebonyRarity = config.getFloat(
                "Ebony Tree Rarity",
                ebony,
                5.0E-4F,
                0F,
                1F,
                "The chance for ebony trees to spawn in a chunk. (0=never, 1.0=always), this means many chunks may not have any trees");

        berryMinTemp = config.getFloat(
                "Berry Bush Spawn Temp Min",
                berry,
                0.2F,
                -1F,
                3F,
                "The minimal biome temperature berries can spawn");
        berryMaxTemp = config.getFloat(
                "Berry Bush Spawn Temp Max",
                berry,
                1.0F,
                -1F,
                3F,
                "The maximum biome temperature berries can spawn");
        berryMinRain = config.getFloat(
                "Berry Bush Spawn Rain Min",
                berry,
                0.3F,
                0F,
                1F,
                "The minimal biome rainfall berries can spawn");
        berryMaxRain = config.getFloat(
                "Berry Bush Spawn Rain Max",
                berry,
                1.0F,
                0F,
                1F,
                "The maximum biome rainfall berries can spawn");

        yewMinTemp = config
                .getFloat("Yew Tree Spawn Temp Min", yew, 0.2F, -1F, 3F, "The minimal biome temperature yew can spawn");
        yewMaxTemp = config
                .getFloat("Yew Tree Spawn Temp Max", yew, 1.0F, -1F, 3F, "The maximum biome temperature yew can spawn");
        yewMinRain = config
                .getFloat("Yew Tree Spawn Rain Min", yew, 0.3F, 0F, 1F, "The minimal biome rainfall yew can spawn");
        yewMaxRain = config
                .getFloat("Yew Tree Spawn Rain Max", yew, 1.0F, 0F, 1F, "The maximum biome rainfall yew can spawn");

        ironbarkMinTemp = config.getFloat(
                "Ironbark Tree Spawn Temp Min",
                ironbark,
                0.4F,
                -1F,
                3F,
                "The minimal biome temperature ironbark can spawn");
        ironbarkMaxTemp = config.getFloat(
                "Ironbark Tree Spawn Temp Max",
                ironbark,
                1.2F,
                -1F,
                3F,
                "The maximum biome temperature ironbark can spawn");
        ironbarkMinRain = config.getFloat(
                "Ironbark Tree Spawn Rain Min",
                ironbark,
                0.0F,
                0F,
                1F,
                "The minimal biome rainfall ironbark can spawn");
        ironbarkMaxRain = config.getFloat(
                "Ironbark Tree Spawn Rain Max",
                ironbark,
                0.8F,
                0F,
                1F,
                "The maximum biome rainfall ironbark can spawn");

        ebonyMinTemp = config.getFloat(
                "Ebony Tree Spawn Temp Min",
                ebony,
                0.2F,
                -1F,
                3F,
                "The minimal biome temperature ebony can spawn");
        ebonyMaxTemp = config.getFloat(
                "Ebony Tree Spawn Temp Max",
                ebony,
                1.0F,
                -1F,
                3F,
                "The maximum biome temperature ebony can spawn");
        ebonyMinRain = config.getFloat(
                "Ebony Tree Spawn Rain Min",
                ebony,
                0.4F,
                0F,
                1F,
                "The minimal biome rainfall ebony can spawn");
        ebonyMaxRain = config.getFloat(
                "Ebony Tree Spawn Rain Max",
                ebony,
                1.0F,
                0F,
                1F,
                "The maximum biome rainfall ebony can spawn");

        structureTickRate = config.getInt(
                "Generation Tick rate",
                structure,
                1,
                1,
                1000,
                "How many ticks (1/20seconds) for each section to generate");
        MFChance = config.getFloat(
                "Spawn Chance",
                ancientForge,
                1.5E-2F,
                0F,
                1F,
                "The chance for this structure to generate in a chunk. (0=never, 1.0=always)");
        MAChance = config.getFloat(
                "Spawn Chance",
                ancientAlter,
                1.0E-2F,
                0F,
                1F,
                "The chance for this structure to generate in a chunk. (0=never, 1.0=always)");
        DSChance = config.getFloat(
                "Spawn Chance",
                dwarvenSH,
                5.0E-2F,
                0F,
                1F,
                "The chance for this structure to generate in a chunk. (0=never, 1.0=always)");

        DSGrid = config.getInt(
                "Stronghold Grid size",
                dwarvenSH,
                8,
                0,
                512,
                "Strongholds generate on a grid on the map to avoid overlaps, this defines the minimum distance between two strongholds (in 16x16 block chunks)");
        DSLength = config.getInt(
                "Stronghold max length",
                dwarvenSH,
                8,
                1,
                64,
                "Max amount of halls making the length of dwarf strongholds (some may be smaller or larger, but this is a guideline)");
        DSDeviations = config.getInt(
                "Stronghold max deviations",
                dwarvenSH,
                1,
                0,
                64,
                "Max amount of intersections in dwarf stronghold hallways (not including living hubs). This can significantly increase complexity of strongholds and in turn lag when generating");

    }

}
