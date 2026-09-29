package minefantasy.mf2.item.tool.advanced;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.api.weapon.IRackItem;
import minefantasy.mf2.block.tileentity.decor.TileEntityRack;
import minefantasy.mf2.item.tool.ItemAxeMF;
import minefantasy.mf2.mechanics.HeavyHarvest;

public class ItemLumberAxe extends ItemAxeMF implements IRackItem {

    private Random rand = new Random();

    public ItemLumberAxe(String name, ToolMaterial material, int rarity) {
        super(name, material, rarity);
        this.setMaxDamage(getMaxDamage() * 5);
    }

    public static boolean canAcceptCost(EntityLivingBase user) {
        return canAcceptCost(user, 0.1F);
    }

    public static boolean canAcceptCost(EntityLivingBase user, float cost) {
        if (user instanceof EntityPlayer && StaminaBar.isSystemActive) {
            return StaminaBar.isPercentStamAvailable(user, cost, true);
        }
        return true;
    }

    public static void tirePlayer(EntityLivingBase user, float points) {
        if (user instanceof EntityPlayer && StaminaBar.isSystemActive) {
            StaminaBar.modifyStaminaValue(user, -StaminaBar.getBaseDecayModifier(user, true, true) * points);
            StaminaBar.ModifyIdleTime(user, 5F * points);
        }
    }

    @Override
    public float getScale(ItemStack itemstack) {
        return 2.0F;
    }

    @Override
    public float getOffsetX(ItemStack itemstack) {
        return 0;
    }

    @Override
    public float getOffsetY(ItemStack itemstack) {
        return 0;
    }

    @Override
    public float getOffsetZ(ItemStack itemstack) {
        return 0;
    }

    @Override
    public float getRotationOffset(ItemStack itemstack) {
        return 0;
    }

    @Override
    public boolean canHang(TileEntityRack rack, ItemStack item, int slot) {
        return true;
    }

    @Override
    public boolean isSpecialRender(ItemStack item) {
        return true;
    }

    /** Fells the tree the log belongs to, up to 32 more logs, clearing the leaves close around each. */
    @Override
    public boolean onBlockStartBreak(ItemStack item, int x, int y, int z, EntityPlayer player) {
        World world = player.worldObj;
        if (HeavyHarvest.breaksMore(player) && world.getBlock(x, y, z).getMaterial() == Material.wood
                && canAcceptCost(player)) {
            float hit = HeavyHarvest.strength(player, world, x, y, z);
            clearLeaves(item, player, world, x, y, z);
            for (int[] log : HeavyHarvest.tree(world, x, y, z, 32)) {
                if (HeavyHarvest.breakExtra(item, player, world, log[0], log[1], log[2], hit)) {
                    clearLeaves(item, player, world, log[0], log[1], log[2]);
                    tirePlayer(player, 0.5F);
                }
            }
        }
        return super.onBlockStartBreak(item, x, y, z, player);
    }

    private void clearLeaves(ItemStack item, EntityPlayer player, World world, int x, int y, int z) {
        for (int x1 = -2; x1 <= 2; x1++) {
            for (int y1 = -2; y1 <= 2; y1++) {
                for (int z1 = -2; z1 <= 2; z1++) {
                    if (world.getBlock(x + x1, y + y1, z + z1).getMaterial() == Material.leaves) {
                        HeavyHarvest.breakExtra(item, player, world, x + x1, y + y1, z + z1, 0F);
                    }
                }
            }
        }
    }

}
