package minefantasy.mf2.block.refining;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.basic.BlockTiled;
import minefantasy.mf2.block.tileentity.TileEntityBellows;
import minefantasy.mf2.item.list.CreativeTabMF;

/**
 * @author Anonymous Productions
 *         <p>
 *         Sources are provided for educational reasons. though small bits of code, or methods can be used in your own
 *         creations.
 */
public class BlockBellows extends BlockTiled<TileEntityBellows> {

    public static int bellows_RI = 105;

    private Random rand = new Random();

    public BlockBellows() {
        super(Material.wood, TileEntityBellows.class);
        GameRegistry.registerBlock(this, "MF_Bellows");
        setBlockName("bellows");
        this.setHardness(1F);
        this.setResistance(0.5F);
        this.setCreativeTab(CreativeTabMF.tabUtil);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 0);
    }

    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return bellows_RI;
    }

    @Override
    protected boolean facesPlacer() {
        return true;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int i, float f, float f1,
            float f2) {
        TileEntityBellows bellows = getTile(world, x, y, z);
        if (bellows != null) {
            bellows.interact(player, 2F);
        }
        return true;
    }

    @Override
    public void onFallenUpon(World world, int x, int y, int z, Entity entity, float fallDistance) {
        super.onFallenUpon(world, x, y, z, entity, fallDistance);
        if (!world.isRemote && entity instanceof EntityLivingBase && fallDistance > 1F) {
            TileEntityBellows tile = getTile(world, x, y, z);
            if (tile != null) {
                tile.interact((EntityLivingBase) entity, 2F);
            }
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityBellows();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister reg) {}
}
