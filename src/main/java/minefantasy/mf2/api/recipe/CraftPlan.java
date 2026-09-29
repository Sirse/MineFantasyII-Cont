package minefantasy.mf2.api.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import minefantasy.mf2.api.helpers.SafeStacks;

/**
 * The whole of one craft as the station worked it out: the recipe chosen, the requirements actually in force (which a
 * material-driven recipe computes from its parts rather than taking from the recipe), what each input slot loses, what
 * comes back (damaged tools, containers, transformed items) and what is produced. {@link #apply} either performs the
 * inventory change in full or not at all: it simulates on copies, checks every slot and every placement, and writes
 * only when all fit.
 *
 * <p>
 * The same plan serves finishing the craft, the station's HUD and its save. Stations keep the plan their progress
 * belongs to through {@link RunningCraft}; any change (recipe replaced, material changed, requirements changed, inputs
 * swapped) resets the progress instead of crafting something else.
 */
public final class CraftPlan {

    private static final class Take {

        final int slot;
        final ItemStack expected;
        final int required;
        final int removed;
        /** Replaces the exact comparison with {@link #expected}, for stacks whose NBT changes while they wait. */
        final Predicate<ItemStack> check;

        Take(int slot, ItemStack expected, int required, int removed, Predicate<ItemStack> check) {
            this.slot = slot;
            this.expected = expected;
            this.required = required;
            this.removed = removed;
            this.check = check;
        }

        boolean accepts(ItemStack stack) {
            return check != null ? check.test(stack) : sameKind(stack, expected);
        }
    }

    private static final class Give {

        final ItemStack stack;
        final int[] slots;
        final boolean product;
        /** False for a product the station finishes and places itself, such as a forged item gaining its quality. */
        final boolean placed;

        Give(ItemStack stack, int[] slots, boolean product, boolean placed) {
            this.stack = stack;
            this.slots = slots;
            this.product = product;
            this.placed = placed;
        }
    }

    private final RecipeId recipeId;
    private final long generation;
    private final List<Take> takes;
    private final List<Give> gives;
    private final RecipeMetadata requirements;
    private final RecipeMetadata effects;
    private final NBTTagCompound fingerprint;

    private CraftPlan(RecipeId recipeId, long generation, List<Take> takes, List<Give> gives,
            RecipeMetadata requirements, RecipeMetadata effects) {
        this.recipeId = recipeId;
        this.generation = generation;
        this.takes = Collections.unmodifiableList(takes);
        this.gives = Collections.unmodifiableList(gives);
        this.requirements = requirements;
        this.effects = effects;
        this.fingerprint = computeFingerprint();
    }

    public static Builder builder(RecipeId recipeId, long generation, int... outputSlots) {
        return new Builder(recipeId, generation, outputSlots);
    }

    public RecipeId getRecipeId() {
        return recipeId;
    }

    /** Registry generation the plan was computed against. */
    public long getGeneration() {
        return generation;
    }

    /** What the craft needs, as worked out for these very inputs: time, tools, tiers, research. */
    public RecipeMetadata getRequirements() {
        return requirements;
    }

    public <T> T require(RecipeMetadataKey<T> key, T fallback) {
        return requirements.get(key, fallback);
    }

    /** Experience, skill and other effects granted after the inventory change succeeds. */
    public RecipeMetadata getEffects() {
        return effects;
    }

    /** The first produced stack, as a copy, or null. */
    public ItemStack getProduct() {
        for (Give give : gives) {
            if (give.product) {
                return give.stack.copy();
            }
        }
        return null;
    }

    /** The produced stacks (not returned containers or tools), as copies, for display. */
    public List<ItemStack> getOutputs() {
        List<ItemStack> out = new ArrayList<>();
        for (Give give : gives) {
            if (give.product) {
                out.add(give.stack.copy());
            }
        }
        return out;
    }

    /** Stable description of the plan for saving; excludes the generation, so an unchanged reload keeps progress. */
    public NBTTagCompound fingerprint() {
        return (NBTTagCompound) fingerprint.copy();
    }

    public boolean sameAs(CraftPlan other) {
        return other != null && fingerprint.equals(other.fingerprint);
    }

    public boolean matchesFingerprint(NBTTagCompound saved) {
        return saved != null && fingerprint.equals(saved);
    }

    /** Checks the plan against the inventory without changing it. */
    public boolean canApply(CraftInventory inventory) {
        return simulate(inventory, null) != null;
    }

    /**
     * As {@link #canApply(CraftInventory)}, letting returns without room spill as {@link #apply(CraftInventory, List)}
     * does.
     */
    public boolean canApplySpilling(CraftInventory inventory) {
        return simulate(inventory, new ArrayList<>()) != null;
    }

