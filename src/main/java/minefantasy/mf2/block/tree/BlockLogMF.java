package minefantasy.mf2.block.tree;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.InventorySlots;

public class BlockLogMF extends BlockLog {

    private IIcon sideTex, topTex;
    private String name;
    private Random rand = new Random();

    public BlockLogMF(String baseWood) {
        name = baseWood.toLowerCase() + "_log";
        GameRegistry.registerBlock(this, name);
        setBlockName(name);
        this.setHarvestLevel("axe", 0);
        this.setCreativeTab(CreativeTabs.tabBlock);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int meta) {
        if (meta == 15) {
            return side <= 1 ? topTex : sideTex;
        }
        return super.getIcon(side, meta);
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected IIcon getSideIcon(int meta) {
        return sideTex;
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected IIcon getTopIcon(int meta) {
        return topTex;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        sideTex = reg.registerIcon("minefantasy2:tree/" + name + "_side");
        topTex = reg.registerIcon("minefantasy2:tree/" + name + "_top");
    }

    private Block getSaplingDrop() {
        return this == BlockListMF.log_ebony ? BlockListMF.sapling_ebony
                : this == BlockListMF.log_ironbark ? BlockListMF.sapling_ironbark : BlockListMF.sapling_yew;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        super.breakBlock(world, x, y, z, block, meta);
        if (meta == 15) {
            InventorySlots.drop(world, x, y, z, new ItemStack(getSaplingDrop(), 1));
        }
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }
}
