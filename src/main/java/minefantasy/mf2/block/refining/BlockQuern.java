package minefantasy.mf2.block.refining;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.block.tileentity.InventorySlots;
import minefantasy.mf2.block.tileentity.TileEntityQuern;
import minefantasy.mf2.item.list.CreativeTabMF;

public class BlockQuern extends BlockContainer {

    public static int quern_RI = 111;
    public IIcon bottomTex, sideTex, topTex;
    private Random rand = new Random();
    private String type;

    public BlockQuern(String type) {
        super(Material.rock);
        this.type = type;
        GameRegistry.registerBlock(this, "MF_Grind_" + type);
        setBlockName(type);
        this.setStepSound(Block.soundTypeStone);
        this.setHardness(5F);
        this.setResistance(5F);
        this.setCreativeTab(CreativeTabMF.tabUtil);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityQuern();
    }

    private TileEntityQuern getTile(IBlockAccess world, int x, int y, int z) {
        return (TileEntityQuern) world.getTileEntity(x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntityQuern tile = getTile(world, x, y, z);

        if (tile != null) {
            InventorySlots.spill(world, x, y, z, tile);

            world.func_147453_f(x, y, z, block);
        }

        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer user, int side, float xOffset,
            float yOffset, float zOffset) {
        TileEntityQuern tile = getTile(world, x, y, z);
        if (tile != null) {
            if (side == 1) {
                tile.onUse(user);
            } else {
                user.openGui(MineFantasyII.instance, 0, world, x, y, z);
            }
        }
        return true;
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer user) {
        TileEntityQuern tile = getTile(world, x, y, z);
        if (tile != null) {
            tile.onUse(user);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister reg) {
        this.topTex = reg.registerIcon("minefantasy2:processor/" + type + "_top");
        this.sideTex = reg.registerIcon("minefantasy2:processor/" + type + "_side");
        this.bottomTex = reg.registerIcon("minefantasy2:processor/" + type + "_base");
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return side == 0 ? bottomTex : side == 1 ? topTex : sideTex;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return quern_RI;
    }
}
