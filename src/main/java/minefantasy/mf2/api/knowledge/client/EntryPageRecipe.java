package minefantasy.mf2.api.knowledge.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.helpers.TextureHelperMF;

/**
 * A page showing a recipe on its station's grid. Where a page holds several variants they take turns, but stand still
 * while the mouse is on the page; under the station's name the arrows step through them, beside which one is shown.
 */
@SideOnly(Side.CLIENT)
public abstract class EntryPageRecipe extends EntryPage {

    /** How long each variant stays, and how much longer after one was picked by hand. */
    private static final long SWITCH_MS = 1500;
    private static final long PICKED_MS = 6000;
    private static final int INK = 0x3A2A1A;
    private static final int INK_FAINT = 0x7A6446;
    private static final int INK_HOVER = 0xB08A2A;
    /** How far each arrow sits from the middle of the counter, and how wide the area that takes its click is. */
    private static final int ARROW_OFFSET = 24;
    private static final int ARROW_HIT = 12;

    /** The stations' icons, by the key of their name, as the mod registers them. */
    private static final Map<String, ItemStack> STATIONS = new HashMap<String, ItemStack>();
    /** Stations whose model draws small in a slot, and how much larger to draw them to match the rest. */
    private static final Map<String, Float> STATION_SCALES = new HashMap<String, Float>();
    /** How far below the grid's own line the station and the switch under it sit. */
    private static final int STATION_DROP = 5;
    protected static final RenderItem ITEMS = new RenderItem();

    protected final Minecraft mc = Minecraft.getMinecraft();
    private final ResourceLocation background;
    private int variant;
    private long nextSwitch;
    private ItemStack hovered;
    /** Where the counter was drawn this frame, for its arrows to be clicked: its middle and top; none when unset. */
    private int switchX = Integer.MIN_VALUE, switchY;

    protected EntryPageRecipe(String background) {
        this.background = TextureHelperMF.getResource("textures/gui/knowledge/" + background + ".png");
    }

    /** Shows the icon beside the name of the station, as {@code method.<key>} names it. */
    public static void registerStation(String key, ItemStack icon) {
        registerStation(key, icon, 1.0F);
    }

    /** As {@link #registerStation(String, ItemStack)}, for an icon that must be drawn this many times larger. */
    public static void registerStation(String key, ItemStack icon, float scale) {
        STATIONS.put(key, icon);
        STATION_SCALES.put(key, scale);
    }

    /** How many variants the page holds; one at least. */
    protected int variantCount() {
        return 1;
    }

    /** The variant shown now. */
    protected final int variant() {
        return variant;
    }

    /** The key of the station the shown variant is made at. */
    protected abstract String station();

    /** How far down the page the station's name sits, below the grid. */
    protected abstract int stationY();

    /** Draws the shown variant, its items through {@link #drawItem}. */
    protected abstract void drawRecipe(GuiScreen parent, int posX, int posY, int mx, int my);

    @Override
    public void render(GuiScreen parent, int mx, int my, float f, int posX, int posY, boolean onTick) {
        int count = Math.max(1, variantCount());
        long now = Minecraft.getSystemTime();
        boolean overPage = mx >= posX && mx < posX + universalBookImageWidth
                && my >= posY
                && my < posY + universalBookImageHeight;
        if (overPage) {
            // Held while the mouse is on the page, so an ingredient can be looked at
            nextSwitch = Math.max(nextSwitch, now + SWITCH_MS);
        } else if (now >= nextSwitch) {
            variant = (variant + 1) % count;
            nextSwitch = now + SWITCH_MS;
        }
        variant %= count;
        hovered = null;

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.getTextureManager().bindTexture(background);
        parent.drawTexturedModalRect(posX, posY, 0, 0, universalBookImageWidth, universalBookImageHeight);

        drawRecipe(parent, posX, posY, mx, my);
        drawStation(posX + universalBookImageWidth / 2, posY + stationY() + STATION_DROP);
        drawSwitch(posX + universalBookImageWidth / 2, posY + stationY() + STATION_DROP + 13, count, mx, my);
    }

