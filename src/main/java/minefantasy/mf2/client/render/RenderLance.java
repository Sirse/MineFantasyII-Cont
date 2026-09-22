package minefantasy.mf2.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
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

    private static final float SCALE = 3F;
    /** The grip corner of the icon, (1, 0) in model space, once the scale is applied. */
    private static final float GRIP_X = SCALE;

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
            // The UVs go to renderItemIn2D swapped, which puts the tip of the icon at the (0, 1) corner of the quad
            // and the grip at (1, 0). Turning about the origin therefore swung both ends at once and landed the tip
            // where the grip had been, so a swing read as the lance pointing back to front. Turn about the grip
            // instead. The offsets are what the original pair of translates worked out to at r = 0, so the resting
            // pose is unchanged.
            GL11.glTranslatef(-1.6F, -0.4F, 0F);
            GL11.glTranslatef(GRIP_X, 0F, 0F);
            GL11.glRotatef(r, 0, 0, -1);
            GL11.glTranslatef(-GRIP_X, 0F, 0F);
            GL11.glScalef(SCALE, SCALE, 1F);

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

    /**
     * Couches the lance for the charge and the swing, and for nothing else. Holding right click used to count too,
     * which stacked this rotation on top of the vanilla block pose that every MF2 weapon already gets from ItemSword
     * and turned the lance across the player. A lance neither blocks nor parries, so that hold means nothing to it.
     */
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
        }
        return false;
    }
}
