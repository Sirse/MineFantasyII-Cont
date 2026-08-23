package minefantasy.mf2.client.model.block;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * Kitchen bench, ported from the 1.12 fork's Blockbench model. Texture is a standard techne-style layout generated from
 * the original atlas.
 */
public class ModelKitchenBench extends ModelBase {

    private final ModelRenderer top;
    private final ModelRenderer bottom;
    private final ModelRenderer leg1;
    private final ModelRenderer leg2;
    private final ModelRenderer leg3;
    private final ModelRenderer leg4;
    private final ModelRenderer wood;
    private final ModelRenderer spoonHandle;
    private final ModelRenderer spoonEnd;
    private final ModelRenderer knifeHandle;
    private final ModelRenderer knifeBlade;
    private final ModelRenderer knifeBlade2;

    public ModelKitchenBench() {
        textureWidth = 64;
        textureHeight = 64;

        top = new ModelRenderer(this, 0, 0);
        top.addBox(-8F, 0F, -8F, 16, 4, 16);
        top.setRotationPoint(0F, 0F, 0F);

        bottom = new ModelRenderer(this, 0, 22);
        bottom.addBox(-6.5F, 11F, -6.5F, 13, 2, 13);
        bottom.setRotationPoint(0F, 0F, 0F);

        leg1 = new ModelRenderer(this, 0, 28);
        leg1.addBox(3F, 4F, -7F, 4, 12, 4);
        leg1.setRotationPoint(0F, 0F, 0F);

        leg2 = new ModelRenderer(this, 16, 28);
        leg2.addBox(-7F, 4F, -7F, 4, 12, 4);
        leg2.setRotationPoint(0F, 0F, 0F);

        leg3 = new ModelRenderer(this, 32, 28);
        leg3.addBox(3F, 4F, 3F, 4, 12, 4);
        leg3.setRotationPoint(0F, 0F, 0F);

        leg4 = new ModelRenderer(this, 48, 28);
        leg4.addBox(-7F, 4F, 3F, 4, 12, 4);
        leg4.setRotationPoint(0F, 0F, 0F);

        wood = new ModelRenderer(this, 0, 46);
        wood.addBox(-2F, 7F, -3F, 4, 4, 4);
        wood.setRotationPoint(0F, 0F, 0F);

        spoonHandle = new ModelRenderer(this, 18, 46);
        spoonHandle.addBox(-6.25F, -0.5F, -5.5F, 1, 1, 5);
        spoonHandle.setRotationPoint(0F, 0F, 0F);

        spoonEnd = new ModelRenderer(this, 34, 46);
        spoonEnd.addBox(-6.75F, -0.5F, -0.5F, 2, 1, 2);
        spoonEnd.setRotationPoint(0F, 0F, 0F);

        knifeHandle = new ModelRenderer(this, 18, 50);
        knifeHandle.addBox(6F, -0.5F, 1F, 3, 1, 1);
        knifeHandle.setRotationPoint(0F, 0F, 0F);

        knifeBlade = new ModelRenderer(this, 44, 46);
        knifeBlade.addBox(2F, -0.5F, 1F, 4, 1, 2);
        knifeBlade.setRotationPoint(0F, 0F, 0F);

        knifeBlade2 = new ModelRenderer(this, 44, 50);
        knifeBlade2.addBox(-0.7252F, -0.5F, 4.32685F, 1, 1, 2);
        knifeBlade2.setRotationPoint(0F, 0F, 0F);
        knifeBlade2.rotateAngleY = -0.3927F;
    }

    public void renderModel(float scale) {
        top.render(scale);
        bottom.render(scale);
        leg1.render(scale);
        leg2.render(scale);
        leg3.render(scale);
        leg4.render(scale);
        wood.render(scale);
        spoonHandle.render(scale);
        spoonEnd.render(scale);
        knifeHandle.render(scale);
        knifeBlade.render(scale);
        knifeBlade2.render(scale);
    }
}
