package minefantasy.mf2.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

import org.lwjgl.opengl.GL11;

import minefantasy.mf2.api.helpers.TextureHelperMF;

/** A station's window: its texture as the background, and whatever gauges the station draws over it. */
public abstract class GuiStation extends GuiContainer {

    protected GuiStation(Container container, int xSize, int ySize) {
        super(container);
        this.xSize = xSize;
        this.ySize = ySize;
    }

    /** The background, as a path under the mod's assets. */
    protected abstract String texture();

    /** Draws the station's gauges over the background, which is still bound. */
    protected void drawGauges(int left, int top) {}

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {}

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TextureHelperMF.getResource(texture()));
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        drawGauges(guiLeft, guiTop);
    }
}
