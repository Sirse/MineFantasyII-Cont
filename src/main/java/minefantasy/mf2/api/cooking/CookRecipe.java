package minefantasy.mf2.api.cooking;

import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeChecks;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.api.recipe.RecipeSource;
import minefantasy.mf2.api.recipe.RecipeTransaction;

/**
 * A cooking recipe for spits, stoves and ovens: one item cooks within a temperature range. A recipe that can burn has a
 * companion recipe, {@code <id>_burnt}, that turns the cooked item into the burnt one when left too long; both are
 * registered and removed together. Immutable; register through {@link #addRecipe} or {@link MFRecipes#COOKING}.
 */
public final class CookRecipe implements RecipeChecks.Validated {

    @Override
    public void validate() {
        RecipeChecks.input("input", input);
        RecipeChecks.output("output", output);
        if (burnt != null) {
            RecipeChecks.output("burnt result", burnt);
        }
        RecipeChecks.temperatures(minTemperature, maxTemperature);
        RecipeChecks.positive("time", time);
        RecipeChecks.notNegative("burn time", burnTime);
    }

    /** Suffix of the companion recipe that burns the cooked output. */
    public static final String BURNT_SUFFIX = "_burnt";

    public static Item burnt_food = null;
    public static boolean canCookBasics = true;

    private final Input input;
    private final ItemStack output;
    private final ItemStack burnt;
    private final int minTemperature, maxTemperature, time, burnTime;
    private final boolean baking, canBurn;

    private CookRecipe(Input input, ItemStack output, ItemStack burnt, int min, int max, int time, int burnTime,
            boolean baking, boolean canBurn) {
        this.input = input;
        this.output = output;
        this.burnt = burnt;
        this.minTemperature = min;
        this.maxTemperature = max;
        this.time = time;
        this.burnTime = burnTime;
        this.baking = baking;
        this.canBurn = canBurn;
    }

    /**
     * @param burnt    what the output becomes when overheated; ignored unless {@code canBurn}
     * @param burnTime ticks a finished product takes to burn
     */
    private static CookRecipe of(Input input, ItemStack output, ItemStack burnt, int min, int max, int time,
            int burnTime, boolean baking, boolean canBurn) {
        if (input == null || output == null || output.getItem() == null) {
            throw new IllegalArgumentException("A cooking recipe needs an input and an output");
        }
        boolean burns = canBurn && burnt != null && burnt.getItem() != null;
        // The station takes the recipe's amount onto its slot: several items can cook into one result
        return new CookRecipe(
                input,
                output.copy(),
                burns ? burnt.copy() : null,
                min,
                max,
                time,
                burnTime,
                baking,
                burns);
    }

    /** The recipe that burns this one's output, or null if it cannot burn. */
    public CookRecipe burnStage() {
        if (!canBurn) {
            return null;
        }
        // The whole cooked result burns, however many it is; a tagged result only, other tags allowed, so foods told
        // apart
        // by NBT keep their own heat
        Input cooked = Input.of(output.getItem(), output.getItemDamage()).amount(output.stackSize);
        if (output.hasTagCompound() && !output.getTagCompound().hasNoTags()) {
            NBTTagCompound tag = (NBTTagCompound) output.getTagCompound().copy();
            cooked = cooked.where(stack -> Input.containsTag(stack.getTagCompound(), tag), "nbt " + tag);
        }
        return new CookRecipe(cooked, burnt.copy(), null, minTemperature, maxTemperature, burnTime, 0, baking, false);
    }

    /** Stages the recipe and its burn stage in the transaction. */
    public void addTo(RecipeTransaction tx, RecipeId id, int priority) {
        tx.add(MFRecipes.COOKING, id, this, priority);
        CookRecipe burn = burnStage();
        if (burn != null) {
            tx.add(MFRecipes.COOKING, burntId(id), burn, priority);
        }
    }

    /** Stages removal of the recipe and its burn stage, if it has one. */
    public static void removeFrom(RecipeTransaction tx, RecipeId id) {
        tx.remove(MFRecipes.COOKING, id);
        RecipeId burnt = burntId(id);
        if (MFRecipes.COOKING.containsWorking(burnt)) {
            tx.remove(MFRecipes.COOKING, burnt);
        }
    }

    public static RecipeId burntId(RecipeId id) {
        return RecipeId.of(id.getNamespace(), id.getPath() + BURNT_SUFFIX);
    }

    // region native registration

    /** A recipe cooking what the input takes into the output; see {@link Builder}. */
    public static Builder builder(Input input, ItemStack output) {
        return new Builder(input, output, null);
    }

    /** A native recipe cooking the input into the output, registered by {@link Builder#register}. */
    public static Builder nativeRecipe(ItemStack input, ItemStack output) {
        return new Builder(NativeRecipes.input(input), output, input);
    }

    /**
     * A cooking recipe set up term by term. The temperatures and the time must be given; by default it cooks over a
     * fire, and once done burns into burnt food after half its time when the heat goes above the maximum.
     */
    public static final class Builder {

