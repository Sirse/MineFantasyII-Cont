package minefantasy.mf2.client.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public final class RenderStateMF {

    private RenderStateMF() {}

    public static void restoreDefaults() {
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }
}
