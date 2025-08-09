package minefantasy.mf2.client.render;

import minefantasy.mf2.api.archery.AmmoMechanicsMF;
import minefantasy.mf2.api.archery.IDisplayMFAmmo;
import minefantasy.mf2.api.archery.IFirearm;
import minefantasy.mf2.api.crafting.IBasicMetre;
import minefantasy.mf2.api.crafting.IQualityBalance;
import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.block.tileentity.TileEntityAnvilMF;
import minefantasy.mf2.block.tileentity.TileEntityCarpenterMF;
import minefantasy.mf2.block.tileentity.TileEntityRoad;
import minefantasy.mf2.block.tileentity.TileEntityTanningRack;
import minefantasy.mf2.config.ConfigClient;
import minefantasy.mf2.entity.EntityCogwork;
import minefantasy.mf2.item.gadget.IScope;
import minefantasy.mf2.item.tool.advanced.ItemMattock;
import minefantasy.mf2.item.weapon.ItemWeaponMF;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@SuppressWarnings("java:S1192") // Suppress warnings about string literal duplication for texture paths
public class MineFantasyHUD extends Gui {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation HUD_TEXTURE = TextureHelperMF.getResource("textures/gui/hud_overlay.png");
    private static final ResourceLocation COGWORK_HELM_TEXTURE = TextureHelperMF.getResource("textures/gui/scopes/cogwork_helm.png");
    private static final ResourceLocation SCOPE_TEXTURE = TextureHelperMF.getResource("textures/gui/scopes/scope_basic.png");

    // Texture constants for hud_overlay.png
    private static final int STAMINA_BAR_BG_U = 0;
    private static final int STAMINA_BAR_BG_V = 0;
    private static final int STAMINA_BAR_FG_U = 0;
    private static final int STAMINA_BAR_FG_V = 5;
    private static final int STAMINA_BAR_FLASH_U = 0;
    private static final int STAMINA_BAR_FLASH_V = 10;
    private static final int STAMINA_BAR_WIDTH = 81;
    private static final int STAMINA_BAR_HEIGHT = 5;

    private static final int CRAFT_METER_BG_U = 84;
    private static final int CRAFT_METER_BG_V = 0;
    private static final int CRAFT_METER_WIDTH = 172;
    private static final int CRAFT_METER_HEIGHT = 20;

    private static final int CRAFT_METER_FG_U = 90;
    private static final int CRAFT_METER_FG_V = 20;
    private static final int CRAFT_METER_FG_WIDTH = 160;
    private static final int CRAFT_METER_FG_HEIGHT = 3;

    private static final int QUALITY_METER_BG_U = 84;
    private static final int QUALITY_METER_BG_V = 23;
    private static final int QUALITY_METER_WIDTH = 172;
    private static final int QUALITY_METER_HEIGHT = 10;
    private static final int QUALITY_MARKER_U = 84;
    private static final int QUALITY_MARKER_V = 33;
    private static final int QUALITY_MARKER_WIDTH = 3;
    private static final int QUALITY_MARKER_HEIGHT = 5;

    private static final int ROAD_LOCK_ICON_UNLOCKED_U = 8;
    private static final int ROAD_LOCK_ICON_LOCKED_U = 0;
    private static final int ROAD_LOCK_ICON_V = 20;
    private static final int ROAD_LOCK_ICON_WIDTH = 8;
    private static final int ROAD_LOCK_ICON_HEIGHT = 12;

    private static int[] getOrientsFor(int screenX, int screenY, int cfgX, int cfgY) {
        int xOrient = cfgX == -1 ? 0 : cfgX == 1 ? screenX : screenX / 2;
        int yOrient = cfgY == -1 ? 0 : cfgY == 1 ? screenY : screenY / 2;

        return new int[]{xOrient, yOrient};
    }

    public void renderViewport() {
        if (mc.thePlayer == null) return;

        EntityPlayer player = mc.thePlayer;
        if (mc.gameSettings.thirdPersonView == 0) {
            ItemStack heldItem = player.getHeldItem();
            if (heldItem != null && heldItem.getItem() instanceof IScope) {
                renderScope(heldItem);
            }
            if (player.ridingEntity instanceof EntityCogwork) {
                renderPowerHelmet((EntityCogwork) player.ridingEntity);
            }
        }
    }

