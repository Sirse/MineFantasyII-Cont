package minefantasy.mf2.commands;

import static minefantasy.mf2.commands.CommandPlayer.*;
import static minefantasy.mf2.gametest.Assert.*;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.item.list.ComponentListMF;

/** /mf zs and the removal lines of /mf recipes, as an operator runs them. */
@GameTestHolder("minefantasy2")
public class ScriptCommandsTest {

    private ScriptCommandsTest() {}

    @GameTest
    public static void zsShowsTheHeldItemAsAResultAndAnIngredient(GameTestHelper helper) {
        List<IChatComponent> sent = CommandPlayer.operator(helper, ComponentListMF.bar("Steel", 2)).run("mf zs");
        assertEquals("a result and an ingredient: " + sent, 2, sent.size());
        assertEquals("command.mf.zs.output", key(sent.get(0)));
        assertTrue(
                sent.get(0).getUnformattedText(),
                sent.get(0).getUnformattedText()
                        .contains("mods.minefantasy.MF.stack(<minefantasy2:custom_bar>, \"steel\") * 2"));
        assertTrue(
                sent.get(1).getUnformattedText(),
                sent.get(1).getUnformattedText()
                        .contains("mods.minefantasy.MF.input(<minefantasy2:custom_bar>, \"steel\") * 2"));
        helper.succeed();
    }

    /** A candidate line puts the script line removing it in the chat box. */
    @GameTest
    public static void aCandidateOffersItsRemovalLine(GameTestHelper helper) {
        RecipeEntry<ProcessRecipe> entry = MFRecipes.QUERN.published().all().get(0);
        ItemStack held = entry.getRecipe().getInput().examples().get(0).copy();
        held.stackSize = entry.getRecipe().getInput().getAmount();
        List<IChatComponent> list = CommandPlayer.operator(helper, held).run("mf recipes quern");
        IChatComponent line = null;
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i).getUnformattedText().contains(entry.getId().toString())) {
                line = list.get(i);
            }
        }
        assertNotNull("the recipe is not listed for its own input: " + list, line);
        String remove = "mods.minefantasy.Quern.remove(\"" + entry.getId() + "\");";
        assertEquals(remove, line.getChatStyle().getChatHoverEvent().getValue().getUnformattedText());
        if (remove.length() <= RecipesCommand.CHAT_LIMIT) {
            assertEquals(remove, line.getChatStyle().getChatClickEvent().getValue());
        }
        helper.succeed();
    }
}
