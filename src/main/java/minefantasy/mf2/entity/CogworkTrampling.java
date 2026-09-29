package minefantasy.mf2.entity;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;

import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.config.ConfigArmour;
import minefantasy.mf2.util.BukkitUtils;

/**
 * What a moving cogwork suit does to the ground under it: kicks up its dust, and, where griefing is allowed, tramples
 * grass and farmland to dirt, breaks glass, ice and leaves, and flattens plants and snow.
 */
final class CogworkTrampling {

    private CogworkTrampling() {}

    /** Called each tick the suit is ridden; now and then, while it moves, it treads on the block under it. */
    static void tread(EntityCogwork cog) {
        if (cog.motionX * cog.motionX + cog.motionZ * cog.motionZ > 2.500000277905201E-7D
                && cog.getRNG().nextInt(5) == 0) {
            int i = MathHelper.floor_double(cog.posX);
            int j = MathHelper.floor_double(cog.posY - 0.20000000298023224D - cog.yOffset);
            int k = MathHelper.floor_double(cog.posZ);
            Block block = cog.worldObj.getBlock(i, j, k);

            if (block.getMaterial() != Material.air) {
                cog.worldObj.spawnParticle(
                        "blockcrack_" + Block.getIdFromBlock(block) + "_" + cog.worldObj.getBlockMetadata(i, j, k),
                        cog.posX + (cog.getRNG().nextFloat() - 0.5D) * cog.width,
                        cog.boundingBox.minY + 0.1D,
                        cog.posZ + (cog.getRNG().nextFloat() - 0.5D) * cog.width,
                        4.0D * (cog.getRNG().nextFloat() - 0.5D),
                        0.5D,
                        (cog.getRNG().nextFloat() - 0.5D) * 4.0D);
            }
            if (!cog.worldObj.isRemote && ConfigArmour.cogworkGrief
                    && cog.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing")
                    && !isProtectedBlock(cog, i, j, k)) {
                damageBlock(cog, block, i, j, k, cog.worldObj.getBlockMetadata(i, j, k));
                block = cog.worldObj.getBlock(i, j + 1, k);
                damageSurface(cog, block, i, j + 1, k, cog.worldObj.getBlockMetadata(i, j, k));
            }
        }
    }

    /**
     * Bukkit protection plugins may forbid the rider from breaking blocks under the suit
     */
    private static boolean isProtectedBlock(EntityCogwork cog, int x, int y, int z) {
        if (!MineFantasyII.isBukkitServer() || !(cog.riddenByEntity instanceof EntityPlayer)) {
            return false;
        }
        return BukkitUtils.cantBreakBlock((EntityPlayer) cog.riddenByEntity, x, y, z);
    }

    private static void damageBlock(EntityCogwork cog, Block block, int x, int y, int z, int blockMetadata) {
        if (block == Blocks.grass || block == Blocks.farmland) {
            cog.worldObj.setBlock(x, y, z, Blocks.dirt, 0, 2);
        }
        if (block.getMaterial() == Material.glass) {
            cog.worldObj.setBlockToAir(x, y, z);
            cog.worldObj.playSoundEffect(
                    x + 0.5D,
                    y + 0.5D,
                    z + 0.5D,
                    "dig.glass",
                    1.0F,
                    0.9F + (cog.getRNG().nextFloat() * 0.2F));
        }
        if (block == Blocks.ice) {
            cog.worldObj.setBlock(x, y, z, Blocks.water, 0, 2);
            cog.worldObj.playSoundEffect(
                    x + 0.5D,
                    y + 0.5D,
                    z + 0.5D,
                    "dig.glass",
                    1.0F,
                    0.9F + (cog.getRNG().nextFloat() * 0.2F));
        }
        if (block.getMaterial() == Material.leaves) {
            cog.worldObj.setBlockToAir(x, y, z);
            cog.worldObj.playSoundEffect(
                    x + 0.5D,
                    y + 0.5D,
                    z + 0.5D,
                    "dig.grass",
                    1.0F,
                    0.9F + (cog.getRNG().nextFloat() * 0.2F));
        }
    }

    private static void damageSurface(EntityCogwork cog, Block block, int x, int y, int z, int blockMetadata) {
        if (block.getBlockHardness(cog.worldObj, x, y, z) == 0
                && (block.getMaterial() == Material.vine || block.getMaterial() == Material.plants)) {
            cog.worldObj.setBlockToAir(x, y, z);
            cog.worldObj.playSoundEffect(
                    x + 0.5D,
                    y + 0.5D,
                    z + 0.5D,
                    "dig.grass",
                    1.0F,
                    0.9F + (cog.getRNG().nextFloat() * 0.2F));
        }
        if (block == Blocks.snow_layer) {
            cog.worldObj.setBlockToAir(x, y, z);
            cog.worldObj.playSoundEffect(
                    x + 0.5D,
                    y + 0.5D,
                    z + 0.5D,
                    "dig.cloth",
                    1.0F,
                    0.9F + (cog.getRNG().nextFloat() * 0.2F));
        }
    }
}
