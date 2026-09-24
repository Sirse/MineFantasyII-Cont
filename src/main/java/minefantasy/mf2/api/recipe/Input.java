package minefantasy.mf2.api.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.heating.IHotItem;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;

/**
 * An immutable recipe ingredient: what fits (item, ore name, hot wrapper, alternatives), the MineFantasy constraints on
 * it (materials, damage, script condition), how many are needed and what crafting does to it.
 *
 * <p>
 * NBT is not compared unless a constraint asks for it, so renaming, enchanting or a foreign tag never breaks a recipe.
 * {@link #matches} ignores the stack size; {@link #hasEnough} checks it.
 */
public final class Input {

    /** Constraint on one material slot of a MineFantasy item. */
    public static final class MaterialRule {

        /** Any material, including none. */
        public static final MaterialRule ANY = new MaterialRule(null, false);
        /** The slot must be empty. */
        public static final MaterialRule NONE = new MaterialRule(null, true);

        private final String name;
        private final boolean none;

        private MaterialRule(String name, boolean none) {
            this.name = name;
            this.none = none;
        }

        public static MaterialRule is(String material) {
            if (material == null || material.isEmpty()) {
                throw new IllegalArgumentException("Material name must not be empty");
            }
            return new MaterialRule(material.toLowerCase(), false);
        }

        public String getName() {
            return name;
        }

        boolean accepts(ItemStack stack, String slot) {
            if (this == ANY) {
                return true;
            }
            NBTTagCompound tag = CustomMaterial.getNBT(stack, false);
            String present = tag != null && tag.hasKey(slot) ? tag.getString(slot).toLowerCase() : null;
            if (none) {
                return present == null || present.isEmpty();
            }
            return name.equals(present);
        }

        @Override
        public String toString() {
            return this == ANY ? "any" : none ? "none" : name;
        }
    }

    private interface Matcher {

        boolean matches(ItemStack stack);

        void indexKeys(Set<Object> keys);

        void examples(List<ItemStack> out, int amount);
    }

    private final Matcher matcher;
    private final int amount;
    private final Usage usage;
    private final MaterialRule main;
    private final MaterialRule haft;
    private final boolean undamaged;
    private final NBTTagCompound exactNbt;
    private final Predicate<ItemStack> condition;
    private final String conditionDescription;

