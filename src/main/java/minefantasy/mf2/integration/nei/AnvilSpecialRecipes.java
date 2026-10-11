package minefantasy.mf2.integration.nei;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.exotic.SpecialForging.SpecialCraft;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeEntry;

/** Registry-backed special outputs for NEI, independent of client classes and item design interfaces. */
public final class AnvilSpecialRecipes {

    private AnvilSpecialRecipes() {}

    public static final class Result {

        public final String design;
        private final ItemStack output;

        private Result(String design, ItemStack output) {
            this.design = design;
            this.output = output;
        }

        public ItemStack getOutput() {
            return output.copy();
        }

        public boolean matches(ItemStack wanted) {
            return CustomToolHelper.areEqual(output, wanted);
        }
    }

    /** One effective transformation per design, in the published registry's lookup order. */
    public static List<Result> results(ItemStack base) {
        List<Result> results = new ArrayList<Result>();
        if (base == null || base.getItem() == null) {
            return results;
        }
        Set<String> designs = new HashSet<String>();
        for (RecipeEntry<SpecialCraft> entry : MFRecipes.SPECIAL_FORGING.published()
                .candidates(Input.lookupKeys(base))) {
            SpecialCraft craft = entry.getRecipe();
            if (craft.getBase() != base.getItem() || !designs.add(craft.getDesign())) {
                continue;
            }
            ItemStack output = new ItemStack(craft.getOutput(), base.stackSize, base.getItemDamage());
            if (base.hasTagCompound()) {
                output.setTagCompound((net.minecraft.nbt.NBTTagCompound) base.getTagCompound().copy());
            }
            results.add(new Result(craft.getDesign(), output));
        }
        return results;
    }
}
