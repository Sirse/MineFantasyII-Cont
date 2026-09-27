package minefantasy.mf2.api.crafting;

import java.util.EnumSet;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.anvil.CraftingManagerAnvil;
import minefantasy.mf2.api.crafting.exotic.SpecialForging;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.InputSupport;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeRegistries;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.api.recipe.Usage;
import minefantasy.mf2.api.refine.Alloy;

/**
 * The station recipe registries. Mods register in their init or postInit; MineFantasy publishes everything on load
 * complete, and script reloads republish.
 */
public final class MFRecipes {

    public static final RecipeRegistries REGISTRIES = new RecipeRegistries();

    /** The bloomery smelts the whole stack in its slot, so the input names one item of it. */
    private static final InputSupport BLOOM_INPUTS = new InputSupport(
            false,
            EnumSet.of(Usage.Kind.CONSUME, Usage.Kind.CONTAINER, Usage.Kind.TRANSFORM));

    /** Stations whose product replaces the input in its one slot: any amount; a returned container drops off. */
    private static final InputSupport SLOT_INPUTS = new InputSupport(
            true,
            EnumSet.of(Usage.Kind.CONSUME, Usage.Kind.CONTAINER, Usage.Kind.TRANSFORM));

    /**
     * The blast furnace shaft: any amount, and an item may stay (a catalyst) or wear down (a tool); something handed
     * back in its place would block the shaft.
     */
    private static final InputSupport SHAFT_INPUTS = new InputSupport(
            true,
            EnumSet.of(Usage.Kind.CONSUME, Usage.Kind.CATALYST, Usage.Kind.DAMAGE));

    public static final RecipeRegistry<BloomRecipe> BLOOMERY = REGISTRIES
            .create("bloomery", BloomRecipe::indexKeys, BLOOM_INPUTS.of(BloomRecipe::getInput));
    /** Blast furnace: each chamber pays its input by plan; the product falls into the crucible below. */
    public static final RecipeRegistry<ProcessRecipe> BLAST_FURNACE = REGISTRIES
            .create("blast_furnace", ProcessRecipe::indexKeys, SHAFT_INPUTS.of(ProcessRecipe::getInput));
    /** Quern: {@link MFRecipeKeys#TIER}, {@link MFRecipeKeys#CONSUME_POT}. */
    public static final RecipeRegistry<ProcessRecipe> QUERN = REGISTRIES
            .create("quern", ProcessRecipe::indexKeys, InputSupport.PLANNED.of(ProcessRecipe::getInput));
    /** Tanning rack: {@link MFRecipeKeys#TIME}, {@link MFRecipeKeys#TOOL}, {@link MFRecipeKeys#TOOL_TIER}. */
    public static final RecipeRegistry<ProcessRecipe> TANNING = REGISTRIES
            .create("tanning", ProcessRecipe::indexKeys, SLOT_INPUTS.of(ProcessRecipe::getInput));
    /** Big furnace: {@link MFRecipeKeys#TIER}. */
    public static final RecipeRegistry<ProcessRecipe> BIG_FURNACE = REGISTRIES
            .create("big_furnace", ProcessRecipe::indexKeys, InputSupport.PLANNED.of(ProcessRecipe::getInput));
    /** Paint oil: a block (as its item) turning into another block. */
    public static final RecipeRegistry<ProcessRecipe> PAINT_OIL = REGISTRIES
            .create("paint_oil", ProcessRecipe::indexKeys, InputSupport.ONE_CONSUMED.of(ProcessRecipe::getInput));

    /** Spits, stoves and ovens; see {@link CookRecipe}. */
    public static final RecipeRegistry<CookRecipe> COOKING = REGISTRIES
            .create("cooking", CookRecipe::indexKeys, SLOT_INPUTS.of(CookRecipe::getInput));

    /** Forge heat profiles; see {@link Heatable}. They only recognise items, so inputs stay plain. */
    public static final RecipeRegistry<Heatable> HEATING = REGISTRIES
            .create("forge_heat", Heatable::indexKeys, InputSupport.ONE_CONSUMED.of(Heatable::getInput));

    /** Salvage parts and aliases; see {@link Salvage}. */
    public static final RecipeRegistry<Salvage.SalvageRecipe> SALVAGE = REGISTRIES.create(
            "salvage",
            Salvage.SalvageRecipe::indexKeys,
            InputSupport.ONE_CONSUMED.of(Salvage.SalvageRecipe::getInput));
    /** Ornate, dragonforged and other designs of anvil results; see {@link SpecialForging}. */
    public static final RecipeRegistry<SpecialForging.SpecialCraft> SPECIAL_FORGING = REGISTRIES
            .create("special_forging", SpecialForging.SpecialCraft::indexKeys);

    /** Anvil recipes; the manager in {@link CraftingManagerAnvil} matches them against the grid. */
    public static final RecipeRegistry<GridRecipe> ANVIL = REGISTRIES.create("anvil", recipe -> null);
    /** Carpenter's bench recipes. */
    public static final RecipeRegistry<GridRecipe> CARPENTER = REGISTRIES.create("carpenter", recipe -> null);
    /** Kitchen bench recipes; the bench has no tier, so a recipe asking for one is refused. */
    public static final RecipeRegistry<GridRecipe> KITCHEN = REGISTRIES
            .create("kitchen", recipe -> null, recipe -> Requirements.KITCHEN.validate(0, recipe.getAnvil()));

    /** Crucible alloys; see {@link minefantasy.mf2.api.refine.AlloyRecipes}. */
    public static final RecipeRegistry<Alloy> ALLOY = REGISTRIES.create("alloy", recipe -> null);

    private MFRecipes() {}

    /** Whether any recipe's input takes the item, ignoring the amount: for slot and placement checks. */
    public static boolean accepts(RecipeRegistry<ProcessRecipe> registry, ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        for (RecipeEntry<ProcessRecipe> entry : registry.published().candidates(Input.lookupKeys(stack))) {
            if (entry.getRecipe().getInput().matches(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The first recipe, in lookup order, whose input the stack satisfies (amount included) and which the station's
     * current state allows. Stations with a simple one-slot input use this; the context test keeps a recipe the station
     * cannot run from shadowing one it can.
     */
    public static RecipeEntry<ProcessRecipe> find(RecipeRegistry<ProcessRecipe> registry, ItemStack stack,
            Predicate<ProcessRecipe> context) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        for (RecipeEntry<ProcessRecipe> entry : registry.published().candidates(Input.lookupKeys(stack))) {
            ProcessRecipe recipe = entry.getRecipe();
            if (recipe.getInput().hasEnough(stack) && (context == null || context.test(recipe))) {
                return entry;
            }
        }
        return null;
    }

    public static RecipeEntry<ProcessRecipe> find(RecipeRegistry<ProcessRecipe> registry, ItemStack stack) {
        return find(registry, stack, null);
    }
}
