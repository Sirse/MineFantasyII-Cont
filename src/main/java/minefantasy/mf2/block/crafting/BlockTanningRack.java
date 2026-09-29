package minefantasy.mf2.block.crafting;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.basic.BlockStation;
import minefantasy.mf2.block.tileentity.TileEntityTanningRack;
import minefantasy.mf2.item.list.CreativeTabMF;

public class BlockTanningRack extends BlockStation<TileEntityTanningRack> {

    public static int tanner_RI = 103;

    public int tier;
    public String tex;
    public Random rand = new Random();

    public BlockTanningRack(int tier, String tex) {
        super(Material.wood, TileEntityTanningRack.class);

        this.tier = tier;
        this.tex = tex;
        String name = "tanner" + tex;
        this.setBlockName(name);
        GameRegistry.registerBlock(this, name);
        this.setHardness(1F + 0.5F * tier);
        this.setResistance(1F);
        this.setLightOpacity(0);
        this.setCreativeTab(CreativeTabMF.tabUtil);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityTanningRack(tier, tex);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer user, int side, float xOffset,
            float yOffset, float zOffset) {
        TileEntityTanningRack tile = getTile(world, x, y, z);
        if (tile != null) {
            return tile.interact(user, false, false);
        }
        return true;
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer user) {
        TileEntityTanningRack tile = getTile(world, x, y, z);
        if (tile != null) {
            tile.interact(user, true, false);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {

    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return tanner_RI;
    }

    @Override
    protected boolean facesPlacer() {
        return true;
    }

    /** Slot 1 only shows what the hide becomes. */
    @Override
    protected int spilledSlots(TileEntityTanningRack station) {
        return 1;
    }
}
