package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.TileEntityBombBench;
import minefantasy.mf2.container.ContainerBombBench;

@SideOnly(Side.CLIENT)
public class GuiBombBench extends GuiStation {

    private final TileEntityBombBench tile;

    public GuiBombBench(InventoryPlayer user, TileEntityBombBench tile) {
        super(new ContainerBombBench(user, tile), 176, 208);
        this.tile = tile;
    }

    @Override
    protected String texture() {
        return "textures/gui/bombCraft.png";
    }
}
