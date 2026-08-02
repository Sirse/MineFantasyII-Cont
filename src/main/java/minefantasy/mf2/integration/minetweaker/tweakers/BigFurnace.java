package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.ArrayList;
import java.util.Map;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.refine.BigFurnaceRecipes;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.mc1710.item.MCItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.BigFurnace")
public class BigFurnace {

    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient input, @Optional int tier) {
        MineTweakerAPI.apply(new AddRecipeAction(output, input, tier));
    }

    @ZenMethod
    public static void remove(@NotNull IIngredient output, IIngredient input) {
        ArrayList<BigFurnaceRecipes> toRemove = new ArrayList<BigFurnaceRecipes>();
        for (Map.Entry<String, BigFurnaceRecipes> entry : BigFurnaceRecipes.recipeList.entrySet()) {
            BigFurnaceRecipes recipe = entry.getValue();
            if (recipe != null && recipe.input != null
                    && recipe.result != null
                    && output.matches(new MCItemStack(recipe.result))
                    && (input == null || input.matches(new MCItemStack(recipe.input)))) {
                toRemove.add(recipe);
            }
        }
        if (toRemove.isEmpty()) {
            MineTweakerAPI.logWarning("No Big Furnace recipes for " + output.toString());
            return;
        }
        MineTweakerAPI.apply(new RemoveAction(toRemove));
    }

    private static class AddRecipeAction implements IUndoableAction {

        private final IItemStack output;
        private final IIngredient input;
        private final int tier;
        private final ArrayList<BigFurnaceRecipes> addedRecipes = new ArrayList<BigFurnaceRecipes>();

        public AddRecipeAction(IItemStack output, IIngredient input, int tier) {
            this.output = output;
            this.input = input;
            this.tier = tier;
        }

        @Override
        public void apply() {
            for (IIngredient ingredient : input.getItems()) {
                ItemStack mcInput = MineTweakerMC.getItemStack(ingredient);
                addedRecipes.add(BigFurnaceRecipes.addRecipe(mcInput, MineTweakerMC.getItemStack(output), tier));
            }
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            for (BigFurnaceRecipes recipeToRemove : addedRecipes) {
                BigFurnaceRecipes.recipeList.remove(CustomToolHelper.getReferenceName(recipeToRemove.input));
            }
            addedRecipes.clear();
        }

        @Override
        public String describe() {
            return "Adding big furnace recipe for " + output.getDisplayName();
        }

        @Override
        public String describeUndo() {
            return "Removing big furnace recipe for " + output.getDisplayName();
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static class RemoveAction implements IUndoableAction {

        private final ArrayList<BigFurnaceRecipes> recipes;

        private RemoveAction(ArrayList<BigFurnaceRecipes> recipes) {
            this.recipes = recipes;
        }

        @Override
        public void apply() {
            for (BigFurnaceRecipes recipe : recipes) {
                BigFurnaceRecipes.recipeList.remove(CustomToolHelper.getReferenceName(recipe.input));
            }
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            for (BigFurnaceRecipes recipe : recipes) {
                BigFurnaceRecipes.recipeList.put(CustomToolHelper.getReferenceName(recipe.input), recipe);
            }
        }

        @Override
        public String describe() {
            return "Removing " + recipes.size() + " Big Furnace recipes";
        }

        @Override
        public String describeUndo() {
            return "Restoring " + recipes.size() + " Big Furnace recipes";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

}
