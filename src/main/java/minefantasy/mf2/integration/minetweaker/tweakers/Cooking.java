package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.helpers.CustomToolHelper;
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

@ZenClass("mods.minefantasy.Cooking")
public class Cooking {

    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient input, int minTemp, int maxTemp, int time, int burnTime,
            boolean requireBaking, @Optional boolean canBurn) {
        MineTweakerAPI
                .apply(new AddRecipeAction(output, input, minTemp, maxTemp, time, burnTime, requireBaking, canBurn));
    }

    @ZenMethod
    public static void remove(@NotNull IIngredient output, IIngredient input) {
        ArrayList<String> keysToRemove = new ArrayList<String>();
        ArrayList<CookRecipe> recipesToRemove = new ArrayList<CookRecipe>();
        for (Map.Entry<String, CookRecipe> entry : CookRecipe.recipeList.entrySet()) {
            if (entry.getValue() != null && entry.getValue().output != null
                    && output.matches(new MCItemStack(entry.getValue().output))
                    && (input == null || matchesInputKey(entry.getKey(), input))) {
                keysToRemove.add(entry.getKey());
                recipesToRemove.add(entry.getValue());
            }
        }
        if (keysToRemove.isEmpty()) {
            MineTweakerAPI.logWarning("No Cooking recipes for " + output.toString());
            return;
        }
        MineTweakerAPI.apply(new RemoveAction(keysToRemove, recipesToRemove));
    }

    private static boolean matchesInputKey(String key, IIngredient input) {
        for (IItemStack stack : input.getItems()) {
            ItemStack mcInput = MineTweakerMC.getItemStack(stack);
            if (mcInput != null && key.equals(CustomToolHelper.getReferenceName(mcInput))) {
                return true;
            }
        }
        return false;
    }

    private static class AddRecipeAction implements IUndoableAction {

        private final IItemStack output;
        private final IIngredient input;
        private final int minTemp, maxTemp, time, burnTime;
        private final boolean requireBaking, canBurn;
        private final List<CookRecipe> addedRecipes = new ArrayList<CookRecipe>();

        public AddRecipeAction(IItemStack output, IIngredient input, int minTemp, int maxTemp, int time, int burnTime,
                boolean requireBaking, boolean canBurn) {
            this.output = output;
            this.input = input;
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
            this.time = time;
            this.burnTime = burnTime;
            this.requireBaking = requireBaking;
            this.canBurn = canBurn;
        }

        @Override
        public void apply() {
            for (IIngredient ingredient : input.getItems()) {
                ItemStack mcInput = MineTweakerMC.getItemStack(ingredient);
                addedRecipes.add(
                        CookRecipe.addRecipe(
                                mcInput,
                                MineTweakerMC.getItemStack(output),
                                new ItemStack(CookRecipe.burnt_food),
                                minTemp,
                                maxTemp,
                                time,
                                burnTime,
                                requireBaking,
                                canBurn));
            }
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            java.util.Iterator<Map.Entry<String, CookRecipe>> it = CookRecipe.recipeList.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, CookRecipe> recipeEntry = it.next();
                for (CookRecipe recipe : addedRecipes) {
                    if (recipeEntry.getValue().equals(recipe)) {
                        it.remove();
                        break;
                    }
                }
            }
            addedRecipes.clear();
        }

        @Override
        public String describe() {
            return "Adding cooking recipe for " + output.getDisplayName();
        }

        @Override
        public String describeUndo() {
            return "Removing cooking recipe for " + output.getDisplayName();
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static class RemoveAction implements IUndoableAction {

        private final ArrayList<RemovedRecipeState> removed = new ArrayList<RemovedRecipeState>();

        private RemoveAction(ArrayList<String> keys, ArrayList<CookRecipe> recipes) {
            if (keys != null && recipes != null) {
                for (int i = 0; i < keys.size() && i < recipes.size(); i++) {
                    this.removed.add(new RemovedRecipeState(keys.get(i), recipes.get(i)));
                }
            }
        }

        @Override
        public void apply() {
            for (RemovedRecipeState state : removed) {
                CookRecipe.recipeList.remove(state.key);
            }
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            for (RemovedRecipeState state : removed) {
                CookRecipe.recipeList.put(state.key, state.recipe);
            }
        }

        @Override
        public String describe() {
            return "Removing " + removed.size() + " Cooking recipes";
        }

        @Override
        public String describeUndo() {
            return "Restoring " + removed.size() + " Cooking recipes";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static class RemovedRecipeState {

        private final String key;
        private final CookRecipe recipe;

        private RemovedRecipeState(String key, CookRecipe recipe) {
            this.key = key;
            this.recipe = recipe;
        }
    }

}
