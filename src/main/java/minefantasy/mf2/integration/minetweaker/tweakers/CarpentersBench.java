package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.carpenter.CraftingManagerCarpenter;
import minefantasy.mf2.api.crafting.carpenter.ICarpenterRecipe;
import minefantasy.mf2.api.crafting.carpenter.ShapedCarpenterRecipes;
import minefantasy.mf2.api.crafting.carpenter.ShapelessCarpenterRecipes;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedShapedCBRecipes;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedShapelessCBRecipes;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.mc1710.item.MCItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.CarpenterBench")
public class CarpentersBench {

    @ZenMethod
    public static void addShapedRecipe(@NotNull IItemStack output, String skill, String research, String sound,
            double exp, String tool, int hammer, int anvil, int time, IIngredient[][] ingreds) {
        MineTweakerAPI.apply(
                new CarpentersAction(
                        output,
                        getSkillOrWarn(skill, output),
                        research,
                        sound,
                        (float) exp,
                        tool,
                        hammer,
                        anvil,
                        time,
                        ingreds));
    }

    @ZenMethod
    public static void addShapelessRecipe(@NotNull IItemStack output, String skill, String research, String sound,
            double exp, String tool, int hammer, int anvil, int time, IIngredient[] ingreds) {
        MineTweakerAPI.apply(
                new CarpentersAction(
                        output,
                        getSkillOrWarn(skill, output),
                        research,
                        sound,
                        (float) exp,
                        tool,
                        hammer,
                        anvil,
                        time,
                        ingreds));
    }

    @ZenMethod
    public static void remove(@NotNull IIngredient output, IIngredient input) {
        ArrayList<ICarpenterRecipe> recipesToRemove = new ArrayList<ICarpenterRecipe>();
        for (Object object : CraftingManagerCarpenter.getInstance().getRecipeList()) {
            if (!(object instanceof ICarpenterRecipe)) {
                continue;
            }
            ICarpenterRecipe recipe = (ICarpenterRecipe) object;
            if (recipe != null && recipe.getRecipeOutput() != null
                    && output.matches(new MCItemStack(recipe.getRecipeOutput()))
                    && (input == null || matchesAnyIngredient(recipe, input))) {
                recipesToRemove.add(recipe);
            }
        }
        if (recipesToRemove.isEmpty()) {
            MineTweakerAPI.logWarning("No Carpenter recipes for " + output.toString());
            return;
        }
        MineTweakerAPI.apply(new RemoveAction(recipesToRemove));
    }

    public static class CarpentersAction implements IUndoableAction {

        IItemStack out;
        Skill s;
        String research, tool, sound;
        float exp;
        int hammer, anvil, time;
        IIngredient[][] ingreds;
        IIngredient[] ingreds2;
        boolean shaped;
        ICarpenterRecipe r;

        // private static final char[] chars =
        // {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','recipe','s','t','u','v','w','x','y','z'};

        public CarpentersAction(IItemStack out, Skill s, String research, String sound, float exp, String tool,
                int hammer, int anvil, int time, IIngredient[][] ingreds) {
            this.out = out;
            this.s = s;
            this.research = research;
            this.tool = tool;
            this.sound = sound;
            this.exp = exp;
            this.hammer = hammer;
            this.anvil = anvil;
            this.time = time;
            this.ingreds = ingreds;
            this.shaped = true;
            r = new TweakedShapedCBRecipes(ingreds, out, tool, time, hammer, anvil, exp, sound, research, s);
        }

        public CarpentersAction(IItemStack out, Skill s, String research, String sound, float exp, String tool,
                int hammer, int anvil, int time, IIngredient[] ingreds) {
            this.out = out;
            this.s = s;
            this.research = research;
            this.tool = tool;
            this.sound = sound;
            this.exp = exp;
            this.hammer = hammer;
            this.anvil = anvil;
            this.time = time;
            this.ingreds2 = ingreds;
            this.shaped = false;
            r = new TweakedShapelessCBRecipes(ingreds2, out, tool, time, hammer, anvil, exp, sound, research, s);
        }

        @Override
        public void apply() {
            CraftingManagerCarpenter.getInstance().recipes.add(r);
        }

        @Override
        public String describe() {
            return "Adding Carpenters Bench Recipe...";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public String describeUndo() {
            return "Undoing Carpenters Recipe";
        }

        @Override
        public void undo() {
            CraftingManagerCarpenter.getInstance().recipes.remove(r);
        }

    }

    private static class RemoveAction implements IUndoableAction {

        private final ArrayList<ICarpenterRecipe> recipes;

        private RemoveAction(ArrayList<ICarpenterRecipe> recipes) {
            this.recipes = recipes;
        }

        @Override
        public void apply() {
            CraftingManagerCarpenter.getInstance().recipes.removeAll(recipes);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            CraftingManagerCarpenter.getInstance().recipes.addAll(recipes);
        }

        @Override
        public String describe() {
            return "Removing " + recipes.size() + " Carpenter recipes";
        }

        @Override
        public String describeUndo() {
            return "Restoring " + recipes.size() + " Carpenter recipes";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static boolean matchesAnyIngredient(ICarpenterRecipe recipe, IIngredient input) {
        if (recipe instanceof TweakedShapedCBRecipes) {
            return matchesIngredientGrid(((TweakedShapedCBRecipes) recipe).getIngredients(), input);
        }
        if (recipe instanceof TweakedShapelessCBRecipes) {
            return matchesIngredientList(((TweakedShapelessCBRecipes) recipe).getIngredients(), input);
        }
        if (recipe instanceof ShapedCarpenterRecipes) {
            return matchesStackArray(((ShapedCarpenterRecipes) recipe).recipeItems, input);
        }
        if (recipe instanceof ShapelessCarpenterRecipes) {
            return matchesStackList(((ShapelessCarpenterRecipes) recipe).recipeItems, input);
        }
        return false;
    }

    private static boolean matchesStackArray(ItemStack[] items, IIngredient input) {
        if (items == null) {
            return false;
        }
        for (ItemStack item : items) {
            if (item != null && input.matches(new MCItemStack(item))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesStackList(List items, IIngredient input) {
        if (items == null) {
            return false;
        }
        for (Object object : items) {
            if (object instanceof ItemStack && input.matches(new MCItemStack((ItemStack) object))) {
                return true;
            }
        }
        return false;
    }

    private static Skill getSkillOrWarn(String skill, IItemStack output) {
        Skill s = RPGElements.getSkillByName(skill);
        if (s == null && skill != null && !skill.isEmpty()) {
            MineTweakerAPI.logWarning("Unknown MineFantasy skill '" + skill + "' for carpenter recipe -> " + output);
        }
        return s;
    }

    private static boolean matchesIngredientGrid(IIngredient[][] ingredients, IIngredient input) {
        if (ingredients == null) {
            return false;
        }
        for (IIngredient[] row : ingredients) {
            if (row == null) {
                continue;
            }
            for (IIngredient ingredient : row) {
                if (ingredient != null) {
                    for (minetweaker.api.item.IItemStack stack : ingredient.getItems()) {
                        if (input.matches(stack)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesIngredientList(IIngredient[] ingredients, IIngredient input) {
        if (ingredients == null) {
            return false;
        }
        for (IIngredient ingredient : ingredients) {
            if (ingredient != null) {
                for (minetweaker.api.item.IItemStack stack : ingredient.getItems()) {
                    if (input.matches(stack)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
