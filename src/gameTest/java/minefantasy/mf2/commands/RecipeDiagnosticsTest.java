package minefantasy.mf2.commands;

import static minefantasy.mf2.commands.CommandPlayer.*;
import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.cooking.CookRecipe;
import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.Stations;
import minefantasy.mf2.block.tileentity.TileEntityQuern;

/**
 * {@code /mf recipes} as the server runs it: what it tells a pack maker about a station's lookup, checked by message
 * keys and arguments so the server's language does not matter.
 */
@GameTestHolder("minefantasy2")
public class RecipeDiagnosticsTest {

    private RecipeDiagnosticsTest() {}

    /** The key of the verdict after a candidate line's "#n id [priority] ". */
    private static String verdict(IChatComponent line) {
        return key((IChatComponent) line.getSiblings().get(0));
    }

    private static RecipeId id(GameTestHelper helper, String path) {
        return RecipeId.of("crafttweaker", path + "." + Integer.toHexString(helper.hashCode()));
    }

    /** Two quern recipes for seeds: the higher priority one wins, the other is shadowed. */
    private static RecipeId[] twoQuernRecipes(GameTestHelper helper) {
        RecipeId[] ids = { id(helper, "quern/diag_first"), id(helper, "quern/diag_second") };
        Stations.reload(tx -> {
            tx.add(MFRecipes.QUERN, ids[0], ProcessRecipe.of(Input.of(seed), new ItemStack(flour)), 10);
            tx.add(MFRecipes.QUERN, ids[1], ProcessRecipe.of(Input.of(seed), new ItemStack(bar)), 5);
        });
        return ids;
    }

