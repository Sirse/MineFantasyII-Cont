package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.TileEntityAnvilMF;
import minefantasy.mf2.container.ContainerAnvilMF;

@SideOnly(Side.CLIENT)
public class GuiAnvilMF extends GuiCraftBench {

    private final TileEntityAnvilMF tile;

    public GuiAnvilMF(InventoryPlayer user, TileEntityAnvilMF tile) {
        super(new ContainerAnvilMF(user, tile), tile, 235, 210, "anvil", 28, 210);
        this.tile = tile;
    }

    @Override
    protected String texture() {
        return "textures/gui/anvil" + tile.getTextureName() + ".png";
    }
}
