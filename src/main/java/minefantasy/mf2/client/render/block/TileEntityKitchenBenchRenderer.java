package minefantasy.mf2.client.render.block;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import minefantasy.mf2.api.helpers.TextureHelperMF;
import minefantasy.mf2.client.model.block.ModelKitchenBench;

public class TileEntityKitchenBenchRenderer extends TileEntitySpecialRenderer {

    private ModelKitchenBench model;
    private Random random = new Random();

    public TileEntityKitchenBenchRenderer() {
        model = new ModelKitchenBench();
    }

    public void renderAModelAt(TileEntity tile, double d, double d1, double d2, float f) {
        int i = 0;
        if (tile.getWorldObj() != null) {
            i = tile.getBlockMetadata();
        }
        renderModelAt(i, d, d1, d2, f);
    }

    public void renderModelAt(int meta, double d, double d1, double d2, float f) {
        int j = 90 * meta;

        if (meta == 0) {
            j = 0;
        }
        if (meta == 1) {
            j = 270;
        }
        if (meta == 2) {
            j = 180;
        }
        if (meta == 3) {
            j = 90;
        }

        bindTextureByName("textures/models/tileentity/kitchen_bench.png");

        GL11.glPushMatrix();
        GL11.glTranslatef((float) d + 0.5F, (float) d1 + 1.25F, (float) d2 + 0.5F);
        GL11.glRotatef(j + 180F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(1F, -1F, -1F);
        GL11.glPushMatrix();
        model.renderModel(0.0625F);

        GL11.glPopMatrix();
        GL11.glColor3f(1F, 1F, 1F);
        GL11.glPopMatrix();
    }

    private void bindTextureByName(String image) {
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureHelperMF.getResource(image));
    }

    @Override
    public void renderTileEntityAt(TileEntity tileentity, double d, double d1, double d2, float f) {
        renderAModelAt(tileentity, d, d1, d2, f);
    }
}
