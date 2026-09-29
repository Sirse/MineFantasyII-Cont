package minefantasy.mf2.block.refining;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.helpers.Tiles;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.block.basic.BlockStation;
import minefantasy.mf2.block.tileentity.blastfurnace.TileEntityBlastFC;
import minefantasy.mf2.item.list.CreativeTabMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

public class BlockBFC extends BlockStation<TileEntityBlastFC> {

    public IIcon bottomTex;
    public IIcon sideTex;
    private Random rand = new Random();

    public BlockBFC() {
        super(Material.anvil, TileEntityBlastFC.class);
        GameRegistry.registerBlock(this, "MF_BlastChamber");
        setBlockName("blastfurnchamber");
        this.setStepSound(Block.soundTypeMetal);
        this.setHardness(8F);
        this.setResistance(10F);
        this.setCreativeTab(CreativeTabMF.tabUtil);
    }

    /**
     * Remember who built this so a smoke-overload blast is attributed to its owner.
     */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack item) {
        super.onBlockPlacedBy(world, x, y, z, placer, item);
        if (!world.isRemote && placer instanceof EntityPlayer) {
            TileEntityBlastFC tile = Tiles.get(world, x, y, z, TileEntityBlastFC.class);
            if (tile != null) {
                tile.setOwner((EntityPlayer) placer);
            }
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityBlastFC();
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
        TileEntityBlastFC tile = getTile(world, x, y, z);
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
        TileEntityBlastFC tile = getTile(world, x, y, z);
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
        sideTex = reg.registerIcon("minefantasy2:processor/blast_chamber_side");
        bottomTex = reg.registerIcon("minefantasy2:processor/blast_chamber_top");
    }

}
