package minefantasy.mf2.entity;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;

import minefantasy.mf2.config.ConfigArmour;
import minefantasy.mf2.mechanics.ProtectionHelper;

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
            if (!cog.worldObj.blockExists(i, j, k)) return;
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
            // Only a rider's tread changes blocks, each asked about on its own just before it changes
            if (!cog.worldObj.isRemote && ConfigArmour.cogworkGrief
                    && cog.worldObj.getGameRules().getGameRuleBooleanValue("mobGriefing")
                    && cog.riddenByEntity instanceof EntityPlayer) {
                EntityPlayer rider = (EntityPlayer) cog.riddenByEntity;
                damageBlock(cog, rider, block, i, j, k);
                if (cog.worldObj.blockExists(i, j + 1, k)) {
                    damageSurface(cog, rider, cog.worldObj.getBlock(i, j + 1, k), i, j + 1, k);
                }
            }
        }
    }

    private static void damageBlock(EntityCogwork cog, EntityPlayer rider, Block block, int x, int y, int z) {
        if (block == Blocks.grass || block == Blocks.farmland) {
            ProtectionHelper.replaceBlock(rider, cog.worldObj, x, y, z, Blocks.dirt, 0);
        }
        if (block.getMaterial() == Material.glass) {
            if (ProtectionHelper.breakBlock(rider, cog.worldObj, x, y, z)) {
                crunch(cog, x, y, z, "dig.glass");
            }
        }
        if (block == Blocks.ice) {
            if (ProtectionHelper.replaceBlock(rider, cog.worldObj, x, y, z, Blocks.water, 0)) {
                crunch(cog, x, y, z, "dig.glass");
            }
        }
        if (block.getMaterial() == Material.leaves) {
            if (ProtectionHelper.breakBlock(rider, cog.worldObj, x, y, z)) {
                crunch(cog, x, y, z, "dig.grass");
            }
        }
    }

    private static void damageSurface(EntityCogwork cog, EntityPlayer rider, Block block, int x, int y, int z) {
        if (block.getBlockHardness(cog.worldObj, x, y, z) == 0
                && (block.getMaterial() == Material.vine || block.getMaterial() == Material.plants)) {
            if (ProtectionHelper.breakBlock(rider, cog.worldObj, x, y, z)) crunch(cog, x, y, z, "dig.grass");
        }
        if (block == Blocks.snow_layer) {
            if (ProtectionHelper.breakBlock(rider, cog.worldObj, x, y, z)) crunch(cog, x, y, z, "dig.cloth");
        }
    }

    private static void crunch(EntityCogwork cog, int x, int y, int z, String sound) {
        cog.worldObj.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, sound, 1.0F, 0.9F + cog.getRNG().nextFloat() * 0.2F);
    }
}
