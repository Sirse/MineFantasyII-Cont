package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import minefantasy.mf2.api.crafting.GridRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeGridRecipe;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.config.ConfigKitchen;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * A grid recipe for the anvil, carpenter's bench or kitchen bench, named step by step instead of by position:
 *
 * <pre>
 * Anvil.shaped("steel_sword", sword)
 *     .pattern([" I ", " I ", " H "])
 *     .key("I", steel)
 *     .key("H", plank)
 *     .tool("hammer", 2)
 *     .stationTier(2)
 *     .research("craftWeapons")
 *     .time(100)
 *     .hot()
 *     .register();
 * </pre>
 *
 * Nothing is added before {@code register()}, which checks everything at once and reports each problem with the
 * recipe's id. A pattern sits in the grid's top left corner and is never mirrored, as other script recipes; {@code
 * anywhere()} lets it shift and mirror as MineFantasy's own recipes do. {@code time} is work, not ticks: each strike or
 * use of the tool adds its efficiency, at least 0.2.
 */
@ZenClass("mods.minefantasy.GridRecipeBuilder")
public final class GridBuilder {

    /** The stations a builder can make recipes for. */
    public enum Station {

        ANVIL("anvil", 6, 4),
        CARPENTER("carpenter", 4, 4),
        KITCHEN("kitchen", 4, 4);

        final String name;
        final int width;
        final int height;

        Station(String name, int width, int height) {
            this.name = name;
            this.width = width;
            this.height = height;
        }

        RecipeRegistry<GridRecipe> registry() {
            switch (this) {
                case ANVIL:
                    return MFRecipes.ANVIL;
                case KITCHEN:
                    return ConfigKitchen.enableBench ? MFRecipes.KITCHEN : MFRecipes.CARPENTER;
                default:
                    return MFRecipes.CARPENTER;
            }
        }
    }

    private final Station station;
    private final String name;
    private final IItemStack output;
    private final boolean shaped;
    private final List<String> problems = new ArrayList<String>();

    private String[] pattern;
    private final Map<Character, IIngredient> keys = new LinkedHashMap<Character, IIngredient>();
    private IIngredient[] ingredients;
    private IIngredient[][] cells;
    private String tool;
    private int toolTier;
    private int stationTier;
    private String research = "";
    private String skill = "";
    private int time;
    private boolean hot;
    private String sound = "";
    private double experience;
    private double dirt;
    private boolean dirtGiven;
    private int priority;
    private boolean anywhere;
    private boolean registered;

    public GridBuilder(Station station, String name, IItemStack output, boolean shaped) {
        this.station = station;
        this.name = name;
        this.output = output;
        this.shaped = shaped;
    }

    /** Rows of the pattern, one character per cell; a space is an empty cell. */
    @ZenMethod
    public GridBuilder pattern(String[] rows) {
        if (!shaped) {
            problems.add("pattern is for shaped recipes; give a shapeless one its ingredients");
        } else if (pattern != null) {
            problems.add("pattern is given twice");
        }
        pattern = rows;
        return this;
    }

    /** What a pattern character stands for. */
    @ZenMethod
    public GridBuilder key(String symbol, IIngredient ingredient) {
        if (!shaped) {
            problems.add("key is for shaped recipes; give a shapeless one its ingredients");
        } else if (symbol == null || symbol.length() != 1 || symbol.charAt(0) == ' ') {
            problems.add("key \"" + symbol + "\" must be one character other than a space");
        } else if (ingredient == null) {
            problems.add("key " + symbol + " has no ingredient");
        } else if (keys.put(symbol.charAt(0), ingredient) != null) {
            problems.add("key " + symbol + " is given twice");
        }
        return this;
    }

    /** The ingredients of a shapeless recipe. */
    @ZenMethod
    public GridBuilder ingredients(IIngredient[] ingredients) {
        if (shaped) {
            problems.add("ingredients are for shapeless recipes; give a shaped one a pattern and keys");
        } else if (this.ingredients != null) {
            problems.add("ingredients are given twice");
        }
        this.ingredients = ingredients;
        return this;
    }

