package minefantasy.mf2.api.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.util.MFLogUtil;
import minefantasy.mf2.util.XSTRandom;

public class Salvage {

    public static HashMap<String, Object[]> salvageList = new HashMap<String, Object[]>();
    public static List<SalvageRecipe> displayList = new ArrayList<SalvageRecipe>();
    public static HashMap<String, Item> sharedSalvage = new HashMap<String, Item>();
    private static XSTRandom random = new XSTRandom();

    public static void shareSalvage(Item item1, Item item2) {
        sharedSalvage.put(CustomToolHelper.getSimpleReferenceName(item1), item2);
    }

    public static void addSalvage(Block input, Object... components) {
        addSalvage(Item.getItemFromBlock(input), components);
    }

    public static void addSalvage(Item input, Object... components) {
        addSalvage(new ItemStack(input, 1, OreDictionary.WILDCARD_VALUE), components);
    }

    public static void addSalvage(ItemStack item, Object... components) {
        if (item == null || item.getItem() == null) {
            return;
        }
        ItemStack normalized = normalizeInput(item);
        salvageList.put(CustomToolHelper.getReferenceName(normalized), components);
        addDisplayRecipe(normalized, components);
    }

    public static void removeSalvage(ItemStack item) {
        if (item == null || item.getItem() == null) {
            return;
        }
        ItemStack normalized = normalizeInput(item);
        salvageList.remove(CustomToolHelper.getReferenceName(normalized));
        salvageList.remove(CustomToolHelper.getSimpleReferenceName(normalized.getItem()));
        removeDisplayRecipe(normalized);
    }

    public static ItemStack normalizeInput(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemStack normalized = item.copy();
        if (normalized.isItemStackDamageable()) {
            normalized.setItemDamage(OreDictionary.WILDCARD_VALUE);
        }
        return normalized;
    }

    public static Object[] getRegisteredSalvage(ItemStack item) {
        if (item == null || item.getItem() == null) {
            return null;
        }
        return salvageList.get(CustomToolHelper.getReferenceName(normalizeInput(item)));
    }

    private static void addDisplayRecipe(ItemStack input, Object[] components) {
        if (input == null || input.getItem() == null || components == null) {
            return;
        }
        removeDisplayRecipe(input);
        displayList.add(new SalvageRecipe(input.copy(), components));
    }

    private static void removeDisplayRecipe(ItemStack input) {
        for (int i = displayList.size() - 1; i >= 0; i--) {
            SalvageRecipe recipe = displayList.get(i);
            if (recipe != null && isSameDisplayInput(input, recipe.input)) {
                displayList.remove(i);
            }
        }
    }

    /**
     * Break an item to its parts
     *
     * @return a list of items
     */
    public static List<ItemStack> salvage(EntityPlayer user, ItemStack item) {
        return salvage(user, item, 1.0F);
    }

    public static List<ItemStack> salvage(EntityPlayer user, ItemStack item, float dropRate) {
        Object[] entryList = getSalvage(item);
        if (entryList == null) {
            return null;
        }

        float durability = 1F;
        if (item.isItemDamaged()) {
            durability = (float) (item.getMaxDamage() - item.getItemDamage()) / (float) item.getMaxDamage();
        }

        float chanceModifier = 1.25F;// 80% Succcess rate
        float chance = dropRate * durability;// Modifier for skill and durability

        return dropItems(item, user, entryList, chanceModifier, chance);
    }

    private static List<ItemStack> dropItems(ItemStack mainItem, EntityPlayer user, Object[] entryList,
            float chanceModifier, float chance) {
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (Object entry : entryList) {
            if (entry != null) {
                if (entry instanceof Item && random.nextFloat() * chanceModifier < chance) {
                    items = dropItemStack(mainItem, user, items, new ItemStack((Item) entry), chanceModifier, chance);
                }
                if (entry instanceof Block && random.nextFloat() * chanceModifier < chance) {
                    items = dropItemStack(mainItem, user, items, new ItemStack((Block) entry), chanceModifier, chance);
                }
                if (entry instanceof ItemStack) {
                    items = dropItemStack(mainItem, user, items, (ItemStack) entry, chanceModifier, chance);
                }
            }
        }
        return items;
    }

    private static List<ItemStack> dropItemStack(ItemStack mainItem, EntityPlayer user, List<ItemStack> items,
            ItemStack entry, float chanceModifier, float chance) {
        for (int a = 0; a < entry.stackSize; a++) {
            if (random.nextFloat() * chanceModifier < chance) {
                boolean canSalvage = true;

                if (entry.getItem() instanceof ISalvageDrop) {
                    canSalvage = ((ISalvageDrop) entry.getItem()).canSalvage(user, entry);
                }
                if (canSalvage) {
                    ItemStack newitem = entry.copy();
                    newitem.stackSize = 1;
                    newitem = CustomToolHelper.tryDeconstruct(newitem, mainItem);
                    items.add(newitem);
                }
            }
        }
        return items;
    }

    public static Object[] getSalvage(ItemStack item) {
        // SHARED
        Item shared = sharedSalvage.get(CustomToolHelper.getReferenceName(item, "any", false));
        if (shared != null) {
            MFLogUtil.logDebug("Found shared: " + shared.getUnlocalizedName());
            return getSalvage(new ItemStack(shared));
        }

        if (item != null && item.getItem() instanceof ISpecialSalvage) {
            Object[] special = ((ISpecialSalvage) item.getItem()).getSalvage(item);
            if (special != null) {
                return special;
            }
        }
        Object[] specific = salvageList.get(CustomToolHelper.getReferenceName(item));
        if (specific != null) {
            return specific;
        }
        Object[] specific2 = salvageList.get(CustomToolHelper.getReferenceName(item, "any"));
        if (specific2 != null) {
            return specific2;
        }

        return salvageList.get(CustomToolHelper.getReferenceName(item, "any", false));
    }

    private static boolean doesMatch(ItemStack item1, ItemStack item2) {
        if (!CustomToolHelper.doesMatchForRecipe(item1, item2)) {
            return false;
        }
        return item2.getItem() == item1.getItem() && (item2.getItemDamage() == OreDictionary.WILDCARD_VALUE
                || item2.getItemDamage() == item1.getItemDamage());
    }

    private static boolean isSameDisplayInput(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null || item1.getItem() != item2.getItem()) {
            return false;
        }
        return item1.getItemDamage() == item2.getItemDamage() && CustomToolHelper.doesMatchForRecipe(item1, item2)
                && CustomToolHelper.doesMatchForRecipe(item2, item1);
    }

    public static class SalvageRecipe {

        public final ItemStack input;
        public final Object[] outputs;

        private SalvageRecipe(ItemStack input, Object[] outputs) {
            this.input = input;
            this.outputs = outputs;
        }
    }
}
