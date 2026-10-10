package minefantasy.mf2.api.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.RecipeChecks;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeRegistry;
import minefantasy.mf2.api.rpg.Skill;

/**
 * A recipe of the anvil, carpenter's bench or kitchen bench, native or scripted. It is immutable, and {@link #match} is
 * a pure function of the grid: it gives the result, what each slot pays and the requirements in force, worked out from
 * the parts for a recipe whose tiers and time come from its materials. The station builds its project from the match;
 * nothing is pushed back onto it.
 */
public final class GridRecipe implements RecipeChecks.Validated {

    /** The station grid a recipe is laid out on. */
    public enum Grid {

        /** 6x4; hot pieces are judged by the item they carry and must be at working heat. */
        ANVIL(6, 4, true),
        /** 4x4 of the carpenter's and kitchen benches. */
        BENCH(4, 4, false);

        public final int width;
        public final int height;
        public final boolean heat;

        Grid(int width, int height, boolean heat) {
            this.width = width;
            this.height = height;
            this.heat = heat;
        }
    }

    /** Where a recipe's tiers and time come from. */
    public enum Tiers {
        /** The recipe's own values. */
        FIXED,
        /**
         * The parts' material: every metal part shares one metal and every wooden part one wood, a tier of -1 takes the
         * material's, the time scales with it, and the result gets the materials.
         */
        MATERIAL
    }

    /** What the grid makes and on which terms, as {@link #match} found it. */
    public static final class Match {

        private final GridRecipe recipe;
        private final int[] amounts;
        private final ItemStack result;
        private final int time;
        private final int toolTier;
        private final int stationTier;
        private final String research;

        private Match(GridRecipe recipe, int[] amounts, ItemStack result, int time, int toolTier, int stationTier,
                String research) {
            this.recipe = recipe;
            this.amounts = amounts;
            this.result = result;
            this.time = time;
            this.toolTier = toolTier;
            this.stationTier = stationTier;
            this.research = research;
        }

        public GridRecipe getRecipe() {
            return recipe;
        }

        /** Items each grid slot pays, indexed {@code col + row * gridWidth}. */
        public int[] getAmounts() {
            return amounts.clone();
        }

        public ItemStack getResult() {
            return result.copy();
        }

        public int getTime() {
            return time;
        }

        public int getToolTier() {
            return toolTier;
        }

        public int getStationTier() {
            return stationTier;
        }

        /** Research needed, or "" for none. */
        public String getResearch() {
            return research;
        }
    }

    private final Grid grid;
    private final boolean shaped;
    private final boolean anchored;
    private final int width;
    private final int height;
    private final Object[] entries;
    private final GridMatch.Cell[] cells;
    /** The cells as given, before anvil heat handling: what each would take cold. */
    private final GridMatch.Cell[] coldCells;
    private final ItemStack output;
    private final Tiers tiers;
    private final String tool;
    private final int toolTier;
    private final int stationTier;
    private final int time;
    private final boolean hot;
    private final String research;
    private final Skill skill;
    private final String sound;
    private final float experience;
    private final float dirtyAmount;

    private GridRecipe(Builder b) {
        this.grid = b.grid;
        this.shaped = b.shaped;
        this.anchored = b.anchored;
        this.width = b.width;
        this.height = b.height;
        this.entries = b.entries;
        this.cells = b.cells;
        this.coldCells = b.coldCells;
        this.output = b.output;
        this.tiers = b.tiers;
        this.tool = b.tool;
        this.toolTier = b.toolTier;
        this.stationTier = b.stationTier;
        this.time = b.time;
        this.hot = b.hot;
        this.research = b.research;
        this.skill = b.skill;
        this.sound = b.sound;
        this.experience = b.experience;
        this.dirtyAmount = b.dirtyAmount;
    }

    // region building

    /**
     * A shaped pattern of {@code width * height} entries, row by row. Each entry is an ItemStack (null for an empty
     * cell) or any other object paired with its cell, such as a script ingredient; {@code cells} may be null when every
     * entry is a stack.
     */
    public static Builder shaped(Grid grid, int width, int height, Object[] entries, GridMatch.Cell[] cells,
            ItemStack output) {
        if (entries == null || width < 1 || height < 1 || entries.length != width * height) {
            throw new IllegalArgumentException(
                    "A " + width
                            + "x"
                            + height
                            + " pattern needs "
                            + width * height
                            + " cells, got "
                            + (entries == null ? "none" : entries.length));
        }
        return new Builder(grid, true, width, height, entries, cells, output);
    }

    /** Shapeless ingredients: stacks, or other objects paired with their cells. */
    public static Builder shapeless(Grid grid, Object[] entries, GridMatch.Cell[] cells, ItemStack output) {
        if (entries == null) {
            throw new IllegalArgumentException("A shapeless recipe needs its ingredients");
        }
        return new Builder(grid, false, 0, 0, entries, cells, output);
    }

    public static final class Builder {

