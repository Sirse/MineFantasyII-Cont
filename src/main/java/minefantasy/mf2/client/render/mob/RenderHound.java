package minefantasy.mf2.client.render.mob;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.api.helpers.TextureHelperMF;
import minefantasy.mf2.entity.mob.EntityHound;

@SideOnly(Side.CLIENT)
public class RenderHound extends RenderLiving {

    public RenderHound(ModelBase modelbase) {
        super(modelbase, 1.0F);
        this.setRenderPassModel(modelbase);
    }

    /**
     * Defines what float the third param in setRotationAngles of ModelBase is
     */
    protected float handleRotationFloat(EntityHound mob, float f) {
        return mob.getTailRotation();
    }

    /**
     * Queries whether should render the specified pass or not.
     */
    protected int shouldRenderPass(EntityHound hound, int layer, float f) {
        if (layer == 0 && hound.getWolfShaking()) {
            float f1 = hound.getBrightness(f) * hound.getShadingWhileShaking(f);
            this.bindTexture("hound");
            GL11.glColor3f(f1, f1, f1);
            return 1;
        } else if (layer == 1 && hound.isTamed()) {
            this.bindTexture("collar");
            int j = hound.getCollarColor();
            GL11.glColor3f(
                    EntitySheep.fleeceColorTable[j][0],
                    EntitySheep.fleeceColorTable[j][1],
                    EntitySheep.fleeceColorTable[j][2]);
            return 1;
        } else {
            return -1;
        }
    }

    private void bindTexture(String string) {
        bindTexture(TextureHelperMF.getResource("textures/models/animal/hound/" + string + ".png"));
    }

    /**
     * Returns the location of an entity's texture. Doesn't seem to be called unless you call Render.bindEntityTexture.
     */
    protected ResourceLocation getEntityTexture(EntityHound entity) {
        return TextureHelperMF.getResource("textures/models/animal/hound/hound.png");
    }

    /**
     * Queries whether should render the specified pass or not.
     */
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return this.shouldRenderPass((EntityHound) entity, pass, partialTicks);
    }

    /**
     * Defines what float the third param in setRotationAngles of ModelBase is
     */
    protected float handleRotationFloat(EntityLivingBase entity, float partialTicks) {
        return this.handleRotationFloat((EntityHound) entity, partialTicks);
    }

    /**
     * Returns the location of an entity's texture. Doesn't seem to be called unless you call Render.bindEntityTexture.
     */
    protected ResourceLocation getEntityTexture(Entity entity) {
        return this.getEntityTexture((EntityHound) entity);
    }
}
