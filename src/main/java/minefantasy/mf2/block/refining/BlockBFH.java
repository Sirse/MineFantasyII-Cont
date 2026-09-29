package minefantasy.mf2.block.refining;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.block.basic.BlockStation;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFH;
import minefantasy.mf2.item.list.CreativeTabMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

public class BlockBFH extends BlockStation<TileEntityBlastFH> {

    private static boolean keepInventory;
    public IIcon bottomTex;
    public IIcon sideTex;
    public boolean isActive;
    private Random rand = new Random();

    public BlockBFH(boolean isActive) {
        super(Material.anvil, TileEntityBlastFH.class);
        this.isActive = isActive;
        GameRegistry.registerBlock(this, isActive ? "MF_BlastHeaterActive" : "MF_BlastHeater");
        setBlockName("blastfurnheater");
        this.setStepSound(Block.soundTypeMetal);
        this.setHardness(10F);
        this.setResistance(10F);
        this.setCreativeTab(CreativeTabMF.tabUtil);
    }

    public static void updateFurnaceBlockState(boolean state, World world, int x, int y, int z) {
        int l = world.getBlockMetadata(x, y, z);
        TileEntity tileentity = world.getTileEntity(x, y, z);
        keepInventory = true;

        if (state) {
            world.setBlock(x, y, z, BlockListMF.blast_heater_active);
        } else {
            world.setBlock(x, y, z, BlockListMF.blast_heater);
        }

        keepInventory = false;
        world.setBlockMetadataWithNotify(x, y, z, l, 2);

        if (tileentity != null) {
            tileentity.validate();
            world.setTileEntity(x, y, z, tileentity);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
        if (isActive && rand.nextInt(20) == 0) {
            world.playSound(
                    x + 0.5F,
                    y + 0.5F,
                    z + 0.5F,
                    "fire.fire",
                    1.0F + rand.nextFloat(),
                    rand.nextFloat() * 0.7F + 0.3F,
                    false);
        }
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        if (!isActive) {
            super.getSubBlocks(item, tab, list);
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityBlastFH();
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
        TileEntityBlastFH tile = getTile(world, x, y, z);
        if (tile != null) tile.updateBuild();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (side == 1 || side == 0) {
            return bottomTex;
        }
        return sideTex;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer user, int side, float xOffset,
            float yOffset, float zOffset) {
        if (!ResearchLogic.hasInfoUnlocked(user, KnowledgeListMF.blastfurn)) {
            if (world.isRemote)
                user.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("knowledge.unknownUse")));
            return false;
        }
        TileEntityBlastFH tile = getTile(world, x, y, z);
        if (tile != null) {
            if (!world.isRemote) {
                user.openGui(MineFantasyII.instance, 0, world, x, y, z);
            }
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        sideTex = isActive ? reg.registerIcon("minefantasy2:processor/blast_heater_active")
                : reg.registerIcon("minefantasy2:processor/blast_heater");
        bottomTex = reg.registerIcon("minefantasy2:processor/blast_chamber_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(BlockListMF.blast_heater);
    }

    @Override
    protected boolean keepsContents() {
        return keepInventory;
    }
}
