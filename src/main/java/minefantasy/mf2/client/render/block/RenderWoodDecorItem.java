package minefantasy.mf2.client.render.block;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.block.decor.BlockAmmoBox;
import minefantasy.mf2.block.decor.BlockRack;
import minefantasy.mf2.block.decor.BlockTrough;

/** Unlike a block inventory renderer, an item renderer receives the stack's wood material. */
public final class RenderWoodDecorItem implements IItemRenderer {

    private final TileEntityAmmoBoxRenderer crates = new TileEntityAmmoBoxRenderer();
    private final TileEntityRackRenderer racks = new TileEntityRackRenderer();
    private final TileEntityTroughRenderer troughs = new TileEntityTroughRenderer();

    public static void register() {
        RenderWoodDecorItem renderer = new RenderWoodDecorItem();
        for (Object entry : Block.blockRegistry) {
            Block block = (Block) entry;
            if (block instanceof BlockAmmoBox || block instanceof BlockRack || block instanceof BlockTrough) {
                MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(block), renderer);
            }
        }
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type != ItemRenderType.FIRST_PERSON_MAP;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        Block block = Block.getBlockFromItem(item.getItem());
        CustomMaterial material = CustomToolHelper.getCustomPrimaryMaterial(item);
        if (material == null) {
            material = CustomMaterial.getMaterial("RefinedWood");
        }
        GL11.glPushMatrix();
        if (type == ItemRenderType.INVENTORY || type == ItemRenderType.ENTITY) {
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        }
        if (block instanceof BlockAmmoBox) {
            BlockAmmoBox crate = (BlockAmmoBox) block;
            crates.renderInvModel(crate.getFullTexName(), crate.storageType, material, 0, 0, 0, 0);
        } else if (block instanceof BlockRack) {
            racks.renderInvModel(material, ((BlockRack) block).getFullTexName(), 0, 0, 0, 0);
        } else if (block instanceof BlockTrough) {
            troughs.renderInvModel(((BlockTrough) block).getFullTexName(), material, 0, 0, 0);
        }
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glPopMatrix();
    }
}
