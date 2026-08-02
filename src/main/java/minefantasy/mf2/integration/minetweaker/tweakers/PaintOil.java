package minefantasy.mf2.integration.minetweaker.tweakers;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.refine.PaintOilRecipe;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.PaintOil")
public class PaintOil {

    @ZenMethod
    public static void addRecipe(@NotNull IItemStack input, @NotNull IItemStack output) {
        MineTweakerAPI.apply(new AddRecipeAction(input, output));
    }

    private static class AddRecipeAction implements IUndoableAction {

        private final IItemStack input;
        private final IItemStack output;
        private ItemStack recipeKey;
        private ItemStack previousValue;

        private AddRecipeAction(IItemStack input, IItemStack output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public void apply() {
            ItemStack mcInput = MineTweakerMC.getItemStack(input);
            ItemStack mcOutput = MineTweakerMC.getItemStack(output);
            Block inputBlock = Block.getBlockFromItem(mcInput.getItem());
            Block outputBlock = Block.getBlockFromItem(mcOutput.getItem());
            if (inputBlock == null || outputBlock == null) {
                MineTweakerAPI.logError("PaintOil recipes require block items.");
                return;
            }

            recipeKey = new ItemStack(inputBlock, 1, mcInput.getItemDamage());
            previousValue = PaintOilRecipe.recipeList.get(recipeKey);
            PaintOilRecipe.recipeList.put(recipeKey, new ItemStack(outputBlock, 1, mcOutput.getItemDamage()));
        }

        @Override
        public void undo() {
            if (recipeKey == null) {
                return;
            }
            if (previousValue == null) {
                PaintOilRecipe.recipeList.remove(recipeKey);
            } else {
                PaintOilRecipe.recipeList.put(recipeKey, previousValue);
            }
        }

        @Override
        public String describe() {
            return "Adding paint oil recipe";
        }

        @Override
        public String describeUndo() {
            return "Undoing paint oil recipe";
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
