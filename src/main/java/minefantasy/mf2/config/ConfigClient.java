package minefantasy.mf2.config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.crafting.*;
import minefantasy.mf2.block.decor.BlockAmmoBox;
import minefantasy.mf2.block.decor.BlockComponent;
import minefantasy.mf2.block.decor.BlockRack;
import minefantasy.mf2.block.decor.BlockTrough;
import minefantasy.mf2.block.refining.*;
import minefantasy.mf2.mechanics.EventManagerMF;

@SideOnly(Side.CLIENT)
public class ConfigClient extends ConfigurationBaseMF {

    public static final String CATEGORY_GUI = "Gui/Hud Features";

    public static final String CATEGORY_AESTHETIC = "Aesthetics";
    public static final String GUI_STAMINA = "Stamina Bar Positioning";
    public static final String GUI_ARATING = "Armour Rating Positioning";
    public static final String GUI_ACOUNT = "Arrow Count Positioning";
    public static final String GUI_CAFUEL = "Clockwork Armour Fuel Positioning";
    public static final String CATEGORY_BLOCK = "Block Render Ids";
    public static final String CATEGORY_DEBUG = "Debug Info";
    public static boolean playBreath;
    public static boolean playHitsound;
    public static boolean customModel;
    public static int stam_xOrient;
    public static int stam_yOrient;
    public static int stam_xPos;
    public static int stam_yPos;
    public static int stam_direction;
    public static int AR_xOrient;
    public static int AR_yOrient;
    public static int AR_xPos;
    public static int AR_yPos;
    public static int AC_xOrient;
    public static int AC_yOrient;
    public static int AC_xPos;
    public static int AC_yPos;
    public static int CF_xOrient;
    public static int CF_yOrient;
    public static int CF_xPos;
    public static int CF_yPos;

