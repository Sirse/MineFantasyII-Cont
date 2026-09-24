package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.List;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeId;
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

@ZenClass("mods.minefantasy.Anvil")
public class Anvil {

    private static final String STATION = "anvil";

    /** Adds {@code crafttweaker:anvil/<name>}; the grid is at most 6 wide and 4 high. */
    @ZenMethod
    public static void addShaped(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            boolean hot, String tool, int hammer, int anvil, int time, IIngredient[][] ingreds,
            @Optional int priority) {
        if (!TweakedIngredients.fitsGrid(ingreds, 6, 4, "anvil")) {
            return;
        }
        RecipeId id = ScriptRecipes.scriptId(STATION, name);
        ScriptRecipes.apply("Adding anvil recipe " + id, tx -> {
            GridRecipe recipe = TweakedIngredients.shaped(GridRecipe.Grid.ANVIL, ingreds, output).tool(tool, hammer)
                    .stationTier(anvil).time(time).hot(hot).research(research).skill(getSkillOrWarn(skill, output))
                    .build();
            tx.add(MFRecipes.ANVIL, id, recipe, 1000 + recipe.getRecipeSize() + priority * 10000);
        });
    }

    @ZenMethod
    public static void addShapeless(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            boolean hot, String tool, int hammer, int anvil, int time, IIngredient[] ingreds, @Optional int priority) {
        RecipeId id = ScriptRecipes.scriptId(STATION, name);
        ScriptRecipes.apply("Adding anvil recipe " + id, tx -> {
            GridRecipe recipe = TweakedIngredients.shapeless(GridRecipe.Grid.ANVIL, ingreds, output).tool(tool, hammer)
                    .stationTier(anvil).time(time).hot(hot).research(research).skill(getSkillOrWarn(skill, output))
                    .build();
            tx.add(MFRecipes.ANVIL, id, recipe, recipe.getRecipeSize() + priority * 10000);
        });
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        RecipeId recipeId = ScriptRecipes.parseId(STATION, id);
        ScriptRecipes.apply("Removing anvil recipe " + recipeId, tx -> tx.remove(MFRecipes.ANVIL, recipeId));
    }

    /** Removes every recipe with a matching output (and a matching ingredient, if given); logs the ids. */
    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional IIngredient input) {
        ScriptRecipes.apply("Removing anvil recipes for " + output, tx -> {
            List<RecipeId> removed = tx.removeWhere(MFRecipes.ANVIL, entry -> {
                GridRecipe recipe = entry.getRecipe();
                return recipe.getRecipeOutput() != null && output.matches(new MCItemStack(recipe.getRecipeOutput()))
                        && (input == null || TweakedIngredients.usesIngredient(recipe, input));
            });
            if (removed.isEmpty()) {
                MineTweakerAPI.logWarning("No anvil recipes for " + output);
            } else {
                MineTweakerAPI.logInfo("Removed anvil recipes " + removed);
            }
        });
    }

    private static Skill getSkillOrWarn(String skill, IItemStack output) {
        Skill s = RPGElements.getSkillByName(skill);
        if (s == null && skill != null && !skill.isEmpty()) {
            MineTweakerAPI.logWarning("Unknown MineFantasy skill '" + skill + "' for anvil recipe -> " + output);
        }
        return s;
    }
}
