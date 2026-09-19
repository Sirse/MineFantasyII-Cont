package minefantasy.mf2.integration.minetweaker.tweakers;

import java.util.ArrayList;
import java.util.List;

import minefantasy.mf2.api.crafting.carpenter.CraftingManagerCarpenter;
import minefantasy.mf2.api.crafting.carpenter.ICarpenterRecipe;
import minefantasy.mf2.api.crafting.kitchen.CraftingManagerKitchen;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.config.ConfigKitchen;
import minefantasy.mf2.integration.minetweaker.helpers.TweakedRemoval;
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

@ZenClass("mods.minefantasy.KitchenBench")
public class KitchenBench {

    @ZenMethod
    public static void addShapedRecipe(@NotNull IItemStack output, String skill, String research, String sound,
            String tool, int time, IIngredient[][] ingreds) {
        MineTweakerAPI
                .apply(new KitchenAction(output, getSkillOrWarn(skill, output), research, sound, tool, time, ingreds));
    }

    @ZenMethod
    public static void addShapedRecipe(@NotNull IItemStack output, String skill, String research, String sound,
            String tool, int time, float dirtyAmount, IIngredient[][] ingreds) {
        MineTweakerAPI.apply(
                new KitchenAction(
                        output,
                        getSkillOrWarn(skill, output),
                        research,
                        sound,
                        tool,
                        time,
                        dirtyAmount,
                        ingreds));
    }

    @ZenMethod
    public static void addShapelessRecipe(@NotNull IItemStack output, String skill, String research, String sound,
            String tool, int time, IIngredient[] ingreds) {
        MineTweakerAPI
                .apply(new KitchenAction(output, getSkillOrWarn(skill, output), research, sound, tool, time, ingreds));
    }

    @ZenMethod
    public static void addShapelessRecipe(@NotNull IItemStack output, String skill, String research, String sound,
            String tool, int time, float dirtyAmount, IIngredient[] ingreds) {
        MineTweakerAPI.apply(
                new KitchenAction(
                        output,
                        getSkillOrWarn(skill, output),
                        research,
                        sound,
                        tool,
                        time,
                        dirtyAmount,
                        ingreds));
    }

    @ZenMethod
    public static void remove(@NotNull IIngredient output, IIngredient input) {
        ArrayList<ICarpenterRecipe> recipesToRemove = new ArrayList<ICarpenterRecipe>();
        for (Object object : targetRecipes()) {
            if (!(object instanceof ICarpenterRecipe)) {
                continue;
            }
            ICarpenterRecipe recipe = (ICarpenterRecipe) object;
            if (recipe != null && recipe.getRecipeOutput() != null
                    && output.matches(new MCItemStack(recipe.getRecipeOutput()))
                    && (input == null || CarpentersBench.matchesAnyIngredient(recipe, input))) {
                recipesToRemove.add(recipe);
            }
        }
        if (recipesToRemove.isEmpty()) {
            MineTweakerAPI.logWarning("No Kitchen Bench recipes for " + output.toString());
            return;
        }
        MineTweakerAPI.apply(new RemoveAction(recipesToRemove));
    }

    public static class KitchenAction implements IUndoableAction {

        private final ArrayList<ICarpenterRecipe> recipes = new ArrayList<ICarpenterRecipe>();

        public KitchenAction(IItemStack out, Skill s, String research, String sound, String tool, int time,
                IIngredient[][] ingreds) {
            recipes.add(new TweakedShapedCBRecipes(ingreds, out, tool, time, -1, -1, 0F, sound, research, s));
        }

        public KitchenAction(IItemStack out, Skill s, String research, String sound, String tool, int time,
                float dirtyAmount, IIngredient[][] ingreds) {
            recipes.add(
                    new TweakedShapedCBRecipes(ingreds, out, tool, time, -1, -1, 0F, sound, research, s)
                            .setDirtyAmount(dirtyAmount));
        }

        public KitchenAction(IItemStack out, Skill s, String research, String sound, String tool, int time,
                IIngredient[] ingreds) {
            recipes.add(new TweakedShapelessCBRecipes(ingreds, out, tool, time, -1, -1, 0F, sound, research, s));
        }

        public KitchenAction(IItemStack out, Skill s, String research, String sound, String tool, int time,
                float dirtyAmount, IIngredient[] ingreds) {
            recipes.add(
                    new TweakedShapelessCBRecipes(ingreds, out, tool, time, -1, -1, 0F, sound, research, s)
                            .setDirtyAmount(dirtyAmount));
        }

        @Override
        public void apply() {
            targetRecipes().addAll(recipes);
        }

        @Override
        public String describe() {
            return "Adding Kitchen Bench Recipe...";
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
            return "Undoing Kitchen Bench Recipe";
        }

        @Override
        public void undo() {
            targetRecipes().removeAll(recipes);
        }
    }

    private static class RemoveAction implements IUndoableAction {

        private final ArrayList<ICarpenterRecipe> recipes;
        private final TweakedRemoval removal;

        private RemoveAction(ArrayList<ICarpenterRecipe> recipes) {
            this.recipes = recipes;
            this.removal = new TweakedRemoval(targetRecipes(), recipes);
        }

        @Override
        public void apply() {
            removal.apply();
        }

        @Override
        public boolean canUndo() {
            return true;
        }

        @Override
        public void undo() {
            removal.undo();
        }

        @Override
        public String describe() {
            return "Removing " + recipes.size() + " Kitchen Bench recipes";
        }

        @Override
        public String describeUndo() {
            return "Restoring " + recipes.size() + " Kitchen Bench recipes";
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
    }

    private static List targetRecipes() {
        return ConfigKitchen.enableBench ? CraftingManagerKitchen.getInstance().recipes
                : CraftingManagerCarpenter.getInstance().recipes;
    }

    private static Skill getSkillOrWarn(String skill, IItemStack output) {
        Skill s = RPGElements.getSkillByName(skill);
        if (s == null && skill != null && !skill.isEmpty()) {
            MineTweakerAPI
                    .logWarning("Unknown MineFantasy skill '" + skill + "' for kitchen bench recipe -> " + output);
        }
        return s;
    }
}
