package minefantasy.mf2.api.crafting.exotic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.NativeRecipes;
import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeChecks;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.recipe.RecipeSource;
import minefantasy.mf2.api.recipe.RecipeTransaction;

/**
 * Designs of anvil results: an anvil with a design item (ornate) or a dragon's heat (dragonforged) turns a base result
 * into its special version. Each craft also makes the special item salvage like its base; the two are registered and
 * removed together.
 */
public final class SpecialForging {

    /** The design name of dragonforged crafts. */
    public static final String DRAGONFORGE = "dragonforge";

    private SpecialForging() {}

    /** One design craft. Immutable. */
    public static final class SpecialCraft implements RecipeChecks.Validated {

        @Override
        public void validate() {
            RecipeChecks.notEmpty("design", design);
            RecipeChecks.require(base != null && output != null, "needs a base and an output item");
        }

        private final String design;
        private final Item base;
        private final Item output;

        public SpecialCraft(String design, Item base, Item output) {
            if (design == null || design.isEmpty() || base == null || output == null) {
                throw new IllegalArgumentException("A special craft needs a design, a base and an output");
            }
            this.design = design;
            this.base = base;
            this.output = output;
        }

        public String getDesign() {
            return design;
        }

        public Item getBase() {
            return base;
        }

        public Item getOutput() {
            return output;
        }

        public Set<Object> indexKeys() {
            return Input.of(base).indexKeys();
        }
    }

    public static RecipeId idFor(String design, Item base) {
        return NativeRecipes.idFor("special_forging", base).withSuffix("." + design.toLowerCase());
    }

    /** Stages the craft and the output's salvage alias; a second craft for the same design and base replaces it. */
    public static void stage(RecipeTransaction tx, SpecialCraft craft, int priority) {
        tx.set(MFRecipes.SPECIAL_FORGING, idFor(craft.design, craft.base), craft, priority);
        Salvage.stageAlias(tx, craft.output, craft.base);
    }

    /** Stages removal of the craft and of the salvage alias it added. */
    public static void stageRemove(RecipeTransaction tx, RecipeId id) {
        RecipeEntry<SpecialCraft> entry = MFRecipes.SPECIAL_FORGING.workingEntry(id);
        tx.remove(MFRecipes.SPECIAL_FORGING, id);
        if (entry != null) {
            RecipeId alias = Salvage.aliasId(entry.getRecipe().output);
            if (MFRecipes.SALVAGE.containsWorking(alias)) {
                tx.remove(MFRecipes.SALVAGE, alias);
            }
        }
    }

    public static void addDragonforgeCraft(Block blackSteel, Block dragon) {
        addDragonforgeCraft(Item.getItemFromBlock(blackSteel), Item.getItemFromBlock(dragon));
    }

    public static void addDragonforgeCraft(Item base, Item dragon) {
        addSpecialCraft(DRAGONFORGE, base, dragon);
    }

    public static void addSpecialCraft(String special, Item base, Item output) {
        try (RecipeTransaction tx = MFRecipes.REGISTRIES.begin(RecipeSource.NATIVE)) {
            stage(tx, new SpecialCraft(special, base, output), 0);
            tx.commit();
        }
    }

    public static Item getDragonCraft(ItemStack blacksteel) {
        return getSpecialCraft(DRAGONFORGE, blacksteel);
    }

    public static Item getSpecialCraft(String special, ItemStack input) {
        if (input == null || input.getItem() == null || special == null) {
            return null;
        }
        for (RecipeEntry<SpecialCraft> entry : MFRecipes.SPECIAL_FORGING.published()
                .candidates(Input.lookupKeys(input))) {
            SpecialCraft craft = entry.getRecipe();
            if (craft.design.equals(special) && craft.base == input.getItem()) {
                return craft.output;
            }
        }
        return null;
    }

    /** Base item to dragonforged item, in lookup order, for display. */
    public static Map<Item, Item> dragonforgeCrafts() {
        Map<Item, Item> crafts = new LinkedHashMap<>();
        for (RecipeEntry<SpecialCraft> entry : MFRecipes.SPECIAL_FORGING.published().all()) {
            if (entry.getRecipe().design.equals(DRAGONFORGE)) {
                crafts.putIfAbsent(entry.getRecipe().base, entry.getRecipe().output);
            }
        }
        return crafts;
    }

    public static String getItemDesign(ItemStack item) {
        if (item != null && item.getItem() instanceof ISpecialCraftItem) {
            return ((ISpecialCraftItem) item.getItem()).getDesign(item);
        }
        return null;
    }
}
