package minefantasy.mf2.block.decor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.helpers.Heading;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.item.list.CreativeTabMF;

public class ItemBedMF extends Item {

    public ItemBedMF(String name) {// ItemBed
        this.setUnlocalizedName(name);
        setMaxStackSize(4);
        setTextureName("minefantasy2:Other/" + name);
        GameRegistry.registerItem(this, name, MineFantasyII.MODID);
        this.setCreativeTab(CreativeTabMF.tabGadget);
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        } else if (side != 1) {
            return false;
        } else {
            ++y;
            BlockBedMF blockbed = (BlockBedMF) BlockListMF.bedroll;
            int i1 = Heading.of(player);
            byte b0 = 0;
            byte b1 = 0;

            if (i1 == 0) {
                b1 = 1;
            }

            if (i1 == 1) {
                b0 = -1;
            }

            if (i1 == 2) {
                b1 = -1;
            }

            if (i1 == 3) {
                b0 = 1;
            }

            if (player.canPlayerEdit(x, y, z, side, stack) && player.canPlayerEdit(x + b0, y, z + b1, side, stack)) {
                if (world.isAirBlock(x, y, z) && world.isAirBlock(x + b0, y, z + b1)
                        && World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)
                        && World.doesBlockHaveSolidTopSurface(world, x + b0, y - 1, z + b1)) {
                    world.setBlock(x, y, z, blockbed, i1, 3);

                    if (world.getBlock(x, y, z) == blockbed) {
                        world.setBlock(x + b0, y, z + b1, blockbed, i1 + 8, 3);
                    }

                    --stack.stackSize;
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
}
