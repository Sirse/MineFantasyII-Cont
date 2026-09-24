package minefantasy.mf2.integration.minetweaker.tweakers;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.exotic.SpecialForging.SpecialCraft;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptInputs;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.NotNull;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * Special forging: with a design ("ornate") or dragon's heat ("dragonforge") the anvil turns a base result into the
 * output. The output also salvages like the base. One craft per design and base item; setting it again replaces it.
 */
@ZenClass("mods.minefantasy.SpecialForging")
public class SpecialForging {

    @ZenMethod
    public static void set(@NotNull String design, @NotNull IIngredient base, @NotNull IItemStack output,
            @Optional int priority) {
        ScriptRecipes.apply("Setting " + design + " craft of " + base, tx -> {
            Item outputItem = ScriptInputs.toOutput(output).getItem();
            int count = 0;
            for (Item baseItem : items(base)) {
                minefantasy.mf2.api.crafting.exotic.SpecialForging
                        .stage(tx, new SpecialCraft(design, baseItem, outputItem), priority);
                count++;
            }
            if (count == 0) {
                throw new IllegalArgumentException("Ingredient " + base + " lists no valid items");
            }
        });
    }

    @ZenMethod
    public static void setDragonforge(@NotNull IIngredient base, @NotNull IItemStack output) {
        set(minefantasy.mf2.api.crafting.exotic.SpecialForging.DRAGONFORGE, base, output, 0);
    }

    @ZenMethod
    public static void setOrnate(@NotNull IIngredient base, @NotNull IItemStack output) {
        set("ornate", base, output, 0);
    }

    /** Removes the design's craft for every base item listed, with the salvage alias it added. */
    @ZenMethod
    public static void remove(@NotNull String design, @NotNull IIngredient base) {
        ScriptRecipes.apply("Removing " + design + " craft of " + base, tx -> {
            for (Item baseItem : items(base)) {
                RecipeId id = minefantasy.mf2.api.crafting.exotic.SpecialForging.idFor(design, baseItem);
                minefantasy.mf2.api.crafting.exotic.SpecialForging.stageRemove(tx, id);
            }
        });
    }

    private static java.util.List<Item> items(IIngredient ingredient) {
        java.util.List<Item> items = new java.util.ArrayList<>();
        for (IItemStack stack : ingredient.getItems()) {
            ItemStack mc = MineTweakerMC.getItemStack(stack);
            if (mc != null && mc.getItem() != null && !items.contains(mc.getItem())) {
                items.add(mc.getItem());
            }
        }
        return items;
    }
}
