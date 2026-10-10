package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedIngredients;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * Forge heat profiles: workable, unstable and ruin temperatures (-1 takes one from the stack's main material). A
 * profile narrower than a native one (one material of a shared item) needs a higher priority to be checked first, or
 * {@code replace} to change the native one itself.
 */
@ZenClass("mods.minefantasy.Forge")
public class Forge {

    private static final String STATION = "forge_heat";

    @ZenMethod
    public static void add(@NotNull String name, @NotNull IIngredient input, int min, int unstable, int max,
            @Optional int priority) {
        RecipeId id = ScriptRecipes.scriptId(STATION, name);
        ScriptRecipes.apply(
                "Adding heat profile " + id,
                tx -> tx.add(MFRecipes.HEATING, id, profile(input, min, unstable, max), priority));
    }

    @ZenMethod
    public static void replace(@NotNull String id, @NotNull IIngredient input, int min, int unstable, int max,
            @Optional int priority) {
        RecipeId profileId = ScriptRecipes.parseId(STATION, id);
        ScriptRecipes.apply(
                "Replacing heat profile " + profileId,
                tx -> tx.replace(MFRecipes.HEATING, profileId, profile(input, min, unstable, max), priority));
    }

    @ZenMethod
    public static void remove(@NotNull String id) {
        RecipeId profileId = ScriptRecipes.parseId(STATION, id);
        ScriptRecipes.apply("Removing heat profile " + profileId, tx -> tx.remove(MFRecipes.HEATING, profileId));
    }

    /** Removes every profile that takes the given stack; with {@code expected}, only if that many match. */
    @ZenMethod
    public static void removeAccepting(@NotNull IItemStack input, @Optional int expected) {
        ScriptRecipes.removeWhere(
                MFRecipes.HEATING,
                "taking " + input,
                profile -> TweakedIngredients.takes(profile.getInput(), input),
                expected);
    }

    private static Heatable profile(IIngredient input, int min, int unstable, int max) {
        return Heatable.of(ScriptInputs.toInput(input), min, unstable, max);
    }
}
