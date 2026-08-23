package minefantasy.mf2.integration.minetweaker.tweakers;

import minefantasy.mf2.api.crafting.MineFantasyFuels;
import minetweaker.MineTweakerAPI;
import minetweaker.OneWayAction;
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

    private static class AddCarbonAction extends OneWayAction {

        private final IItemStack stack;
        private final int uses;

        public AddCarbonAction(IItemStack stack, int uses) {
            this.stack = stack;
            this.uses = uses;
        }

        @Override
        public void apply() {
            MineFantasyFuels.addCarbon(MineTweakerMC.getItemStack(stack), uses);
        }

        @Override
        public String describe() {
            return "Adding carbon fuel source: " + stack.getDisplayName() + " (" + uses + " uses)";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