        private final Grid grid;
        private final boolean shaped;
        private final int width;
        private final int height;
        private final Object[] entries;
        private final GridMatch.Cell[] cells;
        private final GridMatch.Cell[] coldCells;
        private final ItemStack output;
        private boolean anchored;
        private Tiers tiers = Tiers.FIXED;
        private String tool = "hands";
        private int toolTier = -1;
        private int stationTier = -1;
        private int time = 1;
        private boolean hot;
        private String research = "";
        private Skill skill;
        private String sound = "";
        private float experience;
        private float dirtyAmount;

        private Builder(Grid grid, boolean shaped, int width, int height, Object[] entries, GridMatch.Cell[] cells,
                ItemStack output) {
            this.grid = grid;
            this.shaped = shaped;
            this.width = width;
            this.height = height;
            this.entries = new Object[entries.length];
            this.cells = new GridMatch.Cell[entries.length];
            this.coldCells = new GridMatch.Cell[entries.length];
            for (int i = 0; i < entries.length; i++) {
                Object entry = entries[i];
                this.entries[i] = entry instanceof ItemStack ? ((ItemStack) entry).copy() : entry;
                GridMatch.Cell cell = cells != null && cells[i] != null ? cells[i]
                        : entry instanceof ItemStack
                                ? GridMatch.stack((ItemStack) this.entries[i], shaped ? stackSize(entry) : 1)
                                : null;
                if (entry != null && cell == null) {
                    throw new IllegalArgumentException("No cell for recipe entry " + entry);
                }
                this.coldCells[i] = cell;
                // Native anvil stacks are judged hot; script cells bring their own heat handling
                this.cells[i] = grid.heat && entry instanceof ItemStack && cell != null ? GridMatch.anvil(cell) : cell;
            }
            this.output = output == null ? null : output.copy();
        }

        private static int stackSize(Object entry) {
            return Math.max(1, ((ItemStack) entry).stackSize);
        }

        /** A shaped pattern that must sit in the grid's top left corner and is never mirrored, as scripts expect. */
        public Builder anchored() {
            this.anchored = true;
            return this;
        }

        public Builder tool(String type, int tier) {
            this.tool = type;
            this.toolTier = tier;
            return this;
        }

        public Builder stationTier(int tier) {
            this.stationTier = tier;
            return this;
        }

        public Builder time(int time) {
            this.time = time;
            return this;
        }

        public Builder hot(boolean hot) {
            this.hot = hot;
            return this;
        }

        /** Research needed; "tier" asks for the research of the parts' metal (with {@link Tiers#MATERIAL}). */
        public Builder research(String research) {
            this.research = research == null ? "" : research;
            return this;
        }

        public Builder skill(Skill skill) {
            this.skill = skill;
            return this;
        }

        public Builder sound(String sound) {
            this.sound = sound == null ? "" : sound;
            return this;
        }

        public Builder experience(float experience) {
            this.experience = experience;
            return this;
        }

        public Builder dirtyAmount(float dirtyAmount) {
            this.dirtyAmount = dirtyAmount;
            return this;
        }

        public Builder tiers(Tiers tiers) {
            this.tiers = tiers;
            return this;
        }

        public GridRecipe build() {
            return new GridRecipe(this);
        }
    }

    // endregion

    // region matching

    /** A recipe a grid holds: its entry in the registry, id and all, and what the grid makes by it. */
    public static final class Found {

        private final RecipeEntry<GridRecipe> entry;
        private final Match match;

        private Found(RecipeEntry<GridRecipe> entry, Match match) {
            this.entry = entry;
            this.match = match;
        }

        public RecipeId getId() {
            return entry.getId();
        }

        public GridRecipe getRecipe() {
            return entry.getRecipe();
        }

        public Match getMatch() {
            return match;
        }
    }

    /** The first published recipe of the registry the grid holds, in lookup order; null for none. */
    public static Found find(RecipeRegistry<GridRecipe> registry, InventoryCrafting grid) {
        for (RecipeEntry<GridRecipe> entry : registry.published().all()) {
            Match match = entry.getRecipe().match(grid);
            if (match != null) {
                return new Found(entry, match);
            }
        }
        return null;
    }

