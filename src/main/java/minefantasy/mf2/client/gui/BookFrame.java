package minefantasy.mf2.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The open research book both its screens share, the map and an entry: where the spread sits, how much smaller it is
 * drawn on a small screen, the pages themselves and the dust over them. Everything is laid out in the book's own space,
 * which the screen sees scaled down by {@link #scale}.
 */
@SideOnly(Side.CLIENT)
final class BookFrame {

    static final int PAGE_WIDTH = 178;
    static final int PAGE_HEIGHT = 227;
    /** Room on the right for the ribbons that stand out of the book, as far as an open one reaches. */
    static final int RIBBON_ROOM = 40;

    static final ResourceLocation LEFT_PAGE = new ResourceLocation("minefantasy2:textures/gui/knowledge/left_page.png");
    static final ResourceLocation RIGHT_PAGE = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/right_page.png");
    private static final ResourceLocation SPARK = new ResourceLocation(
            "minefantasy2:textures/gui/knowledge/book/spark.png");

    private static final int DUST_MOTES = 12;
    /** Dust is dark, a speck on the paper, not a light. */
    private static final int DUST = 0x5A4028;

    private final Minecraft mc = Minecraft.getMinecraft();
    /** The book is drawn smaller on a screen too small for it, and the mouse is mapped back to match. */
    float scale = 1.0F;
    private int width, height;

    /** Fits the book to the screen: full size where it fits, smaller where it does not. */
    void fit(int screenWidth, int screenHeight) {
        float wide = (screenWidth - 8F) / (PAGE_WIDTH * 2 + RIBBON_ROOM);
        float tall = (screenHeight - 8F) / PAGE_HEIGHT;
        scale = Math.min(1.0F, Math.min(wide, tall));
        width = (int) (screenWidth / scale);
        height = (int) (screenHeight / scale);
    }

    /** A screen coordinate in the book's space. */
    int toBook(int screen) {
        return (int) (screen / scale);
    }

    /** The screen's size in the book's space. */
    int width() {
        return width;
    }

    int height() {
        return height;
    }

    /** The spread sits in the middle, nudged left by half the ribbons that stand out on its right. */
    int left() {
        return width / 2 - PAGE_WIDTH - RIBBON_ROOM / 2;
    }

    int top() {
        return (height - PAGE_HEIGHT) / 2;
    }

    /** Starts drawing in the book's space; {@link #end()} returns to the screen's. */
    void begin() {
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0F);
    }

    void end() {
        GL11.glPopMatrix();
    }

    /** The two pages, side by side. */
    void drawSpread() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.getTextureManager().bindTexture(LEFT_PAGE);
        drawTexel(left(), top(), PAGE_WIDTH, PAGE_HEIGHT);
        mc.getTextureManager().bindTexture(RIGHT_PAGE);
        drawTexel(left() + PAGE_WIDTH, top(), PAGE_WIDTH, PAGE_HEIGHT);
    }

    /** The top left of the bound 256 square texture, one texel to a pixel. */
    private static void drawTexel(int x, int y, int w, int h) {
        float texel = 1F / 256F;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + h, 0, 0, h * texel);
        tessellator.addVertexWithUV(x + w, y + h, 0, w * texel, h * texel);
        tessellator.addVertexWithUV(x + w, y, 0, w * texel, 0);
        tessellator.addVertexWithUV(x, y, 0, 0, 0);
        tessellator.draw();
    }

    /**
     * Motes of dust drifting slowly over the pages, too faint to notice one by one: the book stops looking like a flat
     * screen. Each mote follows its own slow loop, worked out from the time, so nothing needs keeping between frames.
     */
    void drawDust() {
        long now = Minecraft.getSystemTime();
        int x0 = left() + 12, y0 = top() + 12;
        int w = PAGE_WIDTH * 2 - 24, h = PAGE_HEIGHT - 24;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        for (int i = 0; i < DUST_MOTES; i++) {
            // Fixed per mote: where its loop sits, how fast it goes round and how bright it gets
            float seedX = (i * 0.6180339F) % 1F, seedY = (i * 0.3819661F + 0.17F) % 1F;
            float speed = 1.5F / (9000F + i * 1300F);
            float phase = now * speed + i;
            float x = x0 + w * (seedX + 0.12F * MathHelper.sin(phase * 6.283F));
            float y = y0 + h * ((seedY + now * speed * 0.35F) % 1F);
            float alpha = 0.30F + 0.15F * MathHelper.sin(phase * 4F + i * 2F);
            shine(x, y, 3 + i % 3, DUST, alpha);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** A soft round light, or speck, centred on the point. */
    void shine(float x, float y, float size, int rgb, float alpha) {
        tint(rgb, alpha);
        drawImage(SPARK, x - size / 2, y - size / 2, size, size, 0);
    }

    static void tint(int rgb, float alpha) {
        GL11.glColor4f((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, alpha);
    }

    /** Draws a whole texture stretched over the box, whatever its size in texels. */
    void drawImage(ResourceLocation texture, double x, double y, double w, double h, double z) {
        mc.getTextureManager().bindTexture(texture);
        // Smooth, not blocky, when stretched or zoomed: the book's textures are drawn larger than GUI pixels
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        // Each texture is drawn once, not tiled: smoothing must not bleed one edge into the opposite one
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + h, z, 0, 1);
        tessellator.addVertexWithUV(x + w, y + h, z, 1, 1);
        tessellator.addVertexWithUV(x + w, y, z, 1, 0);
        tessellator.addVertexWithUV(x, y, z, 0, 0);
        tessellator.draw();
    }

    /** Lets only the box, in the book's space, be drawn on, until the scissor test is turned off. */
    void clipTo(int x, int y, int w, int h) {
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        float k = res.getScaleFactor() * scale;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
                (int) (x * k),
                (int) (mc.displayHeight - (y + h) * k),
                (int) Math.ceil(w * k),
                (int) Math.ceil(h * k));
    }
}
