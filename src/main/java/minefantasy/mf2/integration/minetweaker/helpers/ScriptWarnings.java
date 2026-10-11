package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import minetweaker.MineTweakerAPI;
import minetweaker.api.data.IData;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;

/** Warnings for script ingredients that do not mean what they look like, logged while the script loads. */
public final class ScriptWarnings {

    /** The old calls already warned about since the last reload: one warning each, not one per recipe. */
    private static final Set<String> DEPRECATED = new HashSet<String>();

    private ScriptWarnings() {}

    /** An old positional call still kept for existing scripts, once per load. */
    public static void deprecated(String api, String instead) {
        if (DEPRECATED.add(api)) {
            MineTweakerAPI.logWarning(api + " is deprecated and will be removed: use " + instead + " instead");
        }
    }

    /** Warns about the old calls again after a reload. */
    public static void resetDeprecated() {
        DEPRECATED.clear();
    }

    /**
     * An input given with {@code withTag}: CraftTweaker matches it by item alone, so the tag does not restrict the
     * input. Conditions ({@code onlyWithTag}, {@code mods.minefantasy.MF.input}) are not item stacks and pass quietly.
     */
    public static void inputTag(IIngredient ingredient) {
        if (ingredient instanceof IItemStack && hasTag((IItemStack) ingredient)) {
            MineTweakerAPI.logWarning(
                    ingredient + " is an input given with withTag. withTag makes a stack with that tag, as an"
                            + " output; as an input it restricts nothing, so the item is taken with any tag. To"
                            + " require the tag use onlyWithTag, and for materials mods.minefantasy.MF.input");
        }
    }

    /** An ingredient an API keeps only the items of: its metadata and tag are dropped (conditions are refused). */
    public static void itemsOnly(String api, IIngredient ingredient) {
        for (IItemStack item : TweakedIngredients.items(ingredient)) {
            ItemStack stack = MineTweakerMC.getItemStack(item);
            if (stack != null && stack.getItemDamage() != 0 && stack.getItemDamage() != OreDictionary.WILDCARD_VALUE) {
                warn(api, ingredient, "its metadata");
                return;
            }
            if (hasTag(item)) {
                warn(api, ingredient, "its tag");
                return;
            }
        }
    }

    private static void warn(String api, IIngredient ingredient, String dropped) {
        MineTweakerAPI.logWarning(api + " keeps only the item of " + ingredient + " and ignores " + dropped);
    }

    private static boolean hasTag(IItemStack stack) {
        IData tag = stack.getTag();
        return tag != null && tag.asMap() != null && !tag.asMap().isEmpty();
    }
}
