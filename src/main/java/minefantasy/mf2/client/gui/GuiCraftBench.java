package minefantasy.mf2.client.gui;

import net.minecraft.inventory.Container;
import net.minecraft.util.StatCollector;

import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.helpers.GuiHelper;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.block.tileentity.CraftBench;

/**
 * The window of a bench worked by hand: the project's name and ghost result, its progress, and icons for the tool and
 * the bench it needs, each naming its tier when pointed at. The anvil's window is wider than the others, and everything
 * in it is set in by {@code inset}.
 */
public abstract class GuiCraftBench extends GuiStation {

    /** Width of the part of the window the icons frame. */
    private static final int FRAME = 176;
    private static final int WHITE = 16777215;
    private static final int ICON = 20;

    private final CraftBench bench;
    private final String benchTool;
    private final int inset;
    private final int progressV;

    /**
     * @param benchTool the tool type the bench counts as, naming its icon and tooltip
     * @param inset     how far the bench's part of the window is set in from its left edge
     * @param progressV where the progress strip is in the texture
     */
    protected GuiCraftBench(Container container, CraftBench bench, int xSize, int ySize, String benchTool, int inset,
            int progressV) {
        super(container, xSize, ySize);
        this.bench = bench;
        this.benchTool = benchTool;
        this.inset = inset;
        this.progressV = progressV;
    }

    private boolean showsProject() {
        return bench.doesPlayerKnowCraft(mc.thePlayer) && bench.hasProject();
    }

    private boolean isToolSufficient() {
        return mc.thePlayer != null && ToolHelper
                .isToolSufficient(mc.thePlayer.getHeldItem(), bench.getToolNeeded(), bench.getToolTierNeeded());
    }

    private static String describe(String tool, int tier) {
        return StatCollector.translateToLocal("tooltype." + tool) + ", "
                + (tier > -1 ? StatCollector.translateToLocal("attribute.mfcrafttier.name") + " " + tier
                        : StatCollector.translateToLocal("attribute.nomfcrafttier.name"));
    }

    private static int colour(boolean sufficient) {
        return sufficient ? WHITE : GuiHelper.getColourForRGB(150, 0, 0);
    }

    private boolean over(int iconX, int mouseX, int mouseY) {
        return mouseX > iconX && mouseX < iconX + ICON && mouseY > guiTop && mouseY < guiTop + ICON;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        boolean knows = bench.doesPlayerKnowCraft(mc.thePlayer);
        if (showsProject()) {
            GuiHelper.renderGhostResult(this, bench.getShownResult());
        }
        String name = bench.getResultName();
        String title = MineFantasyII.isDebug() ? benchTool
                : knows ? name.startsWith("gui.") ? StatCollector.translateToLocal(name) : name : "????";
        fontRendererObj.drawString(title, 10 + inset, 8, 0);
        if (!showsProject()) {
            return;
        }
        int toolX = guiLeft + inset - ICON;
        if (bench.getToolNeeded() != null && over(toolX, mouseX, mouseY)) {
            String text = describe(bench.getToolNeeded(), bench.getToolTierNeeded());
            fontRendererObj.drawStringWithShadow(text, 2 - ICON + inset, -12, colour(isToolSufficient()));
        }
        int benchX = guiLeft + inset + FRAME;
        if (over(benchX, mouseX, mouseY)) {
            String text = describe(benchTool, bench.getBenchTierNeeded());
            fontRendererObj.drawStringWithShadow(
                    text,
                    FRAME + ICON - 2 - fontRendererObj.getStringWidth(text) + inset,
                    -12,
                    colour(bench.isBenchSufficient()));
        }
    }

    @Override
    protected void drawGauges(int left, int top) {
        if (bench.getProgressMax() > 0 && bench.getProgress() > 0) {
            int width = (int) (160F / bench.getProgressMax() * bench.getProgress());
            drawTexturedModalRect(left + 8 + inset, top + 21, 0, progressV, width, 3);
        }
        if (showsProject()) {
            GuiHelper.renderToolIcon(
                    this,
                    benchTool,
                    bench.getBenchTierNeeded(),
                    left + inset + FRAME,
                    top,
                    bench.isBenchSufficient());
            if (bench.getToolNeeded() != null) {
                GuiHelper.renderToolIcon(
                        this,
                        bench.getToolNeeded(),
                        bench.getToolTierNeeded(),
                        left + inset - ICON,
                        top,
                        isToolSufficient());
            }
        }
    }
}