    @GameTest
    public static void heldItemListsTheCandidatesInLookupOrder(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            RecipeId[] ids = twoQuernRecipes(helper);
            List<IChatComponent> sent = CommandPlayer.operator(helper, new ItemStack(seed)).run("mf recipes quern");

            assertEquals("a header and a line per candidate", 3, sent.size());
            assertEquals("command.mf.recipes.header", key(sent.get(0)));
            assertEquals("quern", args(sent.get(0))[0]);
            assertEquals(2, args(sent.get(0))[1]);

            String first = sent.get(1).getUnformattedTextForChat();
            assertTrue(first, first.startsWith("#1 " + ids[0] + " [10]"));
            assertEquals("command.mf.recipes.crafts", verdict(sent.get(1)));
            String second = sent.get(2).getUnformattedTextForChat();
            assertTrue(second, second.startsWith("#2 " + ids[1] + " [5]"));
            assertEquals(CheckResult.Reason.of("shadowed").getTranslationKey(), verdict(sent.get(2)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aStackTooSmallIsExplained(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            Stations.reload(
                    tx -> tx.add(
                            MFRecipes.QUERN,
                            id(helper, "quern/diag_four"),
                            ProcessRecipe.of(Input.of(ore).amount(4), new ItemStack(bar)),
                            0));
            List<IChatComponent> sent = CommandPlayer.operator(helper, new ItemStack(ore, 2)).run("mf recipes quern");
            assertEquals(2, sent.size());
            IChatComponent reason = (IChatComponent) sent.get(1).getSiblings().get(0);
            assertEquals(CheckResult.Reason.of("amount", 2, 4).getTranslationKey(), key(reason));
            assertEquals(2, args(reason)[0]);
            assertEquals(4, args(reason)[1]);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void cookingJudgesTheWholeHeldStack(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            Stations.reload(
                    tx -> CookRecipe.builder(Input.of(junk).amount(4), new ItemStack(bar)).temperature(50, 500).time(30)
                            .burnTime(10).canBurn(false).build().addTo(tx, id(helper, "cooking/diag_four"), 0));
            List<IChatComponent> sent = CommandPlayer.operator(helper, new ItemStack(junk, 5)).run("mf recipes spit");
            assertEquals(2, sent.size());
            assertEquals("four held made the recipe look short", "command.mf.recipes.crafts", verdict(sent.get(1)));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void anOvenRecipeDoesNotShadowASpitOne(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            RecipeId oven = id(helper, "cooking/diag_oven");
            RecipeId spit = id(helper, "cooking/diag_spit");
            Stations.reload(tx -> {
                CookRecipe.builder(Input.of(carbon), new ItemStack(bar)).temperature(50, 500).time(30).burnTime(10)
                        .oven().canBurn(false).build().addTo(tx, oven, 10);
                CookRecipe.builder(Input.of(carbon), new ItemStack(flour)).temperature(50, 500).time(30).burnTime(10)
                        .canBurn(false).build().addTo(tx, spit, 0);
            });
            CommandPlayer player = CommandPlayer.operator(helper, new ItemStack(carbon));
            for (String station : new String[] { "spit", "oven" }) {
                List<IChatComponent> sent = player.run("mf recipes " + station);
                assertEquals(station + ": the other context was listed", 2, sent.size());
                assertEquals(station, args(sent.get(0))[0]);
                String line = sent.get(1).getUnformattedTextForChat();
                assertTrue(line, line.startsWith("#1 " + ("spit".equals(station) ? spit : oven) + " "));
                assertEquals(station, "command.mf.recipes.crafts", verdict(sent.get(1)));
            }
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void aLongListComesAPageAtATime(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        int count = RecipesCommand.PAGE_SIZE + 3;
        try {
            Stations.reload(tx -> {
                for (int i = 0; i < count; i++) {
                    tx.add(
                            MFRecipes.QUERN,
                            id(helper, "quern/diag_page_" + i),
                            ProcessRecipe.of(Input.of(seed), new ItemStack(flour)),
                            count - i);
                }
            });
            CommandPlayer player = CommandPlayer.operator(helper, new ItemStack(seed));
            List<IChatComponent> first = player.run("mf recipes quern");
            assertEquals("header, a page and where next", RecipesCommand.PAGE_SIZE + 2, first.size());
            IChatComponent footer = first.get(first.size() - 1);
            assertEquals("command.mf.recipes.page", key(footer));
            assertEquals(1, args(footer)[0]);
            assertEquals(2, args(footer)[1]);
            assertEquals("/mf recipes quern 2", args(footer)[2]);

            List<IChatComponent> second = player.run("mf recipes quern 2");
            assertEquals(3 + 2, second.size());
            String line = second.get(1).getUnformattedTextForChat();
            assertTrue(line, line.startsWith("#" + (RecipesCommand.PAGE_SIZE + 1) + " "));

            assertEquals("command.mf.recipes.no_page", key(player.answer("mf recipes quern 3")));
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void wrongUseIsAnsweredNotIgnored(GameTestHelper helper) throws Exception {
        assertEquals(
                "command.mf.recipes.no_item",
                key(CommandPlayer.operator(helper, null).answer("mf recipes quern")));

        IChatComponent unknown = CommandPlayer.operator(helper, new ItemStack(seed))
                .answer("mf recipes no_such_station");
        assertEquals("command.mf.recipes.unknown_station", key(unknown));
        assertEquals("no_such_station", args(unknown)[0]);

        CommandPlayer player = CommandPlayer.operator(helper, new ItemStack(seed));
        assertEquals(RecipesCommand.USAGE, key(player.answer("mf recipes quern 1 2")));
        assertEquals("commands.generic.num.invalid", key(player.answer("mf recipes quern first")));

        CommandPlayer lookingAtSky = CommandPlayer.operator(helper, null);
        TestPos above = helper.absolute(1, 20, 1);
        lookingAtSky.setPositionAndRotation(above.x() + 0.5D, above.y(), above.z() + 0.5D, 0F, -90F);
        assertEquals("command.mf.recipes.no_station", key(lookingAtSky.answer("mf recipes")));

        assertEquals(
                "commands.generic.permission",
                key(CommandPlayer.player(helper, new ItemStack(seed)).answer("mf recipes quern")));
        helper.succeed();
    }

    @GameTest
    public static void lookingAtAStationDiagnosesWhatItHolds(GameTestHelper helper) throws Exception {
        Stations.begin(helper);
        try {
            twoQuernRecipes(helper);
            helper.setBlock(1, 1, 1, BlockListMF.quern);
            TileEntityQuern quern = helper.assertTileEntityPresent(TileEntityQuern.class, 1, 1, 1);
            quern.setInventorySlotContents(0, new ItemStack(seed));
            CommandPlayer player = CommandPlayer.operator(helper, null);
            // Standing over the quern, looking straight down at it
            TestPos over = helper.absolute(1, 3, 1);
            player.setPositionAndRotation(over.x() + 0.5D, over.y(), over.z() + 0.5D, 0F, 90F);
            List<IChatComponent> sent = player.run("mf recipes");
            assertEquals("command.mf.recipes.header", key(sent.get(0)));
            assertEquals("quern", args(sent.get(0))[0]);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
