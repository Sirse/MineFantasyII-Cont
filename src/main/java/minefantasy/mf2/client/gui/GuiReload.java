package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.container.ContainerReload;

@SideOnly(Side.CLIENT)
public class GuiReload extends GuiStation {

    public GuiReload(InventoryPlayer user, ItemStack weapon) {
        super(new ContainerReload(user, weapon), 176, 148);
    }

    @Override
    protected String texture() {
        return "textures/gui/reload.png";
    }

    /** The open weapon sits in the hotbar; a number key must not swap it out from under the window. */
    @Override
    protected boolean checkHotbarKeys(int id) {
        return false;
    }
}
