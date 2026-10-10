package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.recipe.Input;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.oredict.IOreDictEntry;

/** Translates CraftTweaker ingredients into recipe {@link Input}s. */
public final class ScriptInputs {

    private ScriptInputs() {}

    /**
     * An ore entry stays an ore name, resolved at lookup time, and {@code MF.input} and {@code MF.carbon} keep their
     * native rules. A plain stack is its item, checked by the stack itself; alternatives and conditions are checked by
     * the ingredient alone, its listed items only showing what it takes.
     */
    public static Input toInput(IIngredient ingredient) {
        try {
            return convert(ingredient);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("input: " + e.getMessage(), e);
        }
    }

    private static Input convert(IIngredient ingredient) {
        if (ingredient == null) {
            throw new IllegalArgumentException("Ingredient must not be null");
        }
        ScriptWarnings.inputTag(ingredient);
        TweakedIngredients.requireNoTransformers(ingredient);
        int amount = Math.max(1, ingredient.getAmount());
        if (ingredient instanceof ExplicitIngredient) {
            return ((ExplicitIngredient) ingredient).toInput();
        }
        if (ingredient instanceof IOreDictEntry) {
            return Input.ore(((IOreDictEntry) ingredient).getName()).amount(amount);
        }
        if (ingredient instanceof IItemStack) {
            ItemStack stack = MineTweakerMC.getItemStack((IItemStack) ingredient);
            if (stack == null || stack.getItem() == null) {
                throw new IllegalArgumentException("Ingredient " + ingredient + " is not an item");
            }
            return Input.of(stack.getItem(), stack.getItemDamage()).amount(amount)
                    .where(item -> ingredient.matches(MineTweakerMC.getIItemStack(item)), String.valueOf(ingredient));
        }
        // Alternatives and conditions are judged by the ingredient itself when looked up; the items it lists now only
        // show what it takes, so a part that grows later, such as MF.carbon(), still counts
        return Input.matching(
                item -> ingredient.matches(MineTweakerMC.getIItemStack(item)),
                () -> listed(ingredient),
                String.valueOf(ingredient)).amount(amount);
    }

    private static List<ItemStack> listed(IIngredient ingredient) {
        List<ItemStack> stacks = new ArrayList<>();
        for (IItemStack item : TweakedIngredients.items(ingredient)) {
            ItemStack stack = MineTweakerMC.getItemStack(item);
            if (stack != null && stack.getItem() != null) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    /** A script output, checked. */
    public static ItemStack toOutput(IItemStack output) {
        ItemStack stack = MineTweakerMC.getItemStack(output);
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("output " + output + " is not an item");
        }
        return stack;
    }
}
