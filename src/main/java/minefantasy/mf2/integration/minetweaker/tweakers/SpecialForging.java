package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.SpecialForging")
public class SpecialForging {

    @ZenMethod
    public static void addDragonforgeCraft(@NotNull IIngredient base, @NotNull IItemStack output) {
        MineTweakerAPI.apply(new DragonforgeAction(base, output));
    }

    @ZenMethod
    public static void addOrnateCraft(@NotNull IIngredient base, @NotNull IItemStack output) {
        MineTweakerAPI.apply(new SpecialAction("ornate", base, output));
    }

    private static class DragonforgeAction implements IUndoableAction {

        private final IIngredient base;
        private final IItemStack output;
        private final List<Item> addedBases = new ArrayList<Item>();
        private final List<Item> previous = new ArrayList<Item>();
        private Item outputItem;

        private DragonforgeAction(IIngredient base, IItemStack output) {
            this.base = base;
            this.output = output;
        }

        @Override
        public void apply() {
            ItemStack mcOutput = MineTweakerMC.getItemStack(output);
            outputItem = mcOutput == null ? null : mcOutput.getItem();
            for (IItemStack stack : base.getItems()) {
                ItemStack mcBase = MineTweakerMC.getItemStack(stack);
                if (mcBase == null || mcBase.getItem() == null || outputItem == null) {
                    continue;
                }
                Item baseItem = mcBase.getItem();
                if (!addedBases.contains(baseItem)) {
                    addedBases.add(baseItem);
                    previous.add(minefantasy.mf2.api.crafting.exotic.SpecialForging.dragonforgeCrafts.get(baseItem));
                    minefantasy.mf2.api.crafting.exotic.SpecialForging.addDragonforgeCraft(baseItem, outputItem);
                }
            }
        }

        @Override
        public void undo() {
            for (int i = 0; i < addedBases.size(); i++) {
                Item baseItem = addedBases.get(i);
                Item old = previous.get(i);
                if (old == null) {
                    minefantasy.mf2.api.crafting.exotic.SpecialForging.removeDragonforgeCraft(baseItem);
                } else {
                    minefantasy.mf2.api.crafting.exotic.SpecialForging.dragonforgeCrafts.put(baseItem, old);
                }
            }
        }

        @Override
        public String describe() {
            return "Adding dragonforge craft";
        }

        @Override
        public String describeUndo() {
            return "Undoing dragonforge craft";
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static class SpecialAction implements IUndoableAction {

        private final String special;
        private final IIngredient base;
        private final IItemStack output;
        private final List<Item> addedBases = new ArrayList<Item>();
        private final List<Item> previous = new ArrayList<Item>();
        private Item outputItem;

        private SpecialAction(String special, IIngredient base, IItemStack output) {
            this.special = special;
            this.base = base;
            this.output = output;
        }

        @Override
        public void apply() {
            ItemStack mcOutput = MineTweakerMC.getItemStack(output);
            outputItem = mcOutput == null ? null : mcOutput.getItem();
            for (IItemStack stack : base.getItems()) {
                ItemStack mcBase = MineTweakerMC.getItemStack(stack);
                if (mcBase == null || mcBase.getItem() == null || outputItem == null) {
                    continue;
                }
                Item baseItem = mcBase.getItem();
                if (!addedBases.contains(baseItem)) {
                    addedBases.add(baseItem);
                    previous.add(minefantasy.mf2.api.crafting.exotic.SpecialForging.getSpecialCraft(special, mcBase));
                    minefantasy.mf2.api.crafting.exotic.SpecialForging.addSpecialCraft(special, baseItem, outputItem);
                }
            }
        }

        @Override
        public void undo() {
            for (int i = 0; i < addedBases.size(); i++) {
                Item baseItem = addedBases.get(i);
                Item old = previous.get(i);
                if (old == null) {
                    minefantasy.mf2.api.crafting.exotic.SpecialForging.removeSpecialCraft(special, baseItem);
                } else {
                    minefantasy.mf2.api.crafting.exotic.SpecialForging.specialCrafts
                            .put("[" + special + "]" + CustomToolHelper.getSimpleReferenceName(baseItem), old);
                }
            }
        }

        @Override
        public String describe() {
            return "Adding " + special + " special craft";
        }

        @Override
        public String describeUndo() {
            return "Undoing " + special + " special craft";
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
