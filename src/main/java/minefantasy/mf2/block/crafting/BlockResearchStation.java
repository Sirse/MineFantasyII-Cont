package minefantasy.mf2.block.crafting;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.block.basic.BlockStation;
import minefantasy.mf2.block.tileentity.TileEntityResearch;
import minefantasy.mf2.item.list.CreativeTabMF;

public class BlockResearchStation extends BlockStation<TileEntityResearch> {

    public static int research_RI = 106;
    private int tier = 0;
    private Random rand = new Random();

    public BlockResearchStation() {
        super(Material.wood, TileEntityResearch.class);

        GameRegistry.registerBlock(this, "MF_Research");
        setBlockName("researchStation");
        this.setStepSound(Block.soundTypeWood);
        this.setHardness(5F);
        this.setResistance(2F);
        this.setLightOpacity(0);
        this.setCreativeTab(CreativeTabMF.tabUtil);
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    /**
     * Called upon block activation (right click on the block.)
     */
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer user, int side, float xOffset,
            float yOffset, float zOffset) {
        TileEntityResearch tile = getTile(world, x, y, z);
        if (tile != null && (world.isAirBlock(x, y + 1, z) || !world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN))) {
            if (!(side == 1 && tile.interact(user))) {
                user.openGui(MineFantasyII.instance, 0, world, x, y, z);
            }
        }
        return true;
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer user) {
        TileEntityResearch tile = getTile(world, x, y, z);
        if (tile != null && (world.isAirBlock(x, y + 1, z) || !world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN))) {
            tile.interact(user);
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityResearch();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.crafting_table.getIcon(side, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {

    }

    @Override
    public int getRenderType() {
        return research_RI;
    }

    @Override
    protected boolean facesPlacer() {
        return true;
    }
}
