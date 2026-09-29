package minefantasy.mf2.client.gui;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.helpers.GuiHelper;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.InformationList;
import minefantasy.mf2.api.knowledge.InformationPage;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.knowledge.KnowledgeListMF;
import minefantasy.mf2.network.packet.ResearchRequest;

/**
 * The research book: a map of every entry that can be dragged and zoomed, a purchase window for entries bought with
 * skill, and the player's skills alongside.
 */
@SideOnly(Side.CLIENT)
public class GuiKnowledge extends GuiScreen {

    /** Grid step between entries on the map, in map pixels. */
    private static final int CELL = 24;
    /** Size of the book frame texture. */
    private static final int FRAME_WIDTH = 256;
    private static final int FRAME_HEIGHT = 202;
    /** The map window inside the frame: where it starts and how big it is. */
    private static final int MAP_LEFT = 16;
    private static final int MAP_TOP = 17;
    private static final int MAP_WIDTH = 224;
    private static final int MAP_HEIGHT = 155;
    /** How far the view can scroll, in map pixels. */
    private static final int VIEW_MIN_X = InformationList.minDisplayColumn * CELL - 112;
    private static final int VIEW_MIN_Y = InformationList.minDisplayRow * CELL - 112;
    private static final int VIEW_MAX_X = InformationList.maxDisplayColumn * CELL - 77;
    private static final int VIEW_MAX_Y = InformationList.maxDisplayRow * CELL - 77;
    private static final int SKILL_PANEL_WIDTH = 143;
    private static final float ZOOM_MIN = 1.0F;
    private static final float ZOOM_MAX = 3.0F;
    private static final float ZOOM_STEP = 0.25F;

    /** How many locked parents deep an entry still shows: links, dim, dimmer, and dimmest with its name hidden. */
    private static final int LINKS_DEPTH = 1;
    private static final int DIM_DEPTH = 2;
    private static final int HIDDEN_DEPTH = 3;

    private static final int LINK_LOCKED = 0x90000000;
    private static final int LINK_DISCOVERED = 0xFFA0A0A0;
    private static final int LINK_AVAILABLE = 0xFF00FF00;
    /** Links among entries still out of reach: faint, so the tree reads whole without giving it away. */
    private static final int LINK_DISTANT = 0x38000000;
    private static final int TOOLTIP_BACKGROUND = 0xC0000000;
    private static final int TOOLTIP_REQUIRES = 0xFF705050;
    private static final int TOOLTIP_DESCRIPTION = 0xFFA0A0A0;
    private static final int TOOLTIP_STATUS = 0xFF9090FF;
    private static final int TITLE_AVAILABLE = 0xFFFFFFFF;
    private static final int TITLE_AVAILABLE_SPECIAL = 0xFFFFFF80;
    private static final int TITLE_LOCKED = 0xFF808080;
    private static final int TITLE_LOCKED_SPECIAL = 0xFF808040;
    private static final int FRAME_TITLE = 0x404040;
    private static final int WHITE = 0xFFFFFF;

    private static final int BUTTON_DONE = 1;
    private static final int BUTTON_CATEGORY = 2;
    private static final int BUTTON_PURCHASE = 3;
    private static final int BUTTON_CANCEL = 4;

    private static final ResourceLocation screenTex = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/knowledge.png");
    private static final ResourceLocation buyTex = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/purchase.png");
    private static final ResourceLocation skillTex = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/skilllist.png");

    // The view is kept between openings of the book: where it was last tick, where it is, and where it glides to
    private static double prevViewX, prevViewY;
    private static double viewX, viewY;
    private static double targetViewX, targetViewY;
    private static float zoom = 1.0F;
    private static int lastMouseX, lastMouseY;
    /** 0 while the button is up, 1 once a drag has started inside the map. */
    private static int dragState;
    private static int currentPage = -1;

    public int buyWidth = 225;
    public int buyHeight = 72;
    int offsetByX = 70;
    int offsetByY = 0;
    private final RenderItem itemRender = new RenderItem();
    private InformationBase selected = null;
    private InformationBase highlighted = null;
    /** An entry asked for and not yet confirmed by the server; the book chimes once it is. */
    private InformationBase purchased = null;
    private GuiButton categoryButton;
    private LinkedList<InformationBase> informationList = new LinkedList<InformationBase>();
    private EntityPlayer player;

    public GuiKnowledge(EntityPlayer user) {
        this.player = user;
        prevViewX = viewX = targetViewX = homeViewX();
        prevViewY = viewY = targetViewY = homeViewY();
        informationList.clear();
        for (Object info : InformationList.knowledgeList) {
            if (!InformationPage.isInfoInPages((InformationBase) info)) {
                informationList.add((InformationBase) info);
            }
        }
    }

