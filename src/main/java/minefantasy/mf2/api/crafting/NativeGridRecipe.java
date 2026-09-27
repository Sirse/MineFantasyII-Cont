package minefantasy.mf2.api.crafting;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.anvil.CraftingManagerAnvil;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.config.ConfigKitchen;

/**
 * A native recipe for the anvil, the carpenter's bench or the kitchen bench, set up term by term and registered by its
 * pattern:
 *
 * <pre>
 * MineFantasyAPI.anvilRecipe(new ItemStack(bar)).skill(artisanry).tool("hammer", 1).stationTier(1).time(200).hot()
 *         .shaped("II", 'I', ingot);
 * </pre>
 *
 * A term left out takes the station's default: any tool tier and station tier, bare hands on the benches and a hammer
 * on the anvil, one unit of work, no research, skill or sound. The pattern is read as by {@link RecipePattern}.
 */
public final class NativeGridRecipe {

    private enum Station {
        ANVIL,
        CARPENTER,
        KITCHEN
    }

    private final Station station;
    private final ItemStack result;
    private Skill skill;
    private String research = "";
    private boolean hot;
    private String tool;
    private int toolTier = -1;
    private int stationTier = -1;
    private int time = 1;
    private String sound = "";
    private GridRecipe.Tiers tiers = GridRecipe.Tiers.FIXED;
    private Float dirtyAmount;

    private NativeGridRecipe(Station station, ItemStack result, String tool) {
        if (result == null) {
            throw new IllegalArgumentException("A recipe needs a result");
        }
        this.station = station;
        this.result = result.copy();
        this.tool = tool;
    }

    public static NativeGridRecipe anvil(ItemStack result) {
        return new NativeGridRecipe(Station.ANVIL, result, "hammer");
    }

    public static NativeGridRecipe carpenter(ItemStack result) {
        return new NativeGridRecipe(Station.CARPENTER, result, "hands");
    }

    /**
     * A kitchen bench recipe; when the bench is disabled in the config it goes to the carpenter's bench instead, so the
     * food stays obtainable.
     */
    public static NativeGridRecipe kitchen(ItemStack result) {
        return new NativeGridRecipe(Station.KITCHEN, result, "hands");
    }

    public static NativeGridRecipe anvil(Item result) {
        return anvil(new ItemStack(result));
    }

    public static NativeGridRecipe carpenter(Item result) {
        return carpenter(new ItemStack(result));
    }

    /** How dirty a kitchen recipe makes the bench when it does not say: more for longer work. */
    public static float kitchenDirt(int time) {
        return Math.max(1F, Math.min(8F, time * 0.04F));
    }

    public NativeGridRecipe skill(Skill skill) {
        this.skill = skill;
        return this;
    }

    public NativeGridRecipe research(String research) {
        this.research = research == null ? "" : research;
        return this;
    }

    /** The result leaves the anvil hot, to be quenched. */
    public NativeGridRecipe hot() {
        this.hot = true;
        return this;
    }

    public NativeGridRecipe hot(boolean hot) {
        this.hot = hot;
        return this;
    }

    /** The tool type to work it with, such as "hammer" or "knife", and the tier it needs; -1 for any. */
    public NativeGridRecipe tool(String type, int tier) {
        this.tool = type;
        this.toolTier = tier;
        return this;
    }

    /** The anvil or bench tier it needs; -1 for any. The kitchen bench has none. */
    public NativeGridRecipe stationTier(int tier) {
        this.stationTier = tier;
        return this;
    }

    /** The work it takes; each hit does about 100. */
    public NativeGridRecipe time(int time) {
        this.time = time;
        return this;
    }

    /** The sound of each hit, such as "step.wood". */
    public NativeGridRecipe sound(String sound) {
        this.sound = sound == null ? "" : sound;
        return this;
    }

    /**
     * The result takes the material of the parts put in: its time scales with the material, a tier of -1 is the
     * material's, and a result smithed from metal needs the research of smelting it.
     */
    public NativeGridRecipe materialTiers() {
        this.tiers = GridRecipe.Tiers.MATERIAL;
        return this;
    }

    /** How dirty the kitchen bench gets; by default {@link #kitchenDirt} of the time. */
    public NativeGridRecipe dirtyAmount(float dirtyAmount) {
        this.dirtyAmount = dirtyAmount;
        return this;
    }

    /** Registers the recipe with a shaped pattern: rows, then each character with its item. */
    public GridRecipe shaped(Object... pattern) {
        RecipePattern read = RecipePattern.shaped(pattern);
        return register(GridRecipe.shaped(grid(), read.width, read.height, read.cells, null, result));
    }

    /** Registers the recipe with ingredients in any arrangement. */
    public GridRecipe shapeless(Object... ingredients) {
        return register(GridRecipe.shapeless(grid(), RecipePattern.shapeless(ingredients), null, result));
    }

    private GridRecipe.Grid grid() {
        return station == Station.ANVIL ? GridRecipe.Grid.ANVIL : GridRecipe.Grid.BENCH;
    }

    private GridRecipe register(GridRecipe.Builder builder) {
        boolean kitchen = station == Station.KITCHEN && ConfigKitchen.enableBench;
        builder.tool(tool, toolTier).stationTier(stationTier).time(time).hot(hot).research(research).skill(skill)
                .sound(sound).tiers(tiers);
        if (kitchen) {
            builder.dirtyAmount(dirtyAmount != null ? dirtyAmount : kitchenDirt(time));
        }
        GridRecipe recipe = builder.build();
        RecipeRegistry<GridRecipe> registry = station == Station.ANVIL ? MFRecipes.ANVIL
                : kitchen ? MFRecipes.KITCHEN : MFRecipes.CARPENTER;
        int priority = station == Station.ANVIL ? CraftingManagerAnvil.priorityOf(recipe) : 0;
        NativeRecipes.addGrid(registry, recipe.getRecipeOutput(), recipe, priority);
        return recipe;
    }
}
