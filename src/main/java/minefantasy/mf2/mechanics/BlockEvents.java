package minefantasy.mf2.mechanics;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.*;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.api.helpers.*;
import minefantasy.mf2.api.helpers.Drops;
import minefantasy.mf2.api.stamina.StaminaBar;
import minefantasy.mf2.config.ConfigHardcore;
import minefantasy.mf2.config.ConfigStamina;
import minefantasy.mf2.farming.FarmingHelper;
import minefantasy.mf2.integration.CustomStone;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.weapon.ItemWeaponMF;
import minefantasy.mf2.util.XSTRandom;

/** Hoeing, harvesting and mining: failed tilling, trampled farmland, rocks and sticks by hand, tiring work. */
public class BlockEvents {

    private static final XSTRandom random = new XSTRandom();

    @SubscribeEvent
    public void useHoe(UseHoeEvent event) {
        Block block = event.world.getBlock(event.x, event.y, event.z);
        if (block != Blocks.farmland && FarmingHelper.didHoeFail(event.current, event.world, block == Blocks.grass)) {
            event.entityPlayer.swingItem();
            event.world.playAuxSFXAtEntity(
                    event.entityPlayer,
                    2001,
                    event.x,
                    event.y,
                    event.z,
                    Block.getIdFromBlock(block) + (event.world.getBlockMetadata(event.x, event.y, event.z) << 12));
            event.setCanceled(true);
        }
    }

    /**
     * Settles what mining gives once a block was really broken: called on vanilla's harvest by the mixin on
     * tryHarvestBlock when it succeeded, and by MineFantasy's own tools after their own removal.
     */
    public static void successfulPlayerBreak(World world, int x, int y, int z, Block block, int meta,
            EntityPlayer player, ItemStack held) {
        if (world == null || world.isRemote) return;
        minedBlock(world, x, y, z, new Mined(block, meta, player, held == null ? null : held.copy()));
    }

    /** A break: the block, and who broke it holding what. */
    private static final class Mined {

        final Block block;
        final int meta;
        final EntityPlayer player;
        final ItemStack held;

        Mined(Block block, int meta, EntityPlayer player, ItemStack held) {
            this.block = block;
            this.meta = meta;
            this.player = player;
            this.held = held;
        }
    }

    /** A block really broken: farmland under it may be ruined, and a player mining by hand gets rocks and tires. */
    private static void minedBlock(World world, int x, int y, int z, Mined mined) {
        EntityPlayer player = mined.player;
        if (player != null && y > 0
                && world.getBlock(x, y - 1, z) == Blocks.farmland
                && FarmingHelper.didHarvestRuinBlock(world, false)) {
            ProtectionHelper.replaceBlock(player, world, x, y - 1, z, Blocks.dirt, 0);
        }
        if (player != null && !player.capabilities.isCreativeMode && !(player instanceof FakePlayer)) {
            playerMineBlock(world, x, y, z, mined);
        }
    }

    private static void playerMineBlock(World world, int x, int y, int z, Mined mined) {
        EntityPlayer player = mined.player;
        ItemStack held = mined.held;
        Block broken = mined.block;

        if (broken != null && ConfigHardcore.HCCallowRocks) {
            if (held == null && CustomStone.isStone(broken, mined.meta)) {
                Drops.fromBlock(world, x, y, z, new ItemStack(ComponentListMF.sharp_rock, random.nextInt(3) + 1));
            }
            if (held != null && held.getItem() == ComponentListMF.sharp_rock && broken instanceof BlockLeavesBase) {
                if (random.nextInt(5) == 0) {
                    Drops.fromBlock(world, x, y, z, new ItemStack(Items.stick, random.nextInt(3) + 1));
                }
                if (random.nextInt(3) == 0) {
                    Drops.fromBlock(world, x, y, z, new ItemStack(ComponentListMF.vine, random.nextInt(3) + 1));
                }
            }
        }

        if (StaminaBar.isSystemActive && ConfigStamina.affectMining && StaminaBar.doesAffectEntity(player)) {
            float points = 2.0F * ConfigStamina.miningSpeed;
            ItemWeaponMF.applyFatigue(player, points, 20F);

            if (points > 0 && !StaminaBar.isAnyStamina(player, false)) {
                player.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, 100, 1));
            }
        }
    }
}