    /**
     * The tool used on the station and its least tier (the kitchen bench ignores it; the carpenter standing in does
     * not).
     */
    @ZenMethod
    public GridBuilder tool(String type, int tier) {
        this.tool = type;
        this.toolTier = tier;
        return this;
    }

    /** The least station tier. */
    @ZenMethod
    public GridBuilder stationTier(int tier) {
        if (station == Station.KITCHEN) {
            problems.add("the kitchen bench has no tiers");
        }
        this.stationTier = tier;
        return this;
    }

    @ZenMethod
    public GridBuilder research(String research) {
        this.research = research == null ? "" : research;
        return this;
    }

    @ZenMethod
    public GridBuilder skill(String skill) {
        this.skill = skill == null ? "" : skill;
        return this;
    }

    /** Work to finish the craft: each strike or use of the tool adds its efficiency, at least 0.2. Not ticks. */
    @ZenMethod
    public GridBuilder time(int time) {
        this.time = time;
        return this;
    }

    /** The anvil gives the result hot. */
    @ZenMethod
    public GridBuilder hot() {
        return hot(true);
    }

    /** For the positional methods, which pass hot as a flag. */
    public GridBuilder hot(boolean hot) {
        if (hot && station != Station.ANVIL) {
            problems.add("only the anvil gives hot results");
        }
        this.hot = hot;
        return this;
    }

    /** A shaped recipe's ingredients as a grid, rows of cells with null for empty, as the positional methods give. */
    public GridBuilder cells(IIngredient[][] grid) {
        if (!shaped || pattern != null) {
            problems.add("cells are for shaped recipes without a pattern");
        }
        this.cells = grid;
        return this;
    }

    @ZenMethod
    public GridBuilder sound(String sound) {
        this.sound = sound == null ? "" : sound;
        return this;
    }

    @ZenMethod
    public GridBuilder experience(double experience) {
        this.experience = experience;
        return this;
    }

    /** How dirty the kitchen bench gets; without it the bench's default for the time. */
    @ZenMethod
    public GridBuilder dirt(double dirt) {
        if (station != Station.KITCHEN) {
            problems.add("only the kitchen bench gets dirty");
        }
        this.dirt = dirt;
        this.dirtGiven = true;
        return this;
    }

    /** Ahead of recipes with a lower priority when several match. */
    @ZenMethod
    public GridBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    /** The pattern may sit anywhere in the grid and match mirrored, as MineFantasy's own recipes do. */
    @ZenMethod
    public GridBuilder anywhere() {
        if (!shaped) {
            problems.add("anywhere is for shaped recipes; shapeless ones already go anywhere");
        }
        this.anywhere = true;
        return this;
    }

