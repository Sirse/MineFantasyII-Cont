package minefantasy.mf2.mechanics;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Wear that fire and acid put on worn and held items. Only an item with durability wears, through its own damage
 * handling, so Unbreaking and other mods' durability rules apply; one without durability or unbreakable is left as it
 * is, and a stack loses only the item that breaks.
 */
public final class ElementalWear {

    /** Whether fire wears armour. */
    public static boolean fire = true;
    /** Whether acid wears armour and the held item. */
    public static boolean acid = true;
    /** Registry names of items neither ever wears. */
    public static Set<String> immune = Collections.emptySet();

    private ElementalWear() {}

    public static void setImmune(String[] names) {
        Set<String> set = new HashSet<>();
        for (String name : names) {
            if (name != null && !name.trim().isEmpty()) set.add(name.trim());
        }
        immune = set;
    }

    /** Wears what is in an equipment slot by this much; whatever breaks leaves the slot. */
    public static void wear(EntityLivingBase user, int slot, int amount) {
        if (user.worldObj.isRemote || amount <= 0) return;
        ItemStack stack = user.getEquipmentInSlot(slot);
        if (stack == null || !stack.isItemStackDamageable() || isImmune(stack)) return;
        stack.damageItem(amount, user);
        if (stack.stackSize <= 0) user.setCurrentItemOrArmor(slot, null);
    }

    private static boolean isImmune(ItemStack stack) {
        if (immune.isEmpty()) return false;
        String name = Item.itemRegistry.getNameForObject(stack.getItem());
        return name != null && immune.contains(name);
    }
}
