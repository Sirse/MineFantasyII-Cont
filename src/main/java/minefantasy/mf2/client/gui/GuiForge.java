package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.TileEntityForge;
import minefantasy.mf2.container.ContainerForge;

@SideOnly(Side.CLIENT)
public class GuiForge extends GuiStation {

    private final TileEntityForge tile;

    public GuiForge(InventoryPlayer user, TileEntityForge tile) {
        super(new ContainerForge(user, tile), 176, 175);
        this.tile = tile;
    }

    @Override
    protected String texture() {
        return "textures/gui/" + tile.getTextureName() + ".png";
    }

    @Override
    protected void drawGauges(int left, int top) {
        if (tile.fuel > 0 && tile.maxFuel > 0) {
            int fuel = tile.getMetreScale(12);
            drawTexturedModalRect(left + 33, top + 63 + 11 - fuel, 176, 11 - fuel + 8, 14, fuel + 1);
        }
        if (tile.temperature > 0) {
            int[] scale = tile.getTempsScaled(53);
            if (tile.isLit) {
                drawTexturedModalRect(left + 32, top + 58 - scale[1], 176, 0, 16, 3);
            }
            drawTexturedModalRect(left + 35, top + 58 - scale[0], 176, 3, 10, 5);
        }
    }
}
