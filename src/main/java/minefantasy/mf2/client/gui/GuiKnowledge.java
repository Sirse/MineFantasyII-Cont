package minefantasy.mf2.client.gui;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.api.knowledge.InformationPage;
import minefantasy.mf2.api.knowledge.ResearchAvailability;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.knowledge.KnowledgeListMF;
import minefantasy.mf2.network.packet.ResearchRequest;

/**
 * The research book as an open spread of the mod's own pages: on the left the open category, the skills in brief and
 * how to get about; on the right a map of the category that can be dragged and zoomed; ribbons down the edge to switch
 * category, and the card of an entry on the left page, where it can be learned or read.
 */
@SideOnly(Side.CLIENT)
public class GuiKnowledge extends GuiScreen {

    /**
     * Grid step between entries on the map, in map pixels: a seal is 26 across, so this leaves room between neighbours
     * for the links and the rings around a seal.
     */
    private static final int CELL = 36;
    /** One page of the book, the same as an entry's pages; two side by side make the spread. */
    private static final int PAGE_WIDTH = BookFrame.PAGE_WIDTH;
    private static final int PAGE_HEIGHT = BookFrame.PAGE_HEIGHT;
    /** The written area of the left page, inside its binding. */
    private static final int TEXT_LEFT = 18;
    private static final int TEXT_WIDTH = 146;
    /** The map fills the right page below the line of the category's count and the close cross. */
    private static final int MAP_LEFT = PAGE_WIDTH;
    private static final int MAP_TOP = 26;
    private static final int MAP_WIDTH = 164;
    private static final int MAP_HEIGHT = 189;
    /** The foot line of the spread: the search on the left page, the centring button on the map beside it. */
    private static final int FOOT_Y = 206;
    /** How far the close cross sits in from the top and right edges of the page, the same both ways. */
    /** The line of the open category's count over the map. */
    private static final int COUNT_Y = 12;
    /** "< 2/5 >" at the right of the foot line, stepping through the search's matches; shown past this many. */
    private static final int SWITCH_WIDTH = 44;
    private static final int SWITCH_ARROW = 16;
    private static final int SWITCH_FROM = 2;
    /**
     * The fold's shadow at the inner edge of the right page, laid back over the map so it sinks in: solid this far from
     * the fold, then fading into the map over as far again.
     */
    private static final int SPINE_SHADOW = 10;
    private static final int SPINE_FADE = 14;
    /** Faint dots where the cells meet, a hint of the grid without its lines. */
    private static final int GRID = 0x24503020;
    /** Compact skill rows on the left page: name and level over a thin bar. */
    private static final int SKILLS_TOP = 12;
    private static final int SKILL_ROW = 16;
    /** Ribbon colours: basics first, then each registered category in order; later ones reuse the last. */
    static final int[] RIBBON_COLOURS = { 0x7A8AA0, 0xA83A2A, 0xA87430, 0x4E8040, 0x3A5A98, 0xC8A040 };
    /** How far the view can scroll, in map pixels. */
    private static final int VIEW_MIN_X = InformationList.minDisplayColumn * CELL - 112;
    private static final int VIEW_MIN_Y = InformationList.minDisplayRow * CELL - 112;
    private static final int VIEW_MAX_X = InformationList.maxDisplayColumn * CELL - 77;
    private static final int VIEW_MAX_Y = InformationList.maxDisplayRow * CELL - 77;
    private static final float ZOOM_MIN = 1.0F;
    private static final float ZOOM_MAX = 3.0F;
    private static final float ZOOM_STEP = 0.25F;

    /** How many locked parents deep an entry still shows: links, dim, dimmer, and dimmest with its name hidden. */
    private static final int LINKS_DEPTH = 1;
    private static final int DIM_DEPTH = 2;
    private static final int HIDDEN_DEPTH = 3;

    /** Links in ink: dark brown between known entries, old gold to what can be learned now, grey towards the rest. */
    private static final int LINK_DISCOVERED = 0xFF5A3A22;
    private static final int LINK_AVAILABLE = 0xFFB08A2A;
    private static final int LINK_LOCKED = 0x906A5A48;
    /** Links among entries still out of reach: faint, so the tree reads whole without giving it away. */
    private static final int LINK_DISTANT = 0x406A5A48;
    /** Seal tints: copper for known entries, gold for those that can be learned now, wax darkening with distance. */
    private static final int SEAL_DISCOVERED = 0xC89060;
    private static final int SEAL_AVAILABLE = 0xE8C860;
    private static final int SEAL_LOCKED = 0xB86A58;
    private static final int SEAL_DIM = 0x9A5A4C;
    private static final int SEAL_DIMMEST = 0x7C4A40;
    private static final int GLOW = 0xF0D070;
    /** Rings around seals in their own shape: warm gold for the pinned entry, clear blue for search matches. */
    private static final int RING_PINNED = 0xE8A830;
    private static final int RING_FOUND = 0x3A7AE0;
    private static final int SPARK = 0xFFE890;
    /** The ceremony for a learned entry, in milliseconds: the seal drops, then light runs to its children. */
    private static final long SEAL_DROP_MS = 450;
    private static final long RUN_START_MS = 300;
    private static final long RUN_MS = 600;
    private static final long FLASH_MS = 600;
    private static final long SPARK_LIFE_MS = 700;
    /** How long a spark of light takes to run the length of a link to something learnable. */
    private static final long FUSE_MS = 1600;
    /** How much a seal rises under the mouse. */
    private static final float LIFT = 1.12F;
    /** How strong the halo around a lit link is: those around the entry under the mouse, and the pinned one's path. */
    private static final float BRANCH_HALO = 0.35F;
    private static final int INK = 0x3A2A1A;
    private static final int INK_FAINT = 0x7A6446;
    /** Card inks: gold for special entries, green for a requirement met, red for one still short. */
    private static final int INK_SPECIAL = 0x8A6410;
    private static final int INK_MET = 0x3A6A2A;
    private static final int INK_SHORT = 0xA03020;
    private static final int INK_LINK = 0x2A4A8A;
    /** The card on the left page: where it starts, and the most lines of description it gives room to. */
    private static final int CARD_TOP = 33;
    /** The entry's item on its card, drawn this many times its usual size. */
    private static final float CARD_ICON_SCALE = 2.0F;
    private static final float TITLE_SCALE = 1.3F;
    /** The "learnable now" list on the overview page: where it starts and how many it names. */
    private static final int LEARNABLE_TOP = 124;
    private static final int LEARNABLE_SHOWN = 5;
    /** Room left around a category when F fits it to the map. */
    private static final int FIT_MARGIN = 16;
    /** How long to wait for the server to confirm a purchase before letting the learn button be pressed again. */
    private static final int PURCHASE_WAIT_TICKS = 40;
    private static final int RULE = 0x40503020;
    /** A skill's bar, after the vanilla experience bar: a dark frame, a sunken track, and a fill lit along its top. */
    private static final int BAR_FRAME = 0xC0301E10;
    private static final int BAR_TRACK = 0x40301E10;
    /** The gleam along a bar: gold at the cap, silver on the way to it. */
    private static final int GLEAM_MAXED = 0xFFE070;
    private static final int GLEAM_RISING = 0xE8EEF4;
    /** Notches across the bar, like the vanilla one's, dividing it into this many parts. */
    private static final int BAR_PARTS = 10;
    private static final int BAR_NOTCH = 0x50301E10;
    /** A gleam running along a bar's fill: how often it passes, how wide it is, and how bright at its middle. */
    private static final long GLEAM_MS = 6000;
    private static final int GLEAM_WIDTH = 28;
    private static final float GLEAM_ALPHA = 0.7F;

    private static final int BUTTON_CLOSE = 2;
    private static final int BUTTON_LEARN = 3;
    private static final int BUTTON_READ = 4;
    private static final int BUTTON_SEARCH = 5;
    private static final int BUTTON_HOME = 6;

    private static final ResourceLocation sealTex = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/book/seal.png");
    private static final ResourceLocation sealSpecialTex = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/book/seal_special.png");
    private static final ResourceLocation sealPerkTex = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/book/seal_perk.png");
    private static final ResourceLocation outlineTex = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/book/outline.png");
    private static final ResourceLocation outlineSpecialTex = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/book/outline_special.png");
    private static final ResourceLocation outlinePerkTex = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/book/outline_perk.png");
    private static final ResourceLocation glowTex = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/book/glow.png");
    private static final ResourceLocation vignetteTex = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/book/vignette.png");