    /** The view with Getting Started near the middle, where the book opens and the home key returns. */
    private static int homeViewX() {
        return KnowledgeListMF.gettingStarted.displayColumn * CELL - 141 / 2 - 12;
    }

    private static int homeViewY() {
        return KnowledgeListMF.gettingStarted.displayRow * CELL - 141 / 2;
    }

    private int frameLeft() {
        return (this.width - FRAME_WIDTH) / 2 + offsetByX;
    }

    private int frameTop() {
        return (this.height - FRAME_HEIGHT) / 2 + offsetByY;
    }

    private int purchaseLeft() {
        return frameLeft() + (FRAME_WIDTH - buyWidth) / 2;
    }

    private int purchaseTop() {
        return frameTop() + (FRAME_HEIGHT - buyHeight) / 2;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(
                new GuiOptionButton(
                        BUTTON_DONE,
                        this.width / 2 + 24,
                        this.height / 2 + 101,
                        80,
                        20,
                        I18n.format("gui.done")));
        this.buttonList.add(
                categoryButton = new GuiButton(
                        BUTTON_CATEGORY,
                        (width - FRAME_WIDTH) / 2 + 24,
                        height / 2 + 101,
                        125,
                        20,
                        InformationPage.getTitle(currentPage)));
        this.buttonList.add(
                new GuiOptionButton(
                        BUTTON_PURCHASE,
                        purchaseLeft() + 19,
                        purchaseTop() + 47,
                        80,
                        20,
                        I18n.format("gui.purchase")));
        this.buttonList.add(
                new GuiOptionButton(
                        BUTTON_CANCEL,
                        purchaseLeft() + 125,
                        purchaseTop() + 47,
                        80,
                        20,
                        I18n.format("gui.cancel")));
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        if (selected == null && button == 0 && highlighted != null) {
            if (ResearchLogic.hasInfoUnlocked(player, highlighted) && !highlighted.getPages().isEmpty()) {
                player.openGui(MineFantasyII.instance, 1, player.worldObj, 0, highlighted.ID, 0);
            } else if (highlighted.isEasy() && ResearchLogic.canPurchase(player, highlighted)) {
                selected = highlighted;
            }
        }
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void actionPerformed(GuiButton pressed) {
        if (pressed.id == BUTTON_DONE) {
            this.mc.displayGuiScreen((GuiScreen) null);
        }

        if (selected == null && pressed.id == BUTTON_CATEGORY) {
            currentPage++;

            if (currentPage >= InformationPage.getInfoPages().size()) {
                currentPage = -1;
            }
            categoryButton.displayString = InformationPage.getTitle(currentPage);
        }

        if (pressed.id == BUTTON_PURCHASE && selected != null) {
            ((EntityClientPlayerMP) player).sendQueue
                    .addToSendQueue(new ResearchRequest(player, selected.ID).generatePacket());
            purchased = selected;
            selected = null;
        }
        if (pressed.id == BUTTON_CANCEL && selected != null) {
            selected = null;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
            this.mc.displayGuiScreen((GuiScreen) null);
            this.mc.setIngameFocus();
        } else if (keyCode == Keyboard.KEY_HOME || keyCode == Keyboard.KEY_H) {
            zoom = ZOOM_MIN;
            targetViewX = homeViewX();
            targetViewY = homeViewY();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        handleDrag(mx, my);
        handleZoom(mx, my);
        targetViewX = MathHelper.clamp_double(targetViewX, VIEW_MIN_X, VIEW_MAX_X - 1);
        targetViewY = MathHelper.clamp_double(targetViewY, VIEW_MIN_Y, VIEW_MAX_Y - 1);

        this.drawDefaultBackground();
        this.renderMainPage(mx, my, partialTicks);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        this.drawOverlay();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        GuiButton purchase = (GuiButton) buttonList.get(2);
        GuiButton cancel = (GuiButton) buttonList.get(3);
        purchase.visible = selected != null;
        purchase.enabled = selected != null && selected.hasSkillsUnlocked(player);
        cancel.visible = selected != null;
    }

    /** Dragging inside the map moves the view with the mouse, scaled by the zoom. */
    private void handleDrag(int mx, int my) {
        if (selected != null || !Mouse.isButtonDown(0)) {
            dragState = 0;
            return;
        }
        int left = frameLeft() + 8;
        int top = frameTop() + MAP_TOP;
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
        zoom = MathHelper.clamp_float(zoom, ZOOM_MIN, ZOOM_MAX);

        if (zoom != oldZoom) {
            int pivotX = mx - (frameLeft() + MAP_LEFT);
            int pivotY = my - (frameTop() + MAP_TOP);
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
        if (purchased != null && ResearchLogic.hasInfoUnlocked(player, purchased)) {
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

    protected void drawOverlay() {
        this.fontRendererObj.drawString(I18n.format("gui.information"), frameLeft() + 15, frameTop() + 5, FRAME_TITLE);
    }

    protected void renderMainPage(int mx, int my, float partialTicks) {
        int scrollX = MathHelper.floor_double(prevViewX + (viewX - prevViewX) * partialTicks);
        int scrollY = MathHelper.floor_double(prevViewY + (viewY - prevViewY) * partialTicks);
        scrollX = MathHelper.clamp_int(scrollX, VIEW_MIN_X, VIEW_MAX_X - 1);
        scrollY = MathHelper.clamp_int(scrollY, VIEW_MIN_Y, VIEW_MAX_Y - 1);

        int mapX = frameLeft() + MAP_LEFT;
        int mapY = frameTop() + MAP_TOP;
        List<InformationBase> entries = currentPage == -1 ? informationList
                : InformationPage.getInfoPage(currentPage).getInfoList();

        this.zLevel = 0.0F;
        GL11.glDepthFunc(GL11.GL_GEQUAL);
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
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(screenTex);
        this.drawTexturedModalRect(frameLeft(), frameTop(), 0, 0, FRAME_WIDTH, FRAME_HEIGHT);

        this.zLevel = 0.0F;
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        if (selected != null) {
            renderPurchaseScreen(purchaseLeft(), purchaseTop());
        }
        drawSkillList();
        super.drawScreen(mx, my, partialTicks);

        highlighted = hovered;
        if (selected == null && hovered != null) {
            drawTooltip(hovered, mx + 12, my - 4);
        }
        if (selected == null) {
            drawSkillTooltip(mx, my);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        RenderHelper.disableStandardItemLighting();
    }

    /** Planks under the map, darker further down. */
    private void drawBackground(int scrollX, int scrollY) {
        int row0 = scrollY + 288 >> 4;
        int shiftX = (scrollX + 288) % 16;
        int shiftY = (scrollY + 288) % 16;
        float tile = 16.0F / zoom;
        IIcon planks = Blocks.planks.getIcon(0, 0);
        this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

        for (int row = 0; row * tile - shiftY < MAP_HEIGHT; ++row) {
            float shade = 0.6F - (row0 + row) / 25.0F * 0.3F;
            GL11.glColor4f(shade, shade, shade, 1.0F);
            for (int col = 0; col * tile - shiftX < MAP_WIDTH; ++col) {
                this.drawTexturedModelRectFromIcon(col * 16 - shiftX, row * 16 - shiftY, planks, 16, 16);
            }
        }
    }

    /** Lines from each entry to its parent, with an arrow at the child, for entries close enough to known ones. */
    private void drawLinks(List<InformationBase> entries, int scrollX, int scrollY) {
        this.mc.getTextureManager().bindTexture(screenTex);
        for (InformationBase entry : entries) {
            if (entry.parentInfo == null || !entries.contains(entry.parentInfo)
                    || ResearchLogic.func_150874_c(player, entry) > HIDDEN_DEPTH) {
                continue;
            }
            int childX = entry.displayColumn * CELL - scrollX + 11;
            int childY = entry.displayRow * CELL - scrollY + 11;
            int parentX = entry.parentInfo.displayColumn * CELL - scrollX + 11;
            int parentY = entry.parentInfo.displayRow * CELL - scrollY + 11;

            boolean distant = ResearchLogic.func_150874_c(player, entry) > LINKS_DEPTH;
            int colour = distant ? LINK_DISTANT : LINK_LOCKED;
            if (ResearchLogic.hasInfoUnlocked(player, entry)) {
                colour = LINK_DISCOVERED;
            } else if (ResearchLogic.canUnlockInfo(player, entry)) {
                colour = LINK_AVAILABLE;
            }
            this.drawHorizontalLine(childX, parentX, childY, colour);
            this.drawVerticalLine(parentX, childY, parentY, colour);
            if (distant) {
                continue;
            }
            // The arrow takes the line's colour, and its transparency too: drawRect leaves blending off
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glColor4f(
                    (colour >> 16 & 255) / 255F,
                    (colour >> 8 & 255) / 255F,
                    (colour & 255) / 255F,
                    (colour >>> 24) / 255F);

            if (childX > parentX) {
                this.drawTexturedModalRect(childX - 11 - 7, childY - 5, 114, 234, 7, 11);
            } else if (childX < parentX) {
                this.drawTexturedModalRect(childX + 11, childY - 5, 107, 234, 7, 11);
            } else if (childY > parentY) {
                this.drawTexturedModalRect(childX - 5, childY - 11 - 7, 96, 234, 11, 7);
            } else if (childY < parentY) {
                this.drawTexturedModalRect(childX - 5, childY + 11, 96, 241, 11, 7);
            }
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    /**
     * Draws each entry in view with its frame and item, shaded by how far it is from being known. Returns the entry
     * under the mouse, given in map pixels.
     */
    private InformationBase drawEntries(List<InformationBase> entries, int scrollX, int scrollY, float mouseX,
            float mouseY) {
        InformationBase hovered = null;
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
            boolean available = ResearchLogic.canUnlockInfo(player, entry);
            int depth = ResearchLogic.func_150874_c(player, entry);
            if (ResearchLogic.hasInfoUnlocked(player, entry)) {
                GL11.glColor4f(0.75F, 0.75F, 0.75F, 1.0F);
            } else if (available) {
                GL11.glColor4f(0.5F, 1.0F, 0.5F, 1.0F);
            } else if (depth < DIM_DEPTH) {
                GL11.glColor4f(0.3F, 0.3F, 0.3F, 1.0F);
            } else if (depth == DIM_DEPTH) {
                GL11.glColor4f(0.2F, 0.2F, 0.2F, 1.0F);
            } else if (depth == HIDDEN_DEPTH) {
                GL11.glColor4f(0.1F, 0.1F, 0.1F, 1.0F);
            } else {
                continue;
            }

            this.mc.getTextureManager().bindTexture(screenTex);
            // Forge: blend is needed here, and RenderItem leaks it otherwise
            GL11.glEnable(GL11.GL_BLEND);
            int frameU = entry.getSpecial() ? 26 : entry.getPerk() ? 52 : 0;
            this.drawTexturedModalRect(x - 2, y - 2, frameU, 202, 26, 26);
            GL11.glDisable(GL11.GL_BLEND);

            if (!available) {
                GL11.glColor4f(0.1F, 0.1F, 0.1F, 1.0F);
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

            if (mouseX >= x && mouseX <= x + 22 && mouseY >= y && mouseY <= y + 22) {
                hovered = entry;
            }
        }
        return hovered;
    }

    /**
     * The name of the entry under the mouse with its description and whether it is known or can be bought; for one not
     * yet in reach, what it needs first, and past that the name is hidden.
     */
    private void drawTooltip(InformationBase entry, int x, int y) {
        String title = entry.getDisplayName();
        boolean available = ResearchLogic.canUnlockInfo(player, entry);
        String body;
        int bodyColour;
        String status = null;

        if (!available) {
            int depth = ResearchLogic.func_150874_c(player, entry);
            if (depth > HIDDEN_DEPTH) {
                return;
            }
            if (depth == HIDDEN_DEPTH) {
                title = I18n.format("achievement.unknown");
            }
            // A root entry has no parent to name; it is locked by its skills alone
            body = entry.parentInfo == null ? ""
                    : new ChatComponentTranslation("achievement.requires", entry.parentInfo.getDisplayName())
                            .getUnformattedText();
            bodyColour = TOOLTIP_REQUIRES;
        } else {
            body = entry.getDescription();
            bodyColour = TOOLTIP_DESCRIPTION;
            if (ResearchLogic.hasInfoUnlocked(player, entry)) {
                status = I18n.format("information.discovered");
            } else if (InformationBase.easyResearch) {
                status = StatCollector.translateToLocal("information.buy");
            }
        }

        int width = Math.max(this.fontRendererObj.getStringWidth(title), 120);
        int bodyHeight = body.isEmpty() ? 0 : this.fontRendererObj.splitStringWidth(body, width);
        // Available entries keep a line below the description for their status, even when it is empty
        int height = 12 + bodyHeight + (available ? 12 : 0);

        // Keep the whole box on screen, flipping to the left of the mouse near the right edge
        if (x + width + 3 > this.width) {
            x = Math.max(3, x - width - 24);
        }
        y = MathHelper.clamp_int(y, 3, Math.max(3, this.height - height - 3));

        this.drawGradientRect(x - 3, y - 3, x + width + 3, y + height + 3, TOOLTIP_BACKGROUND, TOOLTIP_BACKGROUND);
        if (!body.isEmpty()) {
            this.fontRendererObj.drawSplitString(body, x, y + 12, width, bodyColour);
        }
        if (status != null) {
            this.fontRendererObj.drawStringWithShadow(status, x, y + 12 + bodyHeight + 4, TOOLTIP_STATUS);
        }

        int titleColour = available ? (entry.getSpecial() ? TITLE_AVAILABLE_SPECIAL : TITLE_AVAILABLE)
                : (entry.getSpecial() ? TITLE_LOCKED_SPECIAL : TITLE_LOCKED);
        this.fontRendererObj.drawStringWithShadow(title, x, y, titleColour);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /** The window asking whether to buy the selected entry, listing the skills it needs, red where short. */
    private void renderPurchaseScreen(int x, int y) {
        String[] requirements = selected.getRequiredSkills();
        int size = requirements != null ? requirements.length : 0;
        this.mc.getTextureManager().bindTexture(buyTex);
        this.drawTexturedModalRect(x, y, 0, 0, buyWidth, 27);
        for (int a = 0; a < size; a++) {
            this.drawTexturedModalRect(x, y + 27 + (a * 19), 0, 27, buyWidth, 19);
        }
        this.drawTexturedModalRect(x, y + 27 + (size * 19), 0, 46, buyWidth, 26);

        // The buttons sit under the last requirement
        int buttonY = purchaseTop() + 47 - 19 + 19 * size;
        ((GuiButton) buttonList.get(2)).yPosition = buttonY;
        ((GuiButton) buttonList.get(3)).yPosition = buttonY;

        int red = GuiHelper.getColourForRGB(220, 0, 0);
        mc.fontRenderer.drawString(selected.getDisplayName(), x + 22, y + 12, WHITE, false);
        for (int a = 0; a < size; a++) {
            boolean isUnlocked = selected.isUnlocked(a, mc.thePlayer);
            mc.fontRenderer.drawStringWithShadow(requirements[a], x + 20, y + 32 + (a * 19), isUnlocked ? WHITE : red);
        }
        GL11.glColor3f(255, 255, 255);
    }

    /** The panel left of the book with each skill's level and progress to the next. */
    protected void drawSkillList() {
        GL11.glPushMatrix();

        int skillHeight = 156;
        int x = frameLeft() - SKILL_PANEL_WIDTH;
        int y = frameTop();
        this.mc.getTextureManager().bindTexture(skillTex);
        this.drawTexturedModalRect(x, y, 0, 0, SKILL_PANEL_WIDTH, skillHeight);

        Skill[] skills = skills();
        for (int a = 0; a < skills.length; a++) {
            drawSkill(x + 20, y + 20 + a * 24, skills[a]);
        }
        for (int a = 0; a < skills.length; a++) {
            drawSkillName(x + 20, y + 20 + a * 24, skills[a]);
        }

        GL11.glPopMatrix();
    }

    private static Skill[] skills() {
        return new Skill[] { SkillList.artisanry, SkillList.construction, SkillList.provisioning, SkillList.engineering,
                SkillList.combat };
    }

    /** Over a skill in the panel: its level and experience towards the next. */
    private void drawSkillTooltip(int mx, int my) {
        int x = frameLeft() - SKILL_PANEL_WIDTH + 20;
        int y = frameTop() + 20;
        if (mx < x || mx >= x + 100 || my < y) {
            return;
        }
        int row = (my - y) / 24;
        Skill[] skills = skills();
        if (row >= skills.length || skills[row] == null) {
            return;
        }
        Skill skill = skills[row];
        int[] xp = skill.getXP(player);
        List<String> lines = new LinkedList<String>();
        lines.add(skill.getDisplayName());
        lines.add(I18n.format("skill.value", RPGElements.getLevel(player, skill)));
        lines.add(I18n.format("knowledge.skillXP", xp[0], xp[1]));
        this.drawHoveringText(lines, mx, my, this.fontRendererObj);
        GL11.glDisable(GL11.GL_LIGHTING);
    }

    protected void drawSkill(int x, int y, Skill skill) {
        if (skill != null) {
            int[] xp = skill.getXP(player);
            float progress = (float) Math.min(xp[0], xp[1]) / (float) xp[1];
            this.drawTexturedModalRect(x + 22, y + 13, 0, 156, (int) (78F * progress), 5);
        }
    }

    protected void drawSkillName(int x, int y, Skill skill) {
        if (skill != null) {
            int level = RPGElements.getLevel(mc.thePlayer, skill);
            mc.fontRenderer.drawString(skill.getDisplayName(), x + 2, y + 1, 0);
            mc.fontRenderer.drawString("" + level, x + 1, y + 10, 0);
        }
    }
}
