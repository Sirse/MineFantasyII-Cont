package minefantasy.mf2.client.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.TileEntityCrucible;
import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.container.ContainerCrucible;

@SideOnly(Side.CLIENT)
public class GuiCrucible extends GuiStation {

    private final TileEntityCrucible tile;

    public GuiCrucible(InventoryPlayer user, TileEntityCrucible tile) {
        super(new ContainerCrucible(user, tile), 176, 186);
        this.tile = tile;
    }

    @Override
    protected String texture() {
        return tile.getTier() >= 2 ? "textures/gui/crucible_mythic.png"
                : tile.getTier() == 1 ? "textures/gui/crucible_advanced.png" : "textures/gui/crucible.png";
    }

    @Override
    protected void drawGauges(int left, int top) {
        // With Hardcore Ingots, a manual crucible marks the output it would lose if broken
        ItemStack output = tile.getStackInSlot(tile.getSizeInventory() - 1);
        if (output != null && !(output.getItem() instanceof ItemBlock)
                && ConfigHardcore.HCCreduceIngots
                && !tile.isAuto()) {
            drawTexturedModalRect(left + 128, top + 31, 225, 2, 18, 18);
        }
        if (tile.temperature > 0) {
            drawTexturedModalRect(left + 81, top + 75, 243, 0, 14, 12);
        }
        if (tile.progress > 0 && tile.progressMax > 0) {
            drawTexturedModalRect(left + 61, top + 70, 189, 0, (int) (54F / tile.progressMax * tile.progress), 2);
        }
    }
}
