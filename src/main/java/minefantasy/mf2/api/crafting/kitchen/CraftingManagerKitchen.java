package minefantasy.mf2.api.crafting.kitchen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.carpenter.CarpenterCraftMatrix;
import minefantasy.mf2.api.crafting.carpenter.ICarpenter;
import minefantasy.mf2.api.crafting.carpenter.ICarpenterRecipe;
import minefantasy.mf2.api.rpg.Skill;

/**
 * Recipe manager for the kitchen bench. Reuses carpenter recipe types so knowledge pages and NEI handlers keep working,
 * but keeps food recipes in a separate registry and feeds the dirty amount back to the bench.
 */
public class CraftingManagerKitchen {

    private static final CraftingManagerKitchen instance = new CraftingManagerKitchen();

    public final List recipes = new ArrayList();

    private CraftingManagerKitchen() {}

    public static CraftingManagerKitchen getInstance() {
        return instance;
    }

    public ICarpenterRecipe addRecipe(ItemStack result, Skill skill, String research, String sound, String tool,
            int toolTier, int time, float dirtyAmount, Object... input) {
        ICarpenterRecipe recipe = new ShapedCarpenterRecipesShim(
                result,
                skill,
                research,
                sound,
                tool,
                toolTier,
                time,
                dirtyAmount,
                input);
        this.recipes.add(recipe);
        return recipe;
    }

    public ICarpenterRecipe addShapelessRecipe(ItemStack result, Skill skill, String research, String sound,
            String tool, int toolTier, int time, float dirtyAmount, Object... input) {
        ICarpenterRecipe recipe = new ShapelessCarpenterRecipesShim(
                result,
                skill,
                research,
                sound,
                tool,
                toolTier,
                time,
                dirtyAmount,
                input);
        this.recipes.add(recipe);
        return recipe;
    }

    public ItemStack findMatchingRecipe(ICarpenter bench, CarpenterCraftMatrix matrix) {
        Iterator it = this.recipes.iterator();
        ICarpenterRecipe found = null;

        while (it.hasNext()) {
            ICarpenterRecipe rec = (ICarpenterRecipe) it.next();
            if (rec.matches(matrix)) {
                found = rec;
            }
        }

        if (found != null) {
            bench.setForgeTime(found.getCraftTime());
            bench.setToolTier(found.getRecipeHammer());
            bench.setRequiredCarpenter(found.getAnvil());
            bench.setHotOutput(false);
            bench.setToolType(found.getToolType());
            bench.setCraftingSound(found.getSound());
            bench.setResearch(found.getResearch());
            bench.setSkill(found.getSkill());
            bench.setDirtyAmount(found.getDirtyAmount());

            return found.getCraftingResult(matrix);
        }
        return null;
    }

    public List getRecipeList() {
        return this.recipes;
    }

    /**
     * Thin adapters that only exist to carry the dirty amount without touching the shared carpenter recipe
     * constructors.
     */
    private static final class ShapedCarpenterRecipesShim
            extends minefantasy.mf2.api.crafting.carpenter.ShapedCarpenterRecipes {

        private ShapedCarpenterRecipesShim(ItemStack result, Skill skill, String research, String sound, String tool,
                int toolTier, int time, float dirtyAmount, Object... input) {
            super(
                    parseWidth(input),
                    parseHeight(input),
                    parseItems(input),
                    result,
                    tool,
                    time,
                    toolTier,
                    -1,
                    0F,
                    false,
                    sound,
                    research,
                    skill);
            setDirtyAmount(dirtyAmount);
        }
    }

    private static final class ShapelessCarpenterRecipesShim
            extends minefantasy.mf2.api.crafting.carpenter.ShapelessCarpenterRecipes {

        private ShapelessCarpenterRecipesShim(ItemStack result, Skill skill, String research, String sound, String tool,
                int toolTier, int time, float dirtyAmount, Object... input) {
            super(result, tool, 0F, toolTier, -1, time, parseList(input), false, sound, research, skill);
            setDirtyAmount(dirtyAmount);
        }
    }

    private static int parseWidth(Object... input) {
        return patternWidth(input);
    }

    private static int parseHeight(Object... input) {
        return patternHeight(input);
    }

    /**
     * Mirrors the shaped-recipe parsing of {@link minefantasy.mf2.api.crafting.carpenter.CraftingManagerCarpenter}.
     */
    private static ItemStack[] parseItems(Object... input) {
        String pattern = "";
        int keyStart = 0;
        int width = 0;
        int height = 0;

        if (input[keyStart] instanceof String[]) {
            String[] rows = (String[]) input[keyStart++];
            height = rows.length;
            for (String row : rows) {
                width = row.length();
                pattern = pattern + row;
            }
        } else {
            while (input[keyStart] instanceof String) {
                String row = (String) input[keyStart++];
                ++height;
                width = row.length();
                pattern = pattern + row;
            }
        }

        java.util.HashMap keys = new java.util.HashMap();
        for (; keyStart < input.length; keyStart += 2) {
            Character ch = (Character) input[keyStart];
            ItemStack stack = null;
            Object raw = input[keyStart + 1];

            if (raw instanceof Item) {
                stack = new ItemStack((Item) raw, 1, 32767);
            } else if (raw instanceof net.minecraft.block.Block) {
                stack = new ItemStack((Block) raw, 1, 32767);
            } else if (raw instanceof ItemStack) {
                stack = (ItemStack) raw;
            }
            keys.put(ch, stack);
        }

        ItemStack[] items = new ItemStack[width * height];
        for (int i = 0; i < width * height; ++i) {
            char ch = pattern.charAt(i);
            items[i] = keys.containsKey(ch) ? ((ItemStack) keys.get(ch)).copy() : null;
        }
        return items;
    }

    private static int patternWidth(Object... input) {
        Object first = input[0];
        if (first instanceof String[]) {
            String[] rows = (String[]) first;
            return rows.length > 0 ? rows[0].length() : 0;
        }
        return first instanceof String ? ((String) first).length() : 0;
    }

    private static int patternHeight(Object... input) {
        Object first = input[0];
        if (first instanceof String[]) {
            return ((String[]) first).length;
        }
        int height = 0;
        for (Object o : input) {
            if (o instanceof String) {
                ++height;
            } else {
                break;
            }
        }
        return height;
    }

    private static List parseList(Object... input) {
        ArrayList list = new ArrayList();
        for (Object o : input) {
            if (o instanceof ItemStack) {
                list.add(((ItemStack) o).copy());
            } else if (o instanceof Item) {
                list.add(new ItemStack((Item) o));
            } else if (o instanceof net.minecraft.block.Block) {
                list.add(new ItemStack((Block) o));
            }
        }
        return list;
    }
}
