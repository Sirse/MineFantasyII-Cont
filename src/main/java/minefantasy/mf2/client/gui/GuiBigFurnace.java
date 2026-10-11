package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.TileEntityBigFurnace;
import minefantasy.mf2.container.ContainerBigFurnace;

@SideOnly(Side.CLIENT)
public class GuiBigFurnace extends GuiStation {

    private final TileEntityBigFurnace smelter;

    public GuiBigFurnace(EntityPlayer player, TileEntityBigFurnace tile) {
        super(new ContainerBigFurnace(player, tile), 176, 166);
        smelter = tile;
    }

    @Override
    protected String texture() {
        return smelter.isHeater() ? "textures/gui/furnace_heater.png" : "textures/gui/furnace_top.png";
    }

    @Override
    protected void drawGauges(int left, int top) {
        if (!smelter.isBurning()) {
            return;
        }
        if (smelter.isHeater()) {
            if (smelter.fuel > 0) {
                int fuel = smelter.getBurnTimeRemainingScaled(12);
                drawTexturedModalRect(left + 59, top + 27 + 12 - fuel, 176, 12 - fuel, 14, fuel + 2);
            }
            // The thermometer's bore runs from y 11 to 70: markers are centred on it, zero at the bottom
            drawTexturedModalRect(left + 104, top + 69 - smelter.getItemHeatScaled(58), 176, 14, 16, 3);
            drawTexturedModalRect(left + 107, top + 68 - smelter.getHeatScaled(58), 176, 17, 10, 5);
        } else if (smelter.progress > 0) {
            drawTexturedModalRect(left + 76, top + 34, 176, 0, smelter.getCookProgressScaled(24) + 1, 16);
        }
    }
}
