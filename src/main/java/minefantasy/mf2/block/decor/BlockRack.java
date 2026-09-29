package minefantasy.mf2.block.decor;

import static net.minecraftforge.common.util.ForgeDirection.*;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.block.tileentity.InventorySlots;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;
import minefantasy.mf2.item.list.CreativeTabMF;
import minefantasy.mf2.network.packet.RackCommand;

/**
 * @author Anonymous Productions
 *         <p>
 *         Sources are provided for educational reasons. though small bits of code, or methods can be used in your own
 *         creations.
 */
public class BlockRack extends BlockWoodDecor<TileEntityRack> {

    public static int rack_RI = 115;

    public BlockRack(String name) {
        super(name, TileEntityRack.class);

        setHardness(1.0F);
        setResistance(1.0F);
        GameRegistry.registerBlock(this, ItemBlockToolRack.class, name);
        setBlockName(name);
        this.setCreativeTab(CreativeTabMF.tabUtil);
    }

    /**
     * Convert standard South, West, North, East to rack's (North, South, East, West)
     */
    public static int getDirection(int dir) {
        int[] directions = new int[] { 3, 4, 2, 5 };
        return directions[Math.min(3, dir)];
    }

    public static boolean interact(int slot, World world, TileEntityRack tile, EntityPlayer player) {
        if (player.isSneaking()) {
            player.openGui(MineFantasyII.instance, 0, world, tile.xCoord, tile.yCoord, tile.zCoord);
            return false;
        }

        ItemStack held = player.getHeldItem();
        if (held == null) {
            ItemStack hung = tile.getStackInSlot(slot);
            if (hung != null) {
                if (!world.isRemote) {
                    player.setCurrentItemOrArmor(0, hung);
                    tile.setInventorySlotContents(slot, null);
                }
                player.swingItem();
                return true;
            }
        } else {
            ItemStack hung = tile.getStackInSlot(slot);

            if (hung == null && tile.canHang(player.getHeldItem(), slot)) {
                if (!world.isRemote) {
                    tile.setInventorySlotContents(slot, player.getHeldItem().copy());
                    player.setCurrentItemOrArmor(0, null);
                }
                player.swingItem();
                return true;
            } else if (held != null && hung != null) {
                if (hung.isItemEqual(held)) {
                    int space = hung.getMaxStackSize() - hung.stackSize;

                    if (space <= 0) {
                        return false;
                    }
                    if (held.stackSize > space) {
                        if (!world.isRemote) {
                            held.stackSize -= space;
                            hung.stackSize += space;
                            tile.onContentsChanged();
                        }
                        player.swingItem();
                        return true;
                    } else {
                        if (!world.isRemote) {
                            hung.stackSize += held.stackSize;
                            player.setCurrentItemOrArmor(0, null);
                            tile.onContentsChanged();
                        }
                        player.swingItem();
                        return true;
                    }
                }
            }
        }
        player.openGui(MineFantasyII.instance, 0, world, tile.xCoord, tile.yCoord, tile.zCoord);
        return false;
    }

