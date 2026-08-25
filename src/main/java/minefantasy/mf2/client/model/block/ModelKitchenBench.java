package minefantasy.mf2.client.model.block;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * Kitchen bench, ported from the granite Blockbench project (64x64 atlas, hand-fixed UVs). All four legs share the
 * 16x16 slot at (0,0) and are differentiated by mirroring; the decorations use their own per-part pivots.
 */
public class ModelKitchenBench extends ModelBase {

    private final ModelRenderer top;
    private final ModelRenderer bottom;
    private final ModelRenderer legs;
    private final ModelRenderer spoonHandle;
    private final ModelRenderer spoonEnd;
    private final ModelRenderer knifeBlade;
    private final ModelRenderer knifeTip;
    private final ModelRenderer wood;

    public ModelKitchenBench() {
        textureWidth = 64;
        textureHeight = 64;

        top = new ModelRenderer(this, 0, 0);
        top.addBox(-8F, 0F, -8F, 16, 4, 16);
        top.setRotationPoint(0F, 0F, 0F);

        bottom = new ModelRenderer(this, 0, 20);
        bottom.addBox(-6.5F, 11F, -6.5F, 13, 2, 13);
        bottom.setRotationPoint(0F, 0F, 0F);

        legs = new ModelRenderer(this, 0, 0);
        legs.mirror = true;
        legs.addBox(3F, 4F, -7F, 4, 12, 4);
        legs.mirror = true;
        legs.addBox(-7F, 4F, -7F, 4, 12, 4);
        legs.mirror = true;
        legs.addBox(3F, 4F, 3F, 4, 12, 4);
        legs.mirror = false;
        legs.addBox(-7F, 4F, 3F, 4, 12, 4);
        legs.setRotationPoint(0F, 0F, 0F);

        spoonHandle = new ModelRenderer(this, 1, 44);
        spoonHandle.addBox(-6.25F, -0.499F, -5.5F, 1, 1, 5);
        spoonHandle.setRotationPoint(0F, 0F, 0F);

        spoonEnd = new ModelRenderer(this, 9, 45);
        spoonEnd.addBox(-6.75F, -0.5F, -0.5F, 2, 1, 2);
        spoonEnd.setRotationPoint(0F, 0F, 0F);

        knifeBlade = new ModelRenderer(this, 0, 52);
        knifeBlade.setTextureOffset(0, 52);
        knifeBlade.addBox(-7F, -4F, -7F, 4, 1, 2);
        knifeBlade.setTextureOffset(1, 55);
        knifeBlade.addBox(-3F, -4F, -7F, 3, 1, 1);
        knifeBlade.setRotationPoint(9F, 3.5F, 8F);
        knifeBlade.rotateAngleY = 0.3927F;

        knifeTip = new ModelRenderer(this, 1, 58);
        knifeTip.mirror = true;
        knifeTip.addBox(-0.5F, -0.4995F, -1F, 1, 1, 2);
        knifeTip.setRotationPoint(-0.2252F, 0.0005F, 5.3269F);
        knifeTip.rotateAngleY = 0.3927F;

        wood = new ModelRenderer(this, 0, 35);
        wood.addBox(-2.25F, 10.5F, -3.25F, 4, 4, 4);
        wood.setRotationPoint(0.25F, -3.5F, 0.25F);
        wood.rotateAngleY = 0.3927F;
    }

    public void renderModel(float scale) {
        top.render(scale);
        bottom.render(scale);
        legs.render(scale);
        wood.render(scale);
        spoonHandle.render(scale);
        spoonEnd.render(scale);
        knifeBlade.render(scale);
        knifeTip.render(scale);
    }
}
