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
     * An ore entry stays an ore name, resolved at lookup time. Anything else becomes the alternatives it lists, with
     * the ingredient itself as the condition, so NBT, material and transformer checks written in the script hold.
     */
    public static Input toInput(IIngredient ingredient) {
        if (ingredient == null) {
            throw new IllegalArgumentException("Ingredient must not be null");
        }
        int amount = Math.max(1, ingredient.getAmount());
        if (ingredient instanceof IOreDictEntry) {
            return Input.ore(((IOreDictEntry) ingredient).getName()).amount(amount);
        }
        List<Input> alternatives = new ArrayList<>();
        for (IItemStack item : ingredient.getItems()) {
            ItemStack stack = MineTweakerMC.getItemStack(item);
            if (stack != null && stack.getItem() != null) {
                alternatives.add(Input.of(stack.getItem(), stack.getItemDamage()));
            }
        }
        if (alternatives.isEmpty()) {
            throw new IllegalArgumentException("Ingredient " + ingredient + " lists no valid items");
        }
        Input base = alternatives.size() == 1 ? alternatives.get(0) : Input.anyOf(alternatives.toArray(new Input[0]));
        return base.amount(amount)
                .where(stack -> ingredient.matches(MineTweakerMC.getIItemStack(stack)), String.valueOf(ingredient));
    }

    /** A script output, checked. */
    public static ItemStack toOutput(IItemStack output) {
        ItemStack stack = MineTweakerMC.getItemStack(output);
        if (stack == null || stack.getItem() == null) {
            throw new IllegalArgumentException("Invalid output " + output);
        }
        return stack;
    }
}
