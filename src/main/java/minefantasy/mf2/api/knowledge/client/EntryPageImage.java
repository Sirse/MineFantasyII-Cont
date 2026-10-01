package minefantasy.mf2.api.knowledge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import minefantasy.mf2.api.helpers.RenderHelper;
import minefantasy.mf2.api.helpers.TextureHelperMF;

public class EntryPageImage extends EntryPage {

    private Minecraft mc = Minecraft.getMinecraft();
    private String paragraph;
    /**
     * Recommend size: 48x48
     */
    private String image;
    private int[] sizes;

    public EntryPageImage(String tex, String paragraphs) {
        this(tex, 128, 128, paragraphs);
    }

    public EntryPageImage(String tex, int width, int height, String paragraphs) {
        this.paragraph = paragraphs;
        this.image = tex;
        this.sizes = new int[] { width, height };
    }

    @Override
    public void render(GuiScreen parent, int x, int y, float f, int posX, int posY, boolean onTick) {
        String text = BookMarkup.parse(StatCollector.translateToLocal(paragraph));

        mc.fontRenderer.drawSplitString(text, posX + 14, posY + 117, 155, EntryPageText.INK);
    }

    @Override
    public void preRender(GuiScreen parent, int x, int y, float f, int posX, int posY, boolean onTick) {
        mc.renderEngine.bindTexture(TextureHelperMF.getResource(image));
        int xOffset = (universalBookImageWidth - sizes[0]) / 2;
        RenderHelper.drawTexturedModalRect(
                posX + xOffset,
                posY + 15,
                2,
                0,
                0,
                sizes[0],
                sizes[1],
                1F / sizes[0],
                1F / sizes[1]);
    }

}
