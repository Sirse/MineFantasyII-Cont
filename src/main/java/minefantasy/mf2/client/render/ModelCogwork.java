package minefantasy.mf2.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class ModelCogwork extends ModelBiped {

    public ModelCogwork() {
        this(1.0F);
    }

    public ModelCogwork(float scale) {
        float yOffset = -2.0F * scale;

        this.textureWidth = 128;
        this.textureHeight = 64;

        // FRAME
        this.bipedHead = new ModelRenderer(this, 0, 0);
        this.bipedHead.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, scale);
        this.bipedHead.setRotationPoint(0.0F, yOffset, 0.0F);

        this.bipedHeadwear = new ModelRenderer(this, 32, 0);
        this.bipedHeadwear.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, scale + 0.5F);
        this.bipedHeadwear.setRotationPoint(0.0F, yOffset, 0.0F);

        this.bipedBody = new ModelRenderer(this, 16, 16);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, scale);
        this.bipedBody.setRotationPoint(0.0F, yOffset, 0.0F);

        this.bipedRightArm = new ModelRenderer(this, 40, 16);
        this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, scale);
        this.bipedRightArm.setRotationPoint(-5.0F, 2.0F + yOffset, 0.0F);

        this.bipedLeftArm = new ModelRenderer(this, 40, 16);
        this.bipedLeftArm.mirror = true;
        this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, scale);
        this.bipedLeftArm.setRotationPoint(5.0F, 2.0F + yOffset, 0.0F);

        this.bipedRightLeg = new ModelRenderer(this, 0, 16);
        this.bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 8, 4, scale);
        this.bipedRightLeg.setRotationPoint(-1.9F, 12.0F + yOffset, 0.0F);

        this.bipedLeftLeg = new ModelRenderer(this, 0, 16);
        this.bipedLeftLeg.mirror = true;
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 8, 4, scale);
        this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F + yOffset, 0.0F);

        ModelRenderer rightfoot = new ModelRenderer(this, 0, 53);
        rightfoot.addBox(-2F, 10F, -3F, 4, 3, 5, scale); // Y-position is part of the box definition now

        ModelRenderer leftfoot = new ModelRenderer(this, 0, 53);
        leftfoot.mirror = true;
        leftfoot.addBox(-2F, 10F, -3F, 4, 3, 5, scale);

        // ARMOR
        // Armor Pieces
        ModelRenderer headMask = new ModelRenderer(this, 76, 18);
        headMask.addBox(-2F, -3.5F, -6.5F, 4, 5, 3, scale);

        ModelRenderer headArmour = new ModelRenderer(this, 78, 0);
        headArmour.addBox(-4.5F, -8.5F, -4.5F, 9, 9, 9, scale);

        ModelRenderer bodyBack = new ModelRenderer(this, 34, 32);
        bodyBack.addBox(-3.5F, -0.5F, 4F, 7, 9, 3, scale);

        ModelRenderer bodyArmour = new ModelRenderer(this, 0, 32);
        bodyArmour.addBox(-4.5F, -0.5F, -4F, 9, 13, 8, scale);

        ModelRenderer leftarmPauldron = new ModelRenderer(this, 76, 27);
        leftarmPauldron.mirror = true;
        leftarmPauldron.addBox(0F, -4F, -3F, 5, 5, 6, scale);
        leftarmPauldron.rotateAngleZ = 0.2792527F;

        ModelRenderer rightarmPauldron = new ModelRenderer(this, 76, 27);
        rightarmPauldron.addBox(-5F, -4F, -3F, 5, 5, 6, scale);
        rightarmPauldron.rotateAngleZ = -0.2792527F;

        ModelRenderer leftarmArmour = new ModelRenderer(this, 76, 38);
        leftarmArmour.mirror = true;
        leftarmArmour.addBox(-1.0F, -2.5F, -2.5F, 5, 9, 5, scale);

        ModelRenderer rightarmArmour = new ModelRenderer(this, 76, 38);
        rightarmArmour.addBox(-4F, -2.5F, -2.5F, 5, 9, 5, scale);

        ModelRenderer rightlegArmour = new ModelRenderer(this, 56, 16);
        rightlegArmour.addBox(-2.5F, -0.5F, -2.5F, 5, 9, 5, scale);

        ModelRenderer leftlegArmour = new ModelRenderer(this, 56, 16);
        leftlegArmour.mirror = true;
        leftlegArmour.addBox(-2.5F, -0.5F, -2.5F, 5, 9, 5, scale);

        this.bipedBody.addChild(bodyArmour);
        this.bipedBody.addChild(bodyBack);
        this.bipedLeftArm.addChild(leftarmPauldron);
        this.bipedLeftArm.addChild(leftarmArmour);
        this.bipedRightArm.addChild(rightarmPauldron);
        this.bipedRightArm.addChild(rightarmArmour);
        this.bipedLeftLeg.addChild(leftlegArmour);
        this.bipedLeftLeg.addChild(leftfoot);
        this.bipedRightLeg.addChild(rightlegArmour);
        this.bipedRightLeg.addChild(rightfoot);
        this.bipedHead.addChild(headArmour);
        this.bipedHead.addChild(headMask);
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        boolean isPlayer = entity instanceof EntityPlayer;
        boolean isFirstPerson = false;

        if (isPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            isFirstPerson = Minecraft.getMinecraft().thePlayer == player && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;

            // Set animation states for ModelBiped based on player actions
            ItemStack heldItem = player.getHeldItem();
            this.heldItemRight = (heldItem != null) ? 1 : 0;
            this.aimedBow = false;

            if (heldItem != null && player.getItemInUseCount() > 0) {
                EnumAction action = heldItem.getItemUseAction();
                if (action == EnumAction.block) {
                    this.heldItemRight = 3;
                } else if (action == EnumAction.bow) {
                    this.aimedBow = true;
                }
            }
        }

        // Let ModelBiped calculate all the standard rotations (walking, sneaking, etc.)
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);

        // --- Render Cleanup ---
        // 1. Use GL transforms for view-specific adjustments, don't modify model state.
        // 2. Hide parts based on view.

        GL11.glPushMatrix();
        try {
            // If in first person, shift the model forward to prevent clipping.
            // This is a temporary, per-frame adjustment and won't corrupt the model's state.
            if (isFirstPerson) {
                GL11.glTranslatef(0, 0, 6.0F / 16.0F);
            }

            // Hide parts for first-person view
            bipedLeftArm.isHidden = isFirstPerson;
            bipedRightArm.isHidden = isFirstPerson;

            // Render the main body parts
            if (!isFirstPerson) {
                this.bipedHead.render(f5);
            }
            this.bipedBody.render(f5);
            this.bipedRightArm.render(f5);
            this.bipedLeftArm.render(f5);
            this.bipedRightLeg.render(f5);
            this.bipedLeftLeg.render(f5);

        } finally {
            // Always restore the matrix to its original state
            GL11.glPopMatrix();
        }
    }
}