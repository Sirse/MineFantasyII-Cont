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

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.block.tileentity.InventorySlots;
import minefantasy.mf2.block.tileentity.TileEntityKitchenBench;
import minefantasy.mf2.config.ConfigKitchen;
import minefantasy.mf2.item.list.CreativeTabMF;

public class BlockKitchenBench extends BlockContainer {

    public static int kitchen_RI = 119;

    private Random rand = new Random();

    public BlockKitchenBench() {
        super(Material.wood);
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

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase user, ItemStack item) {
        int direction = MathHelper.floor_double(user.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        world.setBlockMetadataWithNotify(x, y, z, direction, 2);
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

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntityKitchenBench tile = getTile(world, x, y, z);

        if (tile != null) {
            InventorySlots.spill(world, x, y, z, tile);
        }

        super.breakBlock(world, x, y, z, block, meta);
    }

    private TileEntityKitchenBench getTile(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileEntityKitchenBench ? (TileEntityKitchenBench) tile : null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister reg) {}

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityKitchenBench();
    }
}
