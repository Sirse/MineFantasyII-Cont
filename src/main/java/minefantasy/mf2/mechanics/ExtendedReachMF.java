package minefantasy.mf2.mechanics;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.network.packet.ExtendedReachPacket;
import mods.battlegear2.api.weapons.IExtendedReachWeapon;

public class ExtendedReachMF {

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        if (mc == null || event == null || event.isCanceled()) {
            return;
        }
        if (mc.currentScreen != null || !mc.inGameHasFocus) {
            return;
        }
        if (event.button == 0 && event.buttonstate) {
            attackExtendedTarget();
        }
    }

    public void attackExtendedTarget() {
        Minecraft mc = FMLClientHandler.instance().getClient();
        EntityPlayer entityPlayer = mc.thePlayer;
        if (entityPlayer == null) {
            return;
        }
        if (mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return;
        }

        ItemStack mainhand = entityPlayer.getCurrentEquippedItem();
        if (mainhand != null && mainhand.getItem() instanceof IExtendedReachWeapon) {
            float extendedReach = ((IExtendedReachWeapon) mainhand.getItem()).getReachModifierInBlocks(mainhand);
            if (extendedReach <= 0F) {
                return;
            }
            MovingObjectPosition mouseOver = getMouseOver(1.0F, extendedReach + 4.0F);
            if (mouseOver != null && mouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                    && mouseOver.entityHit instanceof EntityLivingBase
                    && mouseOver.entityHit != entityPlayer) {
                MineFantasyII.packetHandler.sendPacketToServer(
                        new ExtendedReachPacket(mouseOver.entityHit.getEntityId()).generatePacket());
            }
        }
    }

    public MovingObjectPosition getMouseOver(float tickPart, float maxDist) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        if (mc.renderViewEntity != null) {
            if (mc.theWorld != null) {
                double d0 = maxDist;
                MovingObjectPosition objectMouseOver = mc.renderViewEntity.rayTrace(d0, tickPart);
                double d1 = d0;
                Vec3 vec3 = mc.renderViewEntity.getPosition(tickPart);

                if (objectMouseOver != null) {
                    d1 = objectMouseOver.hitVec.distanceTo(vec3);
                }

                Vec3 vec31 = mc.renderViewEntity.getLook(tickPart);
                Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
                Entity pointedEntity = null;
                float f1 = 1.0F;
                List<Entity> list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                        mc.renderViewEntity,
                        mc.renderViewEntity.boundingBox
                                .addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f1, f1, f1));
                double d2 = d1;

                for (int i = 0; i < list.size(); ++i) {
                    Entity entity = list.get(i);

                    if (entity.canBeCollidedWith()) {
                        float f2 = entity.getCollisionBorderSize();
                        AxisAlignedBB axisalignedbb = entity.boundingBox.expand(f2, f2, f2);
                        MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                        if (axisalignedbb.isVecInside(vec3)) {
                            if (0.0D < d2 || d2 == 0.0D) {
                                pointedEntity = entity;
                                d2 = 0.0D;
                            }
                        } else if (movingobjectposition != null) {
                            double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                            if (d3 < d2 || d2 == 0.0D) {
                                pointedEntity = entity;
                                d2 = d3;
                            }
                        }
                    }
                }

                if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                    objectMouseOver = new MovingObjectPosition(pointedEntity);
                }

                return objectMouseOver;
            }
        }
        return null;
    }

}