    // The view is kept between openings of the book: where it was last tick, where it is, and where it glides to
    private static double prevViewX, prevViewY;
    private static double viewX, viewY;
    private static double targetViewX, targetViewY;
    private static float zoom = 1.0F;
    private static int lastMouseX, lastMouseY;
    /** 0 while the button is up, 1 once a drag has started inside the map. */
    private static int dragState;
    private static int currentPage = -1;
    /** Whether the view has been placed once; after that each opening of the book finds the map where it was left. */
    private static boolean viewPlaced;

    private final RenderItem itemRender = new RenderItem();
    /** The entry clicked on the map, whose card stays when the mouse moves away. */
    private InformationBase pinned = null;
    private InformationBase highlighted = null;
    /** An entry asked for and not yet confirmed by the server; the book chimes once it is. */
    private InformationBase purchased = null;
    /** The tick the request went out on; a request the server turns down is let go after PURCHASE_WAIT_TICKS. */
    private int purchasedAt;
    private int ticks;
    private final BookFrame frame = new BookFrame();
    /** The entry just learned and when the server confirmed it, for the ceremony that follows; null when none. */
    private InformationBase ceremony;
    private long ceremonyStart;
    private final List<Spark> sparks = new LinkedList<Spark>();
    private GuiButton learnButton;
    private GuiButton searchButton;
    private GuiButton readButton;
    /** Where the card's link to the parent entry was drawn this frame, to be clicked: x, y, width, height. */
    private int[] parentLink;
    /** Lines of the "learnable now" list drawn this frame, to be clicked. */
    private final List<LearnableLine> learnableLines = new LinkedList<LearnableLine>();
    /** Search: whether the player is typing a query, the query, and which match Enter goes to next. */
    private boolean searching;
    private String query = "";
    private int shownMatch = -1;
    /** Whether the pinned entry was reached from the search, so its button shares the foot with the match switch. */
    private boolean fromSearch;
    private LinkedList<InformationBase> informationList = new LinkedList<InformationBase>();
    private EntityPlayer player;

    public GuiKnowledge(EntityPlayer user) {
        this.player = user;
        informationList.clear();
        for (Object info : InformationList.knowledgeList) {
            if (!InformationPage.isInfoInPages((InformationBase) info)) {
                informationList.add((InformationBase) info);
            }
        }
    }

    /** The view with Getting Started near the middle, where the book opens and the home key returns. */
    private int homeViewX() {
        return KnowledgeListMF.gettingStarted.displayColumn * CELL + 11 - MAP_WIDTH / 2;
    }

    private int homeViewY() {
        return KnowledgeListMF.gettingStarted.displayRow * CELL + 11 - MAP_HEIGHT / 2;
    }

    private int folioLeft() {
        return frame.left();
    }

    private int folioTop() {
        return frame.top();
    }

    private int toBook(int screen) {
        return frame.toBook(screen);
    }

    private int mapLeft() {
        return folioLeft() + MAP_LEFT;
    }

    private int mapTop() {
        return folioTop() + MAP_TOP;
    }

    /** How many categories there are, the basics included; ribbon i shows page i - 1. */
    private static int categoryCount() {
        return InformationPage.getInfoPages().size() + 1;
    }

    private static List<InformationBase> entriesOf(int page, List<InformationBase> basics) {
        return page == -1 ? basics : InformationPage.getInfoPage(page).getInfoList();
    }

    /** The category ribbons, basics first, and the ribbon at the top that closes the book. */
    private BookRibbon[] ribbons = new BookRibbon[0];
    private int pointX, pointY;
    private final BookRibbon closeRibbon = new BookRibbon();

    private BookRibbon ribbon(int i) {
        if (ribbons.length != categoryCount()) {
            ribbons = new BookRibbon[categoryCount()];
            for (int j = 0; j < ribbons.length; j++) {
                ribbons[j] = new BookRibbon();
            }
        }
        return ribbons[i];
    }

    private int ribbonY(int i) {
        return BookRibbon.slotY(frame, i, categoryCount());
    }

    /** Moves every ribbon a step towards where it belongs, out for the open category and under the mouse; per frame. */
    private void easeRibbons(int mx, int my) {
        for (int i = 0; i < categoryCount(); i++) {
            BookRibbon ribbon = ribbon(i);
            ribbon.ease(i - 1 == currentPage, BookRibbon.over(frame, ribbonY(i), ribbon.out(), mx, my));
        }
        closeRibbon.ease(false, overClose(mx, my));
    }

    private boolean overClose(int mx, int my) {
        return BookRibbon.over(frame, BookRibbon.closeY(frame), closeRibbon.out(), mx, my);
    }

    /** The ribbon under the mouse, as the page it opens, or -2 for none. */
    private int ribbonAt(int mx, int my) {
        for (int i = 0; i < categoryCount(); i++) {
            if (BookRibbon.over(frame, ribbonY(i), ribbon(i).out(), mx, my)) {
                return i - 1;
            }
        }
        return -2;
    }

    public void initGui() {
        frame.fit(this.width, this.height);
        if (!viewPlaced) {
            viewPlaced = true;
            prevViewX = viewX = targetViewX = homeViewX();
            prevViewY = viewY = targetViewY = homeViewY();
        }
        this.buttonList.clear();
        this.buttonList.add(
                BookButton.area(
                        BUTTON_CLOSE,
                        BookRibbon.edge(frame),
                        BookRibbon.closeY(frame),
                        BookRibbon.OUT_OPEN,
                        BookRibbon.HEIGHT));
        this.buttonList.add(
                searchButton = BookButton.icon(
                        BUTTON_SEARCH,
                        folioLeft() + TEXT_LEFT - 1,
                        folioTop() + FOOT_Y - 2,
                        BookButton.MAGNIFIER));
        this.buttonList.add(BookButton.icon(BUTTON_HOME, mapLeft() + 10, folioTop() + FOOT_Y - 3, BookButton.TARGET));
        int y = folioTop() + FOOT_Y - 3;
        this.buttonList.add(
                learnButton = new BookButton(
                        BUTTON_LEARN,
                        folioLeft() + TEXT_LEFT,
                        y,
                        TEXT_WIDTH,
                        13,
                        I18n.format("knowledge.learn")));
        this.buttonList.add(
                readButton = new BookButton(
                        BUTTON_READ,
                        folioLeft() + TEXT_LEFT,
                        y,
                        TEXT_WIDTH,
                        13,
                        I18n.format("knowledge.read")));
    }

    protected void mouseClicked(int screenX, int screenY, int button) {
        int x = toBook(screenX), y = toBook(screenY);
        int ribbon = button == 0 ? ribbonAt(x, y) : -2;
        if (button == 0 && overButton(x, y)) {
            // A button over the map takes the click, not the seal beneath it
        } else if (button == 0 && switchArrowAt(x, y) != 0) {
            stepMatch(switchArrowAt(x, y));
            this.mc.getSoundHandler()
                    .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
        } else if (ribbon != -2 && ribbon != currentPage) {
            currentPage = ribbon;
            pinned = null;
            this.mc.getSoundHandler().playSound(
                    PositionedSoundRecord.func_147674_a(new ResourceLocation("minefantasy2:block.flipPage"), 1.0F));
        } else if (button == 0 && learnableAt(x, y) != null) {
            goTo(learnableAt(x, y));
        } else if (button == 0 && overParentLink(x, y)) {
            goTo(cardEntry().parentInfo);
        } else if (button == 0 && highlighted != null) {
            // A click pins the entry's card, and a second click lets it go
            pinned = pinned == highlighted ? null : highlighted;
            fromSearch = false;
            this.mc.getSoundHandler()
                    .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
        }
        super.mouseClicked(x, y, button);
    }

    private boolean overButton(int x, int y) {
        for (Object button : this.buttonList) {
            if (((GuiButton) button).mousePressed(this.mc, x, y)) {
                return true;
            }
        }
        return false;
    }

    /** Goes to the next or the previous of the search's matches, round from the last to the first. */
    private void stepMatch(int direction) {
        List<InformationBase> matches = matches();
        if (matches.isEmpty()) {
            return;
        }
        shownMatch = ((shownMatch < 0 && direction < 0 ? 0 : shownMatch) + direction + matches.size()) % matches.size();
        goTo(matches.get(shownMatch));
        fromSearch = true;
    }

