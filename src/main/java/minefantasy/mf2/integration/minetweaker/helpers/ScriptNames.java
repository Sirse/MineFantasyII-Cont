package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.armour.ArmourDesign;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.exotic.ISpecialCraftItem;
import minefantasy.mf2.api.crafting.exotic.SpecialForging;
import minefantasy.mf2.api.helpers.ItemTraitRules;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeSource;

/** Names a script author can discover without reading native recipe ids. */
public final class ScriptNames {

    public static final String TOOLS = ItemTraitRules.CRAFTER_TYPES;

    private static Set<String> specialDesignNames;
    // Native designs remain valid if a previous script replaced their last recipe.
    private static final Set<String> nativeDesignNames = new TreeSet<String>();

    private ScriptNames() {}

    /** Items and native recipes are discovered at most once per script load, never once per line. */
    public static void resetSpecialDesigns() {
        specialDesignNames = null;
    }

    public static Set<String> specialDesigns() {
        if (specialDesignNames != null) {
            return specialDesignNames;
        }
        Set<String> names = new TreeSet<String>();
        names.add("ornate");
        names.add("dragonforge");
        for (Object value : Item.itemRegistry) {
            if (value instanceof ISpecialCraftItem) {
                String name = ((ISpecialCraftItem) value).getDesign(new ItemStack((Item) value));
                if (name != null && !name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        // Native extensions can supply a design without an ItemSpecialDesign instance.
        for (RecipeEntry<SpecialForging.SpecialCraft> entry : MFRecipes.SPECIAL_FORGING.published().all()) {
            if (entry.getSource().getKind() == RecipeSource.Kind.NATIVE) {
                nativeDesignNames.add(entry.getRecipe().getDesign());
            }
        }
        names.addAll(nativeDesignNames);
        specialDesignNames = Collections.unmodifiableSet(names);
        return specialDesignNames;
    }

    public static String designs() {
        return "Armour designs: " + new TreeSet<String>(ArmourDesign.designs.keySet())
                + "; armour classes: light, medium, heavy; special forging designs: "
                + specialDesigns();
    }

    public static void requireNamedDesign(String design) {
        Set<String> names = specialDesigns();
        if (design == null || !names.contains(design)) {
            throw new IllegalArgumentException("unknown special forging design '" + design + "'; there are " + names);
        }
    }
}
