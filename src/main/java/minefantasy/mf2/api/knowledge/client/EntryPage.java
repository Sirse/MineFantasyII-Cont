package minefantasy.mf2.api.knowledge.client;

import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class EntryPage {

    public static final int universalBookImageWidth = 178;
    public static final int universalBookImageHeight = 227;

    public abstract void render(GuiScreen parent, int x, int y, float f, int posX, int posY, boolean onTick);

    public abstract void preRender(GuiScreen parent, int x, int y, float f, int posX, int posY, boolean onTick);

    /** Drawn over both pages once they are drawn, such as a tooltip, which the other page must not cover. */
    public void drawOverlay(int mx, int my) {}

    /** A click on the page, where it was last drawn; true when the page took it. */
    public boolean mouseClicked(int mx, int my, int button) {
        return false;
    }
}
