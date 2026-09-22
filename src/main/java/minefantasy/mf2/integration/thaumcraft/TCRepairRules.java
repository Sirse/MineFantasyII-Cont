package minefantasy.mf2.integration.thaumcraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.material.CustomMaterial;

/** Whether an infusion may travel through a repair. {@link MFTCData} only reads and writes the tag. */
public class TCRepairRules {

    /**
     * Keeps the one effect of the pair, whichever piece held it. Refuses the repair on two different effects, and when
     * the result is not of the donor material: a repair pairs two stacks of the same Item and MF keeps the material in
     * NBT, so otherwise an infusion would move onto a material the recipe refuses. A refusal yields no result, so
     * nothing is consumed.
     */
    public static boolean mergeForRepair(ItemStack result, ItemStack first, ItemStack second) {
        String firstEffect = MFTCData.getEffect(first);
        String secondEffect = MFTCData.getEffect(second);

        if (firstEffect != null && secondEffect != null && !firstEffect.equals(secondEffect)) {
            return false;
        }
        ItemStack donor = firstEffect != null ? first : secondEffect != null ? second : null;
        if (donor == null) {
            return true;
        }
        if (!sameMaterial(result, donor)) {
            return false;
        }
        NBTTagCompound tag = MFTCData.getTag(donor);
        if (tag == null) {
            return true;
        }
        if (!result.hasTagCompound()) {
            result.setTagCompound(new NBTTagCompound());
        }
        result.getTagCompound().setTag(MFTCData.TAG, (NBTTagCompound) tag.copy());
        return true;
    }

    /** Two pieces with no material at all count as matching. */
    private static boolean sameMaterial(ItemStack result, ItemStack donor) {
        CustomMaterial resultMaterial = CustomMaterial.getMaterialFor(result, CustomToolHelper.slot_main);
        CustomMaterial donorMaterial = CustomMaterial.getMaterialFor(donor, CustomToolHelper.slot_main);
        if (resultMaterial == null || donorMaterial == null) {
            return resultMaterial == donorMaterial;
        }
        return resultMaterial.name.equalsIgnoreCase(donorMaterial.name);
    }
}
