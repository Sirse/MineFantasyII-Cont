package minefantasy.mf2.client.render.block;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import minefantasy.mf2.block.crafting.BlockKitchenBench;

public class RenderKitchenBench implements ISimpleBlockRenderingHandler {

    private static final TileEntityKitchenBenchRenderer invModel = new TileEntityKitchenBenchRenderer();

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        invModel.renderModelAt(0, 0F, 0F, 0F, 0F);
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
            RenderBlocks renderer) {
        return false;
    }

    @Override
    public int getRenderId() {
        return BlockKitchenBench.kitchen_RI;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }
}
