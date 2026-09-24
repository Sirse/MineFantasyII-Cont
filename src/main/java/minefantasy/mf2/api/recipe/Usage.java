package minefantasy.mf2.api.recipe;

import java.util.function.Function;

import net.minecraft.item.ItemStack;

/** What crafting does to the stack that satisfied an input. */
public final class Usage {

    public enum Kind {
        /** The input's amount is removed. */
        CONSUME,
        /** Checked but left in place. */
        CATALYST,
        /** A single tool loses {@link #getDamage()} durability; a broken tool disappears. */
        DAMAGE,
        /** The amount is removed and as many container items are given back. */
        CONTAINER,
        /** The used part of the stack is replaced by the transform's result. */
        TRANSFORM
    }

    public static final Usage CONSUME = new Usage(Kind.CONSUME, 0, null);
    public static final Usage CATALYST = new Usage(Kind.CATALYST, 0, null);
    public static final Usage CONTAINER = new Usage(Kind.CONTAINER, 0, null);

    private final Kind kind;
    private final int damage;
    private final Function<ItemStack, ItemStack> transform;

    private Usage(Kind kind, int damage, Function<ItemStack, ItemStack> transform) {
        this.kind = kind;
        this.damage = damage;
        this.transform = transform;
    }

    public static Usage damage(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Tool damage must be positive: " + amount);
        }
        return new Usage(Kind.DAMAGE, amount, null);
    }

    /**
     * The function gets a copy of the used part of the stack and returns what replaces it (null for nothing). It must
     * be pure: no side effects, no world access, the same result for the same stack.
     */
    public static Usage transform(Function<ItemStack, ItemStack> transform) {
        if (transform == null) {
            throw new IllegalArgumentException("Transform must not be null");
        }
        return new Usage(Kind.TRANSFORM, 0, transform);
    }

    public Kind getKind() {
        return kind;
    }

    public int getDamage() {
        return damage;
    }

    /** Applies the transform to a copy of {@code used}; returns a fresh stack or null. */
    public ItemStack transform(ItemStack used) {
        if (kind != Kind.TRANSFORM) {
            throw new IllegalStateException("Not a transform usage: " + kind);
        }
        ItemStack result = transform.apply(used.copy());
        return result == null ? null : result.copy();
    }

    @Override
    public String toString() {
        return kind == Kind.DAMAGE ? "DAMAGE(" + damage + ")" : kind.name();
    }
}
