package minefantasy.mf2.block.crafting;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.block.tileentity.InventorySlots;
import minefantasy.mf2.block.tileentity.TileEntityCarpenterMF;
import minefantasy.mf2.item.list.CreativeTabMF;

public class BlockCarpenter extends BlockContainer {

    public static int carpenter_RI = 101;

    @SideOnly(Side.CLIENT)
    public int CarpenterRenderSide;
    private int tier = 0;
    private Random rand = new Random();

    /** The bench tier recipes are checked against (TileEntityCarpenterMF gets it at creation) */
    public int getTier() {
        return tier;
    }

    public BlockCarpenter() {
        super(Material.wood);

        GameRegistry.registerBlock(this, "MF_CarpenterBench");
        setBlockName("carpenterBench");
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
     * Called when the block is placed in the world.
     */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase user, ItemStack item) {
        int direction = MathHelper.floor_double(user.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;

        world.setBlockMetadataWithNotify(x, y, z, direction, 2);
    }

    /**
     * Called upon block activation (right click on the block.)
     */
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer user, int side, float xOffset,
            float yOffset, float zOffset) {
        TileEntityCarpenterMF tile = getTile(world, x, y, z);
        if (tile != null && (world.isAirBlock(x, y + 1, z) || !world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN))) {
            if (side != 1 || !tile.tryCraft(user) && !world.isRemote) {
                user.openGui(MineFantasyII.instance, 0, world, x, y, z);
            }
        }
        return true;
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer user) {
        TileEntityCarpenterMF tile = getTile(world, x, y, z);
        if (tile != null) {
            tile.tryCraft(user);
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCarpenterMF(tier);
    }

    private TileEntityCarpenterMF getTile(World world, int x, int y, int z) {
        return (TileEntityCarpenterMF) world.getTileEntity(x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntityCarpenterMF tile = getTile(world, x, y, z);

        if (tile != null) {
            InventorySlots.spill(world, x, y, z, tile);

            world.func_147453_f(x, y, z, block);
        }

        super.breakBlock(world, x, y, z, block, meta);
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
        return carpenter_RI;
    }
}
