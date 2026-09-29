package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.TileEntityResearch;
import minefantasy.mf2.container.ContainerResearch;

@SideOnly(Side.CLIENT)
public class GuiResearchBlock extends GuiStation {

    private final TileEntityResearch tile;

    public GuiResearchBlock(InventoryPlayer user, TileEntityResearch tile) {
        super(new ContainerResearch(user, tile), 178, 158);
        this.tile = tile;
    }

    @Override
    protected String texture() {
        return "textures/gui/knowledge/research.png";
    }

    @Override
    protected void drawGauges(int left, int top) {
        drawTexturedModalRect(left + 10, top + 33, 0, 158, tile.getMetreScale(161), 3);
        fontRendererObj.drawString(tile.getLocalisedName(), left + 9, top + 11, 0);
    }
}
