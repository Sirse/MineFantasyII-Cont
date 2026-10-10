package minefantasy.mf2.integration.minetweaker.tweakers;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.integration.minetweaker.helpers.CarbonInput;
import minefantasy.mf2.integration.minetweaker.helpers.MaterialInput;
import minefantasy.mf2.item.custom.ItemCustomComponent;
import minetweaker.api.item.IIngredient;
import minetweaker.api.item.IItemStack;
import minetweaker.api.minecraft.MineTweakerMC;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * MineFantasy materials without hand-written NBT. {@code stack} makes an item of the given materials, for outputs;
 * {@code input} accepts only items of those materials, for ingredients, where a {@code withTag} stack would accept the
 * item in any material:
 *
 * <pre>
 * mods.minefantasy.MF.stack(&lt;minefantasy2:custom_bar&gt;, "steel")
 * mods.minefantasy.MF.input(&lt;minefantasy2:custom_bar&gt;, "steel") * 2
 * </pre>
 */
@ZenClass("mods.minefantasy.MF")
public class MaterialStacks {

    /** The item made of the main material and, if given, the haft material; unknown names stop the script. */
    @ZenMethod
    public static IItemStack stack(IItemStack item, String main, @Optional String haft) {
        ItemStack base = MineTweakerMC.getItemStack(item);
        if (base == null || base.getItem() == null) {
            throw new IllegalArgumentException("Invalid item " + item);
        }
        ItemStack made = base.copy();
        // Each item kind as MineFantasy writes it, so the stack equals and stacks with crafted ones: parts keep the
        // registered name ("Steel"), tools the lower case one
        CustomMaterial material = known(main);
        boolean part = made.getItem() instanceof ItemCustomComponent;
        CustomMaterial
                .addMaterial(made, CustomToolHelper.slot_main, part ? material.name : material.name.toLowerCase());
        if (haft != null) {
            CustomMaterial.addMaterial(made, CustomToolHelper.slot_haft, known(haft).name.toLowerCase());
        }
        return MineTweakerMC.getIItemStack(made);
    }

    /**
     * The item in the given materials only, other tags allowed. A haft left unnamed is any haft, none included; use
     * {@code inputNoHaft} for an item without one. Conditions added with {@code only} still apply on top.
     */
    @ZenMethod
    public static IIngredient input(IItemStack item, String main, @Optional String haft) {
        IItemStack made = stack(item, main, haft);
        Input.MaterialRule haftRule = haft == null ? Input.MaterialRule.ANY : Input.MaterialRule.is(haft);
        return new MaterialInput(
                MineTweakerMC.getItemStack(made),
                main,
                haftRule,
                "mods.minefantasy.MF.input(" + item
                        + ", \""
                        + main
                        + "\""
                        + (haft == null ? "" : ", \"" + haft + "\"")
                        + ")");
    }

    /** The item in the main material with no haft material at all. */
    @ZenMethod
    public static IIngredient inputNoHaft(IItemStack item, String main) {
        IItemStack made = stack(item, main, null);
        return new MaterialInput(
                MineTweakerMC.getItemStack(made),
                main,
                Input.MaterialRule.NONE,
                "mods.minefantasy.MF.inputNoHaft(" + item + ", \"" + main + "\")");
    }

    /** Any carbon item the bloomery and blast furnace burn, for alloys and recipes that take carbon of any kind. */
    @ZenMethod
    public static IIngredient carbon() {
        return new CarbonInput();
    }

    private static CustomMaterial known(String name) {
        // "any" is how MineFantasy names an item without a material; it is never one a script can ask for
        if (name == null || name.equalsIgnoreCase("any")) {
            throw new IllegalArgumentException(
                    "\"" + name
                            + "\" is not a material: give the plain item to accept any main material, leave the"
                            + " haft out to accept any haft, or use inputNoHaft for none");
        }
        CustomMaterial material = CustomMaterial.getMaterial(name);
        if (material == null) {
            throw new IllegalArgumentException("Unknown MineFantasy material " + name);
        }
        return material;
    }
}
