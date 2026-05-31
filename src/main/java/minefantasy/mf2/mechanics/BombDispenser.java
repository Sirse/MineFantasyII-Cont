package minefantasy.mf2.mechanics;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import minefantasy.mf2.entity.EntityBomb;
import minefantasy.mf2.entity.EntityMine;
import minefantasy.mf2.item.gadget.ItemBomb;
import minefantasy.mf2.item.gadget.ItemMine;

public class BombDispenser implements IBehaviorDispenseItem {

    @Override
    public ItemStack dispense(IBlockSource dispenser, ItemStack item) {
        if (item == null || item.getItem() == null) {
            return item;
        }

        EnumFacing direction = BlockDispenser.func_149937_b(dispenser.getBlockMetadata());
        World world = dispenser.getWorld();
        IPosition position = BlockDispenser.func_149939_a(dispenser);

        if (item.getItem() instanceof ItemBomb) {
            spawnBomb(world, position, direction, item, 1.5F);
        } else if (item.getItem() instanceof ItemMine) {
            spawnMine(world, position, direction, item, 0.5F);
        }

        return item;
    }

    private void spawnBomb(World world, IPosition position, EnumFacing direction, ItemStack item,
            float velocityModifier) {
        if (world.isRemote) {
            return;
        }

        EntityBomb bomb = new EntityBomb(world).setType(
                ItemBomb.getFilling(item),
                ItemBomb.getCasing(item),
                ItemBomb.getFuse(item),
                ItemBomb.getPowder(item));
        bomb.setPosition(position.getX(), position.getY(), position.getZ());
        bomb.setThrowableHeading(
                direction.getFrontOffsetX(),
                direction.getFrontOffsetY(),
                direction.getFrontOffsetZ(),
                1.0F,
                velocityModifier);
        world.spawnEntityInWorld(bomb);
        if (item.hasTagCompound() && item.getTagCompound().hasKey("stickyBomb")) {
            bomb.getEntityData().setBoolean("stickyBomb", true);
        }
        item.splitStack(1);
    }

    private void spawnMine(World world, IPosition position, EnumFacing direction, ItemStack item,
            float velocityModifier) {
        if (world.isRemote) {
            return;
        }

        EntityMine mine = new EntityMine(world).setType(
                ItemBomb.getFilling(item),
                ItemBomb.getCasing(item),
                ItemBomb.getFuse(item),
                ItemBomb.getPowder(item));
        mine.setPosition(position.getX(), position.getY(), position.getZ());
        mine.setThrowableHeading(
                direction.getFrontOffsetX(),
                direction.getFrontOffsetY(),
                direction.getFrontOffsetZ(),
                1.0F,
                velocityModifier);
        world.spawnEntityInWorld(mine);
        if (item.hasTagCompound() && item.getTagCompound().hasKey("stickyBomb")) {
            mine.getEntityData().setBoolean("stickyBomb", true);
        }
        item.splitStack(1);
    }
}