    private Input(Matcher matcher, int amount, Usage usage, MaterialRule main, MaterialRule haft, boolean undamaged,
            NBTTagCompound exactNbt, Predicate<ItemStack> condition, String conditionDescription) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Input amount must be positive: " + amount);
        }
        if (usage.getKind() == Usage.Kind.DAMAGE && amount != 1) {
            throw new IllegalArgumentException("A damaged tool input must have amount 1, got " + amount);
        }
        this.matcher = matcher;
        this.amount = amount;
        this.usage = usage;
        this.main = main;
        this.haft = haft;
        this.undamaged = undamaged;
        this.exactNbt = exactNbt;
        this.condition = condition;
        this.conditionDescription = conditionDescription;
    }

    private static Input of(Matcher matcher) {
        return new Input(matcher, 1, Usage.CONSUME, MaterialRule.ANY, MaterialRule.ANY, false, null, null, null);
    }

    // region factories

    /** Any metadata of the item. */
    public static Input of(Item item) {
        return of(item, OreDictionary.WILDCARD_VALUE);
    }

    public static Input of(Item item, int meta) {
        if (item == null) {
            throw new IllegalArgumentException("Input item must not be null");
        }
        return of(new ItemMatcher(item, meta));
    }

    /** Any metadata of the block. */
    public static Input of(Block block) {
        return of(Item.getItemFromBlock(block));
    }

    public static Input of(Block block, int meta) {
        return of(Item.getItemFromBlock(block), meta);
    }

    /** The stack's item and metadata; the amount is the stack size. NBT is ignored. */
    public static Input of(ItemStack stack) {
        return of(stack.getItem(), stack.getItemDamage()).amount(Math.max(1, stack.stackSize));
    }

    /** Anything registered under the ore name at lookup time. */
    public static Input ore(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Ore name must not be empty");
        }
        return of(new OreMatcher(name));
    }

    /**
     * A workable hot item carrying something the inner input accepts. The count is the hot stack's own size, and the
     * inner input's amount and usage carry over.
     */
    public static Input hot(Input inner) {
        return new Input(
                new HotMatcher(inner),
                inner.amount,
                inner.usage,
                MaterialRule.ANY,
                MaterialRule.ANY,
                false,
                null,
                null,
                null);
    }

    /** Any of the alternatives, each with its own constraints. */
    public static Input anyOf(Input... alternatives) {
        if (alternatives.length == 0) {
            throw new IllegalArgumentException("anyOf needs at least one alternative");
        }
        return of(new AnyOfMatcher(Arrays.asList(alternatives.clone())));
    }

    // endregion

    // region modifiers, each returns a new input

    public Input amount(int n) {
        return new Input(matcher, n, usage, main, haft, undamaged, exactNbt, condition, conditionDescription);
    }

    public Input usage(Usage u) {
        return new Input(matcher, amount, u, main, haft, undamaged, exactNbt, condition, conditionDescription);
    }

    /** Constrains the main material ({@link CustomToolHelper#slot_main}). */
    public Input material(MaterialRule rule) {
        return new Input(matcher, amount, usage, rule, haft, undamaged, exactNbt, condition, conditionDescription);
    }

    public Input material(String name) {
        return material(MaterialRule.is(name));
    }

    /** Constrains the secondary (haft) material ({@link CustomToolHelper#slot_haft}). */
    public Input haft(MaterialRule rule) {
        return new Input(matcher, amount, usage, main, rule, undamaged, exactNbt, condition, conditionDescription);
    }

    public Input undamaged() {
        return new Input(matcher, amount, usage, main, haft, true, exactNbt, condition, conditionDescription);
    }

    /** Requires the stack's whole tag to equal {@code tag}. Use only when every tag counts. */
    public Input exactNbt(NBTTagCompound tag) {
        return new Input(
                matcher,
                amount,
                usage,
                main,
                haft,
                undamaged,
                tag == null ? null : (NBTTagCompound) tag.copy(),
                condition,
                conditionDescription);
    }

    /**
     * An opaque extra check, as scripts provide. It receives a copy of the stack, sized to this input's amount so
     * amount-aware conditions agree with the recipe. The description is shown where the condition cannot be.
     */
    public Input where(Predicate<ItemStack> check, String description) {
        return new Input(matcher, amount, usage, main, haft, undamaged, exactNbt, check, description);
    }

    // endregion

    // region queries

    /** Whether the stack fits, ignoring its size. */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getItem() == null || !matcher.matches(stack)) {
            return false;
        }
        if (!main.accepts(stack, CustomToolHelper.slot_main) || !haft.accepts(stack, CustomToolHelper.slot_haft)) {
            return false;
        }
        if (undamaged && stack.isItemStackDamageable() && stack.getItemDamage() > 0) {
            return false;
        }
        if (exactNbt != null && !exactNbt.equals(stack.getTagCompound())) {
            return false;
        }
        if (condition != null) {
            ItemStack probe = stack.copy();
            probe.stackSize = amount;
            return condition.test(probe);
        }
        return true;
    }

    /**
     * Why the stack does not pay for this input, or null when it does: another item, a material, damage, NBT, the
     * script condition, or too few items.
     */
    public CheckResult.Reason explain(ItemStack stack) {
        if (stack == null || stack.getItem() == null || !matcher.matches(stack)) {
            return CheckResult.Reason.of("wrong_item");
        }
        if (!main.accepts(stack, CustomToolHelper.slot_main)) {
            return CheckResult.Reason.of("wrong_material", String.valueOf(main));
        }
        if (!haft.accepts(stack, CustomToolHelper.slot_haft)) {
            return CheckResult.Reason.of("wrong_material", String.valueOf(haft));
        }
        if (undamaged && stack.isItemStackDamageable() && stack.getItemDamage() > 0) {
            return CheckResult.Reason.of("damaged");
        }
        if (exactNbt != null && !exactNbt.equals(stack.getTagCompound())) {
            return CheckResult.Reason.of("nbt");
        }
        if (!matches(stack)) {
            return CheckResult.Reason.of("condition", String.valueOf(conditionDescription));
        }
        if (stack.stackSize < amount) {
            return CheckResult.Reason.of("amount", stack.stackSize, amount);
        }
        return null;
    }

    /** Whether the stack fits and holds at least the amount. */
    public boolean hasEnough(ItemStack stack) {
        return matches(stack) && stack.stackSize >= amount;
    }

    public int getAmount() {
        return amount;
    }

    public Usage getUsage() {
        return usage;
    }

    public MaterialRule getMaterial() {
        return main;
    }

    public MaterialRule getHaft() {
        return haft;
    }

    public boolean isConditional() {
        return condition != null;
    }

    public String getConditionDescription() {
        return conditionDescription;
    }

    /** Index keys under which a registry files this input. */
    public Set<Object> indexKeys() {
        Set<Object> keys = new LinkedHashSet<>();
        matcher.indexKeys(keys);
        return keys;
    }

    /**
     * Keys to look an actual stack up with: its item, its current ore names and, for a hot item, the same for the item
     * it carries. They meet {@link #indexKeys()} of every input that could match.
     */
    public static Set<Object> lookupKeys(ItemStack stack) {
        Set<Object> keys = new LinkedHashSet<>();
        addLookupKeys(stack, keys, true);
        return keys;
    }

    /** Example stacks for display, as fresh copies with the amount and a required main material applied. */
    public List<ItemStack> examples() {
        List<ItemStack> out = new ArrayList<>();
        matcher.examples(out, amount);
        if (main.getName() != null) {
            for (ItemStack stack : out) {
                CustomMaterial.addMaterial(stack, CustomToolHelper.slot_main, main.getName());
            }
        }
        return out;
    }

    // endregion

    private static void addLookupKeys(ItemStack stack, Set<Object> keys, boolean unwrap) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        keys.add(stack.getItem());
        for (String name : OreNames.get().namesOf(stack)) {
            keys.add(oreKey(name));
        }
        if (unwrap && stack.getItem() instanceof IHotItem) {
            addLookupKeys(Heatable.getItem(stack), keys, false);
        }
    }

    private static String oreKey(String name) {
        return "ore:" + name;
    }

    private static final class ItemMatcher implements Matcher {

        private final Item item;
        private final int meta;

        ItemMatcher(Item item, int meta) {
            this.item = item;
            this.meta = meta;
        }

        @Override
        public boolean matches(ItemStack stack) {
            return stack.getItem() == item && (meta == OreDictionary.WILDCARD_VALUE || stack.getItemDamage() == meta);
        }

        @Override
        public void indexKeys(Set<Object> keys) {
            keys.add(item);
        }

        @Override
        public void examples(List<ItemStack> out, int amount) {
            out.add(new ItemStack(item, amount, meta));
        }
    }

    private static final class OreMatcher implements Matcher {

        private final String name;

        OreMatcher(String name) {
            this.name = name;
        }

        @Override
        public boolean matches(ItemStack stack) {
            return OreNames.get().namesOf(stack).contains(name);
        }

        @Override
        public void indexKeys(Set<Object> keys) {
            keys.add(oreKey(name));
        }

        @Override
        public void examples(List<ItemStack> out, int amount) {
            for (ItemStack stack : OreNames.get().stacksOf(name)) {
                stack.stackSize = amount;
                out.add(stack);
            }
        }
    }

    private static final class HotMatcher implements Matcher {

        private final Input inner;

        HotMatcher(Input inner) {
            this.inner = inner;
        }

        @Override
        public boolean matches(ItemStack stack) {
            if (!(stack.getItem() instanceof IHotItem) || !Heatable.isWorkable(stack)) {
                return false;
            }
            ItemStack held = Heatable.getItem(stack);
            if (held == null) {
                return false;
            }
            // The carried count is frozen at heating time; the real count is the hot stack's own size.
            held.stackSize = stack.stackSize;
            return inner.matches(held);
        }

        @Override
        public void indexKeys(Set<Object> keys) {
            keys.addAll(inner.indexKeys());
        }

        @Override
        public void examples(List<ItemStack> out, int amount) {
            for (ItemStack stack : inner.examples()) {
                stack.stackSize = amount;
                out.add(stack);
            }
        }
    }

    private static final class AnyOfMatcher implements Matcher {

        private final List<Input> alternatives;

        AnyOfMatcher(List<Input> alternatives) {
            this.alternatives = Collections.unmodifiableList(alternatives);
        }

        @Override
        public boolean matches(ItemStack stack) {
            for (Input alternative : alternatives) {
                if (alternative.matches(stack)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void indexKeys(Set<Object> keys) {
            for (Input alternative : alternatives) {
                keys.addAll(alternative.indexKeys());
            }
        }

        @Override
        public void examples(List<ItemStack> out, int amount) {
            for (Input alternative : alternatives) {
                for (ItemStack stack : alternative.examples()) {
                    stack.stackSize = amount;
                    out.add(stack);
                }
            }
        }
    }
}
