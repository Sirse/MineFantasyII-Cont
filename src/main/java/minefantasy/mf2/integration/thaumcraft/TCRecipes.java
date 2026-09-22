package minefantasy.mf2.integration.thaumcraft;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import minefantasy.mf2.item.list.CustomArmourListMF;
import minefantasy.mf2.util.MFLogUtil;
import thaumcraft.api.ItemApi;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** The shared infusion recipes, registered once per launch. */
public class TCRecipes {

    public static final String MATERIAL_THAUMIUM = "Thaumium";

    private static MFInfusionRecipe revealingHelmet;

    public static MFInfusionRecipe getRevealingHelmet() {
        return revealingHelmet;
    }

    public static void init() {
        if (revealingHelmet != null) {
            return;
        }

        ItemStack thaumiumIngot = ItemApi.getItem("itemResource", 2);
        if (thaumiumIngot == null) {
            MFLogUtil.logWarn("Thaumcraft did not supply a thaumium ingot; the MF infusion recipes are not registered");
            return;
        }

        // The display stack is a plate helmet, but any forged helmet of the material is accepted; the recipe decides
        // that itself in matches(), not through this template.
        ItemStack display = CustomArmourListMF.standard_plate_helmet.construct(MATERIAL_THAUMIUM);

        AspectList aspects = new AspectList().add(Aspect.SENSES, 32).add(Aspect.MAGIC, 16).add(Aspect.AURA, 8);
        ItemStack[] components = new ItemStack[] { thaumiumIngot.copy(), thaumiumIngot.copy(),
                new ItemStack(Blocks.glass_pane), new ItemStack(Blocks.glass_pane) };

        revealingHelmet = new MFInfusionRecipe(
                TCResearch.RESEARCH_REVEALING,
                display,
                2,
                aspects,
                MATERIAL_THAUMIUM,
                MFTCData.EFFECT_REVEALING,
                components);

        // Registered by hand: addInfusionCraftingRecipe would build a plain InfusionRecipe and the dynamic result
        // would be lost.
        ThaumcraftApi.getCraftingRecipes().add(revealingHelmet);
        MFLogUtil.log("Thaumcraft infusion registered: revealing sight on a forged thaumium helmet");
    }
}
