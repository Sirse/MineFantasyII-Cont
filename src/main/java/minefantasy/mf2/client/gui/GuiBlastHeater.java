package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.container.ContainerBlastHeater;

@SideOnly(Side.CLIENT)
public class GuiBlastHeater extends GuiStation {

    private final TileEntityBlastFH tile;

    public GuiBlastHeater(InventoryPlayer user, TileEntityBlastFH tile) {
        super(new ContainerBlastHeater(user, tile), 176, 208);
        this.tile = tile;
    }

    @Override
    protected String texture() {
        return "textures/gui/blast_heater.png";
    }

    @Override
    protected void drawGauges(int left, int top) {
        if (tile.isBurning()) {
            int fuel = tile.getBurnTimeRemainingScaled(13);
            drawTexturedModalRect(left + 82, top + 42 + 12 - fuel, 243, 12 - fuel, 14, fuel + 1);
        }
    }
}
