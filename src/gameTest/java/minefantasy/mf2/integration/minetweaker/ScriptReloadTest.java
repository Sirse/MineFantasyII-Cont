package minefantasy.mf2.integration.minetweaker;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.lang.reflect.Field;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipeKeys;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeId;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.runtime.IScriptProvider;
import minetweaker.runtime.providers.ScriptProviderCustom;

/**
 * A script run through CraftTweaker's own reload: the script is compiled, calls the MineFantasy tweakers, and the
 * reload publishes what it registered. The server's scripts come back afterwards.
 */
@GameTestHolder(value = "minefantasy2", requiredMods = "MineTweaker3")
public class ScriptReloadTest {

    private static final RecipeId HIDE = RecipeId.of("crafttweaker", "tanning/test_hide");

    private ScriptReloadTest() {}

    private static IScriptProvider currentScripts() throws Exception {
        Field provider = MineTweakerAPI.tweaker.getClass().getDeclaredField("scriptProvider");
        provider.setAccessible(true);
        return (IScriptProvider) provider.get(MineTweakerAPI.tweaker);
    }

    private static void reloadWith(IScriptProvider scripts) {
        MineTweakerImplementationAPI.setScriptProvider(scripts);
        MineTweakerImplementationAPI.reload();
    }

    @GameTest
    public static void scriptRecipeArrivesWithTheReloadAndLeavesWithTheNext(GameTestHelper helper) throws Exception {
        IScriptProvider serverScripts = currentScripts();
        try {
            String name = net.minecraft.item.Item.itemRegistry.getNameForObject(flour);
            assertNotNull(
                    "CraftTweaker does not know " + name,
                    minetweaker.mc1710.brackets.ItemBracketHandler.getItem(name, 0));
            ScriptProviderCustom scripts = new ScriptProviderCustom("minefantasy2tests");
            scripts.add(
                    "tanning.zs",
                    "mods.minefantasy.TanningRack.add(\"test_hide\", <minefantasy2tests:flour> * 3,"
                            + " <minefantasy2tests:seed> * 2, 5.0, 0, \"shears\");");
            reloadWith(scripts);

            RecipeEntry<ProcessRecipe> hide = MFRecipes.TANNING.published().get(HIDE);
            assertNotNull("the script's recipe was not published", hide);
            ProcessRecipe recipe = hide.getRecipe();
            assertEquals("the script's amount", 2, recipe.getInput().getAmount());
            assertEquals(flour, recipe.getOutput().getItem());
            assertEquals(3, recipe.getOutput().stackSize);
            assertEquals(5F, recipe.get(MFRecipeKeys.TIME, 0F), 0F);
            assertEquals("shears", recipe.get(MFRecipeKeys.TOOL, ""));

            reloadWith(serverScripts);
            assertNull("the recipe outlived its script", MFRecipes.TANNING.published().get(HIDE));
        } finally {
            reloadWith(serverScripts);
            // Other tests' recipes were dropped by the reloads above: publish them again
            minefantasy.mf2.block.tileentity.Stations.publish();
        }
        helper.succeed();
    }
}
