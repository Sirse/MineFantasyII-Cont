package minefantasy.mf2.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import minefantasy.mf2.client.CameraShake;

/**
 * Shakes the view after a blast where vanilla sways it on a hit, so the world and the hand shake together. Forge 1.7.10
 * has no camera event to do this from.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "hurtCameraEffect", at = @At("TAIL"))
    private void mf2$shake(float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        float[] angles = CameraShake.angles((double) mc.theWorld.getTotalWorldTime() + partialTicks);
        if (angles == null) return;
        GL11.glRotatef(angles[0], 0F, 1F, 0F);
        GL11.glRotatef(angles[1], 1F, 0F, 0F);
        GL11.glRotatef(angles[2], 0F, 0F, 1F);
    }
}
