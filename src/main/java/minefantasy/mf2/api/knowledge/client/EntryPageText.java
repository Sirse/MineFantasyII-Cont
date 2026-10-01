package minefantasy.mf2.api.knowledge.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

public class EntryPageText extends EntryPage {

    /** Where the text starts on the page and how wide it runs. */
    public static final int TEXT_LEFT = 14;
    public static final int TEXT_TOP = 15;
    public static final int TEXT_WIDTH = 155;
    /** The book's ink, which the markup's reset returns to. */
    public static final int INK = 0x3A2A1A;

    private final String paragraph;
    private Object[] additional;

    public EntryPageText(String paragraph, Object... additional) {
        this(paragraph);
        this.additional = additional;
    }

    public EntryPageText(String paragraph) {
        this.paragraph = paragraph;
    }

    /** The page's text, translated and with its markup turned into formatting codes. */
    public String getText() {
        String local = additional != null && additional.length > 0
                ? StatCollector.translateToLocalFormatted(paragraph, additional)
                : StatCollector.translateToLocal(paragraph);
        return BookMarkup.parse(local);
    }

    /** The text broken into lines of the given width, formatting carried from one line to the next. */
    @SuppressWarnings("unchecked")
    public List<String> getLines(int width) {
        return Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(getText(), width);
    }

    @Override
    public void render(GuiScreen parent, int x, int y, float f, int posX, int posY, boolean onTick) {
        Minecraft.getMinecraft().fontRenderer
                .drawSplitString(getText(), posX + TEXT_LEFT, posY + TEXT_TOP, TEXT_WIDTH, INK);
    }

    @Override
    public void preRender(GuiScreen parent, int x, int y, float f, int posX, int posY, boolean onTick) {}
}
