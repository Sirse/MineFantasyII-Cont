package minefantasy.mf2.api.crafting.kitchen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.carpenter.CarpenterCraftMatrix;
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

    public IKitchenRecipe addRecipe(ItemStack result, Skill skill, String research, String sound, String tool,
            int toolTier, int time, float dirtyAmount, Object... input) {
        IKitchenRecipe recipe = new ShapedCarpenterRecipesShim(
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

    public IKitchenRecipe addShapelessRecipe(ItemStack result, Skill skill, String research, String sound, String tool,
            int toolTier, int time, float dirtyAmount, Object... input) {
        IKitchenRecipe recipe = new ShapelessCarpenterRecipesShim(
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

    public ItemStack findMatchingRecipe(IKitchen bench, CarpenterCraftMatrix matrix) {
        Iterator it = this.recipes.iterator();
        IKitchenRecipe found = null;

        while (it.hasNext()) {
            IKitchenRecipe rec = (IKitchenRecipe) it.next();
            if (rec.matches(matrix)) {
                found = rec;
                break; // vanilla semantics: first match wins
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

    /**
     * Mirrors the shaped-recipe parsing of {@link minefantasy.mf2.api.crafting.carpenter.CraftingManagerCarpenter}, but
     * tolerates ragged patterns and unknown keys (they resolve to empty cells).
     */
    private static ItemStack[] parseItems(Object... input) {
        int width = parseWidth(input);
        int height = parseHeight(input);
        List rows = new ArrayList();
        int keyStart = 0;

        if (input[0] instanceof String[]) {
            String[] patternRows = (String[]) input[0];
            keyStart = 1;
            for (String row : patternRows) {
                rows.add(row);
            }
        } else {
            while (keyStart < input.length && input[keyStart] instanceof String) {
                rows.add((String) input[keyStart++]);
            }
        }

        java.util.HashMap keys = new java.util.HashMap();
        for (; keyStart < input.length; keyStart += 2) {
            Character ch = (Character) input[keyStart];
            Object raw = input[keyStart + 1];
            ItemStack stack = null;

            if (raw instanceof Item) {
                stack = new ItemStack((Item) raw, 1, 32767);
            } else if (raw instanceof Block) {
                stack = new ItemStack((Block) raw, 1, 32767);
            } else if (raw instanceof ItemStack) {
                stack = (ItemStack) raw;
            }
            if (stack != null) {
                keys.put(ch, stack);
            }
        }

        ItemStack[] items = new ItemStack[width * height];
        for (int rowIdx = 0; rowIdx < height; ++rowIdx) {
            String row = (String) rows.get(rowIdx);
            for (int col = 0; col < width; ++col) {
                char ch = col < row.length() ? row.charAt(col) : ' ';
                items[rowIdx * width + col] = keys.containsKey(ch) ? ((ItemStack) keys.get(ch)).copy() : null;
            }
        }
        return items;
    }

    private static int parseWidth(Object... input) {
        int width = 0;
        if (input[0] instanceof String[]) {
            for (String row : (String[]) input[0]) {
                width = Math.max(width, row.length());
            }
            return width;
        }
        for (Object o : input) {
            if (o instanceof String) {
                width = Math.max(width, ((String) o).length());
            } else {
                break;
            }
        }
        return width;
    }

    private static int parseHeight(Object... input) {
        if (input[0] instanceof String[]) {
            return ((String[]) input[0]).length;
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
