package minefantasy.mf2.block.decor;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.tileentity.decor.TileEntityTrough;
import minefantasy.mf2.item.list.CreativeTabMF;

public class BlockTrough extends BlockWoodDecor<TileEntityTrough> {

    public static final String NBT_fill = "Fill_Level";
    public static int trough_RI = 107;

    public BlockTrough(String name) {
        super(name, TileEntityTrough.class);
        this.setBlockBounds(0F, 0F, 0F, 1.0F, (7F / 16F), 1.0F);
        GameRegistry.registerBlock(this, ItemBlockTrough.class, name);
        setBlockName(name);
        this.setHardness(1F);
        this.setResistance(0.5F);
        this.setCreativeTab(CreativeTabMF.tabUtil);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {

    }

    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return trough_RI;
    }

    @Override
    protected boolean facesPlacer() {
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase user, ItemStack item) {
        // The material first: the level is kept within the capacity, which depends on it
        super.onBlockPlacedBy(world, x, y, z, user, item);
        TileEntityTrough tile = getTile(world, x, y, z);
        if (tile != null) {
            if (item.hasTagCompound() && item.getTagCompound().hasKey(NBT_fill)) {
                // The tag may not be one this block wrote: keep the level within the trough
                tile.fill = Math.max(0, Math.min(tile.getCapacity(), item.getTagCompound().getInteger(NBT_fill)));
            }
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityTrough();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer user, int side, float xOffset,
            float yOffset, float zOffset) {
        ItemStack held = user.getHeldItem();
        TileEntityTrough tile = getTile(world, x, y, z);
        if (tile != null) {
            if (tile.interact(user, held)) {
                world.playSoundEffect(
                        x + 0.5D,
                        y + 0.5D,
                        z + 0.5D,
                        "random.splash",
                        0.125F + user.getRNG().nextFloat() / 4F,
                        0.5F + user.getRNG().nextFloat());
                tile.syncData();
                return true;
            }
        }
        return false;
    }

    @Override
    protected ItemStack modifyDrop(TileEntityTrough tile, ItemStack item) {
        return modifyFill(tile, super.modifyDrop(tile, item));
    }

    private ItemStack modifyFill(TileEntityTrough tile, ItemStack item) {
        if (tile != null && item != null) {
            item.getTagCompound().setInteger(NBT_fill, tile.fill);
        }
        return item;
    }
}
