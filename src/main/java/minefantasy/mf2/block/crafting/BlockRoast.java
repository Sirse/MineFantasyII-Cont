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
import minefantasy.mf2.block.tileentity.TileEntityRoast;
import minefantasy.mf2.item.list.CreativeTabMF;

public class BlockRoast extends BlockStation<TileEntityRoast> {

    public static int roast_RI = 113;
    public String tex;
    public Random rand = new Random();
    private boolean isOven;
    private int tier;

    public BlockRoast(int tier, String tex, boolean isOven) {
        super(Material.rock, TileEntityRoast.class);
        this.isOven = isOven;
        this.tex = tex;
        this.tier = tier;
        String name = "food_" + (isOven ? "oven" : "roast") + "_" + tex;
        this.setBlockName(name);
        GameRegistry.registerBlock(this, name);
        this.setHardness(1.5F);
        this.setResistance(1F);
        this.setLightOpacity(0);
        this.setCreativeTab(CreativeTabMF.tabUtil);
        if (isOven) {
            setBlockBounds(3F / 16F, 0F, 3F / 16F, 13F / 16F, 1F / 1.6F, 13F / 16F);
        } else {
            setBlockBounds(3F / 16F, 0F, 3F / 16F, 13F / 16F, 0.125F, 13F / 16F);
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityRoast();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer user, int side, float xOffset,
            float yOffset, float zOffset) {
        TileEntityRoast tile = getTile(world, x, y, z);
        if (tile != null) {
            return tile.interact(user);
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.anvil.getIcon(side, 0);
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

    public boolean isOven() {
        return isOven;
    }

    @Override
    public int getRenderType() {
        return isOven ? roast_RI : super.getRenderType();
    }

    @Override
    protected boolean facesPlacer() {
        return true;
    }

    /** Only the food on the spit is an item; the rest is what the spit shows. */
    @Override
    protected int spilledSlots(TileEntityRoast station) {
        return 1;
    }
}
