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
        copperRarity = (float) config.get(
                copper,
                "Copper Rarity",
                1.0D,
                "The chance for copper to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any copper")
                .getDouble();
        copperFrequencyMin = config
                .get(copper, "Copper Frequency Min", 8, "Copper will try spawn between this and max veins per chunk")
                .getInt();
        copperFrequencyMax = config
                .get(copper, "Copper Frequency Max", 8, "Copper will try spawn between min and this veins per chunk")
                .getInt();
        copperLayerMin = config.get(copper, "Copper Layer Min", 48, "Copper veins spawn above this layer").getInt();
        copperLayerMax = config.get(copper, "Copper Layer Max", 96, "Copper veins spawn below this layer").getInt();
        copperSize = config.get(copper, "Copper Size", 8, "How many blocks consist of the vein").getInt();

        tinRarity = (float) config.get(
                tin,
                "Tin Rarity",
                1.0D,
                "The chance for tin to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any tin")
                .getDouble();
        tinFrequencyMin = config
                .get(tin, "Tin Frequency Min", 8, "Tin will try spawn between this and max veins per chunk").getInt();
        tinFrequencyMax = config
                .get(tin, "Tin Frequency Max", 8, "Tin will try spawn between min and this veins per chunk").getInt();
        tinLayerMin = config.get(tin, "Tin Layer Min", 48, "Tin veins spawn above this layer").getInt();
        tinLayerMax = config.get(tin, "Tin Layer Max", 96, "Tin veins spawn below this layer").getInt();
        tinSize = config.get(tin, "Tin Size", 5, "How many blocks consist of the vein").getInt();

        silverRarity = (float) config.get(
                silver,
                "Silver Rarity",
                1.0D,
                "The chance for silver to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any ")
                .getDouble();
        silverFrequencyMin = config
                .get(silver, "Silver Frequency Min", 3, "Silver will try spawn between this and max veins per chunk")
                .getInt();
        silverFrequencyMax = config
                .get(silver, "Silver Frequency Max", 4, "Silver will try spawn between min and this veins per chunk")
                .getInt();
        silverLayerMin = config.get(silver, "Silver Layer Min", 0, "Silver veins spawn above this layer").getInt();
        silverLayerMax = config.get(silver, "Silver Layer Max", 32, "Silver veins spawn below this layer").getInt();
        silverSize = config.get(silver, "Silver Size", 8, "How many blocks consist of the vein").getInt();

        wolframiteRarity = (float) config.get(
                wolframite,
                "Wolframite Rarity",
                1.0D,
                "The chance for wolframite to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any ")
                .getDouble();
        wolframiteFrequencyMin = config.get(
                wolframite,
                "Wolframite Frequency Min",
                1,
                "Wolframite will try spawn between this and max veins per chunk").getInt();
        wolframiteFrequencyMax = config.get(
                wolframite,
                "Wolframite Frequency Max",
                1,
                "Wolframite will try spawn between min and this veins per chunk").getInt();
        wolframiteLayerMin = config
                .get(wolframite, "Wolframite Layer Min", 0, "Wolframite veins spawn above this layer").getInt();
        wolframiteLayerMax = config
                .get(wolframite, "Wolframite Layer Max", 16, "Wolframite veins spawn below this layer").getInt();
        wolframiteSize = config.get(wolframite, "Wolframite Size", 7, "How many blocks consist of the vein").getInt();

        mythicRarity = (float) config.get(
                mythic,
                "Mythic Rarity",
                0.05D,
                "The chance for mythic to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any mythic")
                .getDouble();
        mythicFrequencyMin = config
                .get(mythic, "Mythic Frequency Min", 2, "Mythic will try spawn between this and max veins per chunk")
                .getInt();
        mythicFrequencyMax = config
                .get(mythic, "Mythic Frequency Max", 5, "Mythic will try spawn between min and this veins per chunk")
                .getInt();
        mythicLayerMin = config.get(mythic, "Mythic Layer Min", 4, "Mythic veins spawn above this layer").getInt();
        mythicLayerMax = config.get(mythic, "Mythic Layer Max", 6, "Mythic veins spawn below this layer").getInt();
        mythicSize = config.get(mythic, "Mythic Size", 8, "How many blocks consist of the vein").getInt();

        kaoliniteRarity = (float) config.get(
                kaolinite,
                "Kaolinite Rarity",
                0.25D,
                "The chance for kaolinite to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any kaolinite")
                .getDouble();
        kaoliniteFrequencyMin = config.get(
                kaolinite,
                "Kaolinite Frequency Min",
                1,
                "Kaolinite will try spawn between this and max deposits per chunk").getInt();
        kaoliniteFrequencyMax = config.get(
                kaolinite,
                "Kaolinite Frequency Max",
                1,
                "Kaolinite will try spawn between min and this deposits per chunk").getInt();
        kaoliniteLayerMin = config
                .get(kaolinite, "Kaolinite Layer Min", 48, "Kaolinite deposits spawn above this layer").getInt();
        kaoliniteLayerMax = config
                .get(kaolinite, "Kaolinite Layer Max", 72, "Kaolinite deposits spawn below this layer").getInt();
        kaoliniteSize = config.get(kaolinite, "Kaolinite Size", 16, "How many blocks consist of the deposit").getInt();

        // Coal: 20 deposits of 16: Up to 320 coal value per chunk
        // Rich Coal: 10 deposits of 8: up to 80 coal value
        coalRarity = (float) config.get(
                coal,
                "Rich Coal Rarity",
                1F,
                "The chance for rich coal to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any rich coal deposits")
                .getDouble();
        coalFrequencyMin = config.get(
                coal,
                "Rich Coal Frequency Min",
                5,
                "Rich Coal will try spawn between this and max deposits per chunk").getInt();
        coalFrequencyMax = config.get(
                coal,
                "Rich Coal Frequency Max",
                5,
                "Rich Coal will try spawn between min and this deposits per chunk").getInt();
        coalLayerMin = config.get(coal, "Rich Coal Layer Min", 0, "Rich Coal deposits spawn above this layer").getInt();
        coalLayerMax = config.get(coal, "Rich Coal Layer Max", 64, "Rich Coal deposits spawn below this layer")
                .getInt();
        coalSize = config.get(coal, "Rich Coal Size", 8, "How many blocks consist of the deposit").getInt();

        clayRarity = (float) config.get(
                clay,
                "Clay Rarity",
                0.15D,
                "The chance for clay to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any clay")
                .getDouble();
        clayFrequencyMin = config
                .get(clay, "Clay Frequency Min", 1, "Clay will try spawn between this and max deposits per chunk")
                .getInt();
        clayFrequencyMax = config
                .get(clay, "Clay Frequency Max", 1, "Clay will try spawn between min and this deposits per chunk")
                .getInt();
        clayLayerMin = config.get(clay, "Clay Layer Min", 60, "Clay deposits spawn above this layer").getInt();
        clayLayerMax = config.get(clay, "Clay Layer Max", 68, "Clay deposits spawn below this layer").getInt();
        claySize = config.get(clay, "Clay Size", 32, "How many blocks consist of the deposit").getInt();

        nitreRarity = (float) config.get(
                nitre,
                "Nitre Rarity",
                1.0D,
                "The chance for nitre to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any nitre")
                .getDouble();
        nitreFrequencyMin = config
                .get(nitre, "Nitre Frequency Min", 2, "Nitre will try spawn between this and max deposits per chunk")
                .getInt();
        nitreFrequencyMax = config
                .get(nitre, "Nitre Frequency Max", 5, "Nitre will try spawn between min and this deposits per chunk")
                .getInt();
        nitreLayerMin = config.get(nitre, "Nitre Layer Min", 16, "Nitre deposits spawn above this layer").getInt();
        nitreLayerMax = config.get(nitre, "Nitre Layer Max", 64, "Nitre deposits spawn below this layer").getInt();
        nitreSize = config.get(nitre, "Nitre Size", 8, "How many blocks consist of the deposit").getInt();

        sulfurRarity = (float) config.get(
                sulfur,
                "Sulfur Rarity",
                1.0D,
                "The chance for sulfur to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any sulfur")
                .getDouble();
        sulfurFrequencyMin = config
                .get(sulfur, "Sulfur Frequency Min", 6, "Sulfur will try spawn between this and max deposits per chunk")
                .getInt();
        sulfurFrequencyMax = config.get(
                sulfur,
                "Sulfur Frequency Max",
                12,
                "Sulfur will try spawn between min and this deposits per chunk").getInt();
        sulfurLayerMin = config.get(sulfur, "Sulfur Layer Min", 0, "Sulfur deposits spawn above this layer").getInt();
        sulfurLayerMax = config.get(sulfur, "Sulfur Layer Max", 16, "Sulfur deposits spawn below this layer").getInt();
        sulfurSize = config.get(sulfur, "Sulfur Size", 4, "How many blocks consist of the deposit").getInt();

        boraxRarity = (float) config.get(
                borax,
                "Borax Rarity",
                0.1D,
                "The chance for borax to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any borax")
                .getDouble();
        boraxFrequencyMin = config
                .get(borax, "Borax Frequency Min", 5, "Borax will try spawn between this and max deposits per chunk")
                .getInt();
        boraxFrequencyMax = config
                .get(borax, "Borax Frequency Max", 10, "Borax will try spawn between min and this deposits per chunk")
                .getInt();
        boraxLayerMin = config.get(borax, "Borax Layer Min", 48, "Borax deposits spawn above this layer").getInt();
        boraxLayerMax = config.get(borax, "Borax Layer Max", 96, "Borax deposits spawn below this layer").getInt();
        boraxSize = config.get(borax, "Borax Size", 8, "How many blocks consist of the deposit").getInt();

        limestoneRarity = (float) config.get(
                limestone,
                "Limestone Rarity",
                2.5E-2D,
                "The chance for limestone to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any limestone")
                .getDouble();
        limestoneFrequencyMin = config.get(
                limestone,
                "Limestone Frequency Min",
                1,
                "Limestone will try spawn between this and max deposits per chunk").getInt();
        limestoneFrequencyMax = config.get(
                limestone,
                "Limestone Frequency Max",
                1,
                "Limestone will try spawn between min and this deposits per chunk").getInt();
        limestoneLayerMin = config
                .get(limestone, "Limestone Layer Min", 48, "Limestone deposits spawn above this layer").getInt();
        limestoneLayerMax = config
                .get(limestone, "Limestone Layer Max", 96, "Limestone deposits spawn below this layer").getInt();
        limestoneSize = config.get(limestone, "Limestone Size", 128, "How many blocks consist of the deposit").getInt();

        // Trees and Plants use SN values to make them more compact (since their pretty
        // rare)
        berryRarity = (float) config.get(
                berry,
                "Berry Bush Rarity",
                3.0E-2D,
                "The chance for berry bushes to spawn in a chunk. (0=never, 1.0=always), this means some chunks may not have any berries")
                .getDouble();
        yewRarity = (float) config.get(
                yew,
                "Yew Tree Rarity",
                1.0E-3D,
                "The chance for yew trees to spawn in a chunk. (0=never, 1.0=always), this means many chunks may not have any trees")
                .getDouble();
        ironbarkRarity = (float) config.get(
                ironbark,
                "Ironbark Tree Rarity",
                1.5E-3D,
                "The chance for ironbark trees to spawn in a chunk. (0=never, 1.0=always), this means many chunks may not have any trees")
                .getDouble();
        ebonyRarity = (float) config.get(
                ebony,
                "Ebony Tree Rarity",
                5.0E-4D,
                "The chance for ebony trees to spawn in a chunk. (0=never, 1.0=always), this means many chunks may not have any trees")
                .getDouble();

        berryMinTemp = (float) config
                .get(berry, "Berry Bush Spawn Temp Min", 0.2D, "The minimal biome temperature berries can spawn")
                .getDouble();
        berryMaxTemp = (float) config
                .get(berry, "Berry Bush Spawn Temp Max", 1.0D, "The maximum biome temperature berries can spawn")
                .getDouble();
        berryMinRain = (float) config
                .get(berry, "Berry Bush Spawn Rain Min", 0.3D, "The minimal biome rainfall berries can spawn")
                .getDouble();
        berryMaxRain = (float) config
                .get(berry, "Berry Bush Spawn Rain Max", 1.0D, "The maximum biome rainfall berries can spawn")
                .getDouble();

        yewMinTemp = (float) config
                .get(yew, "Yew Tree Spawn Temp Min", 0.2D, "The minimal biome temperature yew can spawn").getDouble();
        yewMaxTemp = (float) config
                .get(yew, "Yew Tree Spawn Temp Max", 1.0D, "The maximum biome temperature yew can spawn").getDouble();
        yewMinRain = (float) config
                .get(yew, "Yew Tree Spawn Rain Min", 0.3D, "The minimal biome rainfall yew can spawn").getDouble();
        yewMaxRain = (float) config
                .get(yew, "Yew Tree Spawn Rain Max", 1.0D, "The maximum biome rainfall yew can spawn").getDouble();

        ironbarkMinTemp = (float) config
                .get(ironbark, "Ironbark Tree Spawn Temp Min", 0.4D, "The minimal biome temperature ironbark can spawn")
                .getDouble();
        ironbarkMaxTemp = (float) config
                .get(ironbark, "Ironbark Tree Spawn Temp Max", 1.2D, "The maximum biome temperature ironbark can spawn")
                .getDouble();
        ironbarkMinRain = (float) config
                .get(ironbark, "Ironbark Tree Spawn Rain Min", 0.0D, "The minimal biome rainfall ironbark can spawn")
                .getDouble();
        ironbarkMaxRain = (float) config
                .get(ironbark, "Ironbark Tree Spawn Rain Max", 0.8D, "The maximum biome rainfall ironbark can spawn")
                .getDouble();

        ebonyMinTemp = (float) config
                .get(ebony, "Ebony Tree Spawn Temp Min", 0.2D, "The minimal biome temperature ebony can spawn")
                .getDouble();
        ebonyMaxTemp = (float) config
                .get(ebony, "Ebony Tree Spawn Temp Max", 1.0D, "The maximum biome temperature ebony can spawn")
                .getDouble();
        ebonyMinRain = (float) config
                .get(ebony, "Ebony Tree Spawn Rain Min", 0.4D, "The minimal biome rainfall ebony can spawn")
                .getDouble();
        ebonyMaxRain = (float) config
                .get(ebony, "Ebony Tree Spawn Rain Max", 1.0D, "The maximum biome rainfall ebony can spawn")
                .getDouble();

        structureTickRate = config
                .get(structure, "Generation Tick rate", 1, "How many ticks (1/20seconds) for each section to generate")
                .getInt();
        MFChance = (float) config.get(
                ancientForge,
                "Spawn Chance",
                1.5E-2D,
                "The chance for this structure to generate in a chunk. (0=never, 1.0=always)").getDouble();
        MAChance = (float) config.get(
                ancientAlter,
                "Spawn Chance",
                1.0E-2D,
                "The chance for this structure to generate in a chunk. (0=never, 1.0=always)").getDouble();
        DSChance = (float) config.get(
                dwarvenSH,
                "Spawn Chance",
                5.0E-2D,
                "The chance for this structure to generate in a chunk. (0=never, 1.0=always)").getDouble();

        DSGrid = config.get(
                dwarvenSH,
                "Stronghold Grid size",
                8,
                "Strongholds generate on a grid on the map to avoid overlaps, this defines the minimum distance between two strongholds (in 16x16 block chunks)")
                .getInt();
        DSLength = config.get(
                dwarvenSH,
                "Stronghold max length",
                8,
                "Max amount of halls making the length of dwarf strongholds (some may be smaller or larger, but this is a guideline)")
                .getInt();
        DSDeviations = config.get(
                dwarvenSH,
                "Stronghold max deviations",
                1,
                "Max amount of intersections in dwarf stronghold hallways (not including living hubs). This can significantly increase complexity of strongholds and in turn lag when generating")
                .getInt();

    }

}
