package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeTransaction;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptWarnings;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedIngredients;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * Cooking recipes. One that can burn also gets its burn stage, {@code <id>_burnt}, turning the output into burnt food;
 * add, replace and remove handle both together.
 */
@ZenClass("mods.minefantasy.Cooking")
public class Cooking {

    private static final String STATION = "cooking";

    @ZenMethod
    public static void add(@NotNull String name, @NotNull IItemStack output, @NotNull IIngredient input, int minTemp,
            int maxTemp, int time, int burnTime, boolean requireBaking, @Optional boolean canBurn,
            @Optional int priority) {
        ScriptWarnings.deprecated("Cooking.add", "Cooking.recipe(...).register()");
        RecipeId id = ScriptRecipes.scriptId(STATION, name);
        ScriptRecipes.apply(
                "Adding cooking recipe " + id,
                tx -> recipe(output, input, minTemp, maxTemp, time, burnTime, requireBaking, canBurn)
                        .addTo(tx, id, priority));
    }

    /** Replaces a recipe; its old burn stage goes, and the new one comes if the new recipe can burn. */
    @ZenMethod
    public static void replace(@NotNull String id, @NotNull IItemStack output, @NotNull IIngredient input, int minTemp,
            int maxTemp, int time, int burnTime, boolean requireBaking, @Optional boolean canBurn,
            @Optional int priority) {
        RecipeId recipeId = ScriptRecipes.parseId(STATION, id);
        ScriptWarnings.deprecated("Cooking.replace", "Cooking.recipe(...).replace(id)");
        ScriptRecipes.apply(
                "Replacing cooking recipe " + recipeId,
                tx -> replaceIn(
                        tx,
                        recipeId,
                        recipe(output, input, minTemp, maxTemp, time, burnTime, requireBaking, canBurn),
                        priority));
    }

    private static void replaceIn(RecipeTransaction tx, RecipeId id, CookRecipe recipe, int priority) {
        RecipeId burnt = CookRecipe.burntId(id);
        tx.replace(MFRecipes.COOKING, id, recipe, priority);
        if (MFRecipes.COOKING.containsWorking(burnt)) {
            tx.remove(MFRecipes.COOKING, burnt);
        }
        CookRecipe burnStage = recipe.burnStage();
        if (burnStage != null) {
            tx.add(MFRecipes.COOKING, burnt, burnStage, priority);
        }
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        RecipeId recipeId = ScriptRecipes.parseId(STATION, id);
        ScriptRecipes.apply("Removing cooking recipe " + recipeId, tx -> CookRecipe.removeFrom(tx, recipeId));
    }

    /** Removes the recipes making the output and their burn stages; with {@code expected}, only if that many. */
    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional int expected) {
        removeWhere("for " + output, recipe -> TweakedIngredients.names(output, recipe.getOutput()), expected);
    }

    /** Removes every recipe that would take the given stack; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeAccepting(@NotNull IItemStack input, @Optional int expected) {
        removeWhere("taking " + input, recipe -> TweakedIngredients.takes(recipe.getInput(), input), expected);
    }

    private static void removeWhere(String what, java.util.function.Predicate<CookRecipe> filter, int expected) {
        ScriptRecipes.removeWhere(
                MFRecipes.COOKING,
                what,
                entry -> !entry.getId().getPath().endsWith(CookRecipe.BURNT_SUFFIX) && filter.test(entry.getRecipe()),
                expected,
                (tx, removed) -> {
                    for (RecipeId id : removed) {
                        RecipeId burnt = CookRecipe.burntId(id);
                        if (MFRecipes.COOKING.containsWorking(burnt)) {
                            tx.remove(MFRecipes.COOKING, burnt);
                        }
                    }
                });
    }

    @ZenMethod
    public static Builder recipe(String name, IItemStack output, IIngredient input) {
        return new Builder(name, output, input);
    }

    @ZenClass("mods.minefantasy.CookingRecipeBuilder")
    public static final class Builder {

        private final String name;
        private final IItemStack output;
        private final IIngredient input;
        private Integer min, time;
        private int max;
        private Integer burnTime;
        private boolean oven;
        private boolean canBurn = true;
        private int priority;
        private boolean registered;

        private Builder(String name, IItemStack output, IIngredient input) {
            this.name = name;
            this.output = output;
            this.input = input;
        }

        @ZenMethod
        public Builder heat(int min, int max) {
            this.min = min;
            this.max = max;
            return this;
        }

        @ZenMethod
        public Builder time(int ticks) {
            this.time = ticks;
            return this;
        }

        @ZenMethod
        public Builder burnsAfter(int ticks) {
            this.burnTime = ticks;
            this.canBurn = true;
            return this;
        }

        @ZenMethod
        public Builder noBurn() {
            this.canBurn = false;
            return this;
        }

        @ZenMethod
        public Builder oven() {
            this.oven = true;
            return this;
        }

        @ZenMethod
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        private CookRecipe build() {
            if (registered) {
                throw new IllegalArgumentException("builder is already registered");
            }
            registered = true;
            if (min == null || time == null) {
                throw new IllegalArgumentException("give heat(min, max) and time(ticks)");
            }
            return Cooking.recipe(output, input, min, max, time, burnTime == null ? time / 2 : burnTime, oven, canBurn);
        }

        @ZenMethod
        public void register() {
            RecipeId id = ScriptRecipes.scriptId(STATION, name);
            ScriptRecipes.apply("Adding cooking recipe " + id, tx -> build().addTo(tx, id, priority));
        }

        @ZenMethod
        public void replace(String id) {
            RecipeId recipeId = ScriptRecipes.parseId(STATION, id);
            ScriptRecipes
                    .apply("Replacing cooking recipe " + recipeId, tx -> replaceIn(tx, recipeId, build(), priority));
        }

        @ZenMethod
        public void replaceAccepting(IItemStack stack, @Optional int expected) {
            CookRecipe[] built = new CookRecipe[1];
            ScriptRecipes.replaceWhere(
                    MFRecipes.COOKING,
                    "taking " + stack,
                    entry -> !entry.getId().getPath().endsWith(CookRecipe.BURNT_SUFFIX)
                            && TweakedIngredients.takes(entry.getRecipe().getInput(), stack),
                    expected,
                    (tx, id) -> {
                        if (built[0] == null) {
                            built[0] = build();
                        }
                        replaceIn(tx, id, built[0], priority);
                    });
        }
    }

    private static CookRecipe recipe(IItemStack output, IIngredient input, int minTemp, int maxTemp, int time,
            int burnTime, boolean requireBaking, boolean canBurn) {
        return CookRecipe.builder(ScriptInputs.toInput(input), ScriptInputs.toOutput(output))
                .temperature(minTemp, maxTemp).time(time).burnTime(burnTime).oven(requireBaking).canBurn(canBurn)
                .build();
    }
}