    /** Whether the foot line shows the match switch: while searching, or on an entry the search went to. */
    private boolean showsSwitch() {
        return (searching || fromSearch && pinned != null) && matches().size() > SWITCH_FROM;
    }

    /** The switch's arrow under the point: -1 back, 1 on, 0 neither. */
    private int switchArrowAt(int x, int y) {
        if (!showsSwitch()) {
            return 0;
        }
        int centre = folioLeft() + TEXT_LEFT + TEXT_WIDTH - SWITCH_WIDTH / 2, top = folioTop() + FOOT_Y - 3;
        if (y < top || y >= top + 13) {
            return 0;
        }
        for (int direction = -1; direction <= 1; direction += 2) {
            if (Math.abs(x - (centre + direction * SWITCH_ARROW)) <= 6) {
                return direction;
            }
        }
        return 0;
    }

    private void startSearch() {
        searching = true;
        query = "";
        shownMatch = -1;
    }

    /** Back to where the map opens, at its usual size. */
    private void goHome() {
        zoom = ZOOM_MIN;
        targetViewX = homeViewX();
        targetViewY = homeViewY();
    }

    private InformationBase learnableAt(int x, int y) {
        int left = folioLeft() + TEXT_LEFT;
        for (LearnableLine line : learnableLines) {
            if (x >= left && x < left + TEXT_WIDTH && y >= line.top && y < line.top + 10) {
                return line.entry;
            }
        }
        return null;
    }

    private boolean overParentLink(int x, int y) {
        return parentLink != null && x >= parentLink[0]
                && x < parentLink[0] + parentLink[2]
                && y >= parentLink[1]
                && y < parentLink[1] + parentLink[3];
    }

    /** Pins an entry and glides the map to it, turning to its category first when it is in another. */
    private void goTo(InformationBase entry) {
        if (entry == null) {
            return;
        }
        if (!entriesOf(currentPage, informationList).contains(entry)) {
            for (int page = -1; page < categoryCount() - 1; page++) {
                if (entriesOf(page, informationList).contains(entry)) {
                    currentPage = page;
                    break;
                }
            }
        }
        pinned = entry;
        targetViewX = entry.displayColumn * CELL + 11 - MAP_WIDTH * zoom / 2;
        targetViewY = entry.displayRow * CELL + 11 - MAP_HEIGHT * zoom / 2;
    }

    /** The entry whose card the left page shows: the one under the mouse, or else the pinned one. */
    private InformationBase cardEntry() {
        return highlighted != null ? highlighted : pinned;
    }

    protected void actionPerformed(GuiButton pressed) {
        if (pressed.id == BUTTON_CLOSE) {
            this.mc.displayGuiScreen((GuiScreen) null);
            this.mc.setIngameFocus();
        }
        if (pressed.id == BUTTON_LEARN && pinned != null) {
            ((EntityClientPlayerMP) player).sendQueue
                    .addToSendQueue(new ResearchRequest(player, pinned.ID).generatePacket());
            purchased = pinned;
            purchasedAt = ticks;
        }
        if (pressed.id == BUTTON_SEARCH) {
            if (searching) {
                searching = false;
                query = "";
            } else {
                startSearch();
            }
        }
        if (pressed.id == BUTTON_HOME) {
            goHome();
        }
        if (pressed.id == BUTTON_READ && pinned != null) {
            player.openGui(MineFantasyII.instance, 1, player.worldObj, 0, pinned.ID, 0);
        }
    }

