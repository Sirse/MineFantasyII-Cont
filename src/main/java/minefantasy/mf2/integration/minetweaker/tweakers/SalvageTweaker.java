package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedIngredients;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * Salvage parts. {@code set} gives every item the ingredient lists these parts, replacing what it had; damage is
 * ignored for damageable items, and an item's materials make a separate entry.
 */
@ZenClass("mods.minefantasy.Salvage")
public class SalvageTweaker {

    @ZenMethod
    public static void set(@NotNull IIngredient input, @NotNull IItemStack[] parts) {
        ScriptRecipes.apply("Setting salvage of " + input, tx -> {
            Object[] components = new Object[parts.length];
            for (int i = 0; i < parts.length; i++) {
                components[i] = ScriptInputs.toOutput(parts[i]);
            }
            // Salvage is kept per item and material, with nowhere to keep a condition such as onlyWithTag
            for (ItemStack stack : TweakedIngredients.plainItems("Salvage", input, true)) {
                Salvage.SalvageRecipe recipe = Salvage.SalvageRecipe.parts(stack, components);
                tx.set(MFRecipes.SALVAGE, Salvage.partsId(stack), recipe, Salvage.priorityOf(recipe));
            }
        });
    }

    /** Removes the parts entries of every item the ingredient matches (aliases stay). */
    @ZenMethod
    public static void remove(@NotNull IIngredient input) {
        removeWhere(null, input);
    }

    /** Removes parts entries that give a matching part (and take a matching input, if given). */
    @ZenMethod
    public static void removeByPart(@NotNull IIngredient part, @Optional IIngredient input) {
        removeWhere(part, input);
    }

    private static void removeWhere(IIngredient part, IIngredient input) {
        ScriptRecipes.apply("Removing salvage for " + (input != null ? input : part), tx -> {
            List<RecipeId> removed = tx.removeWhere(MFRecipes.SALVAGE, entry -> {
                Salvage.SalvageRecipe recipe = entry.getRecipe();
                if (recipe.getAlias() != null) {
                    return false;
                }
                if (input != null && !TweakedIngredients.names(input, recipe.getDisplayInput())) {
                    return false;
                }
                return part == null
                        || recipe.getParts().stream().anyMatch(stack -> TweakedIngredients.names(part, stack));
            });
            if (removed.isEmpty()) {
                MineTweakerAPI.logWarning("No salvage entries for " + (input != null ? input : part));
            } else {
                MineTweakerAPI.logInfo("Removed salvage entries " + removed);
            }
        });
    }
}