    @Override
    protected void loadConfig() {
        playBreath = config.get(
                CATEGORY_AESTHETIC,
                "Make Breathe Sound",
                true,
                "[With Stamina System] Plays breath sounds when low on energy(sound may be annoying to some...)")
                .getBoolean();
        playHitsound = config.get(
                CATEGORY_AESTHETIC,
                "Make Hit Sound",
                true,
                "Plays sounds when hitting entities with different items").getBoolean();
        customModel = config.get(
                CATEGORY_AESTHETIC,
                "Custom Apparel Model",
                true,
                "Determines if some work apparel (like aprons and clothing) use special models").getBoolean();

        stam_xOrient = config.get(
                GUI_STAMINA,
                "X Orient",
                1,
                "The orientation for the X axis (-1 = left, 0 = middle, 1 = right). Determines what point in the axis to snap to")
                .getInt();
        stam_yOrient = config.get(
                GUI_STAMINA,
                "Y Orient",
                1,
                "The orientation for the Y axis (-1 = top, 0 = middle, 1 = bottom). Determines what point in the axis to snap to")
                .getInt();
        stam_xPos = config
                .get(GUI_STAMINA, "X Position", -82, "The Offset value away from the orient (-)left, (+)right")
                .getInt();
        stam_yPos = config.get(GUI_STAMINA, "Y Position", -7, "The Offset value away from the orient (-)up, (+)down")
                .getInt();
        stam_direction = config.get(
                GUI_STAMINA,
                "Metre Direction",
                1,
                "The direction the metre goes down: -1 = left to right, 0 = middle, 1 = right to left (May have subtle flaws in altered directions 1 and 0)")
                .getInt();

        AR_xOrient = config.get(
                GUI_ARATING,
                "X Orient",
                -1,
                "The orientation for the X axis (-1 = left, 0 = middle, 1 = right). Determines what point in the axis to snap to")
                .getInt();
        AR_yOrient = config.get(
                GUI_ARATING,
                "Y Orient",
                -1,
                "The orientation for the Y axis (-1 = top, 0 = middle, 1 = bottom). Determines what point in the axis to snap to")
                .getInt();
        AR_xPos = config.get(GUI_ARATING, "X Position", 4, "The Offset value away from the orient (-)left, (+)right")
                .getInt();
        AR_yPos = config.get(GUI_ARATING, "Y Position", 4, "The Offset value away from the orient (-)up, (+)down")
                .getInt();

        AC_xOrient = config.get(
                GUI_ACOUNT,
                "X Orient",
                -1,
                "The orientation for the X axis (-1 = left, 0 = middle, 1 = right). Determines what point in the axis to snap to")
                .getInt();
        AC_yOrient = config.get(
                GUI_ACOUNT,
                "Y Orient",
                -1,
                "The orientation for the Y axis (-1 = top, 0 = middle, 1 = bottom). Determines what point in the axis to snap to")
                .getInt();
        AC_xPos = config.get(GUI_ACOUNT, "X Position", 4, "The Offset value away from the orient (-)left, (+)right")
                .getInt();
        AC_yPos = config.get(GUI_ACOUNT, "Y Position", 4, "The Offset value away from the orient (-)up, (+)down")
                .getInt();

        CF_xOrient = config.get(
                GUI_CAFUEL,
                "X Orient",
                1,
                "The orientation for the X axis (-1 = left, 0 = middle, 1 = right). Determines what point in the axis to snap to")
                .getInt();
        CF_yOrient = config.get(
                GUI_CAFUEL,
                "Y Orient",
                -1,
                "The orientation for the Y axis (-1 = top, 0 = middle, 1 = bottom). Determines what point in the axis to snap to")
                .getInt();
        CF_xPos = config.get(GUI_CAFUEL, "X Position", -164, "The Offset value away from the orient (-)left, (+)right")
                .getInt();
        CF_yPos = config.get(GUI_CAFUEL, "Y Position", -4, "The Offset value away from the orient (-)up, (+)down")
                .getInt();

        BlockAnvilMF.anvil_RI = config.get(CATEGORY_BLOCK, "Anvil", -100).getInt();
        BlockCarpenter.carpenter_RI = config.get(CATEGORY_BLOCK, "Carpenter", -101).getInt();
        BlockBombBench.bomb_RI = config.get(CATEGORY_BLOCK, "Bomb Bench", -102).getInt();
        BlockTanningRack.tanner_RI = config.get(CATEGORY_BLOCK, "Tanning Rack", -103).getInt();
        BlockForge.forge_RI = config.get(CATEGORY_BLOCK, "Forge", -104).getInt();
        BlockBellows.bellows_RI = config.get(CATEGORY_BLOCK, "Bellows", -105).getInt();
        BlockResearchStation.research_RI = config.get(CATEGORY_BLOCK, "ResearchTable", -106).getInt();
        BlockBombPress.bpress_RI = config.get(CATEGORY_BLOCK, "Bomb Press", -107).getInt();
        BlockTrough.trough_RI = config.get(CATEGORY_BLOCK, "Trough", -108).getInt();
        BlockBloomery.bloomery_RI = config.get(CATEGORY_BLOCK, "Bloomery", -109).getInt();
        BlockCrossbowBench.crossBench_RI = config.get(CATEGORY_BLOCK, "Crossbow Bench", -110).getInt();
        BlockQuern.quern_RI = config.get(CATEGORY_BLOCK, "Quern", -111).getInt();
        BlockFirepit.firepit_RI = config.get(CATEGORY_BLOCK, "Firepit", -112).getInt();
        BlockRoast.roast_RI = config.get(CATEGORY_BLOCK, "Roast", -113).getInt();
        BlockBigFurnace.furn_RI = config.get(CATEGORY_BLOCK, "Big Furnace", -114).getInt();
        BlockRack.rack_RI = config.get(CATEGORY_BLOCK, "Rack", -115).getInt();
        BlockAmmoBox.ammo_RI = config.get(CATEGORY_BLOCK, "Ammo Box", -116).getInt();
        BlockChimney.pipe_RI = config.get(CATEGORY_BLOCK, "Smoke Pipe", -117).getInt();
        BlockComponent.component_RI = config.get(CATEGORY_BLOCK, "Component Storage", -118).getInt();

        EventManagerMF.displayOreDict = config.get(
                CATEGORY_DEBUG,
                "Show Debug OreDict",
                false,
                "Displays a list of Ore Dictionary entries to tooltips").getBoolean();
    }

}
