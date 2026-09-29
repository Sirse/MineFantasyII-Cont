package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.TileEntityQuern;
import minefantasy.mf2.container.ContainerQuern;

@SideOnly(Side.CLIENT)
public class GuiQuern extends GuiStation {

    private final TileEntityQuern tile;

    public GuiQuern(InventoryPlayer user, TileEntityQuern tile) {
        super(new ContainerQuern(user, tile), 176, 175);
        this.tile = tile;
    }

    @Override
    protected String texture() {
        return "textures/gui/quern.png";
    }
}
