package minefantasy.mf2.client.render.block;

import minefantasy.mf2.block.tileentity.TileEntityFirepit;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelFirepit extends ModelBase {
    private final ModelRenderer Base;
    private final ModelRenderer Plank1;
    private final ModelRenderer Plank2;
    private final ModelRenderer Plank3;
    private final ModelRenderer Plank4;
    private final ModelRenderer Pile;

    public ModelFirepit() {
        textureWidth = 64;
        textureHeight = 32;
        float angle = 0.7853982F; // 45 degrees

        Base = new ModelRenderer(this, 0, 15);
        Base.addBox(-5F, 3F, -5F, 10, 3, 10);
        Base.setRotationPoint(0F, 0F, 0F);

        Pile = new ModelRenderer(this, 0, 6);
        Pile.addBox(-3F, 0F, -3F, 6, 3, 6);
        Pile.setRotationPoint(0F, 0F, 0F);

        Plank1 = new ModelRenderer(this, 24, 0);
        Plank1.addBox(-1.5F, -1.5F, -4.5F, 3, 3, 9);
        Plank1.setRotationPoint(0F, 3F, -5F);
        setRotation(Plank1, angle, 0F, 0F);

        Plank2 = new ModelRenderer(this, 24, 0);
        Plank2.addBox(-1.5F, -1.5F, -4.5F, 3, 3, 9);
        Plank2.setRotationPoint(0F, 3F, 5F);
        setRotation(Plank2, -angle, 0F, 0F);

        Plank3 = new ModelRenderer(this, 0, 0);
        Plank3.addBox(-4.5F, -1.5F, -1.5F, 9, 3, 3);
        Plank3.setRotationPoint(-5F, 3F, 0F);
        setRotation(Plank3, 0F, 0F, -angle);

        Plank4 = new ModelRenderer(this, 0, 0);
        Plank4.addBox(-4.5F, -1.5F, -1.5F, 9, 3, 3);
        Plank4.setRotationPoint(5F, 3F, 0F);
        setRotation(Plank4, 0F, 0F, angle);
    }

    /**
     * Renders the model based on the TileEntity's state.
     *
     * @param pit   The TileEntityFirepit, can be null for item rendering.
     * @param scale The render scale (usually 1/16F).
     */
    public void renderModel(TileEntityFirepit pit, float scale) {
        Base.render(scale);

        // Render logs only if it's for an item (pit == null) or if the TE firepit has fuel.
        if (pit == null || pit.fuel > 0) {
            Plank1.render(scale);
            Plank2.render(scale);
            Plank3.render(scale);
            Plank4.render(scale);
            Pile.render(scale);
        }
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.rotateAngleX = x;
        model.rotateAngleY = y;
        model.rotateAngleZ = z;
    }
}