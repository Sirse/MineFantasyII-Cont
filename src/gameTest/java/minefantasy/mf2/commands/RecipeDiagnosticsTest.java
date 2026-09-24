package minefantasy.mf2.commands;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.mojang.authlib.GameProfile;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.ProcessRecipe;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.block.tileentity.Stations;
import minefantasy.mf2.block.tileentity.TileEntityQuern;

/**
 * {@code /mf recipes}: what it tells a pack maker about a station's lookup, checked by message keys and arguments so
 * the server's language does not matter.
 */
@GameTestHolder("minefantasy2")
public class RecipeDiagnosticsTest {

    private RecipeDiagnosticsTest() {}

    /** A player who keeps the chat messages sent to it. */
    private static final class Listener extends FakePlayer {

        final List<IChatComponent> messages = new ArrayList<>();

        Listener(WorldServer world) {
            super(world, new GameProfile(UUID.randomUUID(), "mf2_diagnostics"));
        }

        @Override
        public void addChatMessage(IChatComponent message) {
            messages.add(message);
        }
    }

    private static Listener listener(GameTestHelper helper, ItemStack held) {
        Listener player = new Listener(helper.getWorld());
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, held);
        return player;
    }

    private static String key(IChatComponent message) {
        assertInstanceOf(message);
        return ((ChatComponentTranslation) message).getKey();
    }

    private static void assertInstanceOf(IChatComponent message) {
        assertTrue("not a translated message: " + message, message instanceof ChatComponentTranslation);
    }

    private static Object[] args(IChatComponent message) {
        assertInstanceOf(message);
        return ((ChatComponentTranslation) message).getFormatArgs();
    }

    /** The key of the verdict after a candidate line's "#n id [priority] ". */
    private static String verdict(IChatComponent line) {
        return key((IChatComponent) line.getSiblings().get(0));
    }

    /** Two quern recipes for seeds: the higher priority one wins, the other is shadowed. */
    private static RecipeId[] twoQuernRecipes(GameTestHelper helper) {
        RecipeId[] ids = new RecipeId[2];
        Stations.reload(tx -> {
            ids[0] = RecipeId.of("crafttweaker", "quern/diag_first." + Integer.toHexString(helper.hashCode()));
            ids[1] = RecipeId.of("crafttweaker", "quern/diag_second." + Integer.toHexString(helper.hashCode()));
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
            Listener player = listener(helper, new ItemStack(seed));
            RecipeDiagnostics.run(player, "quern");

            assertEquals("a header and a line per candidate", 3, player.messages.size());
            assertEquals("command.mf.recipes.header", key(player.messages.get(0)));
            assertEquals("quern", args(player.messages.get(0))[0]);
            assertEquals(2, args(player.messages.get(0))[1]);

            String first = player.messages.get(1).getUnformattedTextForChat();
            assertTrue(first, first.startsWith("#1 " + ids[0] + " [10]"));
            assertEquals("command.mf.recipes.crafts", verdict(player.messages.get(1)));
            String second = player.messages.get(2).getUnformattedTextForChat();
            assertTrue(second, second.startsWith("#2 " + ids[1] + " [5]"));
            assertEquals(CheckResult.Reason.of("shadowed").getTranslationKey(), verdict(player.messages.get(2)));
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
                            RecipeId.of("crafttweaker", "quern/diag_four." + Integer.toHexString(helper.hashCode())),
                            ProcessRecipe.of(Input.of(ore).amount(4), new ItemStack(bar)),
                            0));
            Listener player = listener(helper, new ItemStack(ore, 2));
            RecipeDiagnostics.run(player, "quern");
            assertEquals(2, player.messages.size());
            IChatComponent reason = (IChatComponent) player.messages.get(1).getSiblings().get(0);
            assertEquals(CheckResult.Reason.of("amount", 2, 4).getTranslationKey(), key(reason));
            assertEquals(2, args(reason)[0]);
            assertEquals(4, args(reason)[1]);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }

    @GameTest
    public static void wrongUseIsAnsweredNotIgnored(GameTestHelper helper) throws Exception {
        Listener empty = listener(helper, null);
        RecipeDiagnostics.run(empty, "quern");
        assertEquals("command.mf.recipes.no_item", key(empty.messages.get(0)));

        Listener unknown = listener(helper, new ItemStack(seed));
        RecipeDiagnostics.run(unknown, "no_such_station");
        assertEquals("command.mf.recipes.unknown_station", key(unknown.messages.get(0)));
        assertEquals("no_such_station", args(unknown.messages.get(0))[0]);

        Listener lookingAtSky = listener(helper, null);
        TestPos above = helper.absolute(1, 20, 1);
        lookingAtSky.setPositionAndRotation(above.x() + 0.5D, above.y(), above.z() + 0.5D, 0F, -90F);
        RecipeDiagnostics.run(lookingAtSky, null);
        assertEquals("command.mf.recipes.no_station", key(lookingAtSky.messages.get(0)));
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
            Listener player = listener(helper, null);
            // Standing over the quern, looking straight down at it
            TestPos over = helper.absolute(1, 3, 1);
            player.setPositionAndRotation(over.x() + 0.5D, over.y(), over.z() + 0.5D, 0F, 90F);
            RecipeDiagnostics.run(player, null);
            assertEquals("command.mf.recipes.header", key(player.messages.get(0)));
            assertEquals("quern", args(player.messages.get(0))[0]);
        } finally {
            Stations.end();
        }
        helper.succeed();
    }
}