    @Override
    public void drawOverlay(int mx, int my) {
        if (hovered != null) {
            drawTooltip(hovered, mx, my);
        }
    }

    /** The station's icon and name, centred on the line. */
    private void drawStation(int centre, int y) {
        FontRenderer font = mc.fontRenderer;
        String name = StatCollector.translateToLocal("method." + station());
        ItemStack icon = STATIONS.get(station());
        int width = font.getStringWidth(name) + (icon != null ? 18 : 0);
        int x = centre - width / 2;
        if (icon != null) {
            Float scale = STATION_SCALES.get(station());
            float by = scale == null ? 1.0F : scale;
            // Grown about the slot's middle, so a larger icon stays level with the name
            GL11.glPushMatrix();
            GL11.glTranslatef(x + 8, y + 4, 0);
            GL11.glScalef(by, by, 1.0F);
            renderStack(icon, -8, -8);
            GL11.glPopMatrix();
            x += 18;
        }
        font.drawString(name, x, y, INK);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** "< 2/5 >": the arrows round the counter keep one width however many variants there are. */
    private void drawSwitch(int centre, int y, int count, int mx, int my) {
        if (count < 2) {
            switchX = Integer.MIN_VALUE;
            return;
        }
        switchX = centre;
        switchY = y;
        FontRenderer font = mc.fontRenderer;
        String counter = (variant + 1) + "/" + count;
        font.drawString(counter, centre - font.getStringWidth(counter) / 2, y, INK_FAINT);
        for (int direction = -1; direction <= 1; direction += 2) {
            String arrow = direction < 0 ? "<" : ">";
            int x = centre + direction * ARROW_OFFSET;
            font.drawString(
                    arrow,
                    x - font.getStringWidth(arrow) / 2,
                    y,
                    arrowAt(mx, my) == direction ? INK_HOVER : INK);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The arrow under the point: -1 back, 1 on, 0 neither. */
    private int arrowAt(int mx, int my) {
        if (switchX == Integer.MIN_VALUE || my < switchY - 2 || my >= switchY + ARROW_HIT - 2) {
            return 0;
        }
        for (int direction = -1; direction <= 1; direction += 2) {
            if (Math.abs(mx - (switchX + direction * ARROW_OFFSET)) <= ARROW_HIT / 2) {
                return direction;
            }
        }
        return 0;
    }

    @Override
    public boolean mouseClicked(int mx, int my, int button) {
        int direction = button == 0 ? arrowAt(mx, my) : 0;
        if (direction == 0) {
            return false;
        }
        int count = Math.max(1, variantCount());
        variant = (variant + direction + count) % count;
        nextSwitch = Minecraft.getSystemTime() + PICKED_MS;
        return true;
    }

    /** Draws an ingredient or product at the point, and remembers it for the tooltip when the mouse is over it. */
    protected final void drawItem(ItemStack stack, int x, int y, int mx, int my) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        stack = stack.copy();
        if (stack.getItemDamage() == Short.MAX_VALUE || stack.getItemDamage() == -1) {
            // A wildcard, any damage: show the plain item
            stack.setItemDamage(0);
        }
        if (mx >= x && mx < x + 16 && my >= y && my < y + 16) {
            hovered = stack;
        }
        renderStack(stack, x, y);
    }

    private void renderStack(ItemStack stack, int x, int y) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        ITEMS.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
        ITEMS.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    @SuppressWarnings("unchecked")
    private void drawTooltip(ItemStack stack, int mx, int my) {
        List<String> lines = new ArrayList<String>();
        boolean first = true;
        for (String line : (List<String>) stack.getTooltip(mc.thePlayer, false)) {
            lines.add(first ? line : EnumChatFormatting.GRAY + line);
            first = false;
        }
        minefantasy.mf2.api.helpers.RenderHelper.renderTooltip(mx, my, lines);
    }

    @Override
    public void preRender(GuiScreen parent, int x, int y, float f, int posX, int posY, boolean onTick) {}
}
