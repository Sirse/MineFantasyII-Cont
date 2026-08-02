package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.mc1710.item.MCItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.SalvageTweaker")
public class SalvageTweaker {

    @ZenMethod
    public static void addSalvage(IItemStack output, IIngredient input) {
        MineTweakerAPI.apply(new AddSalvageAction(output, input));
    }

    @ZenMethod
    public static void remove(@NotNull IIngredient output, IIngredient input) {
        ArrayList<RemovedSalvageState> recipesToRemove = new ArrayList<RemovedSalvageState>();
        for (Salvage.SalvageRecipe recipe : Salvage.displayList) {
            if (recipe != null && recipe.input != null
                    && recipe.outputs != null
                    && outputMatches(output, recipe.outputs)
                    && (input == null || inputMatches(input, recipe.input))) {
                recipesToRemove.add(new RemovedSalvageState(recipe.input.copy(), recipe.outputs));
            }
        }
        if (recipesToRemove.isEmpty()) {
            MineTweakerAPI.logWarning("No Salvage recipes for " + (input == null ? output : input));
            return;
        }
        MineTweakerAPI.apply(new RemoveAction(recipesToRemove));
    }

    private static boolean outputMatches(IIngredient output, Object[] outputs) {
        for (Object object : outputs) {
            if (object instanceof ItemStack) {
                if (output.matches(new MCItemStack((ItemStack) object))) {
                    return true;
                }
            } else if (object instanceof net.minecraft.item.Item) {
                if (output.matches(new MCItemStack(new ItemStack((net.minecraft.item.Item) object)))) {
                    return true;
                }
            } else if (object instanceof net.minecraft.block.Block) {
                if (output.matches(new MCItemStack(new ItemStack((net.minecraft.block.Block) object)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean inputMatches(IIngredient input, ItemStack recipeInput) {
        return input == null || (recipeInput != null && input.matches(new MCItemStack(recipeInput)));
    }

    private static class AddSalvageAction implements IUndoableAction {

        private final IItemStack output;
        private final IIngredient input;
        private final Map<String, SalvageState> previousStates = new LinkedHashMap<String, SalvageState>();
        private final List<ItemStack> addedInputs = new ArrayList<ItemStack>();

        public AddSalvageAction(IItemStack output, IIngredient input) {
            this.output = output;
            this.input = input;
        }

        @Override
        public void apply() {
            ItemStack outputStack = MineTweakerMC.getItemStack(output);
            if (outputStack == null || outputStack.getItem() == null) {
                MineTweakerAPI.logWarning("Skipping salvage recipe with invalid output " + output);
                return;
            }
            addedInputs.clear();
            previousStates.clear();
            for (IItemStack stack : input.getItems()) {
                ItemStack s = MineTweakerMC.getItemStack(stack);
                if (s == null) {
                    continue;
                }
                ItemStack normalized = Salvage.normalizeInput(s);
                String key = normalizedKey(normalized);
                if (!previousStates.containsKey(key)) {
                    previousStates
                            .put(key, new SalvageState(normalized.copy(), Salvage.getRegisteredSalvage(normalized)));
                }
                Salvage.addSalvage(normalized, outputStack);
                addedInputs.add(s.copy());
            }
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            for (ItemStack addedInput : addedInputs) {
                Salvage.removeSalvage(addedInput);
            }
            for (SalvageState state : previousStates.values()) {
                if (state != null && state.input != null && state.outputs != null) {
                    Salvage.addSalvage(state.input.copy(), state.outputs);
                }
            }
            addedInputs.clear();
            previousStates.clear();
        }

        @Override
        public String describe() {
            return null;
        }

        @Override
        public String describeUndo() {
            return null;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }

        private String normalizedKey(ItemStack normalized) {
            return normalized == null ? "" : CustomToolHelper.getReferenceName(normalized);
        }
    }

    private static class SalvageState {

        private final ItemStack input;
        private final Object[] outputs;

        private SalvageState(ItemStack input, Object[] outputs) {
            this.input = input;
            this.outputs = outputs;
        }
    }

    private static class RemoveAction implements IUndoableAction {

        private final ArrayList<RemovedSalvageState> states;

        private RemoveAction(ArrayList<RemovedSalvageState> states) {
            this.states = states;
        }

        @Override
        public void apply() {
            for (RemovedSalvageState state : states) {
                Salvage.removeSalvage(state.input);
            }
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            for (RemovedSalvageState state : states) {
                Salvage.addSalvage(state.input.copy(), state.outputs);
            }
        }

        @Override
        public String describe() {
            return "Removing " + states.size() + " salvage recipes";
        }

        @Override
        public String describeUndo() {
            return "Restoring " + states.size() + " salvage recipes";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static class RemovedSalvageState {

        private final ItemStack input;
        private final Object[] outputs;

        private RemovedSalvageState(ItemStack input, Object[] outputs) {
            this.input = input;
            this.outputs = outputs;
        }
    }
}
