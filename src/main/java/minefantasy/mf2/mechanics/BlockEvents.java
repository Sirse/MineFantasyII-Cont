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
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;

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

    @SubscribeEvent
    public void breakBlock(BreakEvent event) {
        Block base = event.world.getBlock(event.x, event.y - 1, event.z);

        if (base != null && base == Blocks.farmland && FarmingHelper.didHarvestRuinBlock(event.world, false)) {
            event.world.setBlock(event.x, event.y - 1, event.z, Blocks.dirt);
        }

        EntityPlayer player = event.getPlayer();
        if (player != null && !player.capabilities.isCreativeMode && !(player instanceof FakePlayer)) {
            playerMineBlock(event);
        }
    }

    public void playerMineBlock(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        ItemStack held = player.getHeldItem();
        Block broken = event.block;

        if (broken != null && ConfigHardcore.HCCallowRocks) {
            if (held == null && CustomStone.isStone(broken, event.blockMetadata)) {
                Drops.fromBlock(
                        event.world,
                        event.x,
                        event.y,
                        event.z,
                        new ItemStack(ComponentListMF.sharp_rock, random.nextInt(3) + 1));
            }
            if (held != null && held.getItem() == ComponentListMF.sharp_rock && broken instanceof BlockLeavesBase) {
                if (random.nextInt(5) == 0) {
                    Drops.fromBlock(
                            event.world,
                            event.x,
                            event.y,
                            event.z,
                            new ItemStack(Items.stick, random.nextInt(3) + 1));
                }
                if (random.nextInt(3) == 0) {
                    Drops.fromBlock(
                            event.world,
                            event.x,
                            event.y,
                            event.z,
                            new ItemStack(ComponentListMF.vine, random.nextInt(3) + 1));
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
