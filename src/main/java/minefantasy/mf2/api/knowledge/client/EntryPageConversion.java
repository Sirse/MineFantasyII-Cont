package minefantasy.mf2.api.knowledge.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

/** One thing made into another at a station, the one above the other. */
public abstract class EntryPageConversion extends EntryPageRecipe {

    private final ItemStack input, output;
    private final String station;

    protected EntryPageConversion(String background, String station, ItemStack input, ItemStack output) {
        super(background);
        this.station = station;
        this.input = input;
        this.output = output;
    }

    @Override
    protected String station() {
        return station;
    }

    @Override
    protected int stationY() {
        return 150;
    }

    @Override
    protected void drawRecipe(GuiScreen parent, int posX, int posY, int mx, int my) {
        drawItem(input, posX + 80, posY + 41, mx, my);
        drawItem(output, posX + 80, posY + 115, mx, my);
    }
}
