package minefantasy.mf2.config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.client.TooltipEvents;

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
    public static boolean showBlockHud = true;
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
        showBlockHud = config
                .get(
                        CATEGORY_GUI,
                        "Show Native Block HUD",
                        true,
                        "Shows MineFantasy's block meters. Disable when using Waila for station information.")
                .getBoolean();
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

        stam_xOrient = config.getInt(
                "X Orient",
                GUI_STAMINA,
                1,
                -1,
                1,
                "The orientation for the X axis (-1 = left, 0 = middle, 1 = right). Determines what point in the axis to snap to");
        stam_yOrient = config.getInt(
                "Y Orient",
                GUI_STAMINA,
                1,
                -1,
                1,
                "The orientation for the Y axis (-1 = top, 0 = middle, 1 = bottom). Determines what point in the axis to snap to");
        stam_xPos = config.getInt(
                "X Position",
                GUI_STAMINA,
                -82,
                -10000,
                10000,
                "The Offset value away from the orient (-)left, (+)right");
        stam_yPos = config.getInt(
                "Y Position",
                GUI_STAMINA,
                -7,
                -10000,
                10000,
                "The Offset value away from the orient (-)up, (+)down");
        stam_direction = config.getInt(
                "Metre Direction",
                GUI_STAMINA,
                1,
                -1,
                1,
                "The direction the metre goes down: -1 = left to right, 0 = middle, 1 = right to left (May have subtle flaws in altered directions 1 and 0)");

        AR_xOrient = config.getInt(
                "X Orient",
                GUI_ARATING,
                -1,
                -1,
                1,
                "The orientation for the X axis (-1 = left, 0 = middle, 1 = right). Determines what point in the axis to snap to");
        AR_yOrient = config.getInt(
                "Y Orient",
                GUI_ARATING,
                -1,
                -1,
                1,
                "The orientation for the Y axis (-1 = top, 0 = middle, 1 = bottom). Determines what point in the axis to snap to");
        AR_xPos = config.getInt(
                "X Position",
                GUI_ARATING,
                4,
                -10000,
                10000,
                "The Offset value away from the orient (-)left, (+)right");
        AR_yPos = config.getInt(
                "Y Position",
                GUI_ARATING,
                4,
                -10000,
                10000,
                "The Offset value away from the orient (-)up, (+)down");

        AC_xOrient = config.getInt(
                "X Orient",
                GUI_ACOUNT,
                -1,
                -1,
                1,
                "The orientation for the X axis (-1 = left, 0 = middle, 1 = right). Determines what point in the axis to snap to");
        AC_yOrient = config.getInt(
                "Y Orient",
                GUI_ACOUNT,
                -1,
                -1,
                1,
                "The orientation for the Y axis (-1 = top, 0 = middle, 1 = bottom). Determines what point in the axis to snap to");
        AC_xPos = config.getInt(
                "X Position",
                GUI_ACOUNT,
                4,
                -10000,
                10000,
                "The Offset value away from the orient (-)left, (+)right");
        AC_yPos = config.getInt(
                "Y Position",
                GUI_ACOUNT,
                4,
                -10000,
                10000,
                "The Offset value away from the orient (-)up, (+)down");

        CF_xOrient = config.getInt(
                "X Orient",
                GUI_CAFUEL,
                1,
                -1,
                1,
                "The orientation for the X axis (-1 = left, 0 = middle, 1 = right). Determines what point in the axis to snap to");
        CF_yOrient = config.getInt(
                "Y Orient",
                GUI_CAFUEL,
                -1,
                -1,
                1,
                "The orientation for the Y axis (-1 = top, 0 = middle, 1 = bottom). Determines what point in the axis to snap to");
        CF_xPos = config.getInt(
                "X Position",
                GUI_CAFUEL,
                -164,
                -10000,
                10000,
                "The Offset value away from the orient (-)left, (+)right");
        CF_yPos = config.getInt(
                "Y Position",
                GUI_CAFUEL,
                -4,
                -10000,
                10000,
                "The Offset value away from the orient (-)up, (+)down");

        // Render ids are handed out by Forge now
        dropCategory(CATEGORY_BLOCK);

        TooltipEvents.displayOreDict = config.get(
                CATEGORY_DEBUG,
                "Show Debug OreDict",
                false,
                "Displays a list of Ore Dictionary entries to tooltips").getBoolean();
    }

}
