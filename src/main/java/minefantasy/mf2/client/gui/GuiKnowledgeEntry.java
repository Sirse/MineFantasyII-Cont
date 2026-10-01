package minefantasy.mf2.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.InformationPage;
import minefantasy.mf2.api.knowledge.client.EntryPage;
import minefantasy.mf2.api.knowledge.client.EntryPageText;
import minefantasy.mf2.api.knowledge.client.PageFlow;

/** An entry read in the research book: its pages, two to a spread, on the same book as the map. */
@SideOnly(Side.CLIENT)
public class GuiKnowledgeEntry extends GuiScreen {

    private static final int PAGE_WIDTH = BookFrame.PAGE_WIDTH;
    private static final int PAGE_HEIGHT = BookFrame.PAGE_HEIGHT;
    /** How far the first page's text moves down to make room for the heading above it. */
    private static final int HEADING_HEIGHT = 27;
    private static final int LINE_HEIGHT = 9;
    /** How far down a page text may run, clear of the arrows and the page number at its foot. */
    private static final int TEXT_BOTTOM = PAGE_HEIGHT - 30;
    /** Where the first line on either page starts, level with the other. */
    private static final int TEXT_TOP = 26;
    /** From the page's outer edge, past the leather, and from the fold, past its shadow. */
    private static final int MARGIN_OUTER = 21;
    private static final int MARGIN_FOLD = 17;
    private static final int TEXT_WIDTH = PAGE_WIDTH - MARGIN_OUTER - MARGIN_FOLD;
    private static final int LINES_PER_PAGE = (TEXT_BOTTOM - TEXT_TOP) / LINE_HEIGHT;
    private static final int HEADING_LINES = (HEADING_HEIGHT + LINE_HEIGHT - 1) / LINE_HEIGHT;
    private static final int INK = 0x3A2A1A;
    private static final int INK_FAINT = 0x7A6446;
    private static final int INK_SPECIAL = 0x8A6410;
    private static final int RULE = 0x40503020;

    private static final int BUTTON_BACK = 0;
    private static final int BUTTON_CLOSE = 3;
    private static final int BUTTON_NEXT = 1;
    private static final int BUTTON_PREVIOUS = 2;

    private static boolean lastTick = true;
    private static boolean canTick = true;
    private final GuiScreen parentGui;
    private final InformationBase infoBase;
    private final BookFrame frame = new BookFrame();
    private final RenderItem itemRender = new RenderItem();
    /** The entry's pages as laid out for reading: text too long for one page runs on to the next. */
    private final List<EntryPage> laidOut = new ArrayList<EntryPage>();
    private int pages;
    private int currentPage = 0;
    private GuiButton buttonNextPage;
    private GuiButton buttonPreviousPage;
    private GuiButton buttonBack;
    /** The ribbon back to the map and the one that closes the book, one block of controls that act alike. */
    private final BookRibbon backRibbon = new BookRibbon();
    private final BookRibbon closeRibbon = new BookRibbon();

    public GuiKnowledgeEntry(GuiScreen parent, InformationBase info) {
        this.parentGui = parent;
        this.infoBase = info;
    }

    @Override
    public void initGui() {
        frame.fit(this.width, this.height);
        layOut();
        int left = frame.left(), top = frame.top();
        // The arrows sit in the foot's corners, the same margin in from the edge as the text
        int arrowY = top + PAGE_HEIGHT - 27;
        this.buttonList.clear();
        // Back to the map from the ribbon under the ribbon that closes the book
        this.buttonList.add(
                buttonBack = BookButton
                        .area(BUTTON_BACK, BookRibbon.edge(frame), ribbonY(), BookRibbon.OUT_OPEN, BookRibbon.HEIGHT));
        this.buttonList.add(
                BookButton.area(
                        BUTTON_CLOSE,
                        BookRibbon.edge(frame),
                        BookRibbon.closeY(frame),
                        BookRibbon.OUT_OPEN,
                        BookRibbon.HEIGHT));
        this.buttonList.add(
                buttonNextPage = BookButton.turn(BUTTON_NEXT, left + PAGE_WIDTH * 2 - MARGIN_OUTER - 14, arrowY, true));
        this.buttonList.add(buttonPreviousPage = BookButton.turn(BUTTON_PREVIOUS, left + MARGIN_OUTER, arrowY, false));
        this.updateButtons();
    }

