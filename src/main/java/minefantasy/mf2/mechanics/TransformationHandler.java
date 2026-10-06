package minefantasy.mf2.mechanics;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import minefantasy.mf2.api.crafting.transformation.TransformationRecipe;
import minefantasy.mf2.api.crafting.transformation.TransformationRecipes;
import minefantasy.mf2.api.helpers.Drops;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.knowledge.ResearchLogic;

/**
 * Applies block transformation recipes when a player hits a block with the proper tool. Left click chops: the event is
 * cancelled so vanilla breaking never starts on a transformable block. Sneaking always breaks normally.
 *
 * Listens at LOWEST priority so region-protection mods/plugins (WorldGuard, Forge Essentials, GriefPrevention,
 * Cauldron-bundled Bukkit protectors) decide first: if they cancel the interaction or deny block/item use, the
 * transformation never runs.
 */
public class TransformationHandler {

    /** Ticks after a transformation during which the same block ignores the player, so a held click does not chain */
    private static final int REPEAT_DELAY = 10;

    private Random rand = new Random();
    private final Map<EntityPlayer, long[]> lastTransform = new WeakHashMap<EntityPlayer, long[]>();

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
        if (!world.blockExists(x, y, z)) return;
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        ItemStack held = player.getHeldItem();
        String toolType = ToolHelper.getCrafterTool(held);
        int toolTier = ToolHelper.getCrafterTier(held);

        TransformationRecipe recipe = TransformationRecipes.findRecipe(block, meta, toolType, toolTier);
        if (recipe == null && held != null && "nothing".equalsIgnoreCase(toolType)) {
            // Plain tools such as axes are not crafting tools, so fall back to their harvest classes
            for (String toolClass : held.getItem().getToolClasses(held)) {
                recipe = TransformationRecipes
                        .findRecipe(block, meta, toolClass, held.getItem().getHarvestLevel(held, toolClass));
                if (recipe != null) {
                    break;
                }
            }
        }
        if (recipe == null) {
            return;
        }
        final TransformationRecipe matchedRecipe = recipe;
        event.setCanceled(true);

        long[] last = lastTransform.get(player);
        long now = world.getTotalWorldTime();
        if (last != null && last[0] == x && last[1] == y && last[2] == z && now - last[3] < REPEAT_DELAY) {
            return;
        }
        if (matchedRecipe.research != null && !matchedRecipe.research.isEmpty()
                && !ResearchLogic.hasInfoUnlocked(player, matchedRecipe.research)) {
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "step.stone", 1.0F, 0.5F);
            return;
        }
        if (matchedRecipe.consumable != null && !player.capabilities.isCreativeMode
                && consumableSlot(player, matchedRecipe.consumable) < 0) {
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "note.hat", 1.0F, 0.5F);
            return;
        }
        boolean finalHit = matchedRecipe.hits <= 1 || meta - matchedRecipe.inputMeta + 1 >= matchedRecipe.hits;
        int resultMeta = finalHit ? matchedRecipe.getOutputMeta(meta) : meta + 1;
        Block resultBlock = finalHit ? matchedRecipe.output : block;
        boolean applied = ProtectionHelper
                .replaceBlock(player, world, x, y, z, resultBlock, resultMeta, new ProtectionHelper.CommitCheck() {

                    @Override
                    public boolean beforeCommit() {
                        // The tool and the payment are looked at again: protection handlers may have changed them
                        if (player.getHeldItem() != held) {
                            return false;
                        }
                        return matchedRecipe.consumable == null || player.capabilities.isCreativeMode
                                || consume(player, matchedRecipe.consumable);
                    }
                });
        if (!applied) return;

        world.playSoundEffect(
                x + 0.5D,
                y + 0.5D,
                z + 0.5D,
                matchedRecipe.sound != null ? matchedRecipe.sound : "dig.wood",
                1.0F,
                1.0F);

        if (matchedRecipe.dropPerHit != null) {
            dropStack(world, x, y, z, matchedRecipe.dropPerHit.copy());
        }

        lastTransform.put(player, new long[] { x, y, z, now });

        if (finalHit) {
            for (int count = 1; count < matchedRecipe.outputCount; count++) {
                dropStack(world, x, y, z, new ItemStack(resultBlock, 1, resultMeta));
            }
            if (matchedRecipe.skill != null && matchedRecipe.skillXp > 0) {
                matchedRecipe.skill.addXP(player, matchedRecipe.skillXp);
            }
        }

        if (held != null && !player.capabilities.isCreativeMode) {
            held.damageItem(1, player);
            if (held.getItemDamage() >= held.getMaxDamage()) {
                player.destroyCurrentEquippedItem();
            }
        }
    }

    /** The inventory slot holding enough of the consumable, or -1. */
    private static int consumableSlot(EntityPlayer player, ItemStack required) {
        ItemStack[] inventory = player.inventory.mainInventory;
        for (int slot = 0; slot < inventory.length; slot++) {
            ItemStack stack = inventory[slot];
            if (stack != null && required.isItemEqual(stack) && stack.stackSize >= required.stackSize) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean consume(EntityPlayer player, ItemStack required) {
        int slot = consumableSlot(player, required);
        if (slot < 0) {
            return false;
        }
        ItemStack[] inventory = player.inventory.mainInventory;
        inventory[slot].stackSize -= required.stackSize;
        if (inventory[slot].stackSize <= 0) {
            inventory[slot] = null;
        }
        player.inventory.markDirty();
        return true;
    }

    private void dropStack(World world, int x, int y, int z, ItemStack stack) {
        Drops.spawn(
                world,
                x + 0.5D,
                y + 0.5D,
                z + 0.5D,
                stack,
                0,
                (rand.nextDouble() - 0.5D) * 0.1D,
                0.2D,
                (rand.nextDouble() - 0.5D) * 0.1D);
    }
}
