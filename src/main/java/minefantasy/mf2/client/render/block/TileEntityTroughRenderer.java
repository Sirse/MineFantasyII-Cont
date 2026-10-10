package minefantasy.mf2.client.render.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import minefantasy.mf2.api.helpers.TextureHelperMF;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.client.render.RenderStateMF;
import minefantasy.mf2.fluid.FluidsMF;

public class TileEntityTroughRenderer extends TileEntitySpecialRenderer {

    private ModelTrough model;

    public TileEntityTroughRenderer() {
        model = new ModelTrough();
    }

    public void renderAModelAt(TileEntityTrough tile, double d, double d1, double d2, float f) {
        int i = 0;
        if (tile.getWorldObj() != null) {
            i = tile.getBlockMetadata();
        }
        this.renderModelAt(tile, i, d, d1, d2, f);
    }

    public void renderModelAt(TileEntityTrough tile, int meta, double d, double d1, double d2, float f) {
        int i = meta;

        int j = 90 * i;

        if (i == 1) {
            j = 0;
        }

        if (i == 2) {
            j = 270;
        }

        if (i == 3) {
            j = 180;
        }

        if (i == 0) {
            j = 90;
        }
        if (i == 0) {
            j = 90;
        }

        GL11.glPushMatrix(); // start
        try {
            float scale = 1.0F;
            float yOffset = 1 / 16F;
            GL11.glTranslatef((float) d + 0.5F, (float) d1 + yOffset, (float) d2 + 0.5F); // size
            GL11.glRotatef(j, 0.0F, 1.0F, 0.0F); // rotate based on metadata
            GL11.glScalef(scale, -scale, -scale);
            GL11.glPushMatrix();

            CustomMaterial material = tile.getMaterial();
            GL11.glColor3f(material.colourRGB[0] / 255F, material.colourRGB[1] / 255F, material.colourRGB[2] / 255F);
            bindTextureByName("textures/models/tileentity/" + tile.getTexName() + "_base.png");
            model.renderModel(0.0625F);
            GL11.glColor3f(1F, 1F, 1F);

            int capacity = tile.getCapacity();
            float height = capacity > 0 ? (float) tile.fill / (float) capacity : 0F;
            if (tile.fill > 0) {
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                Fluid fluid = tile.getFluid();
                int colour = fluid == null ? 0xFFFFFF : fluid.getColor(new FluidStack(fluid, 1));
                GL11.glColor4f(
                        (colour >> 16 & 255) / 255F,
                        (colour >> 8 & 255) / 255F,
                        (colour & 255) / 255F,
                        fluid == FluidsMF.seedOil ? 0.8F : 0.5F);
                GL11.glTranslatef(0F, -height * 0.35F, 0F);
                if (fluid == null || fluid == FluidRegistry.WATER) {
                    bindTextureByName("textures/models/tileentity/" + tile.getTexName() + "_water.png");
                    model.renderWater(0.0625F);
                } else {
                    // A foreign fluid's texture may supply its entire colour while its tint is white.
                    // Our fluids borrow water icons for containers, but here need a neutral surface for their tint.
                    renderFluid(FluidsMF.isOwn(fluid) ? null : fluid.getStillIcon());
                }
            }
            GL11.glPopMatrix();
        } finally {
            RenderStateMF.restoreDefaults();
            GL11.glPopMatrix(); // end
        }

    }

    /** The same surface as ModelTrough.Fill, with UVs belonging to the fluid's block-atlas icon. */
    private void renderFluid(IIcon icon) {
        if (icon != null) bindTexture(TextureMap.locationBlocksTexture);
        else GL11.glDisable(GL11.GL_TEXTURE_2D);
        try {
            double u0 = icon == null ? 0 : icon.getMinU();
            double u1 = icon == null ? 1 : icon.getMaxU();
            double v0 = icon == null ? 0 : icon.getMinV();
            double v1 = icon == null ? 1 : icon.getMaxV();
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.setNormal(0, -1, 0);
            tess.addVertexWithUV(-7 / 16D, 0, -5 / 16D, u0, v0);
            tess.addVertexWithUV(7 / 16D, 0, -5 / 16D, u1, v0);
            tess.addVertexWithUV(7 / 16D, 0, 5 / 16D, u1, v1);
            tess.addVertexWithUV(-7 / 16D, 0, 5 / 16D, u0, v1);
            tess.draw();
        } finally {
            if (icon == null) GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
    }

    public void renderInvModel(String tex, CustomMaterial material, double d, double d1, double d2) {
        int j = 90;

        GL11.glPushMatrix(); // start
        try {
            float scale = 1.0F;
            float yOffset = 1 / 16F;
            GL11.glTranslatef((float) d + 0.5F, (float) d1 + yOffset, (float) d2 + 0.5F); // size
            GL11.glRotatef(j, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(scale, -scale, -scale);
            GL11.glPushMatrix();

            GL11.glColor3f(material.colourRGB[0] / 255F, material.colourRGB[1] / 255F, material.colourRGB[2] / 255F);
            bindTextureByName("textures/models/tileentity/" + tex + "_base.png");
            model.renderModel(0.0625F);
            GL11.glPopMatrix();
        } finally {
            RenderStateMF.restoreDefaults();
            GL11.glPopMatrix(); // end
        }

    }

    private void bindTextureByName(String image) {
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureHelperMF.getResource(image));
    }

    @Override
    public void renderTileEntityAt(TileEntity tileentity, double d, double d1, double d2, float f) {
        renderAModelAt((TileEntityTrough) tileentity, d, d1, d2, f); // where to render
    }
}
