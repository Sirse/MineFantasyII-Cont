package minefantasy.mf2.api.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeChecks;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeSource;
import minefantasy.mf2.api.recipe.RecipeTransaction;
import minefantasy.mf2.util.XSTRandom;

/**
 * Salvaging breaks items back into parts. Entries live in {@link MFRecipes#SALVAGE}: either a list of parts, or an
 * alias sending an item to another item's parts (a dragonforged sword salvages like the plain one).
 *
 * <p>
 * Lookup order: aliases, then {@link ISpecialSalvage} items, then entries by specificity (with materials before
 * without, exact metadata before any).
 */
public final class Salvage {

    public static final int PRIORITY_ALIAS = 100;

    private static XSTRandom random = new XSTRandom();

    private Salvage() {}

    /** One salvage entry: parts, or an alias to another item. Immutable. */
    public static final class SalvageRecipe implements RecipeChecks.Validated {

        @Override
        public void validate() {
            RecipeChecks.input("input", input);
            RecipeChecks.require(alias != null || !parts.isEmpty(), "needs parts or an item to salvage as");
            for (int i = 0; i < parts.size(); i++) {
                RecipeChecks.output("part " + (i + 1), parts.get(i));
            }
        }

        private final Input input;
        private final ItemStack displayInput;
        private final List<ItemStack> parts;
        private final Item alias;

        private SalvageRecipe(Input input, ItemStack displayInput, List<ItemStack> parts, Item alias) {
            this.input = input;
            this.displayInput = displayInput;
            this.parts = parts;
            this.alias = alias;
        }

        public static SalvageRecipe parts(ItemStack input, Object... components) {
            ItemStack normalized = normalizeInput(input);
            List<ItemStack> parts = new ArrayList<>();
            for (Object component : components) {
                ItemStack part = component instanceof ItemStack ? ((ItemStack) component).copy()
                        : component instanceof Item ? new ItemStack((Item) component)
                                : component instanceof Block ? new ItemStack((Block) component) : null;
                if (part == null || part.getItem() == null) {
                    throw new IllegalArgumentException("Not a salvage part: " + component);
                }
                parts.add(part);
            }
            return new SalvageRecipe(inputFor(normalized), normalized, Collections.unmodifiableList(parts), null);
        }

        public static SalvageRecipe alias(Item item, Item salvagesAs) {
            ItemStack display = new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE);
            return new SalvageRecipe(Input.of(item), display, Collections.emptyList(), salvagesAs);
        }

        public Input getInput() {
            return input;
        }

        /** The registered input as a stack, with its materials, for display. */
        public ItemStack getDisplayInput() {
            return displayInput.copy();
        }

        /** Parts as copies; empty for an alias. */
        public List<ItemStack> getParts() {
            List<ItemStack> copies = new ArrayList<>(parts.size());
            for (ItemStack part : parts) {
                copies.add(part.copy());
            }
            return copies;
        }

        /** The item whose parts this one gives, or null. */
        public Item getAlias() {
            return alias;
        }

