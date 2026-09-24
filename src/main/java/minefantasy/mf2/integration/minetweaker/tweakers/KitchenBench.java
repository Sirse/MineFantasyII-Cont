package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.function.Supplier;

import minefantasy.mf2.api.MineFantasyAPI;
import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.config.ConfigKitchen;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedIngredients;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.KitchenBench")
public class KitchenBench {

    /**
     * Adds {@code crafttweaker:kitchen/<name>} (or {@code carpenter/<name>} when the kitchen bench is disabled and food
     * is made on the carpenter's bench); the grid is at most 4 by 4. A dirty amount of 0 uses the bench's default.
     */
    @ZenMethod
    public static void addShaped(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            String sound, String tool, int time, float dirtyAmount, IIngredient[][] ingreds, @Optional int priority) {
        if (!TweakedIngredients.fitsGrid(ingreds, 4, 4, "kitchen bench")) {
            return;
        }
        add(name, () -> {
            return TweakedIngredients.shaped(GridRecipe.Grid.BENCH, ingreds, output).tool(tool, -1).time(time)
                    .sound(sound).research(research).skill(getSkillOrWarn(skill, output))
                    .dirtyAmount(dirtyAmount > 0 ? dirtyAmount : MineFantasyAPI.kitchenDirtyFor(time)).build();
        }, priority);
    }

    @ZenMethod
    public static void addShapeless(@NotNull String name, @NotNull IItemStack output, String skill, String research,
            String sound, String tool, int time, float dirtyAmount, IIngredient[] ingreds, @Optional int priority) {
        add(name, () -> {
            return TweakedIngredients.shapeless(GridRecipe.Grid.BENCH, ingreds, output).tool(tool, -1).time(time)
                    .sound(sound).research(research).skill(getSkillOrWarn(skill, output))
                    .dirtyAmount(dirtyAmount > 0 ? dirtyAmount : MineFantasyAPI.kitchenDirtyFor(time)).build();
        }, priority);
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        CarpentersBench.removeById(target(), id);
    }

    @ZenMethod
    public static void removeByOutput(@NotNull IIngredient output, @Optional IIngredient input) {
        CarpentersBench.removeByOutput(target(), output, input);
    }

    private static void add(String name, Supplier<GridRecipe> recipe, int priority) {
        RecipeRegistry<GridRecipe> registry = target();
        RecipeId id = ScriptRecipes.scriptId(registry.getStation(), name);
        ScriptRecipes.apply(
                "Adding " + registry.getStation() + " recipe " + id,
                tx -> tx.add(registry, id, recipe.get(), priority));
    }

    private static RecipeRegistry<GridRecipe> target() {
        return ConfigKitchen.enableBench ? MFRecipes.KITCHEN : MFRecipes.CARPENTER;
    }

    private static Skill getSkillOrWarn(String skill, IItemStack output) {
        Skill s = RPGElements.getSkillByName(skill);
        if (s == null && skill != null && !skill.isEmpty()) {
            MineTweakerAPI
                    .logWarning("Unknown MineFantasy skill '" + skill + "' for kitchen bench recipe -> " + output);
        }
        return s;
    }
}