    /** Applies the whole plan, or nothing if any slot changed or an output does not fit. */
    public boolean apply(CraftInventory inventory) {
        return apply(inventory, null);
    }

    /**
     * Applies the plan; returned items (containers, tools) that find no room go to {@code spill} for the station to
     * drop, rather than blocking the craft. Products must still fit.
     */
    public boolean apply(CraftInventory inventory, List<ItemStack> spill) {
        List<ItemStack> spilled = spill == null ? null : new ArrayList<>();
        ItemStack[] result = simulate(inventory, spilled);
        if (result == null) {
            return false;
        }
        if (spill != null) {
            spill.addAll(spilled);
        }
        for (int slot = 0; slot < result.length; slot++) {
            if (!ItemStack.areItemStacksEqual(result[slot], inventory.get(slot))) {
                inventory.set(slot, result[slot]);
            }
        }
        return true;
    }

    private ItemStack[] simulate(CraftInventory inventory, List<ItemStack> spill) {
        ItemStack[] slots = new ItemStack[inventory.size()];
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = inventory.get(i);
            slots[i] = stack == null ? null : stack.copy();
        }
        for (Take take : takes) {
            if (take.slot < 0 || take.slot >= slots.length) {
                return null;
            }
            ItemStack stack = slots[take.slot];
            if (stack == null || !take.accepts(stack) || stack.stackSize < take.required) {
                return null;
            }
            stack.stackSize -= take.removed;
            if (stack.stackSize <= 0) {
                slots[take.slot] = null;
            }
        }
        // Products first: a returned container must never take the output slot the product needs
        List<Give> ordered = new ArrayList<>(gives);
        ordered.sort((x, y) -> Boolean.compare(y.product, x.product));
        for (Give give : ordered) {
            if (!give.placed) {
                continue;
            }
            ItemStack left = place(inventory, slots, give.stack.copy(), give.slots);
            if (left != null) {
                if (give.product || spill == null) {
                    return null;
                }
                spill.add(left);
            }
        }
        return slots;
    }

    /** Places the stack into the target slots; returns what did not fit, or null. */
    private static ItemStack place(CraftInventory inventory, ItemStack[] slots, ItemStack stack, int[] targets) {
        for (int slot : targets) {
            ItemStack there = slots[slot];
            if (there != null && sameKind(there, stack)) {
                int room = inventory.limit(slot, there) - there.stackSize;
                int moved = Math.min(room, stack.stackSize);
                if (moved > 0) {
                    there.stackSize += moved;
                    stack.stackSize -= moved;
                }
            }
            if (stack.stackSize <= 0) {
                return null;
            }
        }
        for (int slot : targets) {
            if (slots[slot] == null) {
                int moved = Math.min(inventory.limit(slot, stack), stack.stackSize);
                ItemStack part = stack.copy();
                part.stackSize = moved;
                slots[slot] = part;
                stack.stackSize -= moved;
            }
            if (stack.stackSize <= 0) {
                return null;
            }
        }
        return stack.stackSize <= 0 ? null : stack;
    }

    private static boolean sameKind(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage()
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    private NBTTagCompound computeFingerprint() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("recipe", recipeId.toString());
        NBTTagList takeList = new NBTTagList();
        for (Take take : takes) {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("slot", take.slot);
            t.setTag("item", take.expected.writeToNBT(new NBTTagCompound()));
            t.setInteger("required", take.required);
            t.setInteger("removed", take.removed);
            takeList.appendTag(t);
        }
        tag.setTag("takes", takeList);
        NBTTagList giveList = new NBTTagList();
        for (Give give : gives) {
            NBTTagCompound g = new NBTTagCompound();
            g.setTag("item", give.stack.writeToNBT(new NBTTagCompound()));
            g.setIntArray("slots", give.slots);
            g.setBoolean("product", give.product);
            giveList.appendTag(g);
        }
        tag.setTag("gives", giveList);
        tag.setTag("requirements", metadataTag(requirements));
        tag.setTag("effects", metadataTag(effects));
        return tag;
    }

    private static NBTTagCompound metadataTag(RecipeMetadata metadata) {
        NBTTagCompound tag = new NBTTagCompound();
        for (RecipeMetadataKey<?> key : metadata.keys()) {
            tag.setString(key.getId(), String.valueOf(metadata.get(key)));
        }
        return tag;
    }

    public static final class Builder {

        private final RecipeId recipeId;
        private final long generation;
        private final int[] outputSlots;
        private final List<Take> takes = new ArrayList<>();
        private final List<Give> gives = new ArrayList<>();
        private final RecipeMetadata.Builder requirements = RecipeMetadata.builder();
        private RecipeMetadata effects = RecipeMetadata.EMPTY;
        /** Where returned items go instead of their own slot and the outputs; null for the default. */
        private int[] returnSlots;

        private Builder(RecipeId recipeId, long generation, int[] outputSlots) {
            this.recipeId = recipeId;
            this.generation = generation;
            this.outputSlots = outputSlots.clone();
        }

        /**
         * Uses the input on the stack currently in the slot, according to the input's usage. The stack is only read;
         * the plan keeps a copy to verify the slot has not changed when it is applied.
         */
        public Builder use(int slot, Input input, ItemStack current) {
            ItemStack expected = current.copy();
            expected.stackSize = 1;
            int n = input.getAmount();
            Usage usage = input.getUsage();
            switch (usage.getKind()) {
                case CONSUME:
                    takes.add(new Take(slot, expected, n, n, null));
                    break;
                case CATALYST:
                    takes.add(new Take(slot, expected, n, 0, null));
                    break;
                case DAMAGE: {
                    takes.add(new Take(slot, expected, 1, 1, null));
                    ItemStack tool = current.copy();
                    tool.stackSize = 1;
                    if (tool.isItemStackDamageable()) {
                        tool.setItemDamage(tool.getItemDamage() + usage.getDamage());
                    }
                    if (!tool.isItemStackDamageable() || tool.getItemDamage() <= tool.getMaxDamage()) {
                        giveBack(slot, tool);
                    }
                    break;
                }
                case CONTAINER: {
                    takes.add(new Take(slot, expected, n, n, null));
                    ItemStack container = SafeStacks.containerOf(current);
                    if (container != null) {
                        container.stackSize = n;
                        giveBack(slot, container);
                    }
                    break;
                }
                case TRANSFORM: {
                    takes.add(new Take(slot, expected, n, n, null));
                    ItemStack used = current.copy();
                    used.stackSize = n;
                    ItemStack result = usage.transform(used);
                    if (result != null && result.stackSize > 0) {
                        giveBack(slot, result);
                    }
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown usage " + usage);
            }
            return this;
        }

        /**
         * Takes {@code amount} items from the slot, giving back one container per item taken. The stack is judged by
         * {@code check} when the plan is applied, and described in the fingerprint by {@code shownAs}, so a stack whose
         * NBT changes while it waits (a cooling hot item) keeps the plan valid.
         */
        public Builder consume(int slot, ItemStack current, int amount, ItemStack shownAs, Predicate<ItemStack> check) {
            ItemStack expected = shownAs.copy();
            expected.stackSize = 1;
            takes.add(new Take(slot, expected, amount, amount, check));
            ItemStack container = SafeStacks.containerOf(current);
            if (container != null) {
                container.stackSize = amount;
                giveBack(slot, container);
            }
            return this;
        }

        /** Adds a product, placed into the output slots. */
        public Builder output(ItemStack stack) {
            if (stack != null && stack.stackSize > 0) {
                gives.add(new Give(stack.copy(), outputSlots.clone(), true, true));
            }
            return this;
        }

        /** Records the product the station finishes and places itself; {@link CraftPlan#apply} leaves it out. */
        public Builder product(ItemStack stack) {
            if (stack != null && stack.stackSize > 0) {
                gives.add(new Give(stack.copy(), outputSlots.clone(), true, false));
            }
            return this;
        }

        /**
         * Sends returned items (containers) to these slots only, for a station that keeps them apart from its inputs
         * and outputs. Call it before the inputs that return something.
         */
        public Builder returns(int... slots) {
            this.returnSlots = slots.clone();
            return this;
        }

        /** Records a requirement; null values and empty strings mean "none" and are left out. */
        public <T> Builder require(RecipeMetadataKey<T> key, T value) {
            if (value != null && !"".equals(value)) {
                requirements.put(key, value);
            }
            return this;
        }

        public Builder effects(RecipeMetadata effects) {
            this.effects = effects == null ? RecipeMetadata.EMPTY : effects;
            return this;
        }

        public CraftPlan build() {
            return new CraftPlan(
                    recipeId,
                    generation,
                    new ArrayList<>(takes),
                    new ArrayList<>(gives),
                    requirements.build(),
                    effects);
        }

        /** Returned items go back to their own slot first, then to the outputs, unless {@link #returns} says where. */
        private void giveBack(int slot, ItemStack stack) {
            int[] targets;
            if (returnSlots != null) {
                targets = returnSlots.clone();
            } else {
                targets = new int[outputSlots.length + 1];
                targets[0] = slot;
                System.arraycopy(outputSlots, 0, targets, 1, outputSlots.length);
            }
            gives.add(new Give(stack.copy(), targets, false, true));
        }
    }
}
