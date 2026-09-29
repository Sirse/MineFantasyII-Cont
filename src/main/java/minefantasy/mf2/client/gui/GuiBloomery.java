package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.TileEntityBloomery;
import minefantasy.mf2.container.ContainerBloomery;

@SideOnly(Side.CLIENT)
public class GuiBloomery extends GuiStation {

    private final TileEntityBloomery tile;

    public GuiBloomery(InventoryPlayer user, TileEntityBloomery tile) {
        super(new ContainerBloomery(user, tile), 176, 208);
        this.tile = tile;
    }

    @Override
    protected String texture() {
        return "textures/gui/bloomery.png";
    }
}
