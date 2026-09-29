package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.TileEntityCrossbowBench;
import minefantasy.mf2.container.ContainerCrossbowBench;

@SideOnly(Side.CLIENT)
public class GuiCrossbowBench extends GuiStation {

    private final TileEntityCrossbowBench tile;

    public GuiCrossbowBench(InventoryPlayer user, TileEntityCrossbowBench tile) {
        super(new ContainerCrossbowBench(user, tile), 176, 208);
        this.tile = tile;
    }

    @Override
    protected String texture() {
        return "textures/gui/crossbowCraft.png";
    }
}
