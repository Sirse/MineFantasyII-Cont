package minefantasy.mf2.integration.thaumcraft;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import minefantasy.mf2.item.list.CustomArmourListMF;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

/**
 * The MF category in the Thaumonomicon. Keys are stable: a save stores what a player unlocked, so renaming one takes
 * the research away.
 */
public class TCResearch {

    public static final String CATEGORY = "MF2";
    public static final String RESEARCH_FORGING = "MF2_THAUMIC_FORGING";
    public static final String RESEARCH_REVEALING = "MF2_REVEALING_HELM";

    /** Keys owned by Thaumcraft, confirmed present in 4.2.3.5. */
    private static final String TC_RESEARCH_GOGGLES = "GOGGLES";
    private static final String TC_RESEARCH_INFUSION = "INFUSION";

    private static boolean registered;

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;

        ResearchCategories.registerCategory(
                CATEGORY,
                new ResourceLocation(
                        "minefantasy2",
                        "textures/items/custom/apparel/standard/standard_plate_helmet.png"),
                new ResourceLocation("thaumcraft", "textures/gui/gui_researchback.png"));

        ItemStack thaumiumHelmet = CustomArmourListMF.standard_plate_helmet.construct(TCRecipes.MATERIAL_THAUMIUM);

        // The introduction is free: it explains that MF forging comes first and magic second, which a player needs to
        // read before the category makes any sense.
        new ResearchItem(
                RESEARCH_FORGING,
                CATEGORY,
                new AspectList().add(Aspect.METAL, 1).add(Aspect.MAGIC, 1),
                0,
                0,
                0,
                thaumiumHelmet).setPages(new ResearchPage("tc.research_page.MF2_THAUMIC_FORGING.1")).setRound()
                .setAutoUnlock().registerResearchItem();

        ResearchItem revealing = new ResearchItem(
                RESEARCH_REVEALING,
                CATEGORY,
                new AspectList().add(Aspect.SENSES, 8).add(Aspect.MAGIC, 4).add(Aspect.AURA, 2),
                2,
                0,
                1,
                thaumiumHelmet).setParents(RESEARCH_FORGING)
                // The goggles and infusion research of Thaumcraft itself: a borrowed matrix must not hand a
                // player an effect they never studied. Hidden because they live in another category.
                .setParentsHidden(TC_RESEARCH_GOGGLES, TC_RESEARCH_INFUSION)
                .setPages(new ResearchPage("tc.research_page.MF2_REVEALING_HELM.1"));

        // Only show the recipe page when the recipe actually exists, so a missing thaumium ingot leaves a readable
        // research entry instead of a broken page.
        if (TCRecipes.getRevealingHelmet() != null) {
            revealing.setPages(
                    new ResearchPage("tc.research_page.MF2_REVEALING_HELM.1"),
                    new ResearchPage(TCRecipes.getRevealingHelmet()));
        }
        revealing.registerResearchItem();
    }
}
