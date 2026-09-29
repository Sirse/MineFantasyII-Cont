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

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.block.basic.BlockStation;
import minefantasy.mf2.block.tileentity.TileEntityKitchenBench;
import minefantasy.mf2.config.ConfigKitchen;
import minefantasy.mf2.item.list.CreativeTabMF;

public class BlockKitchenBench extends BlockStation<TileEntityKitchenBench> {

    public static int kitchen_RI = 119;

    private Random rand = new Random();

    public BlockKitchenBench() {
        super(Material.wood, TileEntityKitchenBench.class);
        GameRegistry.registerBlock(this, "MF_KitchenBench");
        setBlockName("kitchen_bench");
        setHardness(2.0F);
        setResistance(1.0F);
        setStepSound(Block.soundTypeWood);
        setCreativeTab(CreativeTabMF.tabFood);
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderType() {
        return kitchen_RI;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 0);
    }

    /**
     * Right click: hitting the top face crafts/washes, any other face opens the GUI.
     */
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer user, int side, float xOffset,
            float yOffset, float zOffset) {
        TileEntityKitchenBench tile = getTile(world, x, y, z);
        if (tile != null && ConfigKitchen.enableBench) {
            if (side == 1 && tile.interact(user)) {
                return true;
            }
            if (!world.isRemote && !user.isSneaking()) {
                user.openGui(MineFantasyII.instance, 0, world, x, y, z);
            }
        }
        return true;
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer user) {
        TileEntityKitchenBench tile = getTile(world, x, y, z);
        if (tile != null && ConfigKitchen.enableBench) {
            tile.interact(user);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister reg) {}

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityKitchenBench();
    }

    @Override
    protected boolean facesPlacer() {
        return true;
    }
}
