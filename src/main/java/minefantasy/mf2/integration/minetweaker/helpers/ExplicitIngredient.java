package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.recipe.Input;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemCondition;
import minetweaker.api.item.IItemStack;
import minetweaker.api.item.IItemTransformer;
import minetweaker.api.item.IngredientOr;
import minetweaker.api.liquid.ILiquidStack;
import minetweaker.api.minecraft.MineTweakerMC;
import minetweaker.api.player.IPlayer;

/**
 * A MineFantasy script ingredient that states what it accepts instead of leaving the adapters to guess it from probes:
 * the rule is known, and plain CraftTweaker conditions added with {@code only} are kept on top. The amount is a count,
 * checked by the station, so a bigger stack still matches. Transformers are refused, as MineFantasy recipes never run
 * them.
 */
public abstract class ExplicitIngredient implements IIngredient {

    private final int amount;
    private final String mark;
    private final List<IItemCondition> conditions;

    protected ExplicitIngredient(int amount, String mark, List<IItemCondition> conditions) {
        this.amount = Math.max(1, amount);
        this.mark = mark;
        this.conditions = Collections.unmodifiableList(new ArrayList<IItemCondition>(conditions));
    }

    /** The native input: the rule as stations check it, with the amount and the added conditions on top. */
    public final Input toInput() {
        Input input = rule().amount(getAmount());
        return conditions.isEmpty() ? input : input.where(this::meetsConditions, toString());
    }

    /** The rule alone, amount and conditions aside. */
    protected abstract Input rule();

    /** How the script would write this ingredient, amount aside. */
    protected abstract String describe();

    protected abstract ExplicitIngredient with(int amount, String mark, List<IItemCondition> conditions);

    /** The plain CraftTweaker conditions added on top of the rule. */
    public final List<IItemCondition> getConditions() {
        return conditions;
    }

    /** Whether the stack passes the conditions added with {@code only}. */
    public final boolean meetsConditions(ItemStack stack) {
        IItemStack item = MineTweakerMC.getIItemStack(stack);
        for (IItemCondition condition : conditions) {
            if (!condition.matches(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final boolean matches(IItemStack item) {
        ItemStack stack = item == null ? null : MineTweakerMC.getItemStack(item);
        return stack != null && stack.getItem() != null
                && stack.stackSize >= amount
                && rule().matches(stack)
                && meetsConditions(stack);
    }

    @Override
    public final boolean matchesExact(IItemStack item) {
        return matches(item);
    }

    @Override
    public final boolean matches(ILiquidStack liquid) {
        return false;
    }

    @Override
    public final boolean contains(IIngredient ingredient) {
        List<IItemStack> items = TweakedIngredients.items(ingredient);
        if (items.isEmpty()) {
            return false;
        }
        for (IItemStack item : items) {
            if (!matches(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final String getMark() {
        return mark;
    }

    @Override
    public final int getAmount() {
        return amount;
    }

    @Override
    public final List<ILiquidStack> getLiquids() {
        return Collections.emptyList();
    }

    @Override
    public final IIngredient amount(int amount) {
        return with(amount, mark, conditions);
    }

    @Override
    public final IIngredient marked(String mark) {
        return with(amount, mark, conditions);
    }

    @Override
    public final IIngredient only(IItemCondition condition) {
        List<IItemCondition> more = new ArrayList<IItemCondition>(conditions);
        more.add(condition);
        return with(amount, mark, more);
    }

    @Override
    public final IIngredient or(IIngredient other) {
        return new IngredientOr(this, other);
    }

    @Override
    public final IIngredient transform(IItemTransformer transformer) {
        throw new IllegalArgumentException(describe() + ": MineFantasy recipes do not run ingredient transformers");
    }

    @Override
    public final boolean hasTransformers() {
        return false;
    }

    @Override
    public final IItemStack applyTransform(IItemStack item, IPlayer player) {
        return item;
    }

    @Override
    public final Object getInternal() {
        return this;
    }

    @Override
    public final String toString() {
        String text = describe() + (conditions.isEmpty() ? "" : ".only(...)");
        return amount > 1 ? text + " * " + amount : text;
    }
}
