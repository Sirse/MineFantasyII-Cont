package minefantasy.mf2.integration.minetweaker;

import cpw.mods.fml.common.Optional;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptRecipes;
import minefantasy.mf2.integration.minetweaker.tweakers.*;
import minetweaker.MineTweakerAPI;
import minetweaker.api.item.IIngredient;

public class MTCompat {

    private static final String[] COMMAND_DESC = { "MineFantasy commands:", "   /minetweaker mf materials",
            "   Lists all MF materials", "   /minetweaker mf skills", "   Lists all MF skills" };

    public static void loadTweakers() {
        MineTweakerAPI.registerClass(Anvil.class);
        MineTweakerAPI.registerClass(Bloomery.class);
        MineTweakerAPI.registerClass(BigFurnace.class);
        MineTweakerAPI.registerClass(BlastFurnace.class);
        MineTweakerAPI.registerClass(CarpentersBench.class);
        MineTweakerAPI.registerClass(KitchenBench.class);
        MineTweakerAPI.registerClass(Cooking.class);
        MineTweakerAPI.registerClass(Crucible.class);
        MineTweakerAPI.registerClass(Forge.class);
        MineTweakerAPI.registerClass(Fuels.class);
        MineTweakerAPI.registerClass(PaintOil.class);
        MineTweakerAPI.registerClass(SpecialForging.class);
        MineTweakerAPI.registerClass(TanningRack.class);
        MineTweakerAPI.registerClass(Quern.class);
        MineTweakerAPI.registerClass(SalvageTweaker.class);
        MineTweakerAPI.registerRemover(new MFRecipeRemover());
        ScriptRecipes.hookReloads();
    }

    public static void registerCommands() {
        MineTweakerAPI.server.addMineTweakerCommand("mf", COMMAND_DESC, new MTCommands());
    }

    @Optional.Interface(iface = "minetweaker.IRecipeRemover", modid = "MineTweaker3")
    public static class MFRecipeRemover implements minetweaker.IRecipeRemover {

        @Optional.Method(modid = "MineTweaker3")
        @Override
        public void remove(IIngredient iIngredient) {
            Anvil.removeByOutput(iIngredient, null);
            CarpentersBench.removeByOutput(iIngredient, null);
            BigFurnace.removeByOutput(iIngredient, null);
            Cooking.removeByOutput(iIngredient, null);
            Crucible.removeByOutput(iIngredient, null);
            SalvageTweaker.removeByPart(iIngredient, null);
            Bloomery.removeByOutput(iIngredient, null);
            Quern.removeByOutput(iIngredient, null);
            TanningRack.removeByOutput(iIngredient, null);
        }
    }
}
