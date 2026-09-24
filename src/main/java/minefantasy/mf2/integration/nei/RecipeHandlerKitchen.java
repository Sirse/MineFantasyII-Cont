package minefantasy.mf2.integration.nei;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.block.list.BlockListMF;

public class RecipeHandlerKitchen extends RecipeHandlerBench {

    public RecipeHandlerKitchen() {
        super("minefantasy2.kitchen");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.kitchenbench");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/kitchen.png";
    }

    @Override
    protected List<GridRecipe> recipes() {
        return MFRecipes.KITCHEN.published().recipes();
    }

    @Override
    protected List<ItemStack> stations(GridRecipe recipe) {
        return NEIStationSlots.single(BlockListMF.kitchenBench);
    }
}
