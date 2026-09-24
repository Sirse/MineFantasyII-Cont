package minefantasy.mf2.integration.nei;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;

public class RecipeHandlerCarpenter extends RecipeHandlerBench {

    public RecipeHandlerCarpenter() {
        super("minefantasy2.carpenter");
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("method.carpenter");
    }

    @Override
    public String getGuiTexture() {
        return "minefantasy2:textures/gui/carpenter.png";
    }

    @Override
    protected List<GridRecipe> recipes() {
        return MFRecipes.CARPENTER.published().recipes();
    }

    @Override
    protected List<ItemStack> stations(GridRecipe recipe) {
        return NEIStationSlots.carpenters(recipe.getAnvil());
    }

    @Override
    protected boolean marksHeat() {
        return true;
    }
}
