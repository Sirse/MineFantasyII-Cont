package minefantasy.mf2.container;

import minefantasy.mf2.api.archery.AmmoMechanicsMF;
import minefantasy.mf2.api.archery.IAmmo;
import minefantasy.mf2.api.archery.IFirearm;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerReload extends ContainerMF {
    private final InventoryBasic weaponInv;
    private final ItemStack weapon;

    public ContainerReload(InventoryPlayer playerInventory, ItemStack weapon) {
        this.weapon = weapon;
        this.weaponInv = new InventoryBasic("reload", false, 1);
        weaponInv.setInventorySlotContents(0, AmmoMechanicsMF.getAmmo(weapon));

        addSlotToContainer(new SlotReload(this, weaponInv, 0, 79, 11));

        addPlayerMainInventory(playerInventory, 0, 66);
        addPlayerHotbarExcept(playerInventory, 0, 124, playerInventory.currentItem);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        ItemStack held = player.getCurrentEquippedItem();
        return held == weapon || (held != null && weapon != null && held.getItem() == weapon.getItem() && ItemStack.areItemStackTagsEqual(held, weapon));
    }

    @Override
    public ItemStack slotClick(int slotId, int mouseButton, int modifier, EntityPlayer player) {
        ItemStack result = super.slotClick(slotId, mouseButton, modifier, player);
        if (weapon != null) {
            ItemStack ammo = weaponInv.getStackInSlot(0);
            AmmoMechanicsMF.setAmmo(weapon, ammo);
        }
        return result;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) {
            return null; // Invalid index
        }

        ItemStack itemstack = null;
        Slot slot = (Slot) inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            if (index == 0) {
                if (!mergeItemStack(stackInSlot, 1, inventorySlots.size(), true)) return null;
            } else {
                if (canAccept(stackInSlot)) {
                    if (!mergeItemStack(stackInSlot, 0, 1, false)) return null;
                } else if (index < 28) {
                    if (!mergeItemStack(stackInSlot, 28, inventorySlots.size(), false)) return null;
                } else {
                    if (!mergeItemStack(stackInSlot, 1, 28, false)) return null;
                }
            }

            if (stackInSlot.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();

            if (stackInSlot.stackSize == itemstack.stackSize) return null;

            slot.onPickupFromSlot(player, stackInSlot);
        }
        return itemstack;
    }

    public boolean canAccept(ItemStack ammo) {
        if (ammo != null && ammo.getItem() instanceof IAmmo) {
            String ammoType = ((IAmmo) ammo.getItem()).getAmmoType(ammo);
            if (weapon != null && weapon.getItem() instanceof IFirearm) {
                return ((IFirearm) weapon.getItem()).canAcceptAmmo(weapon, ammoType);
            }
            return ammoType.equalsIgnoreCase("arrow");
        }
        return false;
    }

    private static class SlotReload extends Slot {
        private final ContainerReload container;

        SlotReload(ContainerReload container, IInventory inventory, int id, int x, int y) {
            super(inventory, id, x, y);
            this.container = container;
        }

        @Override
        public boolean isItemValid(ItemStack item) {
            return container.canAccept(item);
        }
    }
}
