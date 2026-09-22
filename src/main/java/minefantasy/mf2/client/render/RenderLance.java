package minefantasy.mf2.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import minefantasy.mf2.api.helpers.TextureHelperMF;

/**
 * @author Anonymous Productions
 */

public class RenderLance implements IItemRenderer {

    private Minecraft mc;
    private RenderItem itemRenderer;

    public RenderLance() {}

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type.equals(ItemRenderType.EQUIPPED) || type.equals(ItemRenderType.EQUIPPED_FIRST_PERSON);
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GL11.glPushMatrix();

        if (mc == null) {
            mc = FMLClientHandler.instance().getClient();
            itemRenderer = new RenderItem();
        }
        this.mc.renderEngine.bindTexture(TextureMap.locationItemsTexture);
        Tessellator tessellator = Tessellator.instance;

        if (type == ItemRenderType.EQUIPPED_FIRST_PERSON || type == ItemRenderType.EQUIPPED) {
            GL11.glPushMatrix();
            float r = 0F;
            if (getRotationFor(data)) {
                if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                    r = 90F;
                } else {
                    r = 45F;
                }
            }
            // The pivot used to straddle the scale: translating by 0.8 outside it and -0.8 inside left the lance
            // swinging about a point three times too far out, which in first person threw it over a unit sideways
            // and read as a flip rather than a tilt. -1.6 and -0.4 are what the old calls worked out to at r = 0,
            // so the resting pose is unchanged and the rotation now turns the lance about its own grip.
            GL11.glTranslatef(-1.6F, -0.4F, 0F);
            GL11.glRotatef(r, 0, 0, -1);
            GL11.glScalef(3F, 3F, 1F);

            GL11.glPushMatrix();

            for (int layer = 0; layer < item.getItem().getRenderPasses(item.getItemDamage()); layer++) {
                int colour = item.getItem().getColorFromItemStack(item, layer);
                float red = (colour >> 16 & 255) / 255.0F;
                float green = (colour >> 8 & 255) / 255.0F;
                float blue = (colour & 255) / 255.0F;

                GL11.glColor4f(red, green, blue, 1.0F);

                IIcon icon = item.getItem().getIcon(item, layer);

                ItemRenderer.renderItemIn2D(
                        tessellator,
                        icon.getMaxU(),
                        icon.getMinV(),
                        icon.getMinU(),
                        icon.getMaxV(),
                        icon.getIconWidth(),
                        icon.getIconHeight(),
                        1F / 16F);
            }

            if (item != null && item.hasEffect(0)) {
                TextureHelperMF.renderEnchantmentEffects(tessellator);
            }
            GL11.glPopMatrix();
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }

    private boolean getRotationFor(Object... data) {
        for (int a = 0; a < data.length; a++) {
            if (data[a] instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) data[a];
                if (living.isSwingInProgress) {
                    return true;
                }

                if (living.isRiding()) {
                    Entity mount = living.ridingEntity;
                    float speed = (float) Math.hypot(mount.motionX, mount.motionZ) * 20F;

                    if (speed > 4.0F) {
                        return true;
                    }
                }

            }

            if (data[a] instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) data[a];
                if (player.isUsingItem()) {
                    return true;
                }
            }
        }
        return false;
    }
}
