package minefantasy.mf2.api.helpers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import minefantasy.mf2.container.ContainerMF.SlotOutput;

public class GuiHelper {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final RenderItem ghostRender = new RenderItem();

    /**
     * Shows the pending result, dimmed, in a station's empty output slot. Call from drawGuiContainerForegroundLayer;
     * nothing is placed in the slot, so it stays unclickable.
     */
    public static void renderGhostResult(GuiContainer gui, ItemStack result) {
        if (result == null || result.getItem() == null) {
            return;
        }
        for (Object object : gui.inventorySlots.inventorySlots) {
            Slot slot = (Slot) object;
            if (!(slot instanceof SlotOutput) || slot.getHasStack()) {
                continue;
            }
            int x = slot.xDisplayPosition;
            int y = slot.yDisplayPosition;
            GL11.glPushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            ghostRender.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), result, x, y);
            ghostRender.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), result, x, y);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            // Wash it out with the slot colour so it reads as a preview, not a real stack
            Gui.drawRect(x, y, x + 16, y + 16, 0xA08B8B8B);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            GL11.glPopMatrix();
        }
    }

    public static void renderToolIcon(Gui screen, String toolType, int tier, int x, int y, boolean available) {
        renderToolIcon(screen, toolType, tier, x, y, false, available);
    }

    public static void renderToolIcon(Gui screen, String toolType, int tier, int x, int y, boolean outline,
            boolean available) {
        // Set the tint either way: a caller that just drew text leaves the GL colour at the text colour (black on the
        // HUD), which used to turn a usable tool's icon black
        if (available) {
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
        } else {
            GL11.glColor3f(1.0F, 0.3F, 0.3F);
        }
        mc.getTextureManager().bindTexture(TextureHelperMF.getResource("textures/gui/icons.png"));
        int[] icon = getToolTypeIcon(toolType);
        screen.drawTexturedModalRect(x, y, outline ? 20 : 0, 0, 20, 20);
        screen.drawTexturedModalRect(x, y, icon[0], icon[1] + 20, 20, 20);
        // Tier 0 accepts anything, so only a real requirement is shown (as on the NEI pages)
        if (tier > 0) mc.fontRenderer.drawStringWithShadow("" + tier, x + 4, y + 10, 16777215);
        GL11.glColor3f(1F, 1F, 1F);
    }

    public static int[] getToolTypeIcon(String s) {
        int width = 20;
        int height = 20;
        if (s.equalsIgnoreCase("hands")) {
            return new int[] { 0, 0 };
        }
        if (s.equalsIgnoreCase("knife")) {
            return new int[] { width * 1, height * 0 };
        }
        if (s.equalsIgnoreCase("saw")) {
            return new int[] { width * 2, height * 0 };
        }
        if (s.equalsIgnoreCase("mallet")) {
            return new int[] { width * 3, height * 0 };
        }
        if (s.equalsIgnoreCase("needle")) {
            return new int[] { width * 4, height * 0 };
        }
        if (s.equalsIgnoreCase("hammer")) {
            return new int[] { width * 5, height * 0 };
        }
        if (s.equalsIgnoreCase("hvyHammer")) {
            return new int[] { width * 6, height * 0 };
        }
        if (s.equalsIgnoreCase("spoon")) {
            return new int[] { width * 7, height * 0 };
        }
        if (s.equalsIgnoreCase("shears")) {
            return new int[] { width * 8, height * 0 };
        }
        if (s.equalsIgnoreCase("spanner")) {
            return new int[] { width * 9, height * 0 };
        }
        if (s.equalsIgnoreCase("brush")) {
            return new int[] { width * 11, height * 0 };
        }

        // Station icons live on the row that renderToolIcon samples at icon[1] + 20 (i.e. y = 40).
        if (s.equalsIgnoreCase("anvil")) {
            return new int[] { 0, height * 1 };
        }
        if (s.equalsIgnoreCase("carpenter")) {
            return new int[] { width * 1, height * 1 };
        }
        if (s.equalsIgnoreCase("kitchenbench")) {
            return new int[] { width * 2, height * 1 };
        }
        return new int[] { 0, 0 };
    }

    public static int getColourForRGB(int red, int green, int blue) {
        return (red << 16) + (green << 8) + blue;
    }
}
