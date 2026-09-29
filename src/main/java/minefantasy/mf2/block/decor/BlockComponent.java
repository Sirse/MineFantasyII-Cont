package minefantasy.mf2.block.decor;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.block.basic.BlockTiled;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.InventorySlots;
import minefantasy.mf2.block.tileentity.TileEntityComponent;

public class BlockComponent extends BlockTiled<TileEntityComponent> {

    public static int component_RI = 118;
    private final Random rand = new Random();

    public BlockComponent() {
        super(Material.circuits, TileEntityComponent.class);
        GameRegistry.registerBlock(this, ItemBlockAmmoBox.class, "MF_ComponentStorage");
        setBlockName("");
        this.setHardness(1F);
        this.setResistance(1F);
        this.setBlockBounds(1 / 16F, 0F, 1 / 16F, 15 / 16F, 12 / 16F, 15 / 16F);
    }

    public static int placeComponent(EntityPlayer user, ItemStack item, World world, int x, int y, int z, String type,
            String tex, int dir) {
        if (world.isAirBlock(x, y, z) && canBuildOn(world, x, y - 1, z)) {
            world.setBlock(x, y, z, BlockListMF.components, dir, 2);

            int max = getStorageSize(type);
            int size = user.isSneaking() ? Math.min(item.stackSize, max) : 1;

            TileEntityComponent tile = new TileEntityComponent();
            ItemStack newitem = item.copy();
            newitem.stackSize = 1;
            tile.setItem(newitem, type, tex, max, size);
            world.setTileEntity(x, y, z, tile);
            return size;
        }
        return 0;
    }

    public static boolean canBuildOn(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityComponent) {
            return ((TileEntityComponent) tile).isFull();
        }
        return world.isSideSolid(x, y, z, ForgeDirection.UP);
    }

    public static int useComponent(ItemStack item, String type, String tex, EntityPlayer user,
            MovingObjectPosition movingobjectposition) {
        if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            int i = movingobjectposition.blockX;
            int j = movingobjectposition.blockY;
            int k = movingobjectposition.blockZ;

            if (movingobjectposition.sideHit == 0) {
                --j;
            }

            if (movingobjectposition.sideHit == 1) {
                ++j;
            }

            if (movingobjectposition.sideHit == 2) {
                --k;
            }

            if (movingobjectposition.sideHit == 3) {
                ++k;
            }

            if (movingobjectposition.sideHit == 4) {
                --i;
            }

            if (movingobjectposition.sideHit == 5) {
                ++i;
            }

            if (user.canPlayerEdit(i, j, k, movingobjectposition.sideHit, item)) {
                int l = MathHelper.floor_double(user.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
                return placeComponent(user, item, user.worldObj, i, j, k, type, tex, l);
            }
        }
        return 0;
    }

    public static int getStorageSize(String id) {
        if (id == null) return 0;

        if (id.equalsIgnoreCase("bar")) return 64;
        if (id.equalsIgnoreCase("plank")) return 64;
        if (id.equalsIgnoreCase("pot")) return 64;
        if (id.equalsIgnoreCase("jug")) return 32;
        if (id.equalsIgnoreCase("sheet")) return 16;
        if (id.equalsIgnoreCase("bigplate")) return 8;

        return 0;
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
        return Blocks.iron_block.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {

    }

    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return component_RI;
    }

    @Override
    protected boolean facesPlacer() {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityComponent();
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer user) {
        useBlock(world, x, y, z, user, true);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer user, int side, float xOffset,
            float yOffset, float zOffset) {
        useBlock(world, x, y, z, user, false);
        return true;
    }

    private void useBlock(World world, int x, int y, int z, EntityPlayer user, boolean leftClick) {
        ItemStack held = user.getHeldItem();
        TileEntityComponent tile = getTile(world, x, y, z);
        if (tile != null) {
            tile.interact(user, held, leftClick);
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        TileEntityComponent tile = getTile(world, x, y, z);
        if (tile != null) {
            tile.checkStack();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        TileEntityComponent tile = getTile(world, x, y, z);
        if (tile != null) {
            if (tile.item != null) {
                ItemStack item = tile.item.copy();
                item.stackSize = 1;
                return item;
            }
        }
        return null;
    }

    @Override
    public Item getItemDropped(int meta, Random rand, int fortune) {
        return null;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntityComponent tile = getTile(world, x, y, z);
        if (tile != null && tile.item != null) {
            ItemStack drop = tile.item.copy();
            drop.stackSize = tile.stackSize;
            tile.stackSize = 0;
            InventorySlots.drop(world, x, y, z, drop);
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    private AxisAlignedBB getBoundingBox(World world, int x, int y, int z) {
        float height = 1.0F;
        TileEntityComponent tile = getTile(world, x, y, z);
        if (tile != null) {
            height = tile.getBlockHeight();
        }
        return AxisAlignedBB.getBoundingBox(x + 0.0625D, y + 0D, z + 0.0625D, x + 0.9375D, y + height, z + 0.9375D);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return getBoundingBox(world, x, y, z);
    }

    /**
     * Returns the bounding box of the wired rectangular prism to render.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        return getBoundingBox(world, x, y, z);
    }
}
