package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.ArrayList;

import minefantasy.mf2.api.crafting.anvil.CraftingManagerAnvil;
import minefantasy.mf2.api.crafting.anvil.IAnvilRecipe;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedShapedAnvilRecipe;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedShapelessAnvilRecipe;
import minetweaker.IUndoableAction;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.mc1710.item.MCItemStack;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenClass("mods.minefantasy.Anvil")
public class Anvil {

    @ZenMethod
    public static void addShapedRecipe(@NotNull IItemStack output, String skill, String research, boolean hot,
            String tool, int hammer, int anvil, int time, IIngredient[][] ingreds) {
        MineTweakerAPI.apply(
                new AnvilAction(
                        output,
                        RPGElements.getSkillByName(skill),
                        research,
                        hot,
                        tool,
                        hammer,
                        anvil,
                        time,
                        ingreds));
    }

    @ZenMethod
    public static void addShapelessRecipe(@NotNull IItemStack output, String skill, String research, boolean hot,
            String tool, int hammer, int anvil, int time, IIngredient[] ingreds) {
        MineTweakerAPI.apply(
                new AnvilAction(
                        output,
                        RPGElements.getSkillByName(skill),
                        research,
                        hot,
                        tool,
                        hammer,
                        anvil,
                        time,
                        ingreds));
    }

    @ZenMethod
    public static void remove(@NotNull IIngredient output, IIngredient input) {
        ArrayList<IAnvilRecipe> recipesToRemove = new ArrayList<IAnvilRecipe>();
        for (Object object : CraftingManagerAnvil.getInstance().getRecipeList()) {
            if (!(object instanceof IAnvilRecipe)) {
                continue;
            }
            IAnvilRecipe recipe = (IAnvilRecipe) object;
            if (recipe == null || recipe.getRecipeOutput() == null) {
                continue;
            }
            if (output.matches(new MCItemStack(recipe.getRecipeOutput()))
                    && (input == null || matchesAnyIngredient(recipe, input))) {
                recipesToRemove.add(recipe);
            }
        }
        if (recipesToRemove.isEmpty()) {
            MineTweakerAPI.logWarning("No Anvil recipes for " + output.toString());
            return;
        }
        MineTweakerAPI.apply(new RemoveAction(recipesToRemove));
    }

    public static class AnvilAction implements IUndoableAction {

        IItemStack output;
        Skill s;
        String research, tool;
        boolean hot;
        int hammer, anvil, time;
        IIngredient[][] ingreds;
        IIngredient[] ingreds2;
        boolean shaped;
        IAnvilRecipe recipe;

        public AnvilAction(IItemStack out, Skill s, String research, boolean hot, String tool, int hammer, int anvil,
                int time, IIngredient[][] ingreds) {
            this.output = out;
            this.s = s;
            this.research = research;
            this.tool = tool;
            this.hot = hot;
            this.hammer = hammer;
            this.anvil = anvil;
            this.time = time;
            this.ingreds = ingreds;
            this.shaped = true;
            recipe = new TweakedShapedAnvilRecipe(ingreds, out, tool, time, hammer, anvil, hot, research, s);
        }

        public AnvilAction(IItemStack out, Skill s, String research, boolean hot, String tool, int hammer, int anvil,
                int time, IIngredient[] ingreds) {
            this.output = out;
            this.s = s;
            this.research = research;
            this.tool = tool;
            this.hot = hot;
            this.hammer = hammer;
            this.anvil = anvil;
            this.time = time;
            this.ingreds2 = ingreds;
            this.shaped = false;
            recipe = new TweakedShapelessAnvilRecipe(ingreds2, out, tool, time, hammer, anvil, hot, research, s);
        }

        @Override
        public void apply() {
            CraftingManagerAnvil.getInstance().recipes.add(recipe);
        }

        @Override
        public String describe() {
            return "Adding a " + (hot ? "hot" : "")
                    + " Anvil Recipe resulting in "
                    + MineTweakerMC.getItemStack(output);
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
            return "Undoing Anvil Recipe";
        }

        @Override
        public void undo() {
            CraftingManagerAnvil.getInstance().recipes.remove(recipe);
        }
    }

    private static boolean matchesAnyIngredient(IAnvilRecipe recipe, IIngredient input) {
        if (recipe instanceof TweakedShapedAnvilRecipe) {
            return matchesIngredientGrid(((TweakedShapedAnvilRecipe) recipe).getIngredients(), input);
        }
        if (recipe instanceof TweakedShapelessAnvilRecipe) {
            return matchesIngredientList(((TweakedShapelessAnvilRecipe) recipe).getIngredients(), input);
        }
        return false;
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

    private static class RemoveAction implements IUndoableAction {

        private final ArrayList<IAnvilRecipe> recipes;

        private RemoveAction(ArrayList<IAnvilRecipe> recipes) {
            this.recipes = recipes;
        }

        @Override
        public void apply() {
            CraftingManagerAnvil.getInstance().getRecipeList().removeAll(recipes);
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            CraftingManagerAnvil.getInstance().getRecipeList().addAll(recipes);
        }

        @Override
        public String describe() {
            return "Removing " + recipes.size() + " Anvil recipes";
        }

        @Override
        public String describeUndo() {
            return "Restoring " + recipes.size() + " Anvil recipes";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }
}
