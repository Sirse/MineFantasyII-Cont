package minefantasy.mf2.client.render;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.item.archery.ItemBowMF;
import minefantasy.mf2.item.gadget.IScope;

@SideOnly(Side.CLIENT)
public class HudHandlerMF {

    private final MineFantasyHUD inGameGUI = new MineFantasyHUD();

    @SubscribeEvent
    public void postRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
            inGameGUI.renderGameOverlay(event.partialTicks, event.mouseX, event.mouseY);
        }
        if (event.type == RenderGameOverlayEvent.ElementType.HELMET) {
            inGameGUI.renderViewport();
        }
    }

    @SubscribeEvent
    public void onBowFOV(FOVUpdateEvent event) {
        ItemStack stack = event.entity.getItemInUse();
        if (stack == null) {
            return;
        }

        float zoomModifier = 0.0F;
        Item item = stack.getItem();

        if (item instanceof IScope) {
            zoomModifier = ((IScope) item).getZoom(stack);
        } else if (item instanceof ItemBowMF) {
            float chargeProgress = event.entity.getItemInUseDuration() / 20.0F;

            if (chargeProgress > 1.0F) {
                chargeProgress = 1.0F;
            } else {
                chargeProgress *= chargeProgress;
            }
            zoomModifier = chargeProgress * 0.15F;
        }

        if (zoomModifier > 0.0F) {
            event.newfov *= (1.0F - zoomModifier);
        }
    }
}