    /**
     * Checks what the script wrote and adds the recipe, or reports every problem and adds nothing. Whether the recipe
     * itself is sound, its tiers, time, tool, research and fit, the native recipe checks as it is built.
     */
    @ZenMethod
    public void register() {
        RecipeId id = ScriptRecipes.scriptId(station.registry().getStation(), name);
        List<String> found = new ArrayList<String>(problems);
        if (registered) {
            found.add("register is called twice");
        }
        registered = true;
        IIngredient[][] grid = null;
        if (shaped && cells != null) {
            grid = cells;
            checkCells(found);
        } else if (shaped) {
            grid = checkPattern(found);
        } else {
            checkIngredients(found);
        }
        if (!found.isEmpty()) {
            for (String problem : found) {
                MineTweakerAPI.logError(id + ": " + problem + ". " + ScriptRecipes.NOTHING_CHANGED);
            }
            return;
        }
        RecipeRegistry<GridRecipe> registry = station.registry();
        GridRecipe.Grid kind = station == Station.ANVIL ? GridRecipe.Grid.ANVIL : GridRecipe.Grid.BENCH;
        IIngredient[][] pattern = grid;
        ScriptRecipes.apply("Adding " + registry.getStation() + " recipe " + id, tx -> {
            GridRecipe.Builder builder = shaped ? TweakedIngredients.shaped(kind, pattern, output, !anywhere)
                    : TweakedIngredients.shapeless(kind, ingredients, output);
            builder.tool(tool, toolTier).time(time).research(research).skill(ScriptRecipes.skill(skill)).sound(sound)
                    .experience((float) experience);
            if (station != Station.KITCHEN) {
                builder.stationTier(stationTier);
            } else {
                // Without dirt the bench's default for the time; a given one goes as it is, so the recipe refuses a
                // negative
                builder.dirtyAmount(dirtGiven ? (float) dirt : NativeGridRecipe.kitchenDirt(time));
            }
            if (station == Station.ANVIL) {
                builder.hot(hot);
            }
            GridRecipe recipe = builder.build();
            // Anvil recipes order by size within a priority, shaped ahead of shapeless
            int order = station == Station.ANVIL ? (shaped ? 1000 : 0) + recipe.getRecipeSize() + priority * 10000
                    : priority;
            tx.add(registry, id, recipe, order);
        });
    }

    private void checkCells(List<String> found) {
        boolean filled = false;
        for (int y = 0; y < cells.length; y++) {
            IIngredient[] row = cells[y];
            for (int x = 0; row != null && x < row.length; x++) {
                if (row[x] != null) {
                    filled = true;
                    checkIngredient("input at row " + (y + 1) + ", column " + (x + 1), row[x], found);
                }
            }
        }
        if (!filled) {
            found.add("the pattern has no ingredients");
        }
    }

    /** The pattern as an ingredient grid, every character keyed and every key used. */
    private IIngredient[][] checkPattern(List<String> found) {
        if (pattern == null || pattern.length == 0) {
            found.add("no pattern");
            return null;
        }
        int width = pattern[0] == null ? 0 : pattern[0].length();
        IIngredient[][] grid = new IIngredient[pattern.length][width];
        List<Character> used = new ArrayList<Character>();
        boolean filled = false;
        for (int y = 0; y < pattern.length; y++) {
            String row = pattern[y];
            if (row == null || row.length() != width) {
                found.add("pattern row " + (y + 1) + " is not " + width + " wide like the first");
                continue;
            }
            for (int x = 0; x < width; x++) {
                char symbol = row.charAt(x);
                if (symbol == ' ') {
                    continue;
                }
                IIngredient ingredient = keys.get(symbol);
                if (ingredient == null) {
                    if (!used.contains(symbol)) {
                        found.add("pattern symbol " + symbol + " has no key");
                    }
                } else {
                    grid[y][x] = ingredient;
                    filled = true;
                }
                if (!used.contains(symbol)) {
                    used.add(symbol);
                }
            }
        }
        if (!filled) {
            found.add("the pattern has no ingredients");
        }
        for (Map.Entry<Character, IIngredient> key : keys.entrySet()) {
            if (!used.contains(key.getKey())) {
                found.add("key " + key.getKey() + " is not used in the pattern");
            }
            checkIngredient("input " + key.getKey(), key.getValue(), found);
        }
        return grid;
    }

    private void checkIngredients(List<String> found) {
        if (ingredients == null || ingredients.length == 0) {
            found.add("no ingredients");
            return;
        }
        for (int i = 0; i < ingredients.length; i++) {
            if (ingredients[i] == null) {
                found.add("ingredient " + (i + 1) + " is null");
            } else {
                checkIngredient("ingredient " + (i + 1), ingredients[i], found);
            }
        }
    }

    private void checkIngredient(String which, IIngredient ingredient, List<String> found) {
        if (ingredient.hasTransformers()) {
            found.add(
                    which + " has transformers (such as reuse or transformDamage), which the "
                            + station.name
                            + " does not run");
        }
    }
}
