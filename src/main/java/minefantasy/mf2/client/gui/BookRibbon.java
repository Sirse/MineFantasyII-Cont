package minefantasy.mf2.client.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A bookmark ribbon standing out of the research book's right edge, tucked under the right page. The category ribbons
 * gather at the bottom of the edge; the ribbon that closes the book hangs alone at the top. A ribbon slides out when
 * its category opens, and a little way under the mouse.
 */
@SideOnly(Side.CLIENT)
final class BookRibbon {

    static final int WIDTH = 60;
    static final int HEIGHT = 24;
    static final int STEP = 28;
    /** How far a ribbon stands out past the pages: closed, open, and how much further under the mouse. */
    static final int OUT = 30;
    static final int OUT_OPEN = 40;
    static final float OUT_HOVER = (OUT_OPEN - OUT) / 4F;
    /** The close ribbon's top and the lowest ribbon's foot line up with the parchment's edges, inside the leather. */
    private static final int TOP = 7;
    private static final int BOTTOM = 7;
    /** The ribbons out of the book, closing it or going back: dark leather, their mark pale on it. */
    private static final int CLOSE_CLOTH = 0x3A2622;
    private static final int CLOSE_INK = 0xF0E6D0;
    /** Under the mouse the close cross turns red and the back mark gold. */
    static final int CLOSE_HOT = 0xFF6040;
    static final int BACK_HOT = 0xFFD860;
    private static final int MARK_SHADOW = 0xC0180C08;
    /** The marks' pixels are drawn this many to one of the page's. */
    private static final int MARK_SCALE = 2;
    /** The close cross, heavy at the ends like a smith's rivets. */
    static final String[] CROSS = { "##...##", "###.###", ".#####.", "..###..", ".#####.", "###.###", "##...##" };
    /** Two chevrons back, two pixels thick. */
    static final String[] BACK = { "...##...##", "..##...##.", ".##...##..", "##...##...", ".##...##..", "..##...##.",
            "...##...##" };
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/book/ribbon.png");

    private float out = OUT;

    /** Moves a step towards where the ribbon belongs, once a frame, and gives how far it stands out now. */
    float ease(boolean open, boolean hover) {
        float target = (open ? OUT_OPEN : OUT) + (hover ? OUT_HOVER : 0F);
        out = Math.abs(target - out) < 0.2F ? target : out + (target - out) * 0.25F;
        return out;
    }

    float out() {
        return out;
    }

    /** Where ribbon i of count hangs, the last lowest, all gathered at the bottom of the edge. */
    static int slotY(BookFrame frame, int i, int count) {
        return frame.top() + BookFrame.PAGE_HEIGHT - BOTTOM - HEIGHT - (count - 1 - i) * STEP;
    }

    /** The place just under the close ribbon. */
    static int belowCloseY(BookFrame frame) {
        return closeY(frame) + STEP;
    }

    static int closeY(BookFrame frame) {
        return frame.top() + TOP;
    }

    static int edge(BookFrame frame) {
        return frame.left() + BookFrame.PAGE_WIDTH * 2;
    }

    /** Whether the point is on the part of a ribbon at this height that shows, standing out as far as given. */
    static boolean over(BookFrame frame, int y, float out, int mx, int my) {
        int edge = edge(frame);
        return mx >= edge && mx < edge + out && my >= y && my < y + HEIGHT;
    }

    /** The ribbon's cloth in its colour; drawn before the pages, which cover its tucked-in end. */
    static void drawCloth(BookFrame frame, int y, float out, int rgb, double z) {
        // The swallowtail is cut out by the texture's alpha, whatever blending the screen left set
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, 1.0F);
        frame.drawImage(TEXTURE, edge(frame) + out - WIDTH, y, WIDTH, HEIGHT, z);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** A way-out ribbon's dark cloth at this height, before the pages. */
    void drawDarkCloth(BookFrame frame, int y, double z) {
        drawCloth(frame, y, out, CLOSE_CLOTH, z);
    }

    /** A way-out ribbon's pixel mark, such as the close cross, on the part that shows, after the pages. */
    void drawMark(BookFrame frame, int y, String[] pixels, boolean hot, int hotInk) {
        int ink = 0xFF000000 | (hot ? hotInk : CLOSE_INK);
        float left = edge(frame) + 12 + out - OUT - pixels[0].length() * MARK_SCALE / 2F;
        float top = y + (HEIGHT - pixels.length * MARK_SCALE) / 2F;
        GL11.glPushMatrix();
        GL11.glTranslatef(left, top, 0);
        GL11.glScalef(MARK_SCALE, MARK_SCALE, 1.0F);
        // A hard shadow a pixel down and right, as the game's own lettering has, then the mark
        for (int pass = 0; pass < 2; pass++) {
            int shift = pass == 0 ? 1 : 0, colour = pass == 0 ? MARK_SHADOW : ink;
            for (int row = 0; row < pixels.length; row++) {
                for (int column = 0; column < pixels[row].length(); column++) {
                    if (pixels[row].charAt(column) == '#') {
                        Gui.drawRect(column + shift, row + shift, column + shift + 1, row + shift + 1, colour);
                    }
                }
            }
        }
        GL11.glPopMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
