package minefantasy.mf2.integration.thaumcraft;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.item.armour.ItemCustomArmour;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;

/**
 * An infusion whose central item is a forged MF helmet rather than a fixed stack. The result is the helmet the smith
 * made: material, quality, name, enchantments and damage all survive.
 * <p>
 * {@link #matches} is written out because the superclass compares the central item by NBT and every MF helmet keeps its
 * material there, so a template could never match. The research and component checks are therefore reproduced here.
 */
public class MFInfusionRecipe extends InfusionRecipe {

    private final String material;
    private final String effect;

    public MFInfusionRecipe(String research, ItemStack displayInput, int instability, AspectList aspects,
            String material, String effect, ItemStack[] components) {
        super(research, displayInput.copy(), instability, aspects, displayInput.copy(), components);
        this.material = material;
        this.effect = effect;
    }

    @Override
    public boolean matches(ArrayList<ItemStack> input, ItemStack central, World world, EntityPlayer player) {
        if (!isValidBase(central)) {
            return false;
        }
        if (getResearch().length() > 0
                && !ThaumcraftApiHelper.isResearchComplete(player.getCommandSenderName(), getResearch())) {
            return false;
        }
        return componentsPresent(input);
    }

    /** A forged helmet of the right material that carries no effect yet. */
    private boolean isValidBase(ItemStack central) {
        if (central == null || !(central.getItem() instanceof ItemCustomArmour)) {
            return false;
        }
        ItemCustomArmour armour = (ItemCustomArmour) central.getItem();
        if (armour.armorType != 0) {
            return false;
        }
        if (MFTCData.hasAnyEffect(central)) {
            return false;
        }
        CustomMaterial mat = armour.getCustomMaterial(central);
        return mat != null && mat.name.equalsIgnoreCase(material);
    }

    /** Every component must be on the pedestals, counted with multiplicity. */
    private boolean componentsPresent(ArrayList<ItemStack> input) {
        ArrayList<ItemStack> remaining = new ArrayList<ItemStack>();
        for (ItemStack offered : input) {
            if (offered != null) {
                remaining.add(offered.copy());
            }
        }
        for (ItemStack component : getComponents()) {
            boolean found = false;
            for (int i = 0; i < remaining.size(); i++) {
                if (areItemStacksEqual(remaining.get(i), component, false)) {
                    remaining.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    /**
     * Never call the inherited no-argument getRecipeOutput() here: it runs getRecipeOutput(getRecipeInput()) and would
     * come straight back with the template, handing out an infused helmet for a rejected input.
     */
    @Override
    public Object getRecipeOutput(ItemStack input) {
        if (!isValidBase(input)) {
            return recipeOutput instanceof ItemStack ? ((ItemStack) recipeOutput).copy() : recipeOutput;
        }
        return MFTCData.withEffect(input, effect);
    }
}
