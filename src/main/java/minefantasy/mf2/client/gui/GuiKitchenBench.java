package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.helpers.TextureHelperMF;
import minefantasy.mf2.block.tileentity.TileEntityKitchenBench;
import minefantasy.mf2.container.ContainerKitchenBench;

@SideOnly(Side.CLIENT)
public class GuiKitchenBench extends GuiCraftBench {

    private final TileEntityKitchenBench tile;

    public GuiKitchenBench(InventoryPlayer user, TileEntityKitchenBench tile) {
        super(new ContainerKitchenBench(user, tile), tile, 195, 240, "kitchenbench", 0, 240);
        this.tile = tile;
    }

    @Override
    protected String texture() {
        return "textures/gui/kitchen.png";
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        if (tile.isDirty()) {
            fontRendererObj
                    .drawStringWithShadow(StatCollector.translateToLocal("gui.kitchenbench.dirty"), 10, 20, 16733525);
        }
    }

    /** The dirt meter reuses the progress strip, tinted, just under it. */
    @Override
    protected void drawGauges(int left, int top) {
        super.drawGauges(left, top);
        float max = tile.getDirtyMax();
        if (max <= 0 || tile.dirtyProgress <= 0) {
            return;
        }
        // The icons above leave another texture bound
        mc.getTextureManager().bindTexture(TextureHelperMF.getResource(texture()));
        GL11.glColor4f(1.0F, 0.4F, 0.4F, 1.0F);
        drawTexturedModalRect(left + 8, top + 26, 0, 240, (int) (160F / max * Math.min(max, tile.dirtyProgress)), 2);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
