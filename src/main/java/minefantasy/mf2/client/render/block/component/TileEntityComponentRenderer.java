package minefantasy.mf2.client.render.block.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import minefantasy.mf2.api.helpers.TextureHelperMF;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.block.tileentity.TileEntityComponent;

public class TileEntityComponentRenderer extends TileEntitySpecialRenderer {

    private ModelBarStack bars;
    private ModelSheetStack sheets;
    private ModelPlankStack planks;
    private ModelPotStack pots;
    private ModelBigPlateStack bigplates;
    private ModelJugStack jugs;
    /** Stacks of each kind and size, up to the largest pile a component makes. */
    private final ComponentStackMesh mesh = new ComponentStackMesh(KINDS.length, 64);

    public TileEntityComponentRenderer() {
        this.bars = new ModelBarStack();
        this.sheets = new ModelSheetStack();
        this.planks = new ModelPlankStack();
        this.pots = new ModelPotStack();
        this.bigplates = new ModelBigPlateStack();
        this.jugs = new ModelJugStack();
        // A reload of the renderer's resources may follow a reset of OpenGL state; the stacks are compiled again
        IResourceManager resources = Minecraft.getMinecraft().getResourceManager();
        if (resources instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) resources).registerReloadListener(manager -> mesh.release());
        }
    }

    public void renderModelAt(TileEntityComponent tile, double d, double d1, double d2, float f) {
        if (tile.item != null) {
            int i = tile.getBlockMetadata();

            int j = 180 + (-90 * i);

            GL11.glPushMatrix(); // start
            float scale = 1.0F;
            float yOffset = 1.0F;
            GL11.glTranslatef((float) d + 0.5F, (float) d1 + yOffset, (float) d2 + 0.5F); // size
            GL11.glRotatef(j, 0.0F, 1.0F, 0.0F); // rotate based on metadata
            GL11.glScalef(scale, -scale, -scale); // if you read this comment out this line and you can see what happens
            GL11.glPushMatrix();
            float level = 0F;

            CustomMaterial material = tile.material;
            if (material != null) {
                GL11.glColor3f(
                        material.colourRGB[0] / 255F,
                        material.colourRGB[1] / 255F,
                        material.colourRGB[2] / 255F);
            }

            bindTextureByName("textures/models/object/component/placed_" + tile.tex + ".png"); // texture
            render(tile.type, tile.stackSize, 0.0625F);

            GL11.glColor3f(1F, 1F, 1F);

            GL11.glPopMatrix();
            GL11.glColor3f(1F, 1F, 1F);
            GL11.glPopMatrix(); // end
        }

    }

    /** The kinds of stack, in the order of {@link #parts}. */
    private static final String[] KINDS = { "bar", "sheet", "plank", "pot", "bigplate", "jug" };

    private void render(String type, int stackSize, float f) {
        for (int kind = 0; kind < KINDS.length; kind++) {
            if (KINDS[kind].equalsIgnoreCase(type)) {
                mesh.draw(kind, parts(kind), stackSize, f);
                return;
            }
        }
    }

    private ModelRenderer[] parts(int kind) {
        switch (kind) {
            case 0:
                return bars.parts();
            case 1:
                return sheets.parts();
            case 2:
                return planks.parts();
            case 3:
                return pots.parts();
            case 4:
                return bigplates.parts();
            default:
                return jugs.parts();
        }
    }

    private void bindTextureByName(String image) {
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureHelperMF.getResource(image));
    }

    @Override
    public void renderTileEntityAt(TileEntity tileentity, double d, double d1, double d2, float f) {
        renderModelAt((TileEntityComponent) tileentity, d, d1, d2, f); // where to render
    }

}
