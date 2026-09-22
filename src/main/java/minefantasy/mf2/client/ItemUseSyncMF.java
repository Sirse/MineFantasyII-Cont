package minefantasy.mf2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.item.gadget.ItemCrossbow;

/**
 * Keeps a crossbow in use when only its NBT changed.
 * <p>
 * Starting a use writes the action onto the held crossbow, so the server resends the slot and the client ends up
 * holding a different ItemStack object. EntityPlayer.onUpdate compares itemInUse by reference and drops the use, the
 * held button then starts it again, and the aim visibly stutters.
 * <p>
 * ItemRenderer.updateEquippedItem already treats a replacement of the same item and damage as the same item rather than
 * a swap. This applies that reading to the use as well: it runs from onPlayerPreTick, the first thing onUpdate does,
 * and re-points itemInUse at the replacement with the remaining count intact, so the check that follows passes.
 * <p>
 * Only within one hotbar slot, though. Two crossbows share an Item and can share a damage value, so without that the
 * handler would hand the remaining time to whatever the player selected next. The server runs no such handler and would
 * cancel on the slot change, leaving the client aiming a crossbow the server is not, and the held button cannot recover
 * because Minecraft only starts a use while isUsingItem is false.
 */
@SideOnly(Side.CLIENT)
public class ItemUseSyncMF {

    private int lastSlot = -1;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        EntityPlayer player = event.player;
        if (player == null || player != Minecraft.getMinecraft().thePlayer) {
            return;
        }

        int slot = player.inventory.currentItem;
        int heldSince = lastSlot;
        lastSlot = slot;

        ItemStack using = player.getItemInUse();
        if (using == null || !(using.getItem() instanceof ItemCrossbow)) {
            return;
        }
        if (slot != heldSince) {
            // Selecting another slot has to cancel the use the ordinary way, on both sides.
            return;
        }
        ItemStack held = player.inventory.getCurrentItem();
        if (held == null || held == using) {
            return;
        }
        if (held.getItem() != using.getItem() || held.getItemDamage() != using.getItemDamage()) {
            return;
        }
        int left = player.getItemInUseCount();
        if (left > 0) {
            player.setItemInUse(held, left);
        }
    }
}