        public Set<Object> indexKeys() {
            return input.indexKeys();
        }
    }

    // region registration

    /** Makes {@code item1} salvage into what {@code item2} gives. */
    public static void shareSalvage(Item item1, Item item2) {
        try (RecipeTransaction tx = MFRecipes.REGISTRIES.begin(RecipeSource.NATIVE)) {
            stageAlias(tx, item1, item2);
            tx.commit();
        }
    }

    /** Stages an alias entry, replacing the item's previous alias. */
    public static void stageAlias(RecipeTransaction tx, Item item, Item salvagesAs) {
        tx.set(MFRecipes.SALVAGE, aliasId(item), SalvageRecipe.alias(item, salvagesAs), PRIORITY_ALIAS);
    }

    public static RecipeId aliasId(Item item) {
        return NativeRecipes.idFor("salvage", item).withSuffix(".alias");
    }

    public static void addSalvage(Block input, Object... components) {
        addSalvage(Item.getItemFromBlock(input), components);
    }

    public static void addSalvage(Item input, Object... components) {
        addSalvage(new ItemStack(input, 1, OreDictionary.WILDCARD_VALUE), components);
    }

    /** Registers the parts of an item; registering the same item (and materials) again replaces them. */
    public static void addSalvage(ItemStack item, Object... components) {
        if (item == null || item.getItem() == null) {
            return;
        }
        SalvageRecipe recipe = SalvageRecipe.parts(item, components);
        try (RecipeTransaction tx = MFRecipes.REGISTRIES.begin(RecipeSource.NATIVE)) {
            tx.set(MFRecipes.SALVAGE, partsId(recipe.displayInput), recipe, priorityOf(recipe));
            tx.commit();
        }
    }

    /** The id of a parts entry: the item, metadata unless any, and its materials. */
    public static RecipeId partsId(ItemStack input) {
        ItemStack normalized = normalizeInput(input);
        RecipeId id = NativeRecipes.nativeBaseId(MFRecipes.SALVAGE, normalized);
        NBTTagCompound materials = CustomMaterial.getNBT(normalized, false);
        if (materials != null) {
            for (String slot : new String[] { CustomToolHelper.slot_main, CustomToolHelper.slot_haft }) {
                if (materials.hasKey(slot)) {
                    id = id.withSuffix("." + materials.getString(slot).toLowerCase().replaceAll("[^a-z0-9_.-]", "_"));
                }
            }
        }
        return id;
    }

    /** More specific entries first: with materials, then exact metadata. */
    public static int priorityOf(SalvageRecipe recipe) {
        int priority = 0;
        if (CustomToolHelper.hasAnyMaterial(recipe.displayInput)) {
            priority += 2;
        }
        if (recipe.displayInput.getItemDamage() != OreDictionary.WILDCARD_VALUE) {
            priority += 1;
        }
        return priority;
    }

    /** Damageable items salvage the same at any damage. */
    public static ItemStack normalizeInput(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemStack normalized = item.copy();
        normalized.stackSize = 1;
        if (normalized.isItemStackDamageable()) {
            normalized.setItemDamage(OreDictionary.WILDCARD_VALUE);
        }
        return normalized;
    }

    private static Input inputFor(ItemStack normalized) {
        Input input = Input.of(normalized.getItem(), normalized.getItemDamage());
        NBTTagCompound materials = CustomMaterial.getNBT(normalized, false);
        if (materials != null && materials.hasKey(CustomToolHelper.slot_main)) {
            input = input.material(materials.getString(CustomToolHelper.slot_main));
        }
        if (materials != null && materials.hasKey(CustomToolHelper.slot_haft)) {
            input = input.haft(Input.MaterialRule.is(materials.getString(CustomToolHelper.slot_haft)));
        }
        return input;
    }

    // endregion

    /** Parts entries (not aliases) in lookup order, for display. */
    public static List<SalvageRecipe> displayRecipes() {
        List<SalvageRecipe> recipes = new ArrayList<>();
        for (RecipeEntry<SalvageRecipe> entry : MFRecipes.SALVAGE.published().all()) {
            if (entry.getRecipe().alias == null) {
                recipes.add(entry.getRecipe());
            }
        }
        return recipes;
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
            ItemStack stack = null;
            if (entry instanceof Item) {
                stack = new ItemStack((Item) entry);
            } else if (entry instanceof Block) {
                stack = new ItemStack((Block) entry);
            } else if (entry instanceof ItemStack) {
                stack = (ItemStack) entry;
            }
            if (stack != null) {
                items = dropItemStack(mainItem, user, items, stack, chanceModifier, chance);
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

    /** The parts an item salvages into (fresh copies), or null. */
    public static Object[] getSalvage(ItemStack item) {
        return getSalvage(item, 0);
    }

    private static Object[] getSalvage(ItemStack item, int depth) {
        if (item == null || item.getItem() == null) {
            return null;
        }
        List<RecipeEntry<SalvageRecipe>> candidates = MFRecipes.SALVAGE.published().candidates(Input.lookupKeys(item));
        for (RecipeEntry<SalvageRecipe> entry : candidates) {
            SalvageRecipe recipe = entry.getRecipe();
            if (recipe.alias != null && recipe.input.matches(item) && depth < 8) {
                return getSalvage(new ItemStack(recipe.alias), depth + 1);
            }
        }
        if (item.getItem() instanceof ISpecialSalvage) {
            Object[] special = ((ISpecialSalvage) item.getItem()).getSalvage(item);
            if (special != null) {
                return special;
            }
        }
        for (RecipeEntry<SalvageRecipe> entry : candidates) {
            SalvageRecipe recipe = entry.getRecipe();
            if (recipe.alias == null && recipe.input.matches(item)) {
                return recipe.getParts().toArray();
            }
        }
        return null;
    }
}
