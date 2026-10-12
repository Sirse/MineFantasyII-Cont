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

    /** Fuel for the blast furnace heater, burning this many ticks; it counts ahead of MineFantasy's own. */
    @ZenMethod
    public static void addBlastFuel(IItemStack stack, int ticks) {
        MineTweakerAPI.apply(new FuelAction(MineFantasyFuels.BLAST, "blast furnace", stack, ticks));
    }

    /** Takes an item out as blast furnace heater fuel, MineFantasy's own included. */
    @ZenMethod
    public static void removeBlastFuel(IItemStack stack) {
        MineTweakerAPI.apply(new FuelAction(MineFantasyFuels.BLAST, "blast furnace", stack, 0));
    }

    /** Fuel for the firepit, burning this many ticks before wood scaling; it counts ahead of MineFantasy's own. */
    @ZenMethod
    public static void addFirepitFuel(IItemStack stack, int ticks) {
        MineTweakerAPI.apply(new FuelAction(MineFantasyFuels.FIREPIT, "firepit", stack, ticks));
    }

    /** Takes an item out as firepit fuel, MineFantasy's own included. */
    @ZenMethod
    public static void removeFirepitFuel(IItemStack stack) {
        MineTweakerAPI.apply(new FuelAction(MineFantasyFuels.FIREPIT, "firepit", stack, 0));
    }

    private static class FuelAction implements IUndoableAction {

        private final MineFantasyFuels.FuelTable table;
        private final String fire;
        private final IItemStack stack;
        private final int ticks;
        private MineFantasyFuels.Fuel added;

        FuelAction(MineFantasyFuels.FuelTable table, String fire, IItemStack stack, int ticks) {
            this.table = table;
            this.fire = fire;
            this.stack = stack;
            this.ticks = ticks;
        }

        @Override
        public void apply() {
            ItemStack mcStack = MineTweakerMC.getItemStack(stack);
            if (mcStack == null) {
                MineTweakerAPI.logWarning("Skipping " + fire + " fuel with invalid item " + stack);
                return;
            }
            added = table.addScript(mcStack, ticks);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            if (added != null) {
                table.removeScript(added);
                added = null;
            }
        }

        @Override
        public String describe() {
            return ticks > 0 ? "Adding " + fire + " fuel: " + stack.getDisplayName() + " (" + ticks + " ticks)"
                    : "Removing " + fire + " fuel: " + stack.getDisplayName();
        }

        @Override
        public String describeUndo() {
            return "Restoring " + fire + " fuel: " + stack.getDisplayName();
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
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
