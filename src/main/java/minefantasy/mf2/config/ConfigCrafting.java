package minefantasy.mf2.config;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.Quench;
import minefantasy.mf2.api.heating.TongsHelper;
import minefantasy.mf2.block.decor.BlockRoad;
import minefantasy.mf2.block.tileentity.TileEntityAnvilMF;
import minefantasy.mf2.block.tileentity.TileEntityCrucible;
import minefantasy.mf2.block.tileentity.TileEntityForge;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.mechanics.QuenchFlare;
import minefantasy.mf2.mechanics.TransformationHandler;

public class ConfigCrafting extends ConfigurationBaseMF {

    public static final String CATEGORY_REFINING = "Refining";
    public static final String CATEGORY_COOKING = "Cooking";
    public static final String CATEGORY_TRANSFORMATIONS = "Transformations";
    public static final String CATEGORY_SMITHING = "Smithing";
    public static final String CATEGORY_BLOCKS = "Blocks";
    public static boolean allowIronResmelt;
    public static boolean hotBurn = true;
    public static float hotBurnDamage = 1F;
    public static int hotBurnSeconds = 5;

    public static boolean enableTransformations;
    public static int cobbleHammerHits = 3;
    public static boolean transformationLogChopping;
    public static boolean transformationPlankSawing;
    public static boolean transformationCobbleHammering;
    public static boolean transformationRefinedPlankSawing;

    @Override
    protected void loadConfig() {
        allowIronResmelt = config
                .get(
                        CATEGORY_REFINING,
                        "Allow Iron ingots to make Pig Iron",
                        false,
                        "If you're not resoureful: you can allow iron ingots to make prepared iron for refining.")
                .getBoolean();
        TileEntityBlastFH.maxFurnaceHeight = config.getInt(
                "Max Blast Furnace Height",
                CATEGORY_REFINING,
                16,
                1,
                256,
                "The max amount of chambers a blast furnace can read");
        hotBurn = config.get(
                CATEGORY_SMITHING,
                "Hot burns",
                true,
                "You cannot hold hot items (apron or not), tongs must be used.").getBoolean();
        Heatable.HCCquenchRuin = config.get(
                CATEGORY_SMITHING,
                "Hardcore Quench",
                true,
                "Quenching can crack a hot piece, more so in harsher media, from the wrong heat or without skill. Off: quenching never cracks.")
                .getBoolean();
        hotBurnDamage = config.getFloat(
                "Hot Item Burn Damage",
                CATEGORY_SMITHING,
                1F,
                0F,
                100F,
                "Damage a hot item held without tongs deals each time it burns");
        hotBurnSeconds = config.getInt(
                "Hot Item Burn Seconds",
                CATEGORY_SMITHING,
                5,
                0,
                60,
                "Seconds a hot item held without tongs sets you alight for");
        Quench.CRACK_WEAR = config.getFloat(
                "Quench Crack Wear",
                CATEGORY_SMITHING,
                0.25F,
                0F,
                1F,
                "Share of its durability a piece loses when it cracks in a quench");
        Quench.SKILL_SAFETY = config.getFloat(
                "Quench Skill Safety",
                CATEGORY_SMITHING,
                0.6F,
                0F,
                1F,
                "Share of the crack chance full artisanry skill takes away");
        TileEntityAnvilMF.LOW_TIER_HIT_WINDOW = config.getFloat(
                "Low Tier Anvil Hit Window",
                CATEGORY_SMITHING,
                0.25F,
                0.01F,
                1F,
                "Share of the usual hit window left when working on an anvil below the recipe's tier");
        TileEntityCrucible.smeltTime = config.getFloat(
                "Crucible Smelt Time Modifier",
                CATEGORY_REFINING,
                1F,
                0.01F,
                100F,
                "Multiplies how long a crucible takes to smelt");
        TileEntityForge.fuelUse = config.getFloat(
                "Forge Fuel Use Modifier",
                CATEGORY_REFINING,
                1F,
                0.01F,
                100F,
                "Multiplies the fuel a lit forge burns");
        BlockRoad.speedLevel = config.getInt(
                "Road Speed Level",
                CATEGORY_BLOCKS,
                1,
                0,
                5,
                "Speed level walking on a road gives: 0 gives none");
        Quench.hardening = config.getFloat(
                "Quench Hardening Modifier",
                CATEGORY_SMITHING,
                1F,
                0F,
                10F,
                "Multiplies the quality every metal gains from quenching");
        Quench.cracking = config.getFloat(
                "Quench Crack Modifier",
                CATEGORY_SMITHING,
                1F,
                0F,
                10F,
                "Multiplies every metal's crack chance in a quench; the Hardcore Quench rule still turns cracks off");
        TongsHelper.RAW_WATER_RISK = config.getFloat(
                "Raw Water Risk",
                CATEGORY_SMITHING,
                1.5F,
                1F,
                10F,
                "How much more often a piece cracks quenched in open water or a cauldron than in a trough");
        QuenchFlare.SCALD = config.getFloat(
                "Oil Flare Scald",
                CATEGORY_SMITHING,
                3F,
                0F,
                100F,
                "Damage an oil flare deals when an overheated piece goes into oil");
        QuenchFlare.BURN_SECONDS = config
                .getInt("Oil Flare Burning", CATEGORY_SMITHING, 3, 0, 60, "Seconds the smith burns after an oil flare");
        QuenchFlare.APRON_SCALD = config.getFloat(
                "Oil Flare Scald Through Apron",
                CATEGORY_SMITHING,
                1F,
                0F,
                100F,
                "The flare's damage to a smith in a blacksmith's apron");
        QuenchFlare.APRON_BURN_SECONDS = config.getInt(
                "Oil Flare Burning Through Apron",
                CATEGORY_SMITHING,
                1,
                0,
                60,
                "Seconds a smith in a blacksmith's apron burns after an oil flare");
        CookRecipe.canCookBasics = config.get(
                CATEGORY_COOKING,
                "Cook non-mf food on cooktop",
                true,
                "This means non-mf food cooked in a furnace can work on a cooking plate").getBoolean();

        enableTransformations = config
                .get(
                        CATEGORY_TRANSFORMATIONS,
                        "Enable Transformations",
                        false,
                        "Hitting blocks with the proper tool transforms them (chopping logs into planks, etc).")
                .getBoolean();
        cobbleHammerHits = config.getInt(
                "Cobblestone Hammering Hits",
                CATEGORY_TRANSFORMATIONS,
                3,
                1,
                16,
                "Hammer hits that turn cobblestone into stone bricks");
        TransformationHandler.REPEAT_DELAY = config.getInt(
                "Hit Delay",
                CATEGORY_TRANSFORMATIONS,
                10,
                1,
                100,
                "Ticks before the same block takes another transforming hit");
        transformationLogChopping = config.get(
                CATEGORY_TRANSFORMATIONS,
                "Log Chopping",
                true,
                "Any log + axe turns into planks of the same species.").getBoolean(true);
        transformationPlankSawing = config.get(
                CATEGORY_TRANSFORMATIONS,
                "Plank Sawing",
                true,
                "Any planks + saw turns into two slabs of the same species.").getBoolean(true);
        transformationCobbleHammering = config.get(
                CATEGORY_TRANSFORMATIONS,
                "Cobblestone Hammering",
                true,
                "Cobblestone + hammer over three hits turns into stone bricks.").getBoolean(true);
        transformationRefinedPlankSawing = config.get(
                CATEGORY_TRANSFORMATIONS,
                "Refined Plank Sawing",
                true,
                "Refined MF planks + saw turn into two vanilla planks.").getBoolean(true);
    }

}
