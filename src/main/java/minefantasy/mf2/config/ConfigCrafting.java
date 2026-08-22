package minefantasy.mf2.config;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;

public class ConfigCrafting extends ConfigurationBaseMF {

    public static final String CATEGORY_REFINING = "Refining";
    public static final String CATEGORY_COOKING = "Cooking";
    public static boolean allowIronResmelt;

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
    }

}