    /** What the grid makes by this recipe, or null when it does not hold it. */
    public Match match(InventoryCrafting grid) {
        int[] amounts = shaped
                ? GridMatch.shaped(grid, this.grid.width, this.grid.height, cells, width, height, !anchored, !anchored)
                : GridMatch.shapeless(grid, this.grid.width, this.grid.height, filledCells());
        if (amounts == null) {
            return null;
        }
        String ownResearch = "tier".equalsIgnoreCase(research) ? "" : research;
        if (tiers == Tiers.FIXED) {
            return new Match(this, amounts, output.copy(), time, toolTier, stationTier, ownResearch);
        }
        String metal = null;
        String wood = null;
        for (int i = 0; i < grid.getSizeInventory(); i++) {
            ItemStack item = grid.getStackInSlot(i);
            String itemMetal = CustomToolHelper.getComponentMaterial(item, "metal");
            String itemWood = CustomToolHelper.getComponentMaterial(item, "wood");
            if (itemMetal != null) {
                if (metal != null && !metal.equalsIgnoreCase(itemMetal)) {
                    return null;
                }
                metal = itemMetal;
            }
            if (itemWood != null) {
                if (wood != null && !wood.equalsIgnoreCase(itemWood)) {
                    return null;
                }
                wood = itemWood;
            }
        }
        ItemStack result = output.copy();
        if (metal != null) {
            CustomMaterial.addMaterial(result, CustomToolHelper.slot_main, metal);
        }
        if (wood != null) {
            // A wooden tool without metal takes its wood as the main material on the bench
            String slot = metal == null && this.grid == Grid.BENCH ? CustomToolHelper.slot_main
                    : CustomToolHelper.slot_haft;
            CustomMaterial.addMaterial(result, slot, wood);
        }
        CustomMaterial material = CustomMaterial.getMaterial(metal != null ? metal : wood);
        if (material == null) {
            return new Match(this, amounts, result, time, toolTier, stationTier, ownResearch);
        }
        boolean smithed = metal != null && this.grid == Grid.ANVIL;
        return new Match(
                this,
                amounts,
                result,
                (int) (time * material.craftTimeModifier),
                toolTier < 0 ? material.crafterTier : toolTier,
                stationTier < 0 && this.grid == Grid.ANVIL ? material.crafterAnvilTier : stationTier,
                smithed ? "smelt" + material.getName() : ownResearch);
    }

    private GridMatch.Cell[] filledCells() {
        List<GridMatch.Cell> filled = new ArrayList<>();
        for (GridMatch.Cell cell : cells) {
            if (cell != null) {
                filled.add(cell);
            }
        }
        return filled.toArray(new GridMatch.Cell[0]);
    }

    public boolean matches(InventoryCrafting grid) {
        return match(grid) != null;
    }

    // endregion

    // region definition

    public Grid getGrid() {
        return grid;
    }

    public boolean isShaped() {
        return shaped;
    }

    /** Pattern width, or 0 for a shapeless recipe. */
    public int getWidth() {
        return width;
    }

    /** Pattern height, or 0 for a shapeless recipe. */
    public int getHeight() {
        return height;
    }

    /**
     * The entries as registered: row by row for a shaped recipe, null for empty cells. Stacks are copies; other entries
     * (script ingredients) are shared and must not be changed.
     */
    public List<Object> getEntries() {
        List<Object> copy = new ArrayList<>(entries.length);
        for (Object entry : entries) {
            copy.add(entry instanceof ItemStack ? ((ItemStack) entry).copy() : entry);
        }
        return Collections.unmodifiableList(copy);
    }

    /**
     * Whether one of the recipe's cells would take this stack, given as many as the cell asks for. Anvil cells are
     * asked about the stack itself, as it goes into the forge, not about it hot.
     */
    public boolean takes(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        for (GridMatch.Cell cell : coldCells) {
            if (cell != null) {
                ItemStack probe = stack.copy();
                probe.stackSize = Math.max(stack.stackSize, cell.amount());
                if (cell.accepts(probe)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** One entry, as {@link #getEntries} gives it. */
    public Object getEntry(int index) {
        Object entry = entries[index];
        return entry instanceof ItemStack ? ((ItemStack) entry).copy() : entry;
    }

    /** Filled cells the recipe needs; shaped recipes order larger ones first. */
    public int getRecipeSize() {
        return shaped ? width * height : entries.length;
    }

    public ItemStack getRecipeOutput() {
        return output == null ? null : output.copy();
    }

    public Tiers getTiers() {
        return tiers;
    }

    public int getCraftTime() {
        return time;
    }

    public int getRecipeHammer() {
        return toolTier;
    }

    /** The station tier: the anvil's or the bench's. */
    public int getAnvil() {
        return stationTier;
    }

    public boolean outputHot() {
        return hot;
    }

    public String getToolType() {
        return tool;
    }

    public String getResearch() {
        return research;
    }

    public Skill getSkill() {
        return skill;
    }

    public String getSound() {
        return sound;
    }

    public float getExperiance() {
        return experience;
    }

    public float getDirtyAmount() {
        return dirtyAmount;
    }

    // endregion

    @Override
    public void validate() {
        RecipeChecks.output("output", output);
        RecipeChecks.positive("time", time);
        RecipeChecks.tier("tool tier", toolTier);
        RecipeChecks.tier("station tier", stationTier);
        RecipeChecks.notEmpty("tool", tool);
        RecipeChecks.require(dirtyAmount >= 0, "dirty amount must not be negative");
        // "tier" asks for the research of the parts' metal, worked out when the grid is matched
        if (!"tier".equalsIgnoreCase(research)) {
            RecipeChecks.research("research", research);
        }
        int filled = 0;
        for (int i = 0; i < entries.length; i++) {
            if (cells[i] == null) {
                RecipeChecks.require(shaped, "ingredients must not be empty");
                continue;
            }
            filled++;
            RecipeChecks.positive("ingredient amount", cells[i].amount());
            if (entries[i] instanceof ItemStack) {
                RecipeChecks.ingredient("cell " + i, (ItemStack) entries[i]);
            }
        }
        RecipeChecks.grid(width, height, filled, grid.width, grid.height);
    }
}
