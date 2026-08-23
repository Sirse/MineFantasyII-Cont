package minefantasy.mf2.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.helpers.GuiHelper;
import minefantasy.mf2.api.helpers.TextureHelperMF;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.block.tileentity.TileEntityKitchenBench;
import minefantasy.mf2.config.ConfigKitchen;
import minefantasy.mf2.container.ContainerKitchenBench;

@SideOnly(Side.CLIENT)
public class GuiKitchenBench extends GuiContainer {

    private TileEntityKitchenBench tile;
    private int regularXSize = 176;

    public GuiKitchenBench(InventoryPlayer user, TileEntityKitchenBench tile) {
        super(new ContainerKitchenBench(user, tile));
        this.xSize = 195;
        this.ySize = 240;
        this.tile = tile;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        boolean knowsCraft = tile.doesPlayerKnowCraft(mc.thePlayer);
        String resultName = tile.getResultName();
        String title = MineFantasyII.isDebug() ? "Kitchen Bench"
                : knowsCraft ? (resultName.startsWith("gui.") ? StatCollector.translateToLocal(resultName) : resultName)
                        : "????";
        this.fontRendererObj.drawString(title, 10, 8, 0);

        if (tile.isDirty()) {
            String dirty = StatCollector.translateToLocal("gui.kitchenbench.dirty");
            this.fontRendererObj.drawStringWithShadow(dirty, 10, 20, 16733525);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int x, int y) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TextureHelperMF.getResource("textures/gui/kitchen.png"));
        int xPoint = (this.width - this.xSize) / 2;
        int yPoint = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(xPoint, yPoint, 0, 0, this.xSize, this.ySize);

        if (tile.progressMax > 0 && tile.progress > 0) {
            int progressWidth = (int) (160F / tile.progressMax * tile.progress);
            this.drawTexturedModalRect(xPoint + 8, yPoint + 21, 0, 240, progressWidth, 3);
        }
        drawDirtyBar(xPoint, yPoint);

        if (tile.doesPlayerKnowCraft(mc.thePlayer) && tile.getResultName() != null
                && !tile.getResultName().equalsIgnoreCase("")) {
            GuiHelper.renderToolIcon(this, "carpenter", tile.getBenchTierNeeded(), xPoint + regularXSize, yPoint, true);

            if (tile.getToolNeeded() != null) {
                GuiHelper.renderToolIcon(
                        this,
                        tile.getToolNeeded(),
                        tile.getToolTierNeeded(),
                        xPoint - 20,
                        yPoint,
                        isToolSufficient());
            }
        }
    }

    /**
     * The dirt meter reuses the progress bar strip below the crafting grid.
     */
    private void drawDirtyBar(int xPoint, int yPoint) {
        float max = ConfigKitchen.dirtyProgressMax;
        if (max <= 0 || tile.dirtyProgress <= 0) {
            return;
        }
        int dirtyWidth = (int) (160F / max * Math.min(max, tile.dirtyProgress));
        GL11.glColor4f(1.0F, 0.4F, 0.4F, 1.0F);
        this.drawTexturedModalRect(xPoint + 8, yPoint + 26, 0, 240, dirtyWidth, 2);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean isToolSufficient() {
        if (mc.thePlayer != null) {
            return ToolHelper
                    .isToolSufficient(mc.thePlayer.getHeldItem(), tile.getToolNeeded(), tile.getToolTierNeeded());
        }
        return false;
    }
}
