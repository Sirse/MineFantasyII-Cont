package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.container.ContainerBlastChamber;

@SideOnly(Side.CLIENT)
public class GuiBlastChamber extends GuiStation {

    public GuiBlastChamber(InventoryPlayer user, TileEntityBlastFC tile) {
        super(new ContainerBlastChamber(user, tile), 176, 208);
    }

    @Override
    protected String texture() {
        return "textures/gui/blast_chamber.png";
    }
}
