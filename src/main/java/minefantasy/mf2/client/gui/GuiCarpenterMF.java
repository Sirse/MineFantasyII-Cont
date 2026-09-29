package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.TileEntityCarpenterMF;
import minefantasy.mf2.container.ContainerCarpenterMF;

@SideOnly(Side.CLIENT)
public class GuiCarpenterMF extends GuiCraftBench {

    public GuiCarpenterMF(InventoryPlayer user, TileEntityCarpenterMF tile) {
        super(new ContainerCarpenterMF(user, tile), tile, 195, 240, "carpenter", 0, 240);
    }

    @Override
    protected String texture() {
        return "textures/gui/carpenter.png";
    }
}
