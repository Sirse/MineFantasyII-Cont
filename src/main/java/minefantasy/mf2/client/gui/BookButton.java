package minefantasy.mf2.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A small button drawn in ink on the page, in place of the stone-grey vanilla one, for the research book. */
@SideOnly(Side.CLIENT)
public class BookButton extends GuiButton {

    private static final int INK = 0xFF3A2A1A;
    private static final int INK_FAINT = 0xFF9A8666;
    private static final int FILL = 0x28503020;
    private static final int FILL_HOVER = 0x50503020;
    private static final int CLOSE_HOVER = 0xFFC02818;
    private static final int TURN_HOVER = 0xFFB08A2A;

    /** A magnifying glass, for the search. */
    public static final String[] MAGNIFIER = { ".####.....", "#....#....", "#....#....", "#....#....", "#....#....",
            ".####.....", ".....##...", "......##..", ".......##.", "........##" };
    /** A question mark, for how to get about the map. */
    public static final String[] QUESTION = { ".#####.", "##...##", ".....##", "....##.", "...##..", "...##..",
            ".......", "...##..", "...##.." };
    /** A ring with a dot in its middle, for centring the map. */
    public static final String[] TARGET = { ".....#.....", "...##.##...", "..#.....#..", ".#.......#.", ".#.......#.",
            "#....#....#", ".#.......#.", ".#.......#.", "..#.....#..", "...##.##...", ".....#....." };

    /** A bare mark with no frame, taking a colour under the mouse, as the arrows are. */
    private boolean bare;
    /** How many times the font's size a bare mark is drawn. */
    private float scale = 1.75F;
    /** The colour a bare mark or an icon takes away from the mouse. */
    private int restInk = INK;
    /** A small picture drawn in pixels, one string per row with '#' for ink, in place of a mark. */
    private String[] icon;
    /** The colour a bare mark takes under the mouse. */
    private int hoverInk = CLOSE_HOVER;
    /** The sound it makes when pressed; the arrows rustle a page instead of clicking. */
    private ResourceLocation sound = new ResourceLocation("gui.button.press");

    public BookButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    /** A place to click with nothing of its own drawn, over something the screen draws itself; rustles a page. */
    public static BookButton area(int id, int x, int y, int width, int height) {
        BookButton button = new BookButton(id, x, y, width, height, "");
        button.bare = true;
        button.sound = new ResourceLocation("minefantasy2:block.flipPage");
        return button;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    /** A bare pixel icon, gold under the mouse. */
    public static BookButton icon(int id, int x, int y, String[] pixels) {
        BookButton button = new BookButton(id, x, y, pixels[0].length() + 2, pixels.length + 2, "");
        button.bare = true;
        button.icon = pixels;
        button.hoverInk = TURN_HOVER;
        return button;
    }

    /** A bare arrow that turns the page, forward or back, turning gold under the mouse. */
    public static BookButton turn(int id, int x, int y, boolean forward) {
        BookButton button = new BookButton(id, x, y, 14, 14, forward ? ">" : "<");
        button.bare = true;
        button.hoverInk = TURN_HOVER;
        button.sound = new ResourceLocation("minefantasy2:block.flipPage");
        return button;
    }

    @Override
    public void func_146113_a(SoundHandler sounds) {
        sounds.playSound(PositionedSoundRecord.func_147674_a(sound, 1.0F));
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width
                && mouseY < this.yPosition + this.height;
        boolean hot = this.enabled && this.field_146123_n;
        int x0 = this.xPosition, y0 = this.yPosition, x1 = x0 + this.width, y1 = y0 + this.height;
        int ink = this.enabled ? bare ? restInk : INK : INK_FAINT;
        FontRenderer font = mc.fontRenderer;
        int textX = x0 + (this.width - font.getStringWidth(this.displayString)) / 2;
        int textY = y0 + (this.height - 8) / 2;

        if (icon != null) {
            int ix = x0 + (this.width - icon[0].length()) / 2, iy = y0 + (this.height - icon.length) / 2;
            for (int row = 0; row < icon.length; row++) {
                for (int column = 0; column < icon[row].length(); column++) {
                    if (icon[row].charAt(column) == '#') {
                        drawRect(ix + column, iy + row, ix + column + 1, iy + row + 1, hot ? hoverInk : ink);
                    }
                }
            }
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            return;
        }
        if (bare) {
            // Drawn larger than the font, centred in the button
            GL11.glPushMatrix();
            GL11.glTranslatef(x0 + this.width / 2F, y0 + this.height / 2F, 0);
            GL11.glScalef(scale, scale, 1.0F);
            String mark = this.displayString;
            font.drawString(mark, -font.getStringWidth(mark) / 2, -4, (hot ? hoverInk : ink) & 0xFFFFFF);
            GL11.glPopMatrix();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            return;
        }
        drawRect(x0, y0, x1, y1, hot ? FILL_HOVER : FILL);
        drawRect(x0, y0, x1, y0 + 1, ink);
        drawRect(x0, y1 - 1, x1, y1, ink);
        drawRect(x0, y0, x0 + 1, y1, ink);
        drawRect(x1 - 1, y0, x1, y1, ink);
        font.drawString(this.displayString, textX, textY, ink & 0xFFFFFF);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
