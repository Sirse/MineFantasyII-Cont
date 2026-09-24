package minefantasy.mf2.api.recipe;

import static minefantasy.mf2.gametest.Assert.*;
import static minefantasy.mf2.gametest.TestItems.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

@GameTestHolder("minefantasy2")
public class RecipeRegistryTest {

    /** Test recipe: an input key and an output name. */
    private static final class Rec {

        final String input;
        final String output;

        Rec(String input, String output) {
            this.input = input;
            this.output = output;
        }
    }

    private static RecipeRegistries registries;
    private static RecipeRegistry<Rec> bloomery;
    private static RecipeRegistry<Rec> salvage;

    private static RecipeId id(String path) {
        return RecipeId.of("test", path);
    }

    private static List<String> ids(List<? extends RecipeEntry<?>> entries) {
        return entries.stream().map(e -> e.getId().getPath()).collect(Collectors.toList());
    }

    private static void setUp() {
        registries = new RecipeRegistries();
        bloomery = registries.create("bloomery", r -> r.input == null ? null : Collections.singleton(r.input));
        salvage = registries.create("salvage", r -> Collections.singleton(r.input));
    }

    // region ids and operations

    @GameTest
    public static void recipeIdParsesAndValidates(GameTestHelper helper) throws Exception {
        setUp();
        try {
            RecipeId parsed = RecipeId.parse("minefantasy2:bloomery/iron_ore");
            assertEquals("minefantasy2", parsed.getNamespace());
            assertEquals("bloomery", parsed.getStation());
            assertEquals(RecipeId.of("minefantasy2", "bloomery/iron_ore"), parsed);
            for (String bad : new String[] { "bloomery/iron", "mf:iron", "MF:bloomery/iron", "mf:bloomery/Iron" }) {
                try {
                    RecipeId.parse(bad);
                    fail("accepted " + bad);
                } catch (IllegalArgumentException expected) {}
            }
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void addRejectsDuplicateId(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(RecipeRegistrationException.class, () -> {
                bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
                bloomery.add(id("bloomery/a"), new Rec("ore", "bar2"), RecipeSource.NATIVE);
            });
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void replaceRejectsMissingId(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(RecipeRegistrationException.class, () -> {
                bloomery.replace(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE, 0);
            });
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void removeRejectsMissingId(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(RecipeRegistrationException.class, () -> { bloomery.remove(id("bloomery/a")); });
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void idMustNameTheStation(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(RecipeRegistrationException.class, () -> {
                bloomery.add(id("quern/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            });
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void removeWhereReportsAffectedIds(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/b"), new Rec("sand", "glass"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/c"), new Rec("ore2", "bar"), RecipeSource.NATIVE);
            List<RecipeId> removed = bloomery.removeWhere(e -> e.getRecipe().output.equals("bar"));
            assertEquals(Arrays.asList(id("bloomery/a"), id("bloomery/c")), removed);
            registries.finishLoading();
            assertEquals(Collections.singletonList("bloomery/b"), ids(bloomery.published().all()));
        } finally {

        }
        helper.succeed();
    }

    // endregion

    // region transactions

    @GameTest
    public static void failedPartDiscardsWholeTransaction(GameTestHelper helper) throws Exception {
        setUp();
        try {
            salvage.add(id("salvage/sword"), new Rec("sword", "bar"), RecipeSource.NATIVE);
            try (RecipeTransaction tx = registries.begin(RecipeSource.script(""))) {
                tx.add(bloomery, id("bloomery/sword"), new Rec("hunk", "sword"), 0);
                tx.add(salvage, id("salvage/sword"), new Rec("sword", "hunk"), 0); // duplicate
                tx.commit();
                fail("duplicate accepted");
            } catch (RecipeRegistrationException expected) {}
            registries.finishLoading();
            assertFalse(bloomery.published().contains(id("bloomery/sword")));
            assertEquals("bar", salvage.published().get(id("salvage/sword")).getRecipe().output);
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void uncommittedTransactionChangesNothing(GameTestHelper helper) throws Exception {
        setUp();
        try {
            try (RecipeTransaction tx = registries.begin(RecipeSource.NATIVE)) {
                tx.add(bloomery, id("bloomery/a"), new Rec("ore", "bar"), 0);
            }
            registries.finishLoading();
            assertEquals(0, bloomery.published().size());
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void transactionSeesItsOwnOperations(GameTestHelper helper) throws Exception {
        setUp();
        try {
            try (RecipeTransaction tx = registries.begin(RecipeSource.NATIVE)) {
                tx.add(bloomery, id("bloomery/a"), new Rec("ore", "bar"), 0);
                tx.replace(bloomery, id("bloomery/a"), new Rec("ore", "bar2"), 0);
                tx.remove(bloomery, id("bloomery/a"));
                tx.add(bloomery, id("bloomery/a"), new Rec("ore", "bar3"), 0);
                tx.commit();
            }
            registries.finishLoading();
            assertEquals("bar3", bloomery.published().get(id("bloomery/a")).getRecipe().output);
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void writesOutsideLoadingOrReloadAreRejected(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(RecipeRegistrationException.class, () -> {
                registries.finishLoading();
                bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            });
        } finally {

        }
        helper.succeed();
    }

    // endregion

    // region order and lookup

    @GameTest
    public static void higherPriorityFirstThenRegistrationOrder(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/general"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/special"), new Rec("ore", "good_bar"), RecipeSource.NATIVE, 10);
            bloomery.add(id("bloomery/late"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            registries.finishLoading();
            assertEquals(
                    Arrays.asList("bloomery/special", "bloomery/general", "bloomery/late"),
                    ids(bloomery.published().candidates(Collections.singleton("ore"))));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void replaceKeepsPosition(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/b"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            bloomery.replace(id("bloomery/a"), new Rec("ore", "better"), RecipeSource.NATIVE, 0);
            registries.finishLoading();
            assertEquals(Arrays.asList("bloomery/a", "bloomery/b"), ids(bloomery.published().all()));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void candidatesMergeKeysWithoutDuplicatesAndKeepUnindexed(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/b"), new Rec("sand", "glass"), RecipeSource.NATIVE);
            bloomery.add(id("bloomery/any"), new Rec(null, "slag"), RecipeSource.NATIVE);
            registries.finishLoading();
            RecipeSnapshot<Rec> snapshot = bloomery.published();
            assertEquals(
                    Arrays.asList("bloomery/a", "bloomery/b", "bloomery/any"),
                    ids(snapshot.candidates(Arrays.asList("ore", "sand", "ore"))));
            assertEquals(
                    Collections.singletonList("bloomery/any"),
                    ids(snapshot.candidates(Collections.singleton("x"))));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void sameStateGivesSameOrder(GameTestHelper helper) throws Exception {
        setUp();
        try {
            for (int i = 0; i < 20; i++) {
                bloomery.add(id("bloomery/r" + i), new Rec("ore", "bar" + i), RecipeSource.NATIVE, i % 3);
            }
            registries.finishLoading();
            List<String> first = ids(bloomery.published().candidates(Collections.singleton("ore")));
            registries.beginReload();
            registries.publish();
            assertEquals(first, ids(bloomery.published().candidates(Collections.singleton("ore"))));
        } finally {

        }
        helper.succeed();
    }

    // endregion

    // region publication and reload

    @GameTest
    public static void draftInvisibleUntilPublished(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            registries.finishLoading();
            RecipeSnapshot<Rec> before = bloomery.published();

            registries.beginReload();
            bloomery.add(id("bloomery/script"), new Rec("ore", "gold"), RecipeSource.script(""), 5);
            bloomery.remove(id("bloomery/a"));
            assertSame(before, bloomery.published());
            assertTrue(bloomery.published().contains(id("bloomery/a")));

            registries.publish();
            assertEquals(Collections.singletonList("bloomery/script"), ids(bloomery.published().all()));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void publicationSwapsAllStationsWithOneGeneration(GameTestHelper helper) throws Exception {
        setUp();
        try {
            registries.finishLoading();
            long generation = registries.getGeneration();
            registries.beginReload();
            bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.script(""));
            salvage.add(id("salvage/a"), new Rec("bar", "ore"), RecipeSource.script(""));
            registries.publish();
            assertEquals(generation + 1, registries.getGeneration());
            assertEquals(registries.getGeneration(), bloomery.published().getGeneration());
            assertEquals(registries.getGeneration(), salvage.published().getGeneration());
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void repeatedReloadDoesNotDuplicate(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/native"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            registries.finishLoading();
            for (int i = 0; i < 3; i++) {
                registries.beginReload();
                bloomery.add(id("bloomery/script"), new Rec("ore", "gold"), RecipeSource.script(""));
                registries.publish();
            }
            assertEquals(Arrays.asList("bloomery/native", "bloomery/script"), ids(bloomery.published().all()));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void reloadStartsFromBaseSoRemovedNativeRecipesReturn(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/native"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            registries.finishLoading();
            registries.beginReload();
            bloomery.remove(id("bloomery/native"));
            registries.publish();
            assertNull(bloomery.published().get(id("bloomery/native")));
            assertTrue(bloomery.published().candidates(Collections.singleton("ore")).isEmpty());

            registries.beginReload();
            registries.publish();
            assertNotNull(bloomery.published().get(id("bloomery/native")));
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void abortedReloadKeepsPublishedSet(GameTestHelper helper) throws Exception {
        setUp();
        try {
            bloomery.add(id("bloomery/native"), new Rec("ore", "bar"), RecipeSource.NATIVE);
            registries.finishLoading();
            RecipeSnapshot<Rec> before = bloomery.published();
            registries.beginReload();
            bloomery.remove(id("bloomery/native"));
            registries.beginReload(); // the previous reload never published
            registries.abortReload();
            assertSame(before, bloomery.published());
        } finally {

        }
        helper.succeed();
    }

    @GameTest
    public static void publishedListsAreReadOnly(GameTestHelper helper) throws Exception {
        setUp();
        try {
            assertThrows(UnsupportedOperationException.class, () -> {
                bloomery.add(id("bloomery/a"), new Rec("ore", "bar"), RecipeSource.NATIVE);
                registries.finishLoading();
                bloomery.published().all().clear();
            });
        } finally {

        }
        helper.succeed();
    }

    // endregion

    @GameTest
    public static void setSeesTheTransactionsOwnOperations(GameTestHelper helper) throws Exception {
        setUp();
        bloomery.add(id("bloomery/old"), new Rec("ore", "bar"), RecipeSource.NATIVE);
        try (RecipeTransaction tx = registries.begin(RecipeSource.NATIVE)) {
            // A second set of a new id replaces the first
            tx.set(bloomery, id("bloomery/new"), new Rec("ore", "first"), 0);
            tx.set(bloomery, id("bloomery/new"), new Rec("ore", "second"), 0);
            // A set after a removal adds the id again
            tx.remove(bloomery, id("bloomery/old"));
            tx.set(bloomery, id("bloomery/old"), new Rec("ore", "again"), 0);
            tx.commit();
        }
        registries.finishLoading();
        assertEquals("second", bloomery.published().get(id("bloomery/new")).getRecipe().output);
        assertEquals("again", bloomery.published().get(id("bloomery/old")).getRecipe().output);
        helper.succeed();
    }

    // region metadata

    @GameTest
    public static void metadataValidatesTypeAndValue(GameTestHelper helper) throws Exception {
        setUp();
        try {
            RecipeMetadataKey<Integer> tier = RecipeMetadataKey.create("test:tier", Integer.class, v -> v >= 0, "tier");
            RecipeMetadata meta = RecipeMetadata.builder().put(tier, 2).build();
            assertEquals(Integer.valueOf(2), meta.get(tier));
            try {
                RecipeMetadata.builder().put(tier, -1);
                fail("negative tier accepted");
            } catch (IllegalArgumentException expected) {}
            try {
                RecipeMetadataKey.create("test:tier", Integer.class, "tier");
                fail("duplicate key accepted");
            } catch (IllegalArgumentException expected) {}
        } finally {

        }
        helper.succeed();
    }

    // endregion
}