    protected void keyTyped(char typedChar, int keyCode) {
        if (searching) {
            typeSearch(typedChar, keyCode);
            return;
        }
        if (keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
            this.mc.displayGuiScreen((GuiScreen) null);
            this.mc.setIngameFocus();
        } else if (typedChar == '/' || keyCode == Keyboard.KEY_F && isCtrlKeyDown()) {
            startSearch();
        } else if (keyCode == Keyboard.KEY_F) {
            fitCategory();
        } else if (keyCode == Keyboard.KEY_HOME || keyCode == Keyboard.KEY_H) {
            goHome();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    /** While searching, keys edit the query: Enter goes to the next match, Escape ends the search. */
    private void typeSearch(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            searching = false;
            query = "";
        } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            List<InformationBase> matches = matches();
            if (!matches.isEmpty()) {
                stepMatch(1);
            }
        } else if (keyCode == Keyboard.KEY_BACK) {
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                shownMatch = -1;
            }
        } else if (ChatAllowedCharacters.isAllowedCharacter(typedChar) && query.length() < 24) {
            query += typedChar;
            shownMatch = -1;
        }
    }

    /**
     * Entries whose name the book shows and that contain the query, from every category; none for an empty query. An
     * entry too far out of reach to be named cannot be found by its name either.
     */
    private List<InformationBase> matches() {
        List<InformationBase> found = new LinkedList<InformationBase>();
        if (query.trim().isEmpty()) {
            return found;
        }
        String wanted = query.trim().toLowerCase();
        for (InformationBase entry : InformationList.knowledgeList) {
            boolean named = ResearchLogic.hasInfoUnlocked(player, entry)
                    || ResearchLogic.func_150874_c(player, entry) < HIDDEN_DEPTH;
            if (named && EnumChatFormatting.getTextWithoutFormattingCodes(entry.getDisplayName()).toLowerCase()
                    .contains(wanted)) {
                found.add(entry);
            }
        }
        return found;
    }

    /** Zooms and moves the map so every entry of the open category that shows is in view. */
    private void fitCategory() {
        float[] box = categoryBox(true);
        if (box == null) {
            return;
        }
        zoom = MathHelper.clamp_float(zoomToHold(box), ZOOM_MIN, maxZoom());
        targetViewX = (box[0] + box[2]) / 2F - MAP_WIDTH * zoom / 2;
        targetViewY = (box[1] + box[3]) / 2F - MAP_HEIGHT * zoom / 2;
    }

    /**
     * How far the map may zoom out: the usual limit, or further for a category too large for it, so that the whole of
     * it can always be seen at once.
     */
    private float maxZoom() {
        float[] box = categoryBox(false);
        return box == null ? ZOOM_MAX : Math.max(ZOOM_MAX, zoomToHold(box));
    }

    /** The least zoom, in whole steps, that holds the box with a margin round it. */
    private static float zoomToHold(float[] box) {
        float needed = Math
                .max((box[2] - box[0] + FIT_MARGIN * 2) / MAP_WIDTH, (box[3] - box[1] + FIT_MARGIN * 2) / MAP_HEIGHT);
        return (float) Math.ceil(needed / ZOOM_STEP) * ZOOM_STEP;
    }

    /**
     * The box round the open category's entries in map pixels, left, top, right and bottom; only those that show, or
     * all of them. Null when there are none.
     */
    private float[] categoryBox(boolean shownOnly) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (InformationBase entry : entriesOf(currentPage, informationList)) {
            if (shownOnly && !ResearchLogic.hasInfoUnlocked(player, entry)
                    && ResearchLogic.func_150874_c(player, entry) > HIDDEN_DEPTH) {
                continue;
            }
            minX = Math.min(minX, entry.displayColumn * CELL);
            minY = Math.min(minY, entry.displayRow * CELL);
            maxX = Math.max(maxX, entry.displayColumn * CELL + 22);
            maxY = Math.max(maxY, entry.displayRow * CELL + 22);
        }
        return minX > maxX ? null : new float[] { minX, minY, maxX, maxY };
    }

    @Override
    protected void mouseMovedOrUp(int screenX, int screenY, int which) {
        super.mouseMovedOrUp(toBook(screenX), toBook(screenY), which);
    }

    @Override
    public void drawScreen(int screenX, int screenY, float partialTicks) {
        int mx = toBook(screenX), my = toBook(screenY);
        handleDrag(mx, my);
        handleZoom(mx, my);
        targetViewX = MathHelper.clamp_double(targetViewX, VIEW_MIN_X, VIEW_MAX_X - 1);
        targetViewY = MathHelper.clamp_double(targetViewY, VIEW_MIN_Y, VIEW_MAX_Y - 1);
        updateButtons();

        this.drawDefaultBackground();
        frame.begin();
        this.renderMainPage(mx, my, partialTicks);
        frame.end();
    }

    /** The card's buttons act on the pinned entry, and show only while its card is the one on the page. */
    private void updateButtons() {
        boolean pinnedShown = pinned != null && cardEntry() == pinned;
        ResearchAvailability state = pinnedShown ? ResearchAvailability.of(player, pinned) : null;
        readButton.visible = state == ResearchAvailability.KNOWN && !pinned.getPages().isEmpty();
        // One short of a skill still shows the button, greyed, so the card says what is missing beside it
        learnButton.visible = state == ResearchAvailability.BUYABLE || state == ResearchAvailability.NEEDS_SKILL;
        learnButton.enabled = state == ResearchAvailability.BUYABLE && purchased != pinned;
        // A chosen entry's button takes the foot of the page, and the search gives way to it
        searchButton.visible = !readButton.visible && !learnButton.visible;
        if (!searchButton.visible && !fromSearch) {
            searching = false;
        }
        int width = TEXT_WIDTH - (showsSwitch() ? SWITCH_WIDTH + 4 : 0);
        ((BookButton) readButton).setWidth(width);
        ((BookButton) learnButton).setWidth(width);
    }

    /** Dragging inside the map moves the view with the mouse, scaled by the zoom. */
    private void handleDrag(int mx, int my) {
        if (!Mouse.isButtonDown(0)) {
            dragState = 0;
            return;
        }
        int left = mapLeft();
        int top = mapTop();
        if (mx >= left && mx < left + MAP_WIDTH && my >= top && my < top + MAP_HEIGHT) {
            if (dragState == 0) {
                dragState = 1;
            } else {
                viewX -= (mx - lastMouseX) * zoom;
                viewY -= (my - lastMouseY) * zoom;
                targetViewX = prevViewX = viewX;
                targetViewY = prevViewY = viewY;
            }
            lastMouseX = mx;
            lastMouseY = my;
        }
    }

    /** The wheel zooms out and in about the point under the mouse, or the middle of the map when outside it. */
    private void handleZoom(int mx, int my) {
        int wheel = Mouse.getDWheel();
        float oldZoom = zoom;
        if (wheel < 0) {
            zoom += ZOOM_STEP;
        } else if (wheel > 0) {
            zoom -= ZOOM_STEP;
        }
        zoom = MathHelper.clamp_float(zoom, ZOOM_MIN, maxZoom());

        if (zoom != oldZoom) {
            int pivotX = mx - mapLeft();
            int pivotY = my - mapTop();
            if (pivotX < 0 || pivotX >= MAP_WIDTH || pivotY < 0 || pivotY >= MAP_HEIGHT) {
                pivotX = MAP_WIDTH / 2;
                pivotY = MAP_HEIGHT / 2;
            }
            // The map point under the pivot stays under it
            viewX += pivotX * (oldZoom - zoom);
            viewY += pivotY * (oldZoom - zoom);
            targetViewX = prevViewX = viewX;
            targetViewY = prevViewY = viewY;
        }
    }

    /** Glides the view towards where it was dragged or zoomed. */
    @Override
    public void updateScreen() {
        ticks++;
        if (purchased != null && ticks - purchasedAt > PURCHASE_WAIT_TICKS) {
            // The server turned it down, or never answered: let the player try again
            purchased = null;
        }
        if (purchased != null && ResearchLogic.hasInfoUnlocked(player, purchased)) {
            startCeremony(purchased);
            purchased = null;
            this.mc.getSoundHandler()
                    .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("random.levelup"), 1.0F));
        }
        prevViewX = viewX;
        prevViewY = viewY;
        double dx = targetViewX - viewX;
        double dy = targetViewY - viewY;

        if (dx * dx + dy * dy < 4.0D) {
            viewX += dx;
            viewY += dy;
        } else {
            viewX += dx * 0.85D;
            viewY += dy * 0.85D;
        }
    }

    protected void renderMainPage(int mx, int my, float partialTicks) {
        int scrollX = MathHelper.floor_double(prevViewX + (viewX - prevViewX) * partialTicks);
        int scrollY = MathHelper.floor_double(prevViewY + (viewY - prevViewY) * partialTicks);
        scrollX = MathHelper.clamp_int(scrollX, VIEW_MIN_X, VIEW_MAX_X - 1);
        scrollY = MathHelper.clamp_int(scrollY, VIEW_MIN_Y, VIEW_MAX_Y - 1);

        int mapX = mapLeft();
        int mapY = mapTop();
        List<InformationBase> entries = entriesOf(currentPage, informationList);

        // The book lies under everything and writes no depth, so the map's items still sort among themselves
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        pointX = mx;
        pointY = my;
        easeRibbons(mx, my);
        drawRibbons(false);
        frame.drawSpread();
        GL11.glDepthMask(true);
        drawRibbons(true);
        drawLeftPage();
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        clipTo(mapX, mapY, MAP_WIDTH, MAP_HEIGHT);
        // Start the map from a clear depth buffer: nothing drawn before it may hide its frames and items
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        this.zLevel = 0.0F;
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glPushMatrix();
        GL11.glTranslatef(mapX, mapY, -200.0F);
        GL11.glScalef(1.0F / zoom, 1.0F / zoom, 0.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);

        drawBackground(scrollX, scrollY);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        drawLinks(entries, scrollX, scrollY);
        InformationBase hovered = drawEntries(entries, scrollX, scrollY, (mx - mapX) * zoom, (my - mapY) * zoom);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawImage(vignetteTex, mapX, mapY, MAP_WIDTH, MAP_HEIGHT);
        // The map runs to the fold: the page's own shadow there goes back on top, so the map sinks into the spine
        drawSpineShadow(folioLeft() + PAGE_WIDTH, mapY);
        frame.drawDust();

        this.zLevel = 0.0F;
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        super.drawScreen(mx, my, partialTicks);

        // Entries under the edge of the page are clipped away, and cannot be picked there either
        boolean overMap = mx >= mapX && mx < mapX + MAP_WIDTH && my >= mapY && my < mapY + MAP_HEIGHT;
        highlighted = overMap ? hovered : null;
        hovered = highlighted;
        if (cardEntry() == null) {
            drawSkillTooltip(mx, my);
        }
        drawRibbonTooltip(mx, my);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        RenderHelper.disableStandardItemLighting();
    }

    /** Faint dots between the cells, moving with the map; the page itself is the paper. */
    private void drawBackground(int scrollX, int scrollY) {
        int width = (int) (MAP_WIDTH * zoom), height = (int) (MAP_HEIGHT * zoom);
        // Where four cells meet, halfway between entries, so the dots never sit under a seal
        int startX = CELL - Math.floorMod(scrollX - 11 - CELL / 2, CELL);
        int startY = CELL - Math.floorMod(scrollY - 11 - CELL / 2, CELL);
        for (int x = startX; x < width; x += CELL) {
            for (int y = startY; y < height; y += CELL) {
                drawRect(x, y, x + 1, y + 1, GRID);
            }
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawLinks(List<InformationBase> entries, int scrollX, int scrollY) {
        Set<InformationBase> lit = litLinks();
        long now = Minecraft.getSystemTime();
        for (InformationBase entry : entries) {
            if (entry.parentInfo == null || !entries.contains(entry.parentInfo)
                    || ResearchLogic.func_150874_c(player, entry) > HIDDEN_DEPTH) {
                continue;
            }
            Link link = new Link(entry, scrollX, scrollY);
            boolean distant = ResearchLogic.func_150874_c(player, entry) > LINKS_DEPTH;
            ResearchAvailability state = ResearchAvailability.of(player, entry);
            int colour = distant ? LINK_DISTANT : LINK_LOCKED;
            if (state == ResearchAvailability.KNOWN) {
                colour = LINK_DISCOVERED;
            } else if (state.isReachable()) {
                colour = LINK_AVAILABLE;
            }
            if (lit.contains(entry)) {
                // A lit link stands out in solid ink with a soft halo; the rest of the map stays as it is
                colour |= 0xFF000000;
                link.draw(fade(colour, BRANCH_HALO), 3);
            }
            link.draw(colour, 1);

            if (state == ResearchAvailability.BUYABLE) {
                // A spark creeps along the link to what can be learned now, from parent to child, and over again
                float along = (now + entry.ID * 211L) % FUSE_MS / (float) FUSE_MS;
                float[] at = link.pointAt(along);
                GL11.glEnable(GL11.GL_BLEND);
                shine(at[0], at[1], 10, SPARK, 0.9F * MathHelper.sin(along * (float) Math.PI));
                GL11.glDisable(GL11.GL_BLEND);
            }
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * The links that stand out, each named by its child entry: those to the parent and children of the entry under the
     * mouse, and the whole path from the pinned entry back to its root.
     */
    private Set<InformationBase> litLinks() {
        Set<InformationBase> lit = new HashSet<InformationBase>();
        if (pinned != null) {
            lit.addAll(branchOf(pinned));
        }
        if (highlighted != null) {
            lit.add(highlighted);
            for (InformationBase entry : InformationList.knowledgeList) {
                if (entry.parentInfo == highlighted) {
                    lit.add(entry);
                }
            }
        }
        return lit;
    }

    private InformationBase drawEntries(List<InformationBase> entries, int scrollX, int scrollY, float mouseX,
            float mouseY) {
        InformationBase hovered = null;
        List<InformationBase> matches = searching ? matches() : new LinkedList<InformationBase>();
        long now = Minecraft.getSystemTime();
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);

        for (InformationBase entry : entries) {
            int x = entry.displayColumn * CELL - scrollX;
            int y = entry.displayRow * CELL - scrollY;
            if (x < -CELL || y < -CELL || x > MAP_WIDTH * zoom || y > MAP_HEIGHT * zoom) {
                continue;
            }
            ResearchAvailability state = ResearchAvailability.of(player, entry);
            boolean known = state == ResearchAvailability.KNOWN;
            boolean available = state.isReachable();
            int depth = ResearchLogic.func_150874_c(player, entry);
            int tint;
            if (known) {
                tint = SEAL_DISCOVERED;
            } else if (available) {
                tint = SEAL_AVAILABLE;
            } else if (depth < DIM_DEPTH) {
                tint = SEAL_LOCKED;
            } else if (depth == DIM_DEPTH) {
                tint = SEAL_DIM;
            } else if (depth == HIDDEN_DEPTH) {
                tint = SEAL_DIMMEST;
            } else {
                continue;
            }
            boolean under = mouseX >= x && mouseX <= x + 22 && mouseY >= y && mouseY <= y + 22;
            if (under) {
                hovered = entry;
            }
            float alpha = 1.0F;

            // The seal rises under the mouse, and a newly learned one drops into place from above
            float scale = entry == highlighted ? LIFT : 1.0F;
            if (entry == ceremony) {
                scale = dropScale(now - ceremonyStart);
            }
            GL11.glPushMatrix();
            GL11.glTranslatef(x + 11, y + 11, 0);
            GL11.glScalef(scale, scale, 1.0F);
            GL11.glTranslatef(-(x + 11), -(y + 11), 0);

            // Forge: blend is needed here, and RenderItem leaks it otherwise
            GL11.glEnable(GL11.GL_BLEND);
            ResourceLocation seal = entry.getSpecial() ? sealSpecialTex : entry.getPerk() ? sealPerkTex : sealTex;
            if (state == ResearchAvailability.BUYABLE) {
                // What can be learned right now breathes with a slow glow
                float pulse = 0.45F + 0.3F * MathHelper.sin(now / 400F);
                tint(GLOW, pulse * alpha);
                drawImage(glowTex, x - 9, y - 9, 40, 40);
            }

            tint(tint, alpha);
            drawImage(seal, x - 2, y - 2, 26, 26);
            // Rings in the seal's own shape, just outside it
            ResourceLocation outline = entry.getSpecial() ? outlineSpecialTex
                    : entry.getPerk() ? outlinePerkTex : outlineTex;
            if (entry == pinned) {
                tint(RING_PINNED, 0.95F);
                drawImage(outline, x - 6, y - 6, 34, 34);
            }
            if (searching && matches.contains(entry)) {
                tint(RING_FOUND, 0.6F + 0.3F * MathHelper.sin(now / 350F));
                drawImage(outline, x - 6, y - 6, 34, 34);
            }
            float flash = childFlash(entry, now);
            if (flash > 0) {
                // A child of the entry just learned lights up as the running light reaches it
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                tint(GLOW, flash);
                drawImage(glowTex, x - 9, y - 9, 40, 40);
                drawImage(seal, x - 2, y - 2, 26, 26);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            GL11.glDisable(GL11.GL_BLEND);

            if (!available && !known) {
                // A locked entry shows its item as a faded ink silhouette on the wax
                GL11.glColor4f(0.30F, 0.20F, 0.14F, 0.85F);
                itemRender.renderWithColor = false;
            }
            GL11.glDisable(GL11.GL_LIGHTING); // Forge: fixes MC-33065
            GL11.glEnable(GL11.GL_CULL_FACE);
            itemRender.renderItemAndEffectIntoGUI(
                    this.mc.fontRenderer,
                    this.mc.getTextureManager(),
                    entry.theItemStack,
                    x + 3,
                    y + 3);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_LIGHTING);
            itemRender.renderWithColor = true;
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }
        drawCeremony(entries, scrollX, scrollY, now);
        return hovered;
    }

    /** The entry and every entry above it, up to its root; null when nothing is under the mouse. */
    private static Set<InformationBase> branchOf(InformationBase entry) {
        if (entry == null) {
            return null;
        }
        Set<InformationBase> branch = new HashSet<InformationBase>();
        for (InformationBase at = entry; at != null && branch.add(at); at = at.parentInfo) {}
        return branch;
    }

    /** Starts the ceremony for an entry the server has just confirmed, throwing sparks off its seal. */
    private void startCeremony(InformationBase entry) {
        ceremony = entry;
        ceremonyStart = Minecraft.getSystemTime();
        float cx = entry.displayColumn * CELL + 11, cy = entry.displayRow * CELL + 11;
        Random random = new Random();
        for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            float speed = 30F + random.nextFloat() * 50F;
            sparks.add(
                    new Spark(
                            cx,
                            cy,
                            (float) Math.cos(angle) * speed,
                            (float) Math.sin(angle) * speed - 30F,
                            ceremonyStart + random.nextInt(120)));
        }
    }

    /** From 1.6 down to 1 with a small bounce as the seal lands, then 1. */
    private static float dropScale(long elapsed) {
        if (elapsed >= SEAL_DROP_MS) {
            return 1.0F;
        }
        float t = elapsed / (float) SEAL_DROP_MS;
        if (t < 0.7F) {
            float f = t / 0.7F;
            return 1.6F - 0.7F * f * f;
        }
        float f = (t - 0.7F) / 0.3F;
        return 0.9F + 0.1F * MathHelper.sin(f * (float) Math.PI / 2);
    }

    /** How brightly a child of the learned entry flashes now: from the moment the running light reaches it. */
    private float childFlash(InformationBase entry, long now) {
        if (ceremony == null || entry.parentInfo != ceremony) {
            return 0F;
        }
        long since = now - ceremonyStart - RUN_START_MS - RUN_MS;
        if (since < 0 || since > FLASH_MS) {
            return 0F;
        }
        return 0.9F * (1F - since / (float) FLASH_MS);
    }

    /** The sparks off the learned seal, and the lights running down its links to the entries it opens. */
    private void drawCeremony(List<InformationBase> entries, int scrollX, int scrollY, long now) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        for (Iterator<Spark> it = sparks.iterator(); it.hasNext();) {
            Spark spark = it.next();
            float t = (now - spark.born) / 1000F;
            if (t > SPARK_LIFE_MS / 1000F) {
                it.remove();
                continue;
            }
            if (t < 0) {
                continue;
            }
            float x = spark.x + spark.speedX * t - scrollX;
            float y = spark.y + spark.speedY * t + 60F * t * t - scrollY;
            shine(x, y, 6, SPARK, 1F - t / (SPARK_LIFE_MS / 1000F));
        }
        if (ceremony != null) {
            long elapsed = now - ceremonyStart;
            float run = (elapsed - RUN_START_MS) / (float) RUN_MS;
            if (run >= 0 && run <= 1) {
                for (InformationBase child : entries) {
                    if (child.parentInfo == ceremony) {
                        float[] at = new Link(child, scrollX, scrollY).pointAt(run);
                        shine(at[0], at[1], 14, SPARK, 1F);
                    }
                }
            }
            if (elapsed > RUN_START_MS + RUN_MS + FLASH_MS) {
                ceremony = null;
            }
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void shine(float x, float y, float size, int rgb, float alpha) {
        frame.shine(x, y, size, rgb, alpha);
    }

    /** The colour with its transparency scaled. */
    private static int fade(int argb, float by) {
        int alpha = (int) ((argb >>> 24) * by);
        return alpha << 24 | argb & 0xFFFFFF;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    protected void drawLeftPage() {
        int x = folioLeft() + TEXT_LEFT;
        int top = folioTop();
        List<InformationBase> entries = entriesOf(currentPage, informationList);
        if (searching && searchButton.visible) {
            drawSearch(x + 14, top + FOOT_Y);
        }
        if (showsSwitch()) {
            drawSwitch(x + TEXT_WIDTH - SWITCH_WIDTH / 2, top + FOOT_Y);
        }
        // The count of the open category heads the map, level with the close cross
        drawCentred(learnedCount(entries), mapLeft() + MAP_WIDTH / 2, top + COUNT_Y, INK_FAINT);

        parentLink = null;
        learnableLines.clear();
        InformationBase card = cardEntry();
        if (card != null) {
            drawTitle(x, top + 11);
            drawCard(card, x, top + CARD_TOP);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            return;
        }

        Skill[] skills = skills();
        for (int a = 0; a < skills.length; a++) {
            drawSkill(x, top + SKILLS_TOP + a * SKILL_ROW, skills[a], ribbonColour(a + 1));
        }
        int below = top + SKILLS_TOP + skills.length * SKILL_ROW;
        drawRect(x, below, x + TEXT_WIDTH, below + 1, RULE);
        // The skills are the same on every ribbon; the category's own part of the page starts under its name
        drawTitle(x, below + 7);

        drawLearnable(x, top + LEARNABLE_TOP);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The open category's name, larger than the rest of the page, over a rule. */
    private void drawTitle(int x, int y) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x + TEXT_WIDTH / 2, y, 0);
        GL11.glScalef(TITLE_SCALE, TITLE_SCALE, 1.0F);
        drawCentred(InformationPage.getTitle(currentPage), 0, 0, INK);
        GL11.glPopMatrix();
        drawRect(x, y + 16, x + TEXT_WIDTH, y + 17, RULE);
    }

    /**
     * An entry's card: its icon and name, where it stands, what it is about, which skills it needs, which entry comes
     * before it and what it leads to. An entry too far out of reach keeps its name and what it is about to itself.
     */
    /** Where the card must end, clear of the learn and read buttons at the foot of the page. */
    private int cardBottom() {
        return folioTop() + PAGE_HEIGHT - 29;
    }

    /**
     * The names of what the entry leads to; entries still too far to name stay unnamed, and those beyond are not
     * mentioned at all.
     */
    private List<String> opensOf(InformationBase entry) {
        List<String> opens = new LinkedList<String>();
        for (InformationBase child : InformationList.knowledgeList) {
            if (child.parentInfo == entry) {
                int depth = ResearchLogic.func_150874_c(player, child);
                if (depth < HIDDEN_DEPTH || ResearchLogic.hasInfoUnlocked(player, child)) {
                    opens.add(child.getDisplayName());
                } else if (depth == HIDDEN_DEPTH) {
                    opens.add(I18n.format("achievement.unknown"));
                }
            }
        }
        return opens;
    }

    private void drawCard(InformationBase entry, int x, int y) {
        ResearchAvailability state = ResearchAvailability.of(player, entry);
        boolean known = state == ResearchAvailability.KNOWN;
        boolean hidden = !known && ResearchLogic.func_150874_c(player, entry) >= HIDDEN_DEPTH;

        // The item large and centred, with the name centred beneath it
        int centre = x + TEXT_WIDTH / 2;
        GL11.glPushMatrix();
        GL11.glTranslatef(centre - 8 * CARD_ICON_SCALE, y, 0);
        GL11.glScalef(CARD_ICON_SCALE, CARD_ICON_SCALE, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        itemRender.renderItemAndEffectIntoGUI(
                this.fontRendererObj,
                this.mc.getTextureManager(),
                entry.theItemStack,
                0,
                0);
        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        String name = hidden ? I18n.format("achievement.unknown") : entry.getDisplayName();
        int line = y + (int) (16 * CARD_ICON_SCALE) + 4;
        for (Object part : this.fontRendererObj.listFormattedStringToWidth(name, TEXT_WIDTH)) {
            drawCentred((String) part, centre, line, entry.getSpecial() ? INK_SPECIAL : INK);
            line += 9;
        }
        line += 2;

        String status;
        int statusInk;
        switch (state) {
            case KNOWN:
                status = I18n.format("knowledge.status.known");
                statusInk = INK_MET;
                break;
            case BUYABLE:
                status = I18n.format("knowledge.status.available");
                statusInk = INK_SPECIAL;
                break;
            case NEEDS_SKILL:
                status = I18n.format("knowledge.status.needsSkill");
                statusInk = INK_SHORT;
                break;
            case AT_TABLE:
                status = I18n.format("knowledge.status.clues");
                statusInk = INK_SPECIAL;
                break;
            default:
                status = I18n.format("knowledge.status.locked");
                statusInk = INK_SHORT;
        }
        drawCentred(status, centre, line, statusInk);
        line += 11;
        drawRect(x, line, x + TEXT_WIDTH, line + 1, RULE);
        line += 4;

        // What comes after the description, worked out first so the description takes only the room that is left
        String[] requirements = entry.getRequiredSkills();
        boolean parentShown = entry.parentInfo != null && !ResearchLogic.hasInfoUnlocked(player, entry.parentInfo);
        List<String> opens = hidden ? new LinkedList<String>() : opensOf(entry);
        int below = (requirements == null ? 0 : requirements.length * 9) + (parentShown ? 11 : 0)
                + (opens.isEmpty() ? 0 : 9);

        if (!hidden) {
            List<String> description = this.fontRendererObj.listFormattedStringToWidth(entry.getSummary(), TEXT_WIDTH);
            int shown = Math.max(1, (cardBottom() - line - 3 - below) / 9);
            for (int i = 0; i < description.size() && i < shown; i++) {
                String text = description.get(i);
                if (i == shown - 1 && description.size() > shown) {
                    text = this.fontRendererObj.trimStringToWidth(text, TEXT_WIDTH - 8) + "...";
                }
                this.fontRendererObj.drawString(text, x, line, INK_FAINT);
                line += 9;
            }
            line += 3;
        }

        // Skills the entry needs, green where met and red where still short
        for (int i = 0; requirements != null && i < requirements.length; i++) {
            boolean met = entry.isUnlocked(i, player);
            this.fontRendererObj.drawString((met ? "+ " : "- ") + requirements[i], x, line, met ? INK_MET : INK_SHORT);
            line += 9;
        }

        // The entry before this one, as a link that takes the map to it
        if (parentShown) {
            String before = I18n.format("knowledge.requires", "");
            String parent = entry.parentInfo.getDisplayName();
            int width = this.fontRendererObj.getStringWidth(before);
            parent = this.fontRendererObj.trimStringToWidth(parent, TEXT_WIDTH - width);
            this.fontRendererObj.drawString(before, x, line, INK_FAINT);
            this.fontRendererObj.drawString(parent, x + width, line, INK_LINK);
            drawRect(
                    x + width,
                    line + 9,
                    x + width + this.fontRendererObj.getStringWidth(parent),
                    line + 10,
                    0x802A4A8A);
            parentLink = new int[] { x + width, line, this.fontRendererObj.getStringWidth(parent), 10 };
            line += 11;
        }

        if (!opens.isEmpty()) {
            String text = I18n.format("knowledge.opens", joined(opens));
            for (Object part : this.fontRendererObj.listFormattedStringToWidth(text, TEXT_WIDTH)) {
                if (line + 9 > cardBottom()) {
                    break;
                }
                this.fontRendererObj.drawString((String) part, x, line, INK_FAINT);
                line += 9;
            }
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * What can be learned right now, from the open category first and then the rest, each line a link that takes the
     * map there; when nothing can be, how to get about the map instead.
     */
    private void drawLearnable(int x, int y) {
        List<InformationBase> learnable = new LinkedList<InformationBase>();
        List<InformationBase> here = entriesOf(currentPage, informationList);
        for (InformationBase entry : InformationList.knowledgeList) {
            if (ResearchAvailability.of(player, entry) == ResearchAvailability.BUYABLE) {
                if (here.contains(entry)) {
                    learnable.add(0, entry);
                } else {
                    learnable.add(entry);
                }
            }
        }
        if (learnable.isEmpty()) {
            this.fontRendererObj.drawSplitString(I18n.format("knowledge.controls"), x, y + 36, TEXT_WIDTH, INK_FAINT);
            return;
        }
        this.fontRendererObj.drawString(I18n.format("knowledge.learnableNow"), x, y, INK);
        int line = y + 12;
        for (int i = 0; i < learnable.size() && i < LEARNABLE_SHOWN; i++) {
            InformationBase entry = learnable.get(i);
            String name = this.fontRendererObj.trimStringToWidth(entry.getDisplayName(), TEXT_WIDTH - 8);
            this.fontRendererObj.drawString("- " + name, x, line, here.contains(entry) ? INK_LINK : INK_FAINT);
            learnableLines.add(new LearnableLine(entry, line));
            line += 10;
        }
        if (learnable.size() > LEARNABLE_SHOWN) {
            this.fontRendererObj.drawString(
                    I18n.format("knowledge.andMore", learnable.size() - LEARNABLE_SHOWN),
                    x,
                    line,
                    INK_FAINT);
        }
    }

    /** The search line, open once the glass is clicked: the query being typed and how many entries it finds. */
    private void drawSearch(int x, int y) {
        int width = TEXT_WIDTH - 14 - (showsSwitch() ? SWITCH_WIDTH + 4 : 0);
        drawRect(x - 2, y - 3, x + width + 2, y + 10, 0x20503020);
        int found = matches().size();
        String caret = Minecraft.getSystemTime() / 500 % 2 == 0 ? "_" : " ";
        String line = query + caret;
        String count = query.trim().isEmpty() ? "" : " (" + found + ")";
        this.fontRendererObj.drawString(
                this.fontRendererObj.trimStringToWidth(line, width - this.fontRendererObj.getStringWidth(count)),
                x,
                y,
                INK);
        this.fontRendererObj.drawString(
                count,
                x + width - this.fontRendererObj.getStringWidth(count),
                y,
                found == 0 ? INK_SHORT : INK_FAINT);
    }

    /** "< 2/5 >": which match is shown of how many, the arrows gold under the mouse. */
    private void drawSwitch(int centre, int y) {
        int count = matches().size();
        String counter = (shownMatch < 0 ? "-" : String.valueOf(shownMatch % count + 1)) + "/" + count;
        drawCentred(counter, centre, y, INK_FAINT);
        int mx = toBook(Mouse.getX() * this.width / this.mc.displayWidth);
        int my = toBook(this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1);
        drawCentred("<", centre - SWITCH_ARROW, y, switchArrowAt(mx, my) < 0 ? 0xB08A2A : INK);
        drawCentred(">", centre + SWITCH_ARROW, y, switchArrowAt(mx, my) > 0 ? 0xB08A2A : INK);
    }

    private static String joined(List<String> parts) {
        StringBuilder text = new StringBuilder();
        for (String part : parts) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(part);
        }
        return text.toString();
    }

    private static Skill[] skills() {
        return new Skill[] { SkillList.artisanry, SkillList.construction, SkillList.provisioning, SkillList.engineering,
                SkillList.combat };
    }

    /** Over a skill in the panel: its level and experience towards the next. */
    private void drawSkillTooltip(int mx, int my) {
        int x = folioLeft() + TEXT_LEFT;
        int y = folioTop() + SKILLS_TOP;
        if (mx < x || mx >= x + TEXT_WIDTH || my < y) {
            return;
        }
        int row = (my - y) / SKILL_ROW;
        Skill[] skills = skills();
        if (row >= skills.length || skills[row] == null) {
            return;
        }
        Skill skill = skills[row];
        int[] xp = skill.getXP(player);
        List<String> lines = new LinkedList<String>();
        lines.add(skill.getDisplayName());
        lines.add(I18n.format("skill.value", RPGElements.getLevel(player, skill)));
        lines.add(
                skill.isMaxed(player) ? I18n.format("knowledge.skillMaxed")
                        : I18n.format("knowledge.skillXP", xp[0], xp[1]));
        drawTooltip(lines, mx, my);
        GL11.glDisable(GL11.GL_LIGHTING);
    }

    /**
     * A skill's name and level in ink, and its progress to the next level as a thin bar under them in the colour of the
     * skill's own ribbon, a silver gleam running along it, gold at the cap.
     */
    protected void drawSkill(int x, int y, Skill skill, int colour) {
        if (skill == null) {
            return;
        }
        boolean maxed = skill.isMaxed(player);
        // At the cap the gold gleam says so; the number stays plain
        String level = String.valueOf(RPGElements.getLevel(player, skill));
        this.fontRendererObj.drawString(skill.getDisplayName(), x, y, INK);
        this.fontRendererObj
                .drawString(level, x + TEXT_WIDTH - this.fontRendererObj.getStringWidth(level), y, INK_FAINT);
        int[] xp = skill.getXP(player);
        // At the cap there is no next level to fill towards: the bar stands full
        float progress = maxed ? 1F : xp[1] > 0 ? (float) Math.min(xp[0], xp[1]) / (float) xp[1] : 0F;
        drawBar(x, y + 9, TEXT_WIDTH, progress, maxed, colour);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** A bar four pixels tall: frame, track, the fill with a lighter line along its top, and the notches over it. */
    private void drawBar(int x, int y, int width, float progress, boolean maxed, int colour) {
        int x1 = x + width;
        drawRect(x, y, x1, y + 1, BAR_FRAME);
        drawRect(x, y + 3, x1, y + 4, BAR_FRAME);
        drawRect(x, y + 1, x + 1, y + 3, BAR_FRAME);
        drawRect(x1 - 1, y + 1, x1, y + 3, BAR_FRAME);
        drawRect(x + 1, y + 1, x1 - 1, y + 3, BAR_TRACK);
        int filled = x + 1 + Math.round((width - 2) * progress);
        if (filled > x + 1) {
            // The skill's own colour, lit along the top
            drawRect(x + 1, y + 1, filled, y + 2, 0xFF000000 | shade(colour, 1.45F));
            drawRect(x + 1, y + 2, filled, y + 3, 0xFF000000 | shade(colour, 0.85F));
        }
        for (int i = 1; i < BAR_PARTS; i++) {
            int notch = x + width * i / BAR_PARTS;
            drawRect(notch, y + 1, notch + 1, y + 3, BAR_NOTCH);
        }
        if (filled > x + 1) {
            drawGleam(x + 1, y + 1, filled - x - 1, maxed ? GLEAM_MAXED : GLEAM_RISING);
        }
    }

    /** A colour made lighter or darker by the factor, each channel held within its range. */
    private static int shade(int rgb, float by) {
        int r = Math.min(255, (int) ((rgb >> 16 & 255) * by));
        int g = Math.min(255, (int) ((rgb >> 8 & 255) * by));
        int b = Math.min(255, (int) ((rgb & 255) * by));
        return r << 16 | g << 8 | b;
    }

    /**
     * A soft white band sweeping along the bar and off its end, then a pause before the next; bars further down the
     * page gleam a little later, so the light seems to run down the list.
     */
    private void drawGleam(int x, int y, int width, int colour) {
        long cycle = GLEAM_MS + (y & 0xFF) * 3L;
        float along = (Minecraft.getSystemTime() + y * 40L) % cycle / (float) GLEAM_MS;
        // The band crosses in the first two fifths of the cycle and rests for the rest
        float middle = x - GLEAM_WIDTH + along * 2.5F * (width + GLEAM_WIDTH * 2);
        for (int column = 0; column < GLEAM_WIDTH; column++) {
            int at = (int) middle - GLEAM_WIDTH / 2 + column;
            if (at < x || at >= x + width) {
                continue;
            }
            float off = Math.abs(column - GLEAM_WIDTH / 2F) / (GLEAM_WIDTH / 2F);
            int alpha = (int) (255 * GLEAM_ALPHA * (1F - off * off));
            if (alpha > 0) {
                drawRect(at, y, at + 1, y + 2, alpha << 24 | colour);
            }
        }
    }

    /** The colour of ribbon i, basics first; later ones reuse the last. */
    static int ribbonColour(int i) {
        return RIBBON_COLOURS[Math.max(0, Math.min(i, RIBBON_COLOURS.length - 1))];
    }

    private int known(List<InformationBase> entries) {
        int known = 0;
        for (InformationBase entry : entries) {
            if (ResearchLogic.hasInfoUnlocked(player, entry)) {
                known++;
            }
        }
        return known;
    }

    /**
     * The category ribbons down the right edge, each tinted its colour, the open one standing out further. Drawn in two
     * passes: the cloth before the pages, which cover its tucked-in end, then the icons on the part that shows.
     */
    private void drawRibbons(boolean icons) {
        int edge = BookRibbon.edge(frame);
        if (icons) {
            closeRibbon.drawMark(
                    frame,
                    BookRibbon.closeY(frame),
                    BookRibbon.CROSS,
                    overClose(pointX, pointY),
                    BookRibbon.CLOSE_HOT);
        } else {
            closeRibbon.drawDarkCloth(frame, BookRibbon.closeY(frame), this.zLevel);
        }
        for (int i = 0; i < categoryCount(); i++) {
            int y = ribbonY(i);
            float out = ribbon(i).out();
            // The icon and its flame ride out with the ribbon
            float shift = out - BookRibbon.OUT;
            if (!icons) {
                BookRibbon.drawCloth(frame, y, out, ribbonColour(i), this.zLevel);
                continue;
            }
            List<InformationBase> entries = entriesOf(i - 1, informationList);
            InformationBase first = i == 0 ? KnowledgeListMF.gettingStarted : entries.isEmpty() ? null : entries.get(0);
            if (first != null && first.theItemStack != null) {
                GL11.glPushMatrix();
                // Three quarters size, centred on the part of a closed ribbon before its swallowtail
                GL11.glTranslatef(edge + 6 + shift, y + (BookRibbon.HEIGHT - 12) / 2F, 0);
                GL11.glScalef(0.75F, 0.75F, 1.0F);
                RenderHelper.enableGUIStandardItemLighting();
                GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                itemRender.renderItemAndEffectIntoGUI(
                        this.fontRendererObj,
                        this.mc.getTextureManager(),
                        first.theItemStack,
                        0,
                        0);
                RenderHelper.disableStandardItemLighting();
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glPopMatrix();
            }
            if (hasLearnable(entries)) {
                // A small flame on the icon's top right corner when the category has something to learn now
                long now = Minecraft.getSystemTime();
                float flicker = 0.55F + 0.25F * MathHelper.sin(now / 170F + i * 1.7F)
                        + 0.15F * MathHelper.sin(now / 53F + i);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                float flameX = edge + 19 + shift, flameY = y + (BookRibbon.HEIGHT - 12) / 2F - 1;
                shine(flameX, flameY, 7, SPARK, flicker);
                shine(flameX, flameY, 3, 0xFFFFFF, flicker);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Whether any entry in the list can be learned right now. */
    private boolean hasLearnable(List<InformationBase> entries) {
        for (InformationBase entry : entries) {
            if (ResearchAvailability.of(player, entry) == ResearchAvailability.BUYABLE) {
                return true;
            }
        }
        return false;
    }

    /**
     * A tooltip at a point in the book's space, kept inside the screen as the book sees it: the vanilla one measures
     * against the screen's own size, which a smaller book does not share.
     */
    private void drawTooltip(List<String> lines, int mx, int my) {
        int screenWidth = this.width, screenHeight = this.height;
        this.width = frame.width();
        this.height = frame.height();
        this.drawHoveringText(lines, mx, my, this.fontRendererObj);
        this.width = screenWidth;
        this.height = screenHeight;
    }

    /** Over a ribbon: the category's name and how much of it is known. */
    private void drawRibbonTooltip(int mx, int my) {
        int page = ribbonAt(mx, my);
        if (page == -2) {
            return;
        }
        List<InformationBase> entries = entriesOf(page, informationList);
        List<String> lines = new LinkedList<String>();
        lines.add(InformationPage.getTitle(page));
        lines.add(EnumChatFormatting.GRAY + learnedCount(entries));
        drawTooltip(lines, mx, my);
        GL11.glDisable(GL11.GL_LIGHTING);
    }

    /** "Learned 3 of 12": the one way every count of entries is put. */
    private String learnedCount(List<InformationBase> entries) {
        return I18n.format("knowledge.learnedCount", known(entries), entries.size());
    }

    private void drawCentred(String text, int centre, int y, int colour) {
        this.fontRendererObj.drawString(text, centre - this.fontRendererObj.getStringWidth(text) / 2, y, colour);
    }

    private static void tint(int rgb, float alpha) {
        BookFrame.tint(rgb, alpha);
    }

    private void drawImage(ResourceLocation texture, double x, double y, double w, double h) {
        frame.drawImage(texture, x, y, w, h, this.zLevel);
    }

    /**
     * The right page's own strip by the fold, drawn back over the map: solid where the fold is darkest, then fading
     * out, so the map runs into the spine without a seam against the vignette.
     */
    private void drawSpineShadow(int x, int y) {
        this.mc.getTextureManager().bindTexture(BookFrame.RIGHT_PAGE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        float texel = 1F / 256F;
        double v0 = MAP_TOP * texel, v1 = (MAP_TOP + MAP_HEIGHT) * texel;
        double u1 = SPINE_SHADOW * texel, u2 = (SPINE_SHADOW + SPINE_FADE) * texel;
        int x1 = x + SPINE_SHADOW, x2 = x1 + SPINE_FADE, bottom = y + MAP_HEIGHT;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(1F, 1F, 1F, 1F);
        tessellator.addVertexWithUV(x, bottom, this.zLevel, 0, v1);
        tessellator.addVertexWithUV(x1, bottom, this.zLevel, u1, v1);
        tessellator.addVertexWithUV(x1, y, this.zLevel, u1, v0);
        tessellator.addVertexWithUV(x, y, this.zLevel, 0, v0);
        tessellator.setColorRGBA_F(1F, 1F, 1F, 1F);
        tessellator.addVertexWithUV(x1, bottom, this.zLevel, u1, v1);
        tessellator.setColorRGBA_F(1F, 1F, 1F, 0F);
        tessellator.addVertexWithUV(x2, bottom, this.zLevel, u2, v1);
        tessellator.addVertexWithUV(x2, y, this.zLevel, u2, v0);
        tessellator.setColorRGBA_F(1F, 1F, 1F, 1F);
        tessellator.addVertexWithUV(x1, y, this.zLevel, u1, v0);
        tessellator.draw();
        GL11.glShadeModel(GL11.GL_FLAT);
    }

    private void clipTo(int x, int y, int w, int h) {
        frame.clipTo(x, y, w, h);
    }

    /** A spark thrown off a newly set seal, in map pixels, with when it was thrown. */
    private static final class Spark {

        final float x, y, speedX, speedY;
        final long born;

        Spark(float x, float y, float speedX, float speedY, long born) {
            this.x = x;
            this.y = y;
            this.speedX = speedX;
            this.speedY = speedY;
            this.born = born;
        }
    }

    /** A line of the "learnable now" list: the entry it names and where it was drawn. */
    private static final class LearnableLine {

        final InformationBase entry;
        final int top;

        LearnableLine(InformationBase entry, int top) {
            this.entry = entry;
            this.top = top;
        }
    }

    /**
     * The path of a link on the map, from the parent's centre straight down or up to the child's row, then across to
     * the child's centre. The line, its halo and the lights that run along it all follow this one path, and it runs
     * under the seals, which cover its ends.
     */
    private static final class Link {

        final int parentX, parentY, childX, childY;

        Link(InformationBase child, int scrollX, int scrollY) {
            this.childX = child.displayColumn * CELL - scrollX + 11;
            this.childY = child.displayRow * CELL - scrollY + 11;
            this.parentX = child.parentInfo.displayColumn * CELL - scrollX + 11;
            this.parentY = child.parentInfo.displayRow * CELL - scrollY + 11;
        }

        /** Draws the path as a line of the given width, centred on it. */
        void draw(int argb, int width) {
            int before = (width - 1) / 2, after = width / 2 + 1;
            drawRect(parentX - before, Math.min(parentY, childY), parentX + after, Math.max(parentY, childY) + 1, argb);
            drawRect(Math.min(parentX, childX), childY - before, Math.max(parentX, childX) + 1, childY + after, argb);
        }

        /** The point part way along the path, from the parent at 0 to the child at 1, on the middle of the line. */
        float[] pointAt(float along) {
            float down = Math.abs(childY - parentY), across = Math.abs(childX - parentX);
            float d = along * (down + across);
            if (d <= down) {
                return new float[] { parentX + 0.5F, parentY + 0.5F + Math.signum(childY - parentY) * d };
            }
            return new float[] { parentX + 0.5F + Math.signum(childX - parentX) * (d - down), childY + 0.5F };
        }
    }
}
