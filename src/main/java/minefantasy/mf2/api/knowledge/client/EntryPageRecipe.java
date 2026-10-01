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
 * while the mouse is on the page; under the station's name the arrows step through them, beside which one is shown. A
 * variant picked by hand stays. Variants a script has removed since are left out, and a page with none left says so.
 */
@SideOnly(Side.CLIENT)
public abstract class EntryPageRecipe extends EntryPage {

    /** How long each variant stays. */
    private static final long SWITCH_MS = 1500;
    /** A page not drawn for this long has been out of view, and is taken as coming into it afresh. */
    private static final long UNSEEN_MS = 500;
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
    /** The variant shown, as the subclass numbers them, and those of them still registered, in order. */
    private int variant;
    private int[] present = new int[0];
    /** Whether the reader picked the variant, which then stays rather than taking turns. */
    private boolean picked;
    private long nextSwitch, lastDrawn = Long.MIN_VALUE / 2;
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

    /** Whether a variant is still registered; a script may have removed it since the page was made. */
    protected boolean isPresent(int variant) {
        return true;
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
        long now = Minecraft.getSystemTime();
        if (now - lastDrawn > UNSEEN_MS) {
            comeIntoView(now);
        }
        lastDrawn = now;
        int count = present.length;
        boolean overPage = mx >= posX && mx < posX + universalBookImageWidth
                && my >= posY
                && my < posY + universalBookImageHeight;
        if (overPage) {
            // Held while the mouse is on the page, so an ingredient can be looked at
            nextSwitch = Math.max(nextSwitch, now + SWITCH_MS);
        } else if (!picked && count > 1 && now >= nextSwitch) {
            step(1);
            nextSwitch = now + SWITCH_MS;
        }
        hovered = null;

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.getTextureManager().bindTexture(background);
        parent.drawTexturedModalRect(posX, posY, 0, 0, universalBookImageWidth, universalBookImageHeight);

        if (count > 0) {
            drawRecipe(parent, posX, posY, mx, my);
        } else {
            drawDisabled(posX + universalBookImageWidth / 2, posY + 80);
        }
        drawStation(posX + universalBookImageWidth / 2, posY + stationY() + STATION_DROP);
        drawSwitch(posX + universalBookImageWidth / 2, posY + stationY() + STATION_DROP + 13, count, mx, my);
    }

    /**
     * Works out which variants are still registered and starts the turns afresh, the first after a full while rather
     * than at once; a variant picked by hand is kept while it is still there.
     */
    private void comeIntoView(long now) {
        List<Integer> found = new ArrayList<Integer>();
        for (int i = 0; i < variantCount(); i++) {
            if (isPresent(i)) {
                found.add(i);
            }
        }
        present = new int[found.size()];
        for (int i = 0; i < present.length; i++) {
            present[i] = found.get(i);
        }
        if (indexOf(variant) < 0) {
            variant = present.length == 0 ? 0 : present[0];
            picked = false;
        }
        nextSwitch = now + SWITCH_MS;
    }

    private int indexOf(int wanted) {
        for (int i = 0; i < present.length; i++) {
            if (present[i] == wanted) {
                return i;
            }
        }
        return -1;
    }

    /** Moves to the next or the previous of the variants still registered, round from the last to the first. */
    private void step(int direction) {
        if (present.length == 0) {
            return;
        }
        int at = Math.max(0, indexOf(variant));
        variant = present[(at + direction + present.length) % present.length];
    }

    /** In place of the grid when a script has removed every variant the page held. */
    @SuppressWarnings("unchecked")
    private void drawDisabled(int centre, int y) {
        FontRenderer font = mc.fontRenderer;
        List<String> lines = font.listFormattedStringToWidth(
                StatCollector.translateToLocal("knowledge.recipeDisabled"),
                universalBookImageWidth - 50);
        for (String line : lines) {
            font.drawString(line, centre - font.getStringWidth(line) / 2, y, INK_FAINT);
            y += 9;
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getTooltip() {
        if (hovered == null) {
            return null;
        }
        List<String> lines = new ArrayList<String>();
        boolean first = true;
        for (String line : (List<String>) hovered.getTooltip(mc.thePlayer, false)) {
            lines.add(first ? line : EnumChatFormatting.GRAY + line);
            first = false;
        }
        return lines;
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
        String counter = (indexOf(variant) + 1) + "/" + count;
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
        step(direction);
        picked = true;
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

    @Override
    public void preRender(GuiScreen parent, int x, int y, float f, int posX, int posY, boolean onTick) {}
}