    /**
     * Returns a bounding box from the pool of bounding boxes (this means this box can change after the pool has been
     * cleared to be reused)
     */
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World worldIn, int x, int y, int z) {
        this.setBlockBoundsBasedOnState(worldIn, x, y, z);
        return super.getCollisionBoundingBoxFromPool(worldIn, x, y, z);
    }

    /**
     * Updates the blocks bounds based on its current state. Args: world, x, y, z
     */
    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess worldIn, int x, int y, int z) {
        this.modifyBoundingbox(worldIn.getBlockMetadata(x, y, z));
    }

    /**
     * Returns the bounding box of the wired rectangular prism to render.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World worldIn, int x, int y, int z) {
        this.setBlockBoundsBasedOnState(worldIn, x, y, z);
        return super.getSelectedBoundingBoxFromPool(worldIn, x, y, z);
    }

    public void modifyBoundingbox(int meta) {
        float f = 0.25F;

        if (meta == 2) {
            this.setBlockBounds(0.0F, 0.0F, 1.0F - f, 1.0F, 1.0F, 1.0F);
        }

        if (meta == 3) {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, f);
        }

        if (meta == 4) {
            this.setBlockBounds(1.0F - f, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        }

        if (meta == 5) {
            this.setBlockBounds(0.0F, 0.0F, 0.0F, f, 1.0F, 1.0F);
        }
    }

    /**
     * Is this block (a) opaque and (b) a full 1m cube? This determines whether or not to render the shared face of two
     * adjacent blocks and also whether the player can attach torches, redstone wire, etc to this block.
     */
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    /**
     * If this block doesn't render as an ordinary block it will return False (examples: signs, buttons, stairs, etc)
     */
    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    /**
     * Called when a block is placed using its ItemBlock. Args: World, X, Y, Z, side, hitX, hitY, hitZ, block metadata
     */

    /**
     * The type of render function that is called for this block
     */
    @Override
    public int getRenderType() {
        return rack_RI;
    }

    /**
     * Checks to see if its valid to put this block at the specified coordinates. Args: world, x, y, z
     */
    @Override
    public boolean canPlaceBlockAt(World worldIn, int x, int y, int z) {
        return worldIn.isSideSolid(x - 1, y, z, EAST) || worldIn.isSideSolid(x + 1, y, z, WEST)
                || worldIn.isSideSolid(x, y, z - 1, SOUTH)
                || worldIn.isSideSolid(x, y, z + 1, NORTH);
    }

    @Override
    public int onBlockPlaced(World worldIn, int x, int y, int z, int side, float subX, float subY, float subZ,
            int meta) {
        int j1 = meta;

        if ((meta == 0 || side == 2) && worldIn.isSideSolid(x, y, z + 1, NORTH)) {
            j1 = 2;
        }

        if ((j1 == 0 || side == 3) && worldIn.isSideSolid(x, y, z - 1, SOUTH)) {
            j1 = 3;
        }

        if ((j1 == 0 || side == 4) && worldIn.isSideSolid(x + 1, y, z, WEST)) {
            j1 = 4;
        }

        if ((j1 == 0 || side == 5) && worldIn.isSideSolid(x - 1, y, z, EAST)) {
            j1 = 5;
        }

        return j1;
    }

    /**
     * Lets the block know when one of its neighbor changes. Doesn't know which neighbor changed (coordinates passed are
     * their own) Args: x, y, z, neighbor Block
     */
    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        TileEntityRack tile = getTile(world, x, y, z);
        if (tile != null) {
            tile.updateInventory();
        }
        int l = world.getBlockMetadata(x, y, z);
        boolean flag = false;

        if (l == 2 && world.isSideSolid(x, y, z + 1, NORTH)) {
            flag = true;
        }

        if (l == 3 && world.isSideSolid(x, y, z - 1, SOUTH)) {
            flag = true;
        }

        if (l == 4 && world.isSideSolid(x + 1, y, z, WEST)) {
            flag = true;
        }

        if (l == 5 && world.isSideSolid(x - 1, y, z, EAST)) {
            flag = true;
        }

        if (!flag) {
            this.dropBlockAsItem(world, x, y, z, l, 0);
            world.setBlockToAir(x, y, z);
        }

        super.onNeighborBlockChange(world, x, y, z, block);
    }

    /**
     * Returns the quantity of items to drop on block destruction.
     */
    @Override
    public int quantityDropped(Random rand) {
        return 1;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 0);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int m) {
        return new TileEntityRack();
    }

    /**
     * Called whenever the block is removed.
     */
    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntityRack tile = getTile(world, x, y, z);

        if (tile != null) {
            InventorySlots.spill(world, x, y, z, tile);
        }

        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer user, int i, float f, float f1,
            float f2) {
        TileEntityRack tile = getTile(world, x, y, z);
        if (world.isRemote && tile != null) {
            int slot = tile.getSlotFor(f, f2);
            if (slot >= 0 && slot < 4) {
                ((EntityClientPlayerMP) user).sendQueue
                        .addToSendQueue(new RackCommand(slot, user, tile).generatePacket());
            }
        }

        return true;
    }

    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {}

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister reg) {}
}