        private final Input input;
        private final ItemStack output;
        /** The item a native recipe is named after; null for one built from a script input. */
        private final ItemStack nativeInput;
        private ItemStack burnt = new ItemStack(burnt_food);
        private Integer min;
        private int max;
        private Integer time;
        private Integer burnTime;
        private boolean oven;
        private boolean canBurn = true;

        private Builder(Input input, ItemStack output, ItemStack nativeInput) {
            this.input = input;
            this.output = output;
            this.nativeInput = nativeInput;
        }

        /** The heat it cooks above, and the heat it burns above. */
        public Builder temperature(int min, int max) {
            this.min = min;
            this.max = max;
            return this;
        }

        /** The ticks it takes at the least heat. */
        public Builder time(int time) {
            this.time = time;
            return this;
        }

        /** What it burns into; null for nothing. */
        public Builder burnt(ItemStack burnt) {
            this.burnt = burnt;
            return this;
        }

        /** How long the cooked food takes to burn; half the cooking time by default. */
        public Builder burnTime(int burnTime) {
            this.burnTime = burnTime;
            return this;
        }

        /** It needs an oven, enclosed, rather than a fire. */
        public Builder oven() {
            return oven(true);
        }

        public Builder oven(boolean oven) {
            this.oven = oven;
            return this;
        }

        /** Whether it can burn at all; not when it cooks in a container, say. */
        public Builder canBurn(boolean canBurn) {
            this.canBurn = canBurn;
            return this;
        }

        public CookRecipe build() {
            RecipeChecks.require(min != null, "a cooking recipe needs its temperatures");
            RecipeChecks.require(time != null, "a cooking recipe needs its time");
            return of(input, output, burnt, min, max, time, burnTime != null ? burnTime : time / 2, oven, canBurn);
        }

        /** Registers a native recipe, with its burn stage, under an id derived from its input. */
        public RecipeEntry<CookRecipe> register() {
            RecipeChecks.require(nativeInput != null, "only a native cooking recipe registers itself");
            CookRecipe recipe = build();
            RecipeId id = NativeRecipes.nativeId(MFRecipes.COOKING, nativeInput);
            try (RecipeTransaction tx = MFRecipes.REGISTRIES.begin(RecipeSource.NATIVE)) {
                recipe.addTo(tx, id, 0);
                tx.commit();
            }
            return MFRecipes.COOKING.workingEntry(id);
        }
    }

    // endregion

    /**
     * The recipe for the item on a spit ({@code oven} false) or in an oven. Without a registered recipe, food that
     * vanilla smelts cooks on a spit too, when {@link #canCookBasics} allows it.
     */
    /** The recipe an item cooks by, with the id a station saves its work under. */
    public static final class Found {

        public final RecipeId id;
        public final CookRecipe recipe;

        private Found(RecipeId id, CookRecipe recipe) {
            this.id = id;
            this.recipe = recipe;
        }
    }

    /** Vanilla smelting of food on a spit, when no cooking recipe takes the item. */
    public static final RecipeId VANILLA = NativeRecipes.id("cooking/vanilla");

    /** The first recipe for the item whatever the amount, as recipe lists show it. */
    public static CookRecipe getResult(ItemStack item, boolean oven) {
        Found found = find(item, oven, false);
        return found == null ? null : found.recipe;
    }

    /**
     * The recipe a station cooks the stack by: the first, in lookup order, the stack has enough for, so a recipe for
     * several items does not keep a single one from its own recipe. Vanilla smelting of food on a spit comes last.
     */
    public static Found find(ItemStack item, boolean oven) {
        return find(item, oven, true);
    }

    private static Found find(ItemStack item, boolean oven, boolean enough) {
        if (item == null || item.getItem() == null) {
            return null;
        }
        RecipeRegistry<CookRecipe> registry = MFRecipes.COOKING;
        for (RecipeEntry<CookRecipe> entry : registry.published().candidates(Input.lookupKeys(item))) {
            CookRecipe recipe = entry.getRecipe();
            if (recipe.baking == oven && (enough ? recipe.input.hasEnough(item) : recipe.input.matches(item))) {
                return new Found(entry.getId(), recipe);
            }
        }
        if (canCookBasics && !oven) {
            ItemStack smelted = FurnaceRecipes.smelting().getSmeltingResult(item);
            if (smelted != null && smelted.getItem() instanceof ItemFood) {
                return new Found(
                        VANILLA,
                        of(
                                Input.of(item.getItem(), item.getItemDamage()),
                                smelted,
                                new ItemStack(burnt_food),
                                100,
                                300,
                                20,
                                10,
                                false,
                                true));
            }
        }
        return null;
    }

    public Input getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    /** What the output burns into, or null. */
    public ItemStack getBurnt() {
        return burnt == null ? null : burnt.copy();
    }

    public int getMinTemperature() {
        return minTemperature;
    }

    public int getMaxTemperature() {
        return maxTemperature;
    }

    public int getTime() {
        return time;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public boolean isBaking() {
        return baking;
    }

    public boolean canBurn() {
        return canBurn;
    }

    public Set<Object> indexKeys() {
        return input.indexKeys();
    }
}
