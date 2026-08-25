package minefantasy.mf2.block.basic;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

import cpw.mods.fml.common.registry.GameRegistry;

public class BlockPaneMF extends BlockPane {

    public BlockPaneMF(String name, String frontTex, String sideTex, Material material, boolean recoverable) {
        super("minefantasy2:basic/" + frontTex, "minefantasy2:basic/" + sideTex, material, recoverable);

        GameRegistry.registerBlock(this, name);
        setBlockName(name);
    }

    /**
     * Decorative metal bars: single texture, metal sound and pickaxe harvest.
     */
    public BlockPaneMF(String name, String texture, float hardness, float resistance) {
        super("minefantasy2:decor/" + texture, "minefantasy2:decor/" + texture, Material.iron, true);

        GameRegistry.registerBlock(this, name);
        setBlockName(name);
        setHardness(hardness);
        setResistance(resistance);
        setStepSound(Block.soundTypeMetal);
        setCreativeTab(CreativeTabs.tabBlock);
        setHarvestLevel("pickaxe", 0);
    }
}