    public void renderGameOverlay(float partialTicks, int mouseX, int mouseY) {
        if (mc.thePlayer == null) return;

        EntityPlayer player = mc.thePlayer;
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();

        boolean inContainer = mc.currentScreen instanceof GuiInventory || mc.currentScreen instanceof GuiContainerCreative;

        if (inContainer) {
            renderArmourRating(player, width, height);
        } else {
            renderAmmo(player, width, height);
        }

        if (StaminaBar.isSystemActive && !player.capabilities.isCreativeMode && !PowerArmour.isWearingCogwork(player)) {
            renderStaminaBar(player, width, height);
        }

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null) {
            if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit instanceof EntityCogwork) {
                lookAtCogwork((EntityCogwork) mop.entityHit, width, height);
            } else if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                renderBlockHUD(player, mop.blockX, mop.blockY, mop.blockZ, width, height);
            }
        }
    }

    private void renderBlockHUD(EntityPlayer player, int x, int y, int z, int screenWidth, int screenHeight) {
        TileEntity tile = player.worldObj.getTileEntity(x, y, z);
        if (tile == null) return;

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        if (tile instanceof TileEntityAnvilMF) {
            renderAnvilMetre(player, (TileEntityAnvilMF) tile, screenWidth, screenHeight);
        }
        if (tile instanceof TileEntityCarpenterMF) {
            renderCarpenterMetre(player, (TileEntityCarpenterMF) tile, screenWidth, screenHeight);
        }
        if (tile instanceof TileEntityTanningRack) {
            renderTanningRackMetre(player, (TileEntityTanningRack) tile, screenWidth, screenHeight);
        }
        if (tile instanceof IBasicMetre) {
            renderGenericMetre((IBasicMetre) tile, screenWidth, screenHeight);
        }

        if (tile instanceof IQualityBalance) {
            renderQualityBalance((IQualityBalance) tile, screenWidth, screenHeight);
        }
        if (tile instanceof TileEntityRoad) {
            renderRoad(player, (TileEntityRoad) tile, screenWidth, screenHeight);
        }
    }

    private void renderScope(ItemStack item) {
        if (!(item.getItem() instanceof IFirearm)) return;

        float factor = ((IScope) item.getItem()).getZoom(item);
        if (factor > 0.1F) {
            GL11.glPushMatrix();
            ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int width = res.getScaledWidth();
            int height = res.getScaledHeight();

            mc.renderEngine.bindTexture(SCOPE_TEXTURE);
            drawTexturedModalRect(width / 2 - 128, height / 2 - 128, 0, 0, 256, 256);

            GL11.glPopMatrix();
        }
    }

    private void lookAtCogwork(EntityCogwork suit, int width, int height) {
        GL11.glPushMatrix();
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        renderCogworkFuel(width, height, suit);
        GL11.glPopMatrix();
    }

    private void renderPowerHelmet(EntityCogwork suit) {
        GL11.glPushMatrix();
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        renderHelmetBlur(width, height);
        renderCogworkFuel(width, height, suit);

        GL11.glPopMatrix();
    }

    private void renderCogworkFuel(int width, int height, EntityCogwork suit) {
        mc.renderEngine.bindTexture(HUD_TEXTURE);
        int[] orient = getOrientsFor(width, height, ConfigClient.CF_xOrient, ConfigClient.CF_yOrient);
        int xPos = orient[0] + ConfigClient.CF_xPos;
        int yPos = orient[1] + ConfigClient.CF_yPos;

        this.drawTexturedModalRect(xPos, yPos, CRAFT_METER_BG_U, 38, CRAFT_METER_WIDTH, CRAFT_METER_HEIGHT);
        int progress = suit.getMetreScaled(CRAFT_METER_FG_WIDTH);
        this.drawTexturedModalRect(xPos + 6 + (CRAFT_METER_FG_WIDTH - progress), yPos + 11, CRAFT_METER_FG_U, CRAFT_METER_FG_V, progress, CRAFT_METER_FG_HEIGHT);
    }

    private void renderHelmetBlur(int width, int height) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_ALPHA_TEST);

        mc.renderEngine.bindTexture(COGWORK_HELM_TEXTURE);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(0.0D, height, -90.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(width, height, -90.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(width, 0.0D, -90.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(0.0D, 0.0D, -90.0D, 0.0D, 0.0D);
        tessellator.draw();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderArmourRating(EntityPlayer player, int width, int height) {
        int[] orient = getOrientsFor(width, height, ConfigClient.AR_xOrient, ConfigClient.AR_yOrient);
        int xPos = orient[0] + ConfigClient.AR_xPos;
        int yPos = orient[1] + ConfigClient.AR_yPos;
        int yOffset = 8;

        int baseAR = (player.ridingEntity instanceof EntityCogwork) ? ((EntityCogwork) player.ridingEntity).getArmourRating() : 0;
        float weight = (player.ridingEntity instanceof EntityCogwork) ? ((EntityCogwork) player.ridingEntity).getWeight() : 0;

        if (ArmourCalculator.advancedDamageTypes) {
            mc.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("attribute.armour.protection"), xPos, yPos, Color.WHITE.getRGB());
            displayTraitValue(xPos, yPos + 8, 0, player, baseAR); // Cutting
            displayTraitValue(xPos, yPos + 16, 2, player, baseAR); // Piercing
            displayTraitValue(xPos, yPos + 24, 1, player, baseAR); // Bludgeoning
            yOffset = 32;
        } else {
            float generalAR = (int) (ArmourCalculator.getDRDisplay(player, 0) * 100F) + baseAR;
            mc.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("attribute.armour.protection") + ": " + ItemWeaponMF.decimal_format.format(generalAR), xPos, yPos, Color.WHITE.getRGB());
        }

        for (int i = 1; i <= 4; i++) {
            weight += ArmourCalculator.getPieceWeight(player.getEquipmentInSlot(i), i - 1);
        }

        mc.fontRenderer.drawStringWithShadow(CustomMaterial.getWeightString(weight), xPos, yPos + yOffset, Color.WHITE.getRGB());
    }

    private void displayTraitValue(int x, int y, int id, EntityPlayer player, int base) {
        float rating = (int) (ArmourCalculator.getDRDisplay(player, id) * 100F) + base;
        String text = StatCollector.translateToLocal("attribute.armour.rating." + id) + " " + ItemWeaponMF.decimal_format.format(rating);
        mc.fontRenderer.drawStringWithShadow(text, x, y, Color.WHITE.getRGB());
    }

    private void renderAmmo(EntityPlayer player, int width, int height) {
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof IDisplayMFAmmo)) return;

        int[] orient = getOrientsFor(width, height, ConfigClient.AC_xOrient, ConfigClient.AC_yOrient);
        int xPos = orient[0] + ConfigClient.AC_xPos;
        int yPos = orient[1] + ConfigClient.AC_yPos;

        ItemStack ammo = AmmoMechanicsMF.getAmmo(held);
        String text = (ammo != null) ? (ammo.getDisplayName() + " x" + ammo.stackSize) : StatCollector.translateToLocal("info.bow.reload");
        mc.fontRenderer.drawStringWithShadow(text, xPos, yPos, Color.WHITE.getRGB());

        int capacity = ((IDisplayMFAmmo) held.getItem()).getAmmoCapacity(held);
        if (capacity > 1) {
            ItemStack loadedAmmo = AmmoMechanicsMF.getArrowOnBow(held);
            int ammoCount = loadedAmmo == null ? 0 : loadedAmmo.stackSize;
            String ammoString = StatCollector.translateToLocalFormatted("info.firearm.ammo", ammoCount, capacity);
            mc.fontRenderer.drawStringWithShadow(ammoString, xPos, yPos + 10, Color.WHITE.getRGB());
        }
    }

    private void renderStaminaBar(EntityPlayer player, int width, int height) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        mc.renderEngine.bindTexture(HUD_TEXTURE);

        int[] orient = getOrientsFor(width, height, ConfigClient.stam_xOrient, ConfigClient.stam_yOrient);
        int xPos = orient[0] + ConfigClient.stam_xPos;
        int yPos = orient[1] + ConfigClient.stam_yPos;

        this.drawTexturedModalRect(xPos, yPos, STAMINA_BAR_BG_U, STAMINA_BAR_BG_V, STAMINA_BAR_WIDTH, STAMINA_BAR_HEIGHT);

        float staminaMax = StaminaBar.getTotalMaxStamina(player);
        float staminaCurrent = StaminaBar.getStaminaValue(player);
        float staminaPercentage = staminaMax > 0 ? Math.min(1.0F, staminaCurrent / staminaMax) : 0F;
        int stamWidth = (int) (STAMINA_BAR_WIDTH * staminaPercentage);

        int modifier = modifyMetre(stamWidth, ConfigClient.stam_direction);
        this.drawTexturedModalRect(xPos + modifier, yPos, STAMINA_BAR_FG_U, STAMINA_BAR_FG_V, stamWidth, STAMINA_BAR_HEIGHT);

        if (StaminaBar.getFlashTime(player) > 0 && player.ticksExisted % 10 < 5) {
            this.drawTexturedModalRect(xPos, yPos, STAMINA_BAR_FLASH_U, STAMINA_BAR_FLASH_V, STAMINA_BAR_WIDTH, STAMINA_BAR_HEIGHT);
        }

        if (mc.currentScreen instanceof GuiInventory) {
            String stamTxt = (int) staminaCurrent + " / " + (int) staminaMax;
            boolean bonus = StaminaBar.getBonusStamina(player) > 0;
            mc.fontRenderer.drawStringWithShadow(stamTxt, xPos + 41 - (mc.fontRenderer.getStringWidth(stamTxt) / 2), yPos - 2, bonus ? Color.CYAN.getRGB() : Color.WHITE.getRGB());
        }
        GL11.glDisable(GL11.GL_BLEND);
    }

    private int modifyMetre(int progress, int direction) {
        if (direction == 0) return (MineFantasyHUD.STAMINA_BAR_WIDTH - progress) / 2; // Center
        if (direction == 1) return MineFantasyHUD.STAMINA_BAR_WIDTH - progress; // Right to Left
        return 0; // Left to Right
    }

    private void renderAnvilMetre(EntityPlayer player, TileEntityAnvilMF tile, int width, int height) {
        GL11.glPushMatrix();
        mc.renderEngine.bindTexture(HUD_TEXTURE);
        int xPos = width / 2 - 86;
        int yPos = height - 69;

        this.drawTexturedModalRect(xPos, yPos, CRAFT_METER_BG_U, CRAFT_METER_BG_V, CRAFT_METER_WIDTH, CRAFT_METER_HEIGHT);
        this.drawTexturedModalRect(xPos + 6, yPos + 12, CRAFT_METER_FG_U, CRAFT_METER_FG_V, tile.getProgressBar(CRAFT_METER_FG_WIDTH), CRAFT_METER_FG_HEIGHT);

        boolean knowsCraft = tile.doesPlayerKnowCraft(player);
        String resultName = tile.getResultName();
        String s = knowsCraft ? (resultName.startsWith("gui.") ? StatCollector.translateToLocal(resultName) : resultName) : "????";
        mc.fontRenderer.drawString(s, xPos + 86 - (mc.fontRenderer.getStringWidth(s) / 2), yPos + 3, 0);

        if (knowsCraft && tile.getResultName() != null && !tile.getResultName().equalsIgnoreCase("") && tile.getToolNeeded() != null) {
            boolean hasTool = ToolHelper.isToolSufficient(player.getHeldItem(), tile.getToolNeeded(), tile.getToolTierNeeded());
            GuiHelper.renderToolIcon(this, tile.getToolNeeded(), tile.getToolTierNeeded(), xPos - 20, yPos, hasTool);
            if (tile.getAnvilTierNeeded() > -1) {
                GuiHelper.renderToolIcon(this, "anvil", tile.getAnvilTierNeeded(), xPos + 172, yPos, tile.tier >= tile.getAnvilTierNeeded());
            }
        }
        GL11.glPopMatrix();
    }

    private void renderCarpenterMetre(EntityPlayer player, TileEntityCarpenterMF tile, int width, int height) {
        GL11.glPushMatrix();
        mc.renderEngine.bindTexture(HUD_TEXTURE);
        int xPos = width / 2 - 86;
        int yPos = height - 69;

        this.drawTexturedModalRect(xPos, yPos, CRAFT_METER_BG_U, CRAFT_METER_BG_V, CRAFT_METER_WIDTH, CRAFT_METER_HEIGHT);
        this.drawTexturedModalRect(xPos + 6, yPos + 12, CRAFT_METER_FG_U, CRAFT_METER_FG_V, tile.getProgressBar(CRAFT_METER_FG_WIDTH), CRAFT_METER_FG_HEIGHT);

        boolean knowsCraft = tile.doesPlayerKnowCraft(player);
        String s = knowsCraft ? tile.getResultName() : "????";
        mc.fontRenderer.drawString(s, xPos + 86 - (mc.fontRenderer.getStringWidth(s) / 2), yPos + 3, 0);

        if (knowsCraft && tile.getResultName() != null && !tile.getResultName().equalsIgnoreCase("") && tile.getToolNeeded() != null) {
            boolean hasTool = ToolHelper.isToolSufficient(player.getHeldItem(), tile.getToolNeeded(), tile.getToolTierNeeded());
            GuiHelper.renderToolIcon(this, tile.getToolNeeded(), tile.getToolTierNeeded(), xPos - 20, yPos, hasTool);
        }
        GL11.glPopMatrix();
    }

    private void renderTanningRackMetre(EntityPlayer player, TileEntityTanningRack tile, int width, int height) {
        GL11.glPushMatrix();
        mc.renderEngine.bindTexture(HUD_TEXTURE);
        int xPos = width / 2 - 86;
        int yPos = height - 69;

        this.drawTexturedModalRect(xPos, yPos, CRAFT_METER_BG_U, CRAFT_METER_BG_V, CRAFT_METER_WIDTH, CRAFT_METER_HEIGHT);
        this.drawTexturedModalRect(xPos + 6, yPos + 12, CRAFT_METER_FG_U, CRAFT_METER_FG_V, tile.getProgressBar(CRAFT_METER_FG_WIDTH), CRAFT_METER_FG_HEIGHT);

        boolean knowsCraft = tile.doesPlayerKnowCraft(player);
        String s = knowsCraft ? tile.getResultName() : "????";
        ItemStack result = tile.getStackInSlot(1);
        if (result != null && result.stackSize > 1) {
            s += " x" + result.stackSize;
        }
        mc.fontRenderer.drawString(s, xPos + 86 - (mc.fontRenderer.getStringWidth(s) / 2), yPos + 3, 0);

        if (knowsCraft && !tile.getResultName().equalsIgnoreCase("") && tile.toolType != null) {
            boolean hasTool = ToolHelper.isToolSufficient(player.getHeldItem(), tile.toolType, -1);
            GuiHelper.renderToolIcon(this, tile.toolType, tile.tier, xPos - 20, yPos, hasTool);
        }
        GL11.glPopMatrix();
    }

    private void renderGenericMetre(IBasicMetre tile, int width, int height) {
        if (!tile.shouldShowMetre()) return;

        GL11.glPushMatrix();
        mc.renderEngine.bindTexture(HUD_TEXTURE);
        int xPos = width / 2 - 86;
        int yPos = height - 69;

        this.drawTexturedModalRect(xPos, yPos, CRAFT_METER_BG_U, CRAFT_METER_BG_V, CRAFT_METER_WIDTH, CRAFT_METER_HEIGHT);
        this.drawTexturedModalRect(xPos + 6, yPos + 12, CRAFT_METER_FG_U, CRAFT_METER_FG_V, tile.getMetreScale(CRAFT_METER_FG_WIDTH), CRAFT_METER_FG_HEIGHT);

        String s = tile.getLocalisedName();
        mc.fontRenderer.drawString(s, xPos + 86 - (mc.fontRenderer.getStringWidth(s) / 2), yPos + 3, 0);
        GL11.glPopMatrix();
    }

    private void renderQualityBalance(IQualityBalance tile, int width, int height) {
        if (!tile.shouldShowMetre()) return;

        GL11.glPushMatrix();
        mc.renderEngine.bindTexture(HUD_TEXTURE);

        int xPos = width / 2 - 86;
        int yPos = height - 69 + 17;
        int barWidth = 160;
        int centre = xPos + 5 + (barWidth / 2);

        this.drawTexturedModalRect(xPos, yPos, QUALITY_METER_BG_U, QUALITY_METER_BG_V, QUALITY_METER_WIDTH, QUALITY_METER_HEIGHT);

        // Marker
        int markerPos = (int) (centre + (tile.getMarkerPosition() * barWidth / 2F));
        this.drawTexturedModalRect(markerPos, yPos + 1, QUALITY_MARKER_U, QUALITY_MARKER_V, QUALITY_MARKER_WIDTH, QUALITY_MARKER_HEIGHT);

        // Thresholds
        int offset = (int) (tile.getThresholdPosition() / 2F * barWidth);
        this.drawTexturedModalRect(centre - offset - 1, yPos + 1, 87, 33, 2, 4);
        this.drawTexturedModalRect(centre + offset, yPos + 1, 89, 33, 2, 4);

        int offset2 = (int) (tile.getSuperThresholdPosition() / 2F * barWidth);
        this.drawTexturedModalRect(centre - offset2, yPos + 1, 91, 33, 1, 4);
        this.drawTexturedModalRect(centre + offset2, yPos + 1, 91, 33, 1, 4);

        GL11.glPopMatrix();
    }

    private void renderRoad(EntityPlayer player, TileEntityRoad tile, int width, int height) {
        ItemStack heldItem = player.getHeldItem();
        if (heldItem != null && heldItem.getItem() instanceof ItemMattock) {
            GL11.glPushMatrix();
            mc.renderEngine.bindTexture(HUD_TEXTURE);

            int xPos = width / 2 + 12;
            int yPos = height / 2 - 6;
            int u = tile.isLocked ? ROAD_LOCK_ICON_LOCKED_U : ROAD_LOCK_ICON_UNLOCKED_U;

            this.drawTexturedModalRect(xPos, yPos, u, ROAD_LOCK_ICON_V, ROAD_LOCK_ICON_WIDTH, ROAD_LOCK_ICON_HEIGHT);
            GL11.glPopMatrix();
        }
    }
}

