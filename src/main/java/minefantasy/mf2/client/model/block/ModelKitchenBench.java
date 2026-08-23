package minefantasy.mf2.client.model.block;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/**
 * Kitchen bench: carpenter-style table with a knife and spoon on top instead of a hammer and vice. Reuses the carpenter
 * texture layout.
 */
public class ModelKitchenBench extends ModelBase {

    ModelRenderer top;
    ModelRenderer FRleg;
    ModelRenderer bottom;
    ModelRenderer BRleg;
    ModelRenderer BLleg;
    ModelRenderer FLleg;
    ModelRenderer plank2;
    ModelRenderer knifeHandle;
    ModelRenderer block;
    ModelRenderer nail;
    ModelRenderer spoonHandle;
    ModelRenderer knifeBlade;
    ModelRenderer plank;

    public ModelKitchenBench() {
        textureWidth = 64;
        textureHeight = 64;

        top = new ModelRenderer(this, 0, 0);
        top.addBox(-8F, 4F, -8F, 16, 4, 16);
        top.setRotationPoint(0F, 0F, 0F);
        top.setTextureSize(64, 64);
        top.mirror = true;
        setRotation(top, 0F, 0F, 0F);
        FRleg = new ModelRenderer(this, 0, 35);
        FRleg.addBox(3F, 8F, -7F, 4, 12, 4);
        FRleg.setRotationPoint(0F, 0F, 0F);
        FRleg.setTextureSize(64, 64);
        FRleg.mirror = true;
        setRotation(FRleg, 0F, 0F, 0F);
        bottom = new ModelRenderer(this, 0, 20);
        bottom.addBox(-6.5F, 14F, -6.5F, 13, 2, 13);
        bottom.setRotationPoint(0F, 0F, 0F);
        bottom.setTextureSize(64, 64);
        bottom.mirror = true;
        setRotation(bottom, 0F, 0F, 0F);
        BRleg = new ModelRenderer(this, 0, 35);
        BRleg.addBox(3F, 8F, 3F, 4, 12, 4);
        BRleg.setRotationPoint(0F, 0F, 0F);
        BRleg.setTextureSize(64, 64);
        BRleg.mirror = true;
        setRotation(BRleg, 0F, 0F, 0F);
        BLleg = new ModelRenderer(this, 0, 35);
        BLleg.addBox(-7F, 8F, 3F, 4, 12, 4);
        BLleg.setRotationPoint(0F, 0F, 0F);
        BLleg.setTextureSize(64, 64);
        BLleg.mirror = true;
        setRotation(BLleg, 0F, 0F, 0F);
        FLleg = new ModelRenderer(this, 0, 35);
        FLleg.addBox(-7F, 8F, -7F, 4, 12, 4);
        FLleg.setRotationPoint(0F, 0F, 0F);
        FLleg.setTextureSize(64, 64);
        FLleg.mirror = true;
        setRotation(FLleg, 0F, 0F, 0F);
        plank2 = new ModelRenderer(this, 0, 51);
        plank2.addBox(-5F, -1F, -1F, 10, 1, 2);
        plank2.setRotationPoint(2F, 4F, 6F);
        plank2.setTextureSize(64, 64);
        plank2.mirror = true;
        setRotation(plank2, 0F, -0.0698132F, 0F);

        // Knife lying on the table (flat blade + handle)
        knifeBlade = new ModelRenderer(this, 5, 51);
        knifeBlade.addBox(-3F, 0F, -1F, 6, 1, 1);
        knifeBlade.setRotationPoint(-2F, 13F, 2F);
        knifeBlade.setTextureSize(64, 64);
        knifeBlade.mirror = true;
        setRotation(knifeBlade, 0F, 0.7853982F, 0F);
        knifeHandle = new ModelRenderer(this, 5, 51);
        knifeHandle.addBox(-2F, 0F, 0F, 3, 1, 1);
        knifeHandle.setRotationPoint(1.5F, 13F, 3.5F);
        knifeHandle.setTextureSize(64, 64);
        knifeHandle.mirror = true;
        setRotation(knifeHandle, 0F, 0.7853982F, 0F);

        block = new ModelRenderer(this, 0, 0);
        block.addBox(-2F, -2F, -2F, 4, 4, 4);
        block.setRotationPoint(0F, 2F, 0F);
        block.setTextureSize(64, 64);
        block.mirror = true;
        setRotation(block, 0F, -0.0523599F, 0F);
        nail = new ModelRenderer(this, 0, 8);
        nail.addBox(0F, -2F, 0F, 1, 3, 1);
        nail.setRotationPoint(0F, 0F, 0F);
        nail.setTextureSize(64, 64);
        nail.mirror = true;
        setRotation(nail, 0F, 0F, 0F);
        // Spoon resting by the bowl
        spoonHandle = new ModelRenderer(this, 5, 51);
        spoonHandle.addBox(-3F, 0F, 0F, 6, 1, 1);
        spoonHandle.setRotationPoint(3F, 13F, -3F);
        spoonHandle.setTextureSize(64, 64);
        spoonHandle.mirror = true;
        setRotation(spoonHandle, 0F, 2.3561945F, 0F);
        plank = new ModelRenderer(this, 0, 51);
        plank.addBox(-5F, -1F, -1F, 10, 1, 2);
        plank.setRotationPoint(-4F, 4F, -3F);
        plank.setTextureSize(64, 64);
        plank.mirror = true;
        setRotation(plank, 0F, 0.7679449F, 0F);
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.rotateAngleX = x;
        model.rotateAngleY = y;
        model.rotateAngleZ = z;
    }

    public void renderModel(float f) {
        top.render(f);
        FRleg.render(f);
        bottom.render(f);
        BRleg.render(f);
        BLleg.render(f);
        FLleg.render(f);
        plank2.render(f);
        knifeBlade.render(f);
        knifeHandle.render(f);
        block.render(f);
        nail.render(f);
        spoonHandle.render(f);
        plank.render(f);
    }
}
