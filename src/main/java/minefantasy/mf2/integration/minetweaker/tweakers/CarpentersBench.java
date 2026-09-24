package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.List;
import java.util.function.Supplier;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedIngredients;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.mc1710.item.MCItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.CarpenterBench")
public class CarpentersBench {

    /** Adds {@code crafttweaker:carpenter/<name>}; the grid is at most 4 by 4. */
    @ZenMethod
    public static void addShaped(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            String sound, double exp, String tool, int hammer, int anvil, int time, IIngredient[][] ingreds,
            @Optional int priority) {
        if (!TweakedIngredients.fitsGrid(ingreds, 4, 4, "carpenter")) {
            return;
        }
        add(
                name,
                () -> TweakedIngredients.shaped(GridRecipe.Grid.BENCH, ingreds, output).tool(tool, hammer)
                        .stationTier(anvil).time(time).experience((float) exp).sound(sound).research(research)
                        .skill(getSkillOrWarn(skill, output)).build(),
                priority);
    }

    @ZenMethod
    public static void addShapeless(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            String sound, double exp, String tool, int hammer, int anvil, int time, IIngredient[] ingreds,
            @Optional int priority) {
        add(
                name,
                () -> TweakedIngredients.shapeless(GridRecipe.Grid.BENCH, ingreds, output).tool(tool, hammer)
                        .stationTier(anvil).time(time).experience((float) exp).sound(sound).research(research)
                        .skill(getSkillOrWarn(skill, output)).build(),
                priority);
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        removeById(MFRecipes.CARPENTER, id);
    }

    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional IIngredient input) {
        removeByOutput(MFRecipes.CARPENTER, output, input);
    }

    private static void add(String name, Supplier<GridRecipe> recipe, int priority) {
        RecipeId id = ScriptRecipes.scriptId(MFRecipes.CARPENTER.getStation(), name);
        ScriptRecipes
                .apply("Adding carpenter recipe " + id, tx -> tx.add(MFRecipes.CARPENTER, id, recipe.get(), priority));
    }

    static void removeById(RecipeRegistry<GridRecipe> registry, String id) {
        RecipeId recipeId = ScriptRecipes.parseId(registry.getStation(), id);
        ScriptRecipes.apply(
                "Removing " + registry.getStation() + " recipe " + recipeId,
                tx -> tx.remove(registry, recipeId));
    }

    /** Removes every recipe with a matching output (and a matching ingredient, if given); logs the ids. */
    static void removeByOutput(RecipeRegistry<GridRecipe> registry, IIngredient output, IIngredient input) {
        ScriptRecipes.apply("Removing " + registry.getStation() + " recipes for " + output, tx -> {
            List<RecipeId> removed = tx.removeWhere(registry, entry -> {
                GridRecipe recipe = entry.getRecipe();
                return recipe.getRecipeOutput() != null && output.matches(new MCItemStack(recipe.getRecipeOutput()))
                        && (input == null || TweakedIngredients.usesIngredient(recipe, input));
            });
            if (removed.isEmpty()) {
                MineTweakerAPI.logWarning("No " + registry.getStation() + " recipes for " + output);
            } else {
                MineTweakerAPI.logInfo("Removed " + registry.getStation() + " recipes " + removed);
            }
        });
    }

    private static Skill getSkillOrWarn(String skill, IItemStack output) {
        Skill s = RPGElements.getSkillByName(skill);
        if (s == null && skill != null && !skill.isEmpty()) {
            MineTweakerAPI.logWarning("Unknown MineFantasy skill '" + skill + "' for carpenter recipe -> " + output);
        }
        return s;
    }

}
