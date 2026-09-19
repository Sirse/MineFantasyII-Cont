package minefantasy.mf2.integration.minetweaker.tweakers;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.heating.Heatable;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedMapEdit;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.Forge")
public class Forge {

    @ZenMethod
    public static void addHeatableItem(IIngredient input, int min, int unstable, int max) {
        MineTweakerAPI.apply(new AddHeatableAction(input, min, unstable, max));
    }

    private static class AddHeatableAction implements IUndoableAction {

        private final IIngredient input;
        private final int min, unstable, max;
        private final TweakedMapEdit<Heatable> edit = new TweakedMapEdit<Heatable>(Heatable.registerList);

        public AddHeatableAction(IIngredient input, int min, int unstable, int max) {
            this.input = input;
            this.min = min;
            this.unstable = unstable;
            this.max = max;
        }

        @Override
        public void apply() {
            for (IItemStack s : input.getItems()) {
                ItemStack stack = MineTweakerMC.getItemStack(s);
                if (stack == null) {
                    MineTweakerAPI.logWarning("Skipping heatable registration for invalid item " + s);
                    continue;
                }
                // addItem replaces on put, so record the previous profile for this key before overwriting it
                edit.put(Heatable.getRegistrationForItem(stack), new Heatable(stack, min, unstable, max));
            }
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public String describe() {
            return "Adding a heatable item";
        }

        @Override
        public String describeUndo() {
            return "Removing heatable item";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }

        @Override
        public void undo() {
            edit.undo();
        }
    }
}
