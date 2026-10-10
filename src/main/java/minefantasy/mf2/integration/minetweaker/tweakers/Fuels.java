package minefantasy.mf2.integration.minetweaker.tweakers;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.Fuel")
public class Fuels {

    @ZenMethod
    public static void addCarbon(IItemStack stack, int uses) {
        MineTweakerAPI.apply(new AddCarbonAction(stack, uses));
    }

    /**
     * Script carbon is kept apart from the ore names MineFantasy and other mods register carbon under, so a reload can
     * take it back out: 1.7.10 Forge cannot remove an ore dictionary entry.
     */
    private static class AddCarbonAction implements IUndoableAction {

        private final IItemStack stack;
        private final int uses;
        private MineFantasyFuels.ScriptCarbon added;

        public AddCarbonAction(IItemStack stack, int uses) {
            this.stack = stack;
            this.uses = uses;
        }

        @Override
        public void apply() {
            ItemStack mcStack = MineTweakerMC.getItemStack(stack);
            if (mcStack == null) {
                MineTweakerAPI.logWarning("Skipping carbon fuel with invalid item " + stack);
                return;
            }
            added = MineFantasyFuels.addScriptCarbon(mcStack, uses);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            if (added != null) {
                MineFantasyFuels.removeScriptCarbon(added);
                added = null;
            }
        }

        @Override
        public String describe() {
            return "Adding carbon fuel source: " + stack.getDisplayName() + " (" + uses + " uses)";
        }

        @Override
        public String describeUndo() {
            return "Removing carbon fuel source: " + stack.getDisplayName();
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
