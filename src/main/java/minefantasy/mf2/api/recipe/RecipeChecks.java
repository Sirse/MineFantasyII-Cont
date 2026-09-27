package minefantasy.mf2.api.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.material.CustomMaterial;

/**
 * Value checks recipes run on themselves when they are registered. Each throws IllegalArgumentException naming the
 * value; the registry adds the recipe id, its source and the station.
 */
public final class RecipeChecks {

    /** A recipe that checks its own values; the registry calls it on every add and replace. */
    public interface Validated {

        void validate();
    }

    private RecipeChecks() {}

    public static void require(boolean ok, String problem) {
        if (!ok) {
            throw new IllegalArgumentException(problem);
        }
    }

    public static void positive(String what, int value) {
        require(value > 0, what + " must be positive, got " + value);
    }

    public static void notNegative(String what, int value) {
        require(value >= 0, what + " must not be negative, got " + value);
    }

    /** A tier: -1 for any, otherwise 0 or more. */
    public static void tier(String what, int value) {
        require(value >= -1, what + " must be -1 (any) or more, got " + value);
    }

    /**
     * A research the player must have, if any: it must be one the mod knows. An unknown name would count as known to
     * everyone, so a misspelt research would quietly lift the requirement.
     */
    public static void research(String what, String name) {
        if (name != null && !name.isEmpty()) {
            require(ResearchLogic.getResearch(name) != null, what + " '" + name + "' is not a known research");
        }
    }

    public static void notEmpty(String what, String value) {
        require(value != null && !value.isEmpty(), what + " must be given");
    }

    /** A produced stack: a registered item, at least one of it, known materials. */
    public static void output(String what, ItemStack stack) {
        require(stack != null && stack.getItem() != null, what + " must be an item");
        positive(what + " size", stack.stackSize);
        materials(what, stack);
    }

    /** An ingredient stack: as {@link #output}, with a positive amount. */
    public static void ingredient(String what, ItemStack stack) {
        output(what, stack);
    }

    /** Every material named in the stack's material tag must exist. */
    public static void materials(String what, ItemStack stack) {
        NBTTagCompound tag = stack == null ? null : CustomMaterial.getNBT(stack, false);
        if (tag == null) {
            return;
        }
        for (String slot : new String[] { CustomToolHelper.slot_main, CustomToolHelper.slot_haft }) {
            if (tag.hasKey(slot)) {
                material(what + " " + slot, tag.getString(slot));
            }
        }
    }

    public static void material(String what, String name) {
        require(CustomMaterial.getMaterial(name) != null, what + " names an unknown material '" + name + "'");
    }

    /** An input: a positive amount and known materials in its rules. */
    public static void input(String what, Input input) {
        require(input != null, what + " must be given");
        positive(what + " amount", input.getAmount());
        if (input.getMaterial().getName() != null) {
            material(what + " material", input.getMaterial().getName());
        }
        if (input.getHaft().getName() != null) {
            material(what + " haft", input.getHaft().getName());
        }
    }

    /** A temperature band in degrees: -1 takes that bound from the item's material. */
    public static void temperatures(int... ascending) {
        int previous = Integer.MIN_VALUE;
        for (int value : ascending) {
            if (value == -1) {
                continue;
            }
            positive("temperature", value);
            require(value >= previous, "temperatures must rise, got " + java.util.Arrays.toString(ascending));
            previous = value;
        }
    }

    /**
     * A grid layout: a shaped pattern must fit the station grid; a shapeless recipe needs between one ingredient and a
     * full grid.
     */
    public static void grid(int patternWidth, int patternHeight, int ingredients, int gridWidth, int gridHeight) {
        if (patternWidth > 0 || patternHeight > 0) {
            require(
                    patternWidth > 0 && patternHeight > 0 && patternWidth <= gridWidth && patternHeight <= gridHeight,
                    "pattern " + patternWidth
                            + "x"
                            + patternHeight
                            + " does not fit the "
                            + gridWidth
                            + "x"
                            + gridHeight
                            + " grid");
        }
        require(
                ingredients > 0 && ingredients <= gridWidth * gridHeight,
                "needs 1 to " + gridWidth * gridHeight + " ingredients, has " + ingredients);
    }
}
