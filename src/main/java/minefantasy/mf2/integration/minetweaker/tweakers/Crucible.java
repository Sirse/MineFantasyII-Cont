package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.ArrayList;
import java.util.List;

import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.api.refine.AlloyRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedAlloyRecipe;
import minetweaker.MineTweakerAPI;
import minetweaker.OneWayAction;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.mc1710.item.MCItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.Crucible")
public class Crucible {

    @ZenMethod
    public static void addAlloy(IItemStack out, @Optional int level, int dupe, IIngredient[] ingred) {
        MineTweakerAPI.apply(new AddAlloyAction(out, level, ingred, dupe));
    }

    @ZenMethod
    public static void remove(@NotNull IIngredient output, IIngredient input) {
        ArrayList<Alloy> toRemove = new ArrayList<Alloy>();
        for (Alloy alloy : AlloyRecipes.alloys) {
            if (alloy != null && alloy.recipeOutput != null
                    && output.matches(new MCItemStack(alloy.recipeOutput))
                    && (input == null || matchesInput(alloy, input))) {
                toRemove.add(alloy);
            }
        }
        if (toRemove.isEmpty()) {
            MineTweakerAPI.logWarning("No Crucible recipes for " + output.toString());
            return;
        }
        MineTweakerAPI.apply(new RemoveAction(toRemove));
    }

    private static boolean matchesInput(Alloy alloy, IIngredient input) {
        for (Object object : alloy.recipeItems) {
            if (object instanceof IIngredient) {
                IIngredient ingredient = (IIngredient) object;
                for (IItemStack stack : ingredient.getItems()) {
                    if (input.matches(stack)) {
                        return true;
                    }
                }
            } else if (object instanceof IItemStack) {
                if (input.matches((IItemStack) object)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class AddAlloyAction extends OneWayAction {

        private Alloy alloy;
        private IItemStack out;
        private int level;
        private List<IIngredient> ingreds;
        private int dupe;

        public AddAlloyAction(IItemStack out, int level, IIngredient[] ingreds, int dupe) {
            this.out = out;
            this.level = level;
            this.ingreds = new ArrayList<IIngredient>();
            this.dupe = dupe;
            for (IIngredient i : ingreds) {
                this.ingreds.add(i);
            }
            alloy = new TweakedAlloyRecipe(out, level, this.ingreds);
        }

        @Override
        public void apply() {
            AlloyRecipes.addAlloy(alloy);
        }

        @Override
        public String describe() {
            return "Adding Custom Alloy";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }

    }

    private static class RemoveAction extends OneWayAction {

        private final ArrayList<Alloy> recipes;

        private RemoveAction(ArrayList<Alloy> recipes) {
            this.recipes = recipes;
        }

        @Override
        public void apply() {
            AlloyRecipes.alloys.removeAll(recipes);
        }

        @Override
        public String describe() {
            return "Removing " + recipes.size() + " Crucible recipes";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }

    }

}
