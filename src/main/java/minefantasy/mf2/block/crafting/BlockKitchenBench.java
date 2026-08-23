package minefantasy.mf2.block.crafting;

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
import minefantasy.mf2.block.tileentity.TileEntityKitchenBench;
import minefantasy.mf2.config.ConfigKitchen;
import minefantasy.mf2.item.list.CreativeTabMF;

public class BlockKitchenBench extends BlockContainer {

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
        return true;
    }

    @Override
    public boolean isOpaqueCube() {
        return true;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return Blocks.crafting_table.getIcon(side, meta);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase user, ItemStack item) {
        int direction = MathHelper.floor_double(user.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        world.setBlockMetadataWithNotify(x, y, z, direction, 2);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer user, int side, float xOffset,
            float yOffset, float zOffset) {
        if (!world.isRemote && !user.isSneaking() && ConfigKitchen.enableBench) {
            user.openGui(MineFantasyII.instance, 0, world, x, y, z);
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister reg) {}

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityKitchenBench();
    }
}
