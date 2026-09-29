package minefantasy.mf2.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.player.*;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.armour.IPowerArmour;
import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.mechanics.*;

/** Hides what a cogwork suit covers on the player riding in it. */
public class CogworkRenderEvents {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void renderEntity(RenderPlayerEvent.Specials.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        boolean showHeld = true;
        if (PowerArmour.isWearingCogwork(event.entityPlayer) && mc.gameSettings.thirdPersonView != 0) {
            IPowerArmour cogwork = (IPowerArmour) event.entity.ridingEntity;
            showHeld = !cogwork.isArmoured("right_arm");
        }
        event.renderItem = showHeld;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void renderEntity(RenderLivingEvent.Pre event) {
        if (!(event.renderer instanceof RenderPowerArmour)) {
            boolean renderHead = false;
            boolean renderBody = false;
            boolean renderLeftArm = false;
            boolean renderRightArm = false;
            boolean renderLeftLeg = false;
            boolean renderRightLeg = false;
            Minecraft mc = Minecraft.getMinecraft();

            if (event.entity instanceof EntityPlayer && !(event.entity == mc.thePlayer
                    && (mc.currentScreen instanceof GuiContainerCreative || mc.currentScreen instanceof GuiInventory))
                    && PowerArmour.isWearingCogwork(event.entity)
                    && mc.gameSettings.thirdPersonView != 0) {
                IPowerArmour cogwork = (IPowerArmour) event.entity.ridingEntity;
                renderHead = cogwork.isArmoured("left_leg");
                renderBody = cogwork.isArmoured("right_leg");
                renderLeftArm = cogwork.isArmoured("left_arm");
                renderRightArm = cogwork.isArmoured("right_arm");
                renderLeftLeg = cogwork.isArmoured("left_leg");
                renderRightLeg = cogwork.isArmoured("right_leg");
            }

            if (event.renderer instanceof RenderPlayer) {
                RenderPlayer RP = (RenderPlayer) event.renderer;
                ModelBiped[] layers = new ModelBiped[] { RP.modelArmor, RP.modelArmorChestplate };

                for (ModelBiped model : layers) {
                    model.bipedHead.isHidden = model.bipedHeadwear.isHidden = model.bipedEars.isHidden = renderHead;
                    model.bipedBody.isHidden = model.bipedCloak.isHidden = renderBody;

                    model.bipedLeftArm.isHidden = renderLeftArm;
                    model.bipedRightArm.isHidden = renderRightArm;
                    model.bipedLeftLeg.isHidden = renderLeftLeg;
                    model.bipedRightLeg.isHidden = renderRightLeg;
                }
            }
        }
    }
}
