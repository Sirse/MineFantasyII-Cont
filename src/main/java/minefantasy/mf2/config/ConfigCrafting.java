package minefantasy.mf2.config;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;

public class ConfigCrafting extends ConfigurationBaseMF {

    public static final String CATEGORY_REFINING = "Refining";
    public static final String CATEGORY_COOKING = "Cooking";
    public static final String CATEGORY_TRANSFORMATIONS = "Transformations";
    public static boolean allowIronResmelt;

    public static boolean enableTransformations;
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
        TileEntityBlastFH.maxFurnaceHeight = config.get(
                CATEGORY_REFINING,
                "Max Blast Furnace Height",
                16,
                "The max amount of chambers a blast furnace can read").getInt();
        CookRecipe.canCookBasics = config.get(
                CATEGORY_COOKING,
                "Cook non-mf food on cooktop",
                true,
                "This means non-mf food cooked in a furnace can work on a cooking plate").getBoolean();

        enableTransformations = config
                .get(
                        CATEGORY_TRANSFORMATIONS,
                        "Enable Transformations",
                        true,
                        "Hitting blocks with the proper tool transforms them (chopping logs into planks, etc).")
                .getBoolean(true);
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
