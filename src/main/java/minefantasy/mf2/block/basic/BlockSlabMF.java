package minefantasy.mf2.block.basic;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.item.ItemSlabMF;

/**
 * Self-registering slab pair (half + double) with a single shared texture and a 3-into-6 crafting recipe, mirroring how
 * ConstructionBlockMF bundles stairs.
 */
public class BlockSlabMF extends BlockSlab {

    private final String texture;
    /**
     * The half slab of this pair; set on both instances after construction
     */
    private BlockSlabMF single;

    /**
     * Factory: registers the half slab (creative), the double slab and the crafting recipe.
     *
     * @param texture path relative to textures/blocks, e.g. "basic/mud_brick"
     */
    public static BlockSlabMF create(String name, String texture, Material material, float hardness, float resistance,
            CreativeTabs tab, ItemStack source) {
        BlockSlabMF half = new BlockSlabMF(name, texture, material, false);
        BlockSlabMF full = new BlockSlabMF(name + "_double", texture, material, true);
        half.single = half;
        half.full = full;

        half.setBlockName(name).setCreativeTab(tab);
        full.setBlockName(name);

        for (BlockSlabMF slab : new BlockSlabMF[] { half, full }) {
            slab.setHardness(hardness);
            slab.setResistance(resistance);
            if (material == Material.rock) {
                slab.setHarvestLevel("pickaxe", 0);
            }
            if (material == Material.wood) {
                slab.setStepSound(Block.soundTypeWood);
            }
        }

        GameRegistry.registerBlock(half, ItemSlabMF.class, name);
        GameRegistry.registerBlock(full, name + "_double");
        GameRegistry.addRecipe(new ItemStack(half, 6), new Object[] { "XXX", 'X', source });

        return half;
    }

    private BlockSlabMF full;

    /** The double slab of this pair; read by ItemSlabMF, which Forge constructs with the half slab alone. */
    public BlockSlabMF getDoubleSlab() {
        return full;
    }

    private BlockSlabMF(String name, String texture, Material material, boolean isDouble) {
        super(isDouble, material);
        this.texture = texture;
        this.setBlockName(name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return blockIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(MineFantasyII.MODID + ":" + texture);
    }

    @Override
    public String func_150002_b(int meta) {
        return getUnlocalizedName();
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return isDouble() ? Item.getItemFromBlock(single != null ? single : this) : Item.getItemFromBlock(this);
    }

    @Override
    protected ItemStack createStackedBlock(int meta) {
        return new ItemStack(this, 1, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        if (!isDouble()) {
            list.add(new ItemStack(item, 1, 0));
        }
    }

    private boolean isDouble() {
        return field_150004_a;
    }
}