    /** The entry's pages as {@link PageFlow} lays them out, text drawn by this screen and the rest by the page. */
    private void layOut() {
        List<EntryPage> source = infoBase.getPages();
        List<List<String>> texts = new ArrayList<List<String>>();
        for (EntryPage page : source) {
            texts.add(page instanceof EntryPageText ? ((EntryPageText) page).getLines(TEXT_WIDTH) : null);
        }
        laidOut.clear();
        for (PageFlow.Page page : PageFlow.lay(texts, LINES_PER_PAGE - HEADING_LINES, LINES_PER_PAGE)) {
            laidOut.add(
                    page.lines == null ? source.get(page.source)
                            : new FlowPage(page.lines, laidOut.size() % 2 == 0, page.endsText));
        }
        pages = laidOut.size();
        currentPage = Math.max(0, Math.min(currentPage, pages - 1) & ~1);
    }

    private void updateButtons() {
        this.buttonNextPage.visible = this.currentPage < this.pages - 2;
        this.buttonPreviousPage.visible = this.currentPage > 1;
    }

    @Override
    public void drawScreen(int screenX, int screenY, float f) {
        int mx = frame.toBook(screenX), my = frame.toBook(screenY);
        boolean onTick = false;
        boolean currTick = mc.theWorld.getTotalWorldTime() % 10 == 0;
        if (currTick != lastTick) {
            canTick = !canTick;
            if (canTick) {
                onTick = true;
            }
        }
        lastTick = currTick;

        this.drawDefaultBackground();
        frame.begin();
        drawRibbons(mx, my);
        frame.drawSpread();
        closeRibbon
                .drawMark(frame, BookRibbon.closeY(frame), BookRibbon.CROSS, overClose(mx, my), BookRibbon.CLOSE_HOT);
        backRibbon.drawMark(
                frame,
                ribbonY(),
                BookRibbon.BACK,
                BookRibbon.over(frame, ribbonY(), backRibbon.out(), mx, my),
                BookRibbon.BACK_HOT);
        int left = frame.left(), top = frame.top();
        drawPage(mx, my, f, currentPage, left, top, onTick);
        drawPage(mx, my, f, currentPage + 1, left + PAGE_WIDTH, top, onTick);
        drawPageNumber(left, top);
        for (int side = 0; side < 2 && currentPage + side < pages; side++) {
            laidOut.get(currentPage + side).drawOverlay(mx, my);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        frame.drawDust();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        super.drawScreen(mx, my, f);
        frame.end();
    }

    private void drawPage(int mx, int my, float f, int num, int x, int y, boolean onTick) {
        if (num >= pages) {
            return;
        }
        EntryPage page = laidOut.get(num);
        if (page == null) {
            return;
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        int pageY = y;
        if (num == 0 && page instanceof FlowPage) {
            // The entry opens with its heading, the text beneath it
            drawHeading(x, y);
            pageY += HEADING_HEIGHT;
        }
        page.preRender(this, mx, my, f, x, pageY, onTick);
        page.render(this, mx, my, f, x, pageY, onTick);
    }

    /** The entry's item, its name, and the category it belongs to, over a thin rule. */
    private void drawHeading(int x, int y) {
        int left = x + MARGIN_OUTER, top = y + TEXT_TOP;
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        itemRender.renderItemAndEffectIntoGUI(
                this.fontRendererObj,
                this.mc.getTextureManager(),
                infoBase.theItemStack,
                left,
                top);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        int textLeft = left + 20, textWidth = TEXT_WIDTH - 20;
        String name = this.fontRendererObj.trimStringToWidth(infoBase.getDisplayName(), textWidth);
        this.fontRendererObj.drawString(name, textLeft, top, infoBase.getSpecial() ? INK_SPECIAL : INK);
        String category = this.fontRendererObj.trimStringToWidth(categoryTitle(), textWidth);
        this.fontRendererObj.drawString(category, textLeft, top + 9, INK_FAINT);
        drawRect(left, top + 20, left + TEXT_WIDTH, top + 21, RULE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The name of the category the entry is filed under, the basics when it is in none. */
    private String categoryTitle() {
        return InformationPage.getTitle(categoryIndex());
    }

    /** The category the entry is filed under, as the map numbers them: -1 for the basics. */
    private int categoryIndex() {
        for (int i = 0; i < InformationPage.getInfoPages().size(); i++) {
            if (InformationPage.getInfoPage(i).getInfoList().contains(infoBase)) {
                return i;
            }
        }
        return -1;
    }

    /** The back ribbon hangs just under the close ribbon, the two ways out of the entry together. */
    private int ribbonY() {
        return BookRibbon.belowCloseY(frame);
    }

    /** The back and the close ribbons, both dark, eased a step; before the pages. */
    private void drawRibbons(int mx, int my) {
        backRibbon.ease(false, BookRibbon.over(frame, ribbonY(), backRibbon.out(), mx, my));
        closeRibbon.ease(false, overClose(mx, my));
        backRibbon.drawDarkCloth(frame, ribbonY(), 0);
        closeRibbon.drawDarkCloth(frame, BookRibbon.closeY(frame), 0);
    }

    private boolean overClose(int mx, int my) {
        return BookRibbon.over(frame, BookRibbon.closeY(frame), closeRibbon.out(), mx, my);
    }

    /** Each page's own number, centred under its text, level with the arrows. */
    private void drawPageNumber(int left, int top) {
        int[] centres = { left + MARGIN_OUTER + TEXT_WIDTH / 2, left + PAGE_WIDTH + MARGIN_FOLD + TEXT_WIDTH / 2 };
        for (int side = 0; side < 2 && currentPage + side < pages; side++) {
            String text = String.valueOf(currentPage + side + 1);
            this.fontRendererObj.drawString(
                    text,
                    centres[side] - this.fontRendererObj.getStringWidth(text) / 2,
                    top + PAGE_HEIGHT - 24,
                    INK_FAINT);
        }
    }

    @Override
    protected void mouseClicked(int screenX, int screenY, int button) {
        int x = frame.toBook(screenX), y = frame.toBook(screenY);
        for (int side = 0; side < 2 && currentPage + side < pages; side++) {
            if (laidOut.get(currentPage + side).mouseClicked(x, y, button)) {
                this.mc.getSoundHandler()
                        .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
                return;
            }
        }
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void mouseMovedOrUp(int screenX, int screenY, int which) {
        super.mouseMovedOrUp(frame.toBook(screenX), frame.toBook(screenY), which);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BUTTON_CLOSE) {
            this.mc.displayGuiScreen(null);
            this.mc.setIngameFocus();
        }
        if (button.id == BUTTON_BACK) {
            this.mc.displayGuiScreen(parentGui);
        }
        if (button.id == BUTTON_NEXT && currentPage < pages - 2) {
            currentPage += 2;
        }
        if (button.id == BUTTON_PREVIOUS && currentPage > 1) {
            currentPage -= 2;
        }
        updateButtons();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            // Back to the map, as it was left, rather than out of the book altogether
            this.mc.displayGuiScreen(parentGui);
        } else if (keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_D) {
            turn(1);
        } else if (keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_A) {
            turn(-1);
        } else if (keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
            mc.thePlayer.openGui(MineFantasyII.instance, 1, mc.thePlayer.worldObj, 0, -1, 0);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    /** The wheel turns the pages: down for the next spread, up for the one before. */
    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            turn(wheel < 0 ? 1 : -1);
        }
    }

    /** Turns one spread forward or back, if there is one, with the page's sound. */
    private void turn(int direction) {
        GuiButton button = direction > 0 ? buttonNextPage : buttonPreviousPage;
        if (button.visible) {
            button.func_146113_a(this.mc.getSoundHandler());
            actionPerformed(button);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /** A run of a text page's lines, as many as fit on one page, closed with a small flourish where the text ends. */
    private final class FlowPage extends EntryPage {

        private final List<String> lines;
        private final boolean onLeft;
        private final boolean last;

        FlowPage(List<String> lines, boolean onLeft, boolean last) {
            this.onLeft = onLeft;
            this.lines = new ArrayList<String>(lines);
            this.last = last;
        }

        @Override
        public void render(GuiScreen parent, int x, int y, float f, int posX, int posY, boolean onTick) {
            // The fold is on the right of a left page and on the left of a right one
            int left = posX + (onLeft ? MARGIN_OUTER : MARGIN_FOLD), line = posY + TEXT_TOP;
            for (String text : lines) {
                fontRendererObj.drawString(text, left, line, EntryPageText.INK);
                line += LINE_HEIGHT;
            }
            if (last && line + 6 <= frame.top() + TEXT_BOTTOM) {
                drawFlourish(left + TEXT_WIDTH / 2, line + 3);
            }
        }

        @Override
        public void preRender(GuiScreen parent, int x, int y, float f, int posX, int posY, boolean onTick) {}
    }

    /** A short rule with a diamond in its middle, where an entry's text comes to an end. */
    private static void drawFlourish(int centre, int y) {
        drawRect(centre - 22, y, centre - 4, y + 1, RULE);
        drawRect(centre + 4, y, centre + 22, y + 1, RULE);
        drawRect(centre - 1, y - 2, centre + 1, y + 3, RULE);
        drawRect(centre - 2, y - 1, centre + 2, y + 2, RULE);
    }
}
