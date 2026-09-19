package minefantasy.mf2.mechanics;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.crafting.transformation.TransformationRecipe;
import minefantasy.mf2.api.crafting.transformation.TransformationRecipes;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.util.BukkitUtils;

/**
 * Applies block transformation recipes when a player hits a block with the proper tool. Left click chops: the event is
 * cancelled so vanilla breaking never starts on a transformable block. Sneaking always breaks normally.
 *
 * Listens at LOWEST priority so region-protection mods/plugins (WorldGuard, Forge Essentials, GriefPrevention,
 * Cauldron-bundled Bukkit protectors) decide first: if they cancel the interaction or deny block/item use, the
 * transformation never runs.
 */
public class TransformationHandler {

    private Random rand = new Random();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            return;
        }
        World world = event.world;
        if (world.isRemote || event.entityPlayer == null) {
            return;
        }
        // Respect protections: cancelled event or explicit DENY from a higher-priority handler
        if (event.isCanceled() || event.useBlock == Event.Result.DENY || event.useItem == Event.Result.DENY) {
            return;
        }
        EntityPlayer player = event.entityPlayer;
        if (player.isSneaking()) {
            return;
        }

        int x = event.x;
        int y = event.y;
        int z = event.z;
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        ItemStack held = player.getHeldItem();
        String toolType = ToolHelper.getCrafterTool(held);
        int toolTier = ToolHelper.getCrafterTier(held);

        TransformationRecipe recipe = TransformationRecipes.findRecipe(block, meta, toolType, toolTier);
        if (recipe == null) {
            return;
        }
        // Bukkit-side protection plugins decide through their own event pipeline
        if (MineFantasyII.isBukkitServer() && BukkitUtils.cantBreakBlock(player, x, y, z)) {
            return;
        }

        event.setCanceled(true);

        if (recipe.research != null && !recipe.research.isEmpty()
                && !ResearchLogic.hasInfoUnlocked(player, recipe.research)) {
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "step.stone", 1.0F, 0.5F);
            return;
        }

        if (recipe.consumable != null && !player.capabilities.isCreativeMode) {
            if (!consumeFromInventory(player, recipe.consumable)) {
                world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "note.hat", 1.0F, 0.5F);
                return;
            }
        }

        world.playSoundEffect(
                x + 0.5D,
                y + 0.5D,
                z + 0.5D,
                recipe.sound != null ? recipe.sound : "dig.wood",
                1.0F,
                1.0F);

        if (recipe.dropPerHit != null) {
            dropStack(world, x, y, z, recipe.dropPerHit.copy());
        }

        boolean finalHit;
        if (recipe.hits <= 1) {
            finalHit = true;
        } else {
            // Metadata holds the hit counter: inputMeta is stage 0, each non-final hit increments it
            int stage = meta - recipe.inputMeta + 1;
            finalHit = stage >= recipe.hits;
            if (!finalHit) {
                world.setBlockMetadataWithNotify(x, y, z, meta + 1, 3);
            }
        }

        if (finalHit) {
            int outMeta = recipe.getOutputMeta(meta);
            world.setBlock(x, y, z, recipe.output, outMeta, 3);
            for (int count = 1; count < recipe.outputCount; count++) {
                dropStack(world, x, y, z, new ItemStack(recipe.output, 1, outMeta));
            }
            if (recipe.skill != null && recipe.skillXp > 0) {
                recipe.skill.addXP(player, recipe.skillXp);
            }
        }

        if (held != null && !player.capabilities.isCreativeMode) {
            held.damageItem(1, player);
            if (held.getItemDamage() >= held.getMaxDamage()) {
                player.destroyCurrentEquippedItem();
            }
        }
    }

    private boolean consumeFromInventory(EntityPlayer player, ItemStack required) {
        ItemStack[] inventory = player.inventory.mainInventory;
        for (int slot = 0; slot < inventory.length; slot++) {
            ItemStack stack = inventory[slot];
            if (stack != null && required.isItemEqual(stack) && stack.stackSize >= required.stackSize) {
                stack.stackSize -= required.stackSize;
                if (stack.stackSize <= 0) {
                    inventory[slot] = null;
                }
                player.inventory.markDirty();
                return true;
            }
        }
        return false;
    }

    private void dropStack(World world, int x, int y, int z, ItemStack stack) {
        EntityItem entityitem = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, stack);
        entityitem.motionX = (rand.nextDouble() - 0.5D) * 0.1D;
        entityitem.motionY = 0.2D;
        entityitem.motionZ = (rand.nextDouble() - 0.5D) * 0.1D;
        world.spawnEntityInWorld(entityitem);
    }
}
